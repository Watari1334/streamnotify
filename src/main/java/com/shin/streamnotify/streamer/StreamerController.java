package com.shin.streamnotify.streamer;

import com.shin.streamnotify.registration.Registration;
import com.shin.streamnotify.registration.RegistrationRepository;
import com.shin.streamnotify.twitch.TwitchEventSubService;
import com.shin.streamnotify.user.User;
import com.shin.streamnotify.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerRepository streamerRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final TwitchEventSubService twitchEventSubService;

    @PostMapping("/streamers/register")
    public String registerStreamer(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestBody StreamerRegistrationRequest request
    ) {
        User currentUser = userRepository.findByTwitchSubject(oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        Optional<Streamer> existingStreamer = streamerRepository
                .findByPlatformAndPlatformChannelId(request.platform(), request.platformChannelId());

        boolean isNewStreamer = existingStreamer.isEmpty();

        Streamer streamer = existingStreamer.orElseGet(() -> streamerRepository.save(
                new Streamer(request.platform(), request.platformChannelId(), request.channelName())
        ));

        Registration registration = new Registration(currentUser, streamer);
        registrationRepository.save(registration);

        if (isNewStreamer && "twitch".equals(request.platform())) {
            twitchEventSubService.subscribeToStreamOnline(request.platformChannelId());
        }

        return "登録しました: " + streamer.getChannelName();
    }
    @GetMapping("/debug/eventsub-subscriptions")
    public String debugListSubscriptions() {
        return twitchEventSubService.listSubscriptions();
    }

    @GetMapping("/streamers")
    public List<StreamerResponse> listStreamers(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        User currentUser = userRepository.findByTwitchSubject(oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

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

    @DeleteMapping("/streamers/{streamerId}")
    public String deleteStreamer(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable Long streamerId
    ) {
        User currentUser = userRepository.findByTwitchSubject(oidcUser.getSubject())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));

        Registration registration = registrationRepository
                .findByUser_UserIdAndStreamer_StreamerId(currentUser.getUserId(), streamerId)
                .orElseThrow(() -> new IllegalStateException("登録が見つかりません"));

        registrationRepository.delete(registration);

        return "削除しました";
    }
}