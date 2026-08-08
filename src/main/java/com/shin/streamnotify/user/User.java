package com.shin.streamnotify.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * streamnotifyのユーザーを表すエンティティ。
 * TwitchまたはGoogleのOIDCログインで初回認証時にCustomOidcUserServiceによって自動作成される。
 * oauthProvider("twitch"または"google")とoauthSubject(各プロバイダ内でのユーザー固有ID)の
 * 組み合わせで、外部のOIDCアイデンティティと一意に紐づく。
 */
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

    /**
     * 新しいUserを作成する。
     *
     * @param userName 表示名(OIDCのpreferred_usernameか、無ければフォールバック名)
     * @param oauthProvider ログインに使われたプロバイダ("twitch"または"google")
     * @param oauthSubject プロバイダ内でのユーザー固有ID(sub)
     */
    public User(String userName, String oauthProvider, String oauthSubject) {
        this.userName = userName;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
        this.createdAt = LocalDateTime.now();
    }
}