# 開発規約

## 命名規則

- フィールド名・メソッド名: camelCase(例: `platformChannelId`, `twitchSubscriptionId`)
- DBカラム名: snake_case(例: `platform_channel_id`)。`@Column(name = "...")`で明示的にマッピングする
- テストメソッド名: 日本語での説明的な命名(例: `新規配信者を登録するとTwitchのサブスクリプションが作成される`)
- パッケージ構成: 機能ドメインごとに分割(`streamer`, `registration`, `twitch`, `user`, `web`, `webhook`, `config`)

## null / Optionalの扱い

- 存在しない可能性のある値を返すメソッドは `Optional<T>` を返す(例: `findByPlatformAndPlatformChannelId`)
- 呼び出し側で「見つからない場合」を早期に例外化する(例: `orElseThrow(() -> new IllegalStateException("..."))`)
- 空のコレクションは `null` ではなく空リスト(`List.of()`)を返す
- `Optional`をフィールドやDTOのプロパティとして保持しない(あくまで戻り値としてのみ使う)

## 例外処理

- ユーザー起因のエラー(見つからない、権限がないなど)は `IllegalStateException` + 具体的な日本語メッセージ
- エラーメッセージは「〜が見つかりません」のように統一する

## テスト方針

- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = Replace.NONE)`: リポジトリ層(実際のJPA/SQLの検証)
- `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`: Controller/Service層(外部依存を排除したロジック検証)
- 優先してテストする対象: 分岐のあるロジック、セキュリティに関わる処理(他人のデータを操作できないか等)、外部API呼び出しの有無
- 優先度を下げる対象: 単純なgetter/setter、Spring Data JPAの自動実装メソッド自体