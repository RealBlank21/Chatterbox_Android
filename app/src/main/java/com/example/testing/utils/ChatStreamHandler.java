package com.example.testing.utils;

import android.text.TextUtils;

import com.example.testing.data.local.entity.Message;
import com.example.testing.data.repository.ConversationRepository;
import com.example.testing.data.repository.MessageRepository;
import com.example.testing.data.remote.response.ChatCompletionResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ChatStreamHandler {

    public static void handleStream(Response<ResponseBody> response,
                                    Message currentAiMessage,
                                    MessageRepository messageRepository,
                                    ConversationRepository conversationRepository) throws IOException {

        if (response.isSuccessful() && response.body() != null) {
            InputStream is = response.body().byteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder contentBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonPart = line.substring(6).trim();
                    if (jsonPart.equals("[DONE]")) break;

                    try {
                        ChatCompletionResponse chunk = new Gson().fromJson(jsonPart, ChatCompletionResponse.class);

                        if (chunk.getUsage() != null) {
                            currentAiMessage.setPromptTokens(chunk.getUsage().getPromptTokens());
                            currentAiMessage.setCompletionTokens(chunk.getUsage().getCompletionTokens());
                            currentAiMessage.setTokenCount(chunk.getUsage().getTotalTokens());
                        }

                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            ChatCompletionResponse.Choice choice = chunk.getChoices().get(0);

                            ChatCompletionResponse.Message delta = choice.getDelta();
                            if (delta != null && delta.getContent() != null) {
                                contentBuilder.append(delta.getContent());
                                currentAiMessage.setContent(contentBuilder.toString());
                                messageRepository.updateSync(currentAiMessage);
                                conversationRepository.updateLastUpdatedSync(currentAiMessage.getConversationId(), System.currentTimeMillis());
                            }

                            if (choice.getFinishReason() != null) {
                                currentAiMessage.setFinishReason(choice.getFinishReason());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            String finalContent = contentBuilder.toString().trim();
            if (TextUtils.isEmpty(finalContent)) {
                finalContent = "...";
            }
            currentAiMessage.setContent(finalContent);
            messageRepository.updateSync(currentAiMessage);

        } else {
            currentAiMessage.setContent("Error: " + response.code() + " " + response.message());
            messageRepository.updateSync(currentAiMessage);
        }
    }
}