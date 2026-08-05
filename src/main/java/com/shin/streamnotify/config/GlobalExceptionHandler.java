package com.shin.streamnotify.config;

import com.shin.streamnotify.streamer.ChannelLimitExceededException;
import com.shin.streamnotify.streamer.SearchLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * アプリ全体の例外を横断的に処理するクラス。
 * 各コントローラに個別のtry-catchを書かずに、統一されたエラーレスポンスを返す。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ChannelLimitExceededExceptionを、400 Bad Requestとメッセージ入りのJSONに変換する。
     *
     * @param e スローされた例外
     * @return エラーメッセージを含むレスポンス
     */
    @ExceptionHandler(ChannelLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleChannelLimitExceeded(ChannelLimitExceededException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }

    /**
     * SearchLimitExceededExceptionを、429 Too Many Requestsとメッセージ入りのJSONに変換する。
     *
     * @param e スローされた例外
     * @return エラーメッセージを含むレスポンス
     */
    @ExceptionHandler(SearchLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleSearchLimitExceeded(SearchLimitExceededException e) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", e.getMessage()));
    }
}