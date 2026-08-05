package com.shin.streamnotify.streamer;

/**
 * ユーザーごとの検索回数の上限を超えた場合にスローされる例外。
 */
public class SearchLimitExceededException extends RuntimeException {

    /**
     * @param message クライアントに返すエラーメッセージ
     */
    public SearchLimitExceededException(String message) {
        super(message);
    }
}