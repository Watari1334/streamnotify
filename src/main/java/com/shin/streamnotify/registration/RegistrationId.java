package com.shin.streamnotify.registration;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
public class RegistrationId implements Serializable {

    private Long user;
    private Long streamer;

    public RegistrationId(Long user, Long streamer) {
        this.user = user;
        this.streamer = streamer;
    }
}