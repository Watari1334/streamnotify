package com.shin.streamnotify.registration;

import com.shin.streamnotify.streamer.Streamer;
import com.shin.streamnotify.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserとStreamerの多対多の関連を表す中間テーブルのエンティティ。
 * どのユーザーがどの配信者を登録しているかを表す。
 * 複合主キー(user, streamer)を@IdClassでRegistrationIdに委譲している。
 */
@Entity
@Table(name = "registrations")
@IdClass(RegistrationId.class)
@Getter
@NoArgsConstructor
public class Registration {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "streamer_id")
    private Streamer streamer;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * @param user 登録するユーザー
     * @param streamer 登録される配信者
     */
    public Registration(User user, Streamer streamer) {
        this.user = user;
        this.streamer = streamer;
        this.createdAt = LocalDateTime.now();
    }
}