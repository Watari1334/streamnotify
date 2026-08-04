# 開発規約

## 命名規則

- フィールド名・メソッド名: camelCase(例: `platformChannelId`, `twitchSubscriptionId`)
- DBカラム名: snake_case(例: `platform_channel_id`)。`@Column(name = "...")`で明示的にマッピングする
- テストメソッド名: 日本語での説明的な命名(例: `新規配信者を登録するとTwitchのサブスクリプションが作成される`)
- パッケージ構成: 機能ドメインごとに分割(`streamer`, `registration`, `twitch`, `youtube`, `user`, `web`, `webhook`, `config`, `notification`)
- プラットフォーム非依存の識別子は `oauth_provider` + `oauth_subject` の組み合わせで一意性を担保する(例: `User`エンティティ)。特定のプラットフォーム名を冠したカラム名(`twitch_subject`など)は避ける

## null / Optionalの扱い

- 存在しない可能性のある値を返すメソッドは `Optional<T>` を返す(例: `findByPlatformAndPlatformChannelId`)
- 呼び出し側で「見つからない場合」を早期に例外化する(例: `orElseThrow(() -> new IllegalStateException("..."))`)
- 空のコレクションは `null` ではなく空リスト(`List.of()`)を返す
- `Optional`をフィールドやDTOのプロパティとして保持しない(あくまで戻り値としてのみ使う)

## 例外処理

- ユーザー起因の一般的なエラー(見つからない、権限がないなど)は `IllegalStateException` + 具体的な日本語メッセージ
- クライアント(フロントエンド)に、エラーの具体的な理由をJSONで返す必要がある場合は、専用の例外クラス(例: `ChannelLimitExceededException`)を作成し、`@RestControllerAdvice` + `@ExceptionHandler` で捕捉して、明示的にレスポンスを組み立てる(`spring.web.error.include-message=always`のような、グローバル設定での対応は避ける。予期しない例外の詳細が外部に漏れるリスクがあるため)
- エラーメッセージは「〜が見つかりません」「〜は最大20件までです」のように統一する

## マルチプラットフォーム対応の方針

- Twitch・YouTubeなど、複数の外部サービスに対応する機能は、`Streamer.platform`(`"twitch"` / `"youtube"`)の値で分岐する
- プラットフォームごとの、外部API呼び出しは、専用のServiceクラスに分離する(`TwitchEventSubService` / `YouTubeEventSubService`)
- 通知など、プラットフォームをまたいで共通化できる処理は、`platform`を引数として受け取り、内部で分岐する(例: `DiscordNotificationService.sendStreamOnlineNotification`)

## キャッシュ方針

- 外部APIの検索結果は、レート制限・クォータの厳しさに応じて、キャッシュ期間を調整する
  - Twitch検索: 5分(ライブ状況の鮮度を優先)
  - YouTube検索: 24時間(クォータが厳しく、チャンネル基本情報は頻繁に変わらないため)
- キャッシュのシリアライズは、型を固定した `JacksonJsonRedisSerializer` を使う(型情報をJSONに埋め込む `enableUnsafeDefaultTyping` は、デシリアライズ脆弱性のリスクがあるため使用しない)

## テスト方針

- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = Replace.NONE)`: リポジトリ層(実際のJPA/SQLの検証)。ただしAWS RDSへの直接接続が前提のため、ローカル環境からは`@Disabled`でスキップする運用とする
- `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`: Controller/Service層(外部依存を排除したロジック検証)
- 優先してテストする対象: 分岐のあるロジック、セキュリティに関わる処理(他人のデータを操作できないか等)、外部API呼び出しの有無
- 優先度を下げる対象: 単純なgetter/setter、Spring Data JPAの自動実装メソッド自体
