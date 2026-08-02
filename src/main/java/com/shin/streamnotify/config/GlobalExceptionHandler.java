package com.shin.streamnotify.config;

import com.shin.streamnotify.streamer.ChannelLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChannelLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleChannelLimitExceeded(ChannelLimitExceededException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }
}