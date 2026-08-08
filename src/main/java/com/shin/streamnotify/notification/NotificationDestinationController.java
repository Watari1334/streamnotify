package com.shin.streamnotify.notification;

import com.shin.streamnotify.user.CurrentUserResolver;
import com.shin.streamnotify.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * ユーザーのDiscord通知先(Webhook URL)の取得・登録を扱うコントローラ。
 */
@RestController
@RequiredArgsConstructor
public class NotificationDestinationController {

    private final NotificationDestinationRepository notificationDestinationRepository;
    private final CurrentUserResolver currentUserResolver;

    /**
     * ログイン中のユーザーの、登録済み通知先を取得する。
     *
     * @param oidcUser 認証済みユーザー情報
     * @return 登録済みのDiscord Webhook URL。未登録の場合はnull
     */
    @GetMapping("/notifications/destinations")
    public NotificationDestinationResponse getDestination(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);

        Optional<NotificationDestination> destination =
                notificationDestinationRepository.findByUser_UserId(currentUser.getUserId());

        return new NotificationDestinationResponse(
                destination.map(NotificationDestination::getDiscordWebhookUrl).orElse(null)
        );
    }

    /**
     * ログイン中のユーザーのDiscord通知先を登録・更新する。
     * 既に登録済みの場合はURLを上書きし、未登録の場合は新規作成する。
     *
     * @param oidcUser 認証済みユーザー情報
     * @param request 登録するDiscord Webhook URL
     * @return 登録結果のメッセージ
     */
    @PostMapping("/notifications/destinations")
    public String registerDestination(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestBody NotificationDestinationRequest request
    ) {
        User currentUser = currentUserResolver.resolve(oidcUser);

        NotificationDestination destination = notificationDestinationRepository
                .findByUser_UserId(currentUser.getUserId())
                .orElseGet(() -> new NotificationDestination(currentUser, request.discordWebhookUrl()));

        destination.setDiscordWebhookUrl(request.discordWebhookUrl());
        notificationDestinationRepository.save(destination);

        return "Discord通知先を登録しました";
    }

    /**
     * /notifications/destinations(GET)のレスポンスDTO。
     *
     * @param discordWebhookUrl 登録済みのDiscord Webhook URL。未登録の場合はnull
     */
    private record NotificationDestinationResponse(String discordWebhookUrl) {
    }

    /**
     * /notifications/destinations(POST)のリクエストDTO。
     *
     * @param discordWebhookUrl 登録するDiscord Webhook URL
     */
    private record NotificationDestinationRequest(String discordWebhookUrl) {
    }
}