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

    @Column(name = "oauth_provider", nullable = false)
    private String oauthProvider;

    @Column(name = "oauth_subject", nullable = false)
    private String oauthSubject;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public User(String userName, String oauthProvider, String oauthSubject) {
        this.userName = userName;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
        this.createdAt = LocalDateTime.now();
    }
}