package com.shin.streamnotify.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Userのリポジトリ。
 * OAuthプロバイダとサブジェクトの組み合わせで、外部のOIDCアイデンティティに
 * 対応するUserを検索する(CurrentUserResolver、CustomOidcUserServiceから利用される)。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * OAuthプロバイダとサブジェクトの組み合わせから、対応するUserを検索する。
     *
     * @param oauthProvider ログインに使われたプロバイダ("twitch"または"google")
     * @param oauthSubject プロバイダ内でのユーザー固有ID(sub)
     * @return 該当するUser。存在しない場合は空
     */
    Optional<User> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);
}