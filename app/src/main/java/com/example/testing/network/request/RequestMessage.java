package com.example.testing.network.request;

import com.google.gson.annotations.SerializedName;

public class RequestMessage {
    private final String role;

    // Changed from String to Object to support both String and List<ContentPart>
    private final Object content;

    public RequestMessage(String role, Object content) {
        this.role = role;
        this.content = content;
    }
}