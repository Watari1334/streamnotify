package com.shin.streamnotify.registration;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Registrationエンティティの複合主キークラス。
 * フィールド名(user, streamer)はRegistrationの@Idフィールド名と
 * 一致させる必要がある(@IdClassの仕様上の制約)。
 * 各フィールドの型は、対応するエンティティの主キー型(Long)にする。
 */
@EqualsAndHashCode
@NoArgsConstructor
public class RegistrationId implements Serializable {

    private Long user;
    private Long streamer;

    /**
     * @param user Userの主キー(userId)
     * @param streamer Streamerの主キー(streamerId)
     */
    public RegistrationId(Long user, Long streamer) {
        this.user = user;
        this.streamer = streamer;
    }
}