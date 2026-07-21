package com.shin.streamnotify.notification;

import com.shin.streamnotify.user.User;
import com.shin.streamnotify.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationDestinationController {

    private final NotificationDestinationRepository notificationDestinationRepository;
    private final UserRepository userRepository;

    @PostMapping("/notifications/destinations")
    public String registerDestination(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestBody NotificationDestinationRequest request
    ) {
        User currentUser = userRepository.findByTwitchSubject(oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        NotificationDestination destination =
                new NotificationDestination(currentUser, request.discordWebhookUrl());
        notificationDestinationRepository.save(destination);

        return "Discord通知先を登録しました";
    }
}