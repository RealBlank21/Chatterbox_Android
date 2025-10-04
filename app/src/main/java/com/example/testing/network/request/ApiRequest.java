// In network/request/ApiRequest.java
package com.example.testing.network.request;

import com.google.gson.annotations.SerializedName; // Import this
import java.util.List;

public class ApiRequest {
    private final String model;
    private final List<RequestMessage> messages;

    // --- ADD THESE FIELDS ---
    private final Float temperature;
    @SerializedName("max_tokens") // This maps the Java field to the JSON field
    private final Integer maxTokens;

    // --- UPDATE THE CONSTRUCTOR ---
    public ApiRequest(String model, List<RequestMessage> messages, Float temperature, Integer maxTokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }
}