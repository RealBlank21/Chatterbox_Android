package com.example.testing.data.remote.request;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiRequest {
    private final String model;
    private final List<RequestMessage> messages;
    private final Float temperature;

    @SerializedName("max_tokens")
    private final Integer maxTokens;

    private final Boolean stream;

    @SerializedName("top_p")
    private final Float topP;

    @SerializedName("top_k")
    private final Integer topK;

    @SerializedName("frequency_penalty")
    private final Float frequencyPenalty;

    @SerializedName("presence_penalty")
    private final Float presencePenalty;

    @SerializedName("repetition_penalty")
    private final Float repetitionPenalty;

    public ApiRequest(String model, List<RequestMessage> messages, Float temperature, Integer maxTokens, Boolean stream,
                      Float topP, Integer topK, Float frequencyPenalty, Float presencePenalty, Float repetitionPenalty) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
        this.topP = topP;
        this.topK = topK;
        this.frequencyPenalty = frequencyPenalty;
        this.presencePenalty = presencePenalty;
        this.repetitionPenalty = repetitionPenalty;
    }

    // Constructor for backward compatibility if strictly needed (optional)
    public ApiRequest(String model, List<RequestMessage> messages, Float temperature, Integer maxTokens, Boolean stream) {
        this(model, messages, temperature, maxTokens, stream, null, null, null, null, null);
    }
}