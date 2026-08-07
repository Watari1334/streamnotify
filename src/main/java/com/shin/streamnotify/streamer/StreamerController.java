package com.shin.streamnotify.streamer;

import com.shin.streamnotify.ratelimit.RateLimitService;
import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.twitch.TwitchEventSubService;
import com.shin.streamnotify.user.CurrentUserResolver;
import com.shin.streamnotify.user.User;
import com.shin.streamnotify.youtube.YouTubeEventSubService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.shin.streamnotify.twitch.TwitchEventSubService.ChannelSearchResult;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 配信者の検索・登録・一覧取得・削除を扱うコントローラ。
 * TwitchとYouTubeの両プラットフォームに対応する。
 * 検索エンドポイントを含め、SecurityConfigによって全メソッドが認証必須。
 */
@RestController
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerRepository streamerRepository;
    private final CurrentUserResolver currentUserResolver;
    private final RegistrationRepository registrationRepository;
    private final TwitchEventSubService twitchEventSubService;
    private final YouTubeEventSubService youTubeEventSubService;
    private final RateLimitService rateLimitService;

    private static final long YOUTUBE_SEARCH_LIMIT_PER_DAY = 20;

    /**
     * ログイン中のユーザーが配信者を登録する。
     * 1ユーザーあたり最大20件まで登録できる。
     * 初めて登録される配信者(誰も登録していない配信者)の場合のみ、
     * TwitchEventSubまたはYouTube PubSubHubbubへの購読を開始する。
     *
     * @param oidcUser 認証済みユーザー情報
     * @param request 登録する配信者の情報(プラットフォーム、チャンネルIDなど)
     * @return 登録結果のメッセージ
     * @throws ChannelLimitExceededException 登録数が20件を超える場合
     */
    @Transactional
    @PostMapping("/streamers/register")
    public String registerStreamer(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestBody StreamerRegistrationRequest request
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);

        long currentCount = registrationRepository.countByUser_UserId(currentUser.getUserId());
        if (currentCount >= 20) {
            throw new ChannelLimitExceededException("登録できるチャンネルは、最大20件までです");
        }

        Optional<Streamer> existingStreamer = streamerRepository
                .findByPlatformAndPlatformChannelId(request.platform(), request.platformChannelId());

        Streamer streamer = existingStreamer.orElseGet(() -> registerNewStreamerAndSubscribe(request));

        Registration registration = new Registration(currentUser, streamer);
        registrationRepository.save(registration);

        return "登録しました: " + streamer.getChannelName();
    }

    /**
     * 新しいStreamerレコードを作成し、プラットフォームに応じた配信検知の購読を開始する。
     * 既に誰かが登録済みの配信者に対しては呼ばれない(registerStreamerで判定済み)。
     *
     * @param request 登録する配信者の情報
     * @return 作成されたStreamerエンティティ
     */
    private Streamer registerNewStreamerAndSubscribe(StreamerRegistrationRequest request) {
        Streamer streamer = streamerRepository.save(
                new Streamer(request.platform(), request.platformChannelId(), request.channelName(), request.channelLogin())
        );

        if ("twitch".equals(request.platform())) {
            String subscriptionId = twitchEventSubService.subscribeToStreamOnline(request.platformChannelId());
            streamer.setTwitchSubscriptionId(subscriptionId);
            streamerRepository.save(streamer);
        } else if ("youtube".equals(request.platform())) {
            youTubeEventSubService.subscribe(request.platformChannelId());
        }

        return streamer;
    }

    /**
     * ログイン中のユーザーが登録している配信者の一覧を返す。
     *
     * @param oidcUser 認証済みユーザー情報
     * @return 登録済み配信者のレスポンス一覧
     */
    @GetMapping("/streamers")
    public List<StreamerResponse> listStreamers(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);

        return registrationRepository.findByUser_UserId(currentUser.getUserId()).stream()
                .map(registration -> new StreamerResponse(
                        registration.getStreamer().getStreamerId(),
                        registration.getStreamer().getPlatform(),
                        registration.getStreamer().getPlatformChannelId(),
                        registration.getStreamer().getChannelName(),
                        registration.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Twitchのチャンネルをキーワードで検索する。
     *
     * @param query 検索キーワード
     * @return 検索結果一覧
     */
    @GetMapping("/streamers/search/twitch")
    public List<ChannelSearchResult> searchStreamers(@RequestParam String query) {
        return twitchEventSubService.searchChannels(query);
    }

    /**
     * YouTubeのチャンネルをキーワードで検索する。
     * クォータ濫用防止のため、1ユーザーあたり1日20回まで。
     *
     * @param oidcUser 認証済みユーザー情報
     * @param query 検索キーワード
     * @return 検索結果一覧
     * @throws SearchLimitExceededException 1日の検索回数上限を超えた場合
     */
    @GetMapping("/streamers/search/youtube")
    public List<YouTubeEventSubService.ChannelSearchResult> searchYouTubeStreamers(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam String query
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);
        String key = "search:youtube:" + currentUser.getUserId();

        boolean allowed = rateLimitService.tryAcquire(key, YOUTUBE_SEARCH_LIMIT_PER_DAY, Duration.ofHours(24));
        if (!allowed) {
            throw new SearchLimitExceededException("YouTube検索の1日の上限(20回)に達しました。24時間後に再度お試しください");
        }

        return youTubeEventSubService.searchChannels(query);
    }

    /**
     * ログイン中のユーザーの配信者登録を削除する。
     * 削除の結果、その配信者を誰も登録していない状態になった場合は、
     * 配信検知の購読を解除し、Streamerレコード自体も削除する。
     *
     * @param oidcUser 認証済みユーザー情報
     * @param streamerId 削除対象のStreamerのID
     * @return 削除結果のメッセージ
     * @throws IllegalStateException 該当する登録が存在しない場合
     */
    @Transactional
    @DeleteMapping("/streamers/{streamerId}")
    public String deleteStreamer(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable Long streamerId
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);

        Registration registration = registrationRepository
                .findByUser_UserIdAndStreamer_StreamerId(currentUser.getUserId(), streamerId)
                .orElseThrow(() -> new IllegalStateException("登録が見つかりません"));

        registrationRepository.delete(registration);

        List<Registration> remainingRegistrations =
                registrationRepository.findByStreamer_StreamerId(streamerId);

        if (remainingRegistrations.isEmpty()) {
            unsubscribeAndDeleteStreamer(registration.getStreamer());
        }

        return "削除しました";
    }

    /**
     * 配信検知の購読を解除し、Streamerレコードを削除する。
     *
     * @param streamer 削除対象の配信者
     */
    private void unsubscribeAndDeleteStreamer(Streamer streamer) {
        if ("twitch".equals(streamer.getPlatform()) && streamer.getTwitchSubscriptionId() != null) {
            twitchEventSubService.unsubscribe(streamer.getTwitchSubscriptionId());
        } else if ("youtube".equals(streamer.getPlatform())) {
            youTubeEventSubService.unsubscribe(streamer.getPlatformChannelId());
        }
        streamerRepository.delete(streamer);
    }
}