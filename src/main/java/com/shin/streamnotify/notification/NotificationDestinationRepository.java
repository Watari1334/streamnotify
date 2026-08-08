package com.shin.streamnotify.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * NotificationDestinationのリポジトリ。
 * 1ユーザーにつき1件のみ登録される想定なので、findByUser_UserIdはOptionalで単一件を返す。
 */
public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

    /**
     * 指定したユーザーの通知先を取得する。
     *
     * @param userId 対象ユーザーのID
     * @return 登録済みの通知先。未登録の場合は空
     */
    Optional<NotificationDestination> findByUser_UserId(Long userId);
}