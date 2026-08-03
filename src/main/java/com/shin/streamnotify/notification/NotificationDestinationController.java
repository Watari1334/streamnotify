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

@RestController
@RequiredArgsConstructor
public class NotificationDestinationController {

    private final NotificationDestinationRepository notificationDestinationRepository;
    private final CurrentUserResolver currentUserResolver;

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

    private record NotificationDestinationResponse(String discordWebhookUrl) {
    }
}