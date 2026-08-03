package com.shin.streamnotify.streamer;

import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.twitch.TwitchEventSubService;
import com.shin.streamnotify.user.CurrentUserResolver;
import com.shin.streamnotify.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.shin.streamnotify.twitch.TwitchEventSubService.ChannelSearchResult;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerRepository streamerRepository;
    private final CurrentUserResolver currentUserResolver;
    private final RegistrationRepository registrationRepository;
    private final TwitchEventSubService twitchEventSubService;

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

        boolean isNewStreamer = existingStreamer.isEmpty();

        Streamer streamer = existingStreamer.orElseGet(() -> streamerRepository.save(
                new Streamer(request.platform(), request.platformChannelId(), request.channelName(), request.channelLogin())
        ));

        Registration registration = new Registration(currentUser, streamer);
        registrationRepository.save(registration);

        if (isNewStreamer && "twitch".equals(request.platform())) {
            String subscriptionId = twitchEventSubService.subscribeToStreamOnline(request.platformChannelId());
            streamer.setTwitchSubscriptionId(subscriptionId);
            streamerRepository.save(streamer);
        }

        return "登録しました: " + streamer.getChannelName();
    }

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

    @GetMapping("/streamers/search")
    public List<ChannelSearchResult> searchStreamers(@RequestParam String query) {
        return twitchEventSubService.searchChannels(query);
    }

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
            Streamer streamer = registration.getStreamer();
            if (streamer.getTwitchSubscriptionId() != null) {
                twitchEventSubService.unsubscribe(streamer.getTwitchSubscriptionId());
            }
            streamerRepository.delete(streamer);
        }

        return "削除しました";
    }
}