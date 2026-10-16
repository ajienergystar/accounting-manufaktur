package com.aji.accounting.common.web;

public record MessageResponse(String message) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
