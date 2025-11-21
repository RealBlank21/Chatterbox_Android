package com.example.testing.network.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatCompletionResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public String getId() { return id; }
    public String getObject() { return object; }
    public long getCreated() { return created; }
    public String getModel() { return model; }
    public List<Choice> getChoices() { return choices; }
    public Usage getUsage() { return usage; }

    public static class Choice {
        private int index;
        private Message message;

        @SerializedName("delta")
        private Message delta;

        @SerializedName("finish_reason")
        private String finishReason;

        public int getIndex() { return index; }
        public Message getMessage() { return message; }
        public Message getDelta() { return delta; }
        public String getFinishReason() { return finishReason; }
    }

    public static class Message {
        private String role;
        private String content;

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static class Usage {
        @SerializedName("prompt_tokens")
        private int promptTokens;

        @SerializedName("completion_tokens")
        private int completionTokens;

        @SerializedName("total_tokens")
        private int totalTokens;

        @SerializedName("total_cost")
        private double totalCost;

        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public int getTotalTokens() { return totalTokens; }
        public double getTotalCost() { return totalCost; }
    }
}