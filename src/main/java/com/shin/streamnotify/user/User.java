package com.shin.streamnotify.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "twitch_subject", unique = true)
    private String twitchSubject;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public User(String userName, String twitchSubject) {
        this.userName = userName;
        this.twitchSubject = twitchSubject;
        this.createdAt = LocalDateTime.now();
    }
}