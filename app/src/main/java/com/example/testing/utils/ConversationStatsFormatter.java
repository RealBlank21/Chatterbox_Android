package com.example.testing.utils;

import android.text.TextUtils;

import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Message;
import com.example.testing.data.local.entity.User;
import com.example.testing.data.remote.response.Model;
import com.example.testing.data.repository.ModelRepository;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ConversationStatsFormatter {

    public static String generateStatsInfo(List<Message> messages, Character character, User user) {
        if (messages == null || messages.isEmpty() || character == null) {
            return "No information available yet.";
        }

        int userMsgCount = 0;
        int botMsgCount = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = 0;
        String lastFinishReason = "N/A";

        for (Message msg : messages) {
            if ("user".equals(msg.getRole())) {
                userMsgCount++;
            } else if ("assistant".equals(msg.getRole())) {
                botMsgCount++;
                totalInputTokens += msg.getPromptTokens();
                totalOutputTokens += msg.getCompletionTokens();
                if (msg.getFinishReason() != null) {
                    lastFinishReason = msg.getFinishReason();
                }
            }
            if (msg.getTimestamp() < firstTimestamp) firstTimestamp = msg.getTimestamp();
            if (msg.getTimestamp() > lastTimestamp) lastTimestamp = msg.getTimestamp();
        }

        // Duration Calculation
        String durationStr = calculateDuration(firstTimestamp, lastTimestamp);

        // Cost Calculation
        String modelId = !TextUtils.isEmpty(character.getModel()) ? character.getModel() : (user != null ? user.getPreferredModel() : null);
        String[] costStrings = calculateCost(modelId, totalInputTokens, totalOutputTokens);

        StringBuilder info = new StringBuilder();
        info.append("Model: ").append(modelId != null ? modelId : "Default").append("\n");
        info.append("Input Price: ").append(costStrings[1]).append("\n");
        info.append("Output Price: ").append(costStrings[2]).append("\n\n");

        info.append("User Messages: ").append(userMsgCount).append("\n");
        info.append("Bot Messages: ").append(botMsgCount).append("\n");
        info.append("Duration: ").append(durationStr).append("\n\n");

        info.append("--- Token Usage ---\n");
        info.append("Total Input Tokens: ").append(totalInputTokens).append("\n");
        info.append("Total Output Tokens: ").append(totalOutputTokens).append("\n");
        info.append("Last Finish Reason: ").append(lastFinishReason).append("\n\n");
        info.append("Total Cost: ").append(costStrings[0]);

        return info.toString();
    }

    private static String calculateDuration(long start, long end) {
        if (start == Long.MAX_VALUE || end == 0) return "N/A";

        long diffInMillis = end - start;
        if (diffInMillis < 0) diffInMillis = 0;

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else {
            return minutes + "m";
        }
    }

    // Returns [TotalCost, InputPriceString, OutputPriceString]
    private static String[] calculateCost(String modelId, long inputTokens, long outputTokens) {
        String costStr = "Unknown Model Price";
        String inputPriceStr = "N/A";
        String outputPriceStr = "N/A";

        if (modelId != null) {
            Model model = ModelRepository.getInstance().getModelById(modelId);
            if (model != null && model.getPricing() != null) {
                try {
                    double promptPrice = Double.parseDouble(model.getPricing().getPrompt());
                    double completionPrice = Double.parseDouble(model.getPricing().getCompletion());
                    double totalCost = (inputTokens * promptPrice) + (outputTokens * completionPrice);

                    if (totalCost < 0.0001) {
                        costStr = String.format(Locale.getDefault(), "$%.6f", totalCost);
                    } else {
                        costStr = String.format(Locale.getDefault(), "$%.4f", totalCost);
                    }
                    inputPriceStr = String.format(Locale.getDefault(), "$%.2f/1M", promptPrice * 1000000);
                    outputPriceStr = String.format(Locale.getDefault(), "$%.2f/1M", completionPrice * 1000000);
                } catch (Exception e) {
                    costStr = "Pricing Error";
                }
            }
        }
        return new String[]{costStr, inputPriceStr, outputPriceStr};
    }
}