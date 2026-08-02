package com.shin.streamnotify.streamer;

public class ChannelLimitExceededException extends RuntimeException {
    public ChannelLimitExceededException(String message) {
        super(message);
    }
}