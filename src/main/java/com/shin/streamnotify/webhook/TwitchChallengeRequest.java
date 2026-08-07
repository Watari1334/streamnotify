package com.shin.streamnotify.webhook;

/**
 * TwitchのEventSub Webhookが送ってくる検証チャレンジのDTO。
 * messageTypeが"webhook_callback_verification"の場合のリクエストボディに対応する。
 * challengeの値をそのまま返すことで、購読登録を確定させる。
 *
 * @param challenge Twitchが発行する検証用の文字列
 */
public record TwitchChallengeRequest(
        String challenge
) {
}