# StreamNotify

Twitch配信の開始を検知し、Discordに通知するSpring Bootアプリケーション。

## 概要

登録したTwitchチャンネルの配信開始を、Twitch EventSub(Webhook)で検知し、Discordへ自動通知します。Twitchアカウントでのログイン(OIDC)により、ユーザーごとに好きな配信者を登録・管理できます。

## 技術スタック

- Java 25 / Spring Boot 4.1.0
- PostgreSQL 16 / Flyway(マイグレーション管理)
- Spring Security(OAuth2 Client, Twitch OIDC)
- Twitch Helix API / EventSub Webhook
- JUnit 5 / Mockito / AssertJ

## 主な機能

- Twitchアカウントでのログイン
- チャンネル検索・登録・削除
- 配信開始のリアルタイム検知(EventSub)
- Discordへの自動通知

## セットアップ

```bash
docker-compose up -d        # PostgreSQL起動
./mvnw spring-boot:run       # アプリ起動
```

環境変数(`.env`または実行時に設定)