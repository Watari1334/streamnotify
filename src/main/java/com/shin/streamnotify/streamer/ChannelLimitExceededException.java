package com.shin.streamnotify.streamer;

/**
 * 1ユーザーあたりの登録チャンネル数の上限(20件)を超えた場合にスローされる例外。
 */
public class ChannelLimitExceededException extends RuntimeException {

    /**
     * @param message クライアントに返すエラーメッセージ
     */
    public ChannelLimitExceededException(String message) {
        super(message);
    }
}