package com.example.testing.network.request;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiRequest {
    private final String model;
    private final List<RequestMessage> messages;
    private final Float temperature;

    @SerializedName("max_tokens")
    private final Integer maxTokens;

    private final Boolean stream; // Add this field

    public ApiRequest(String model, List<RequestMessage> messages, Float temperature, Integer maxTokens, Boolean stream) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }

    // Keep old constructor for backward compatibility if needed, or just update calls
    public ApiRequest(String model, List<RequestMessage> messages, Float temperature, Integer maxTokens) {
        this(model, messages, temperature, maxTokens, false);
    }
}