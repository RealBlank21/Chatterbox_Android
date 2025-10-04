package com.example.testing.network.request;

public class RequestMessage {
    private final String role;
    private final String content;

    public RequestMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}