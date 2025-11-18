package com.example.testing;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
import com.example.testing.network.request.ApiRequest;
import com.example.testing.network.request.RequestMessage;
import com.example.testing.network.response.ApiResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationViewModel extends AndroidViewModel {

    private final MessageRepository messageRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    // --- ADD THIS ---
    private final ConversationRepository conversationRepository;
    // ----------------
    private final ApiService apiService;
    private final ExecutorService executorService;

    private final SimpleDateFormat dayFormatter;
    private final SimpleDateFormat timeFormatter;

    private LiveData<List<Message>> messages;
    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
        characterRepository = new CharacterRepository(application);
        userRepository = new UserRepository(application);
        // --- INITIALIZE THIS ---
        conversationRepository = new ConversationRepository(application);
        // -----------------------
        apiService = ApiClient.getClient().create(ApiService.class);
        executorService = Executors.newSingleThreadExecutor();

        dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public void loadData(int characterId, int conversationId) {
        this.currentCharacter = characterRepository.getCharacterById(characterId);
        this.currentUser = userRepository.getUser();
        this.messages = messageRepository.getMessagesForConversation(conversationId);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }

    public void sendMessage(String content, int conversationId, User user, Character character) {
        triggerApiCall(content, conversationId, user, character, false);
    }

    public void regenerateLastResponse(int conversationId, User user, Character character) {
        executorService.execute(() -> {
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            Message lastAssistantMessage = null;
            for (int i = messageHistory.size() - 1; i >= 0; i--) {
                if ("assistant".equals(messageHistory.get(i).getRole())) {
                    lastAssistantMessage = messageHistory.get(i);
                    break;
                }
            }

            if (lastAssistantMessage != null) {
                messageRepository.delete(lastAssistantMessage);
                triggerApiCall("", conversationId, user, character, true);
            }
        });
    }

    public void update(Message message) {
        messageRepository.update(message);
    }

    private void triggerApiCall(String content, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) {
            Log.e("VIEW_MODEL_ERROR", "User or Character is null.");
            return;
        }

        if (!isRegeneration) {
            Message userMessage = new Message("user", content, conversationId);
            messageRepository.insert(userMessage);
            // --- UPDATE TIMESTAMP ON USER MESSAGE ---
            conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
            // ----------------------------------------
        }

        executorService.execute(() -> {
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            List<RequestMessage> requestMessages = new ArrayList<>();

            long creationTimestamp;
            if (!messageHistory.isEmpty()) {
                creationTimestamp = messageHistory.get(0).getTimestamp();
            } else {
                creationTimestamp = System.currentTimeMillis();
            }
            Date creationDate = new Date(creationTimestamp);

            String formattedDay = dayFormatter.format(creationDate);
            String formattedTime = timeFormatter.format(creationDate);

            String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
            String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";

            if (globalPrompt.contains("{day}")) globalPrompt = globalPrompt.replace("{day}", formattedDay);
            if (globalPrompt.contains("{time}")) globalPrompt = globalPrompt.replace("{time}", formattedTime);
            if (characterPersonality.contains("{day}")) characterPersonality = characterPersonality.replace("{day}", formattedDay);
            if (characterPersonality.contains("{time}")) characterPersonality = characterPersonality.replace("{time}", formattedTime);

            String finalSystemPrompt = globalPrompt + "\n" + characterPersonality;

            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            for (Message msg : messageHistory) {
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
            }

            if (!isRegeneration && !content.isEmpty()) {
                requestMessages.add(new RequestMessage("user", content));
            }

            String model = character.getModel();
            if (TextUtils.isEmpty(model)) {
                model = user.getPreferredModel();
            }
            if (TextUtils.isEmpty(model)) {
                Log.e("API_ERROR", "No model specified.");
                return;
            }

            ApiRequest apiRequest = new ApiRequest(model, requestMessages, character.getTemperature(), character.getMaxTokens());
            String apiKey = "Bearer " + user.getApiKey();

            Call<ApiResponse> call = apiService.getChatCompletion(apiKey, apiRequest);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                        String aiResponseContent = response.body().getChoices().get(0).getMessage().getContent();
                        Message aiMessage = new Message("assistant", aiResponseContent, conversationId);
                        messageRepository.insert(aiMessage);

                        // --- UPDATE TIMESTAMP ON AI RESPONSE ---
                        // This ensures the conversation stays at top when AI replies
                        conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
                        // ---------------------------------------

                    } else {
                        Log.e("API_ERROR", "Response not successful: " + response.code() + " - " + response.message());
                        Message errorMessage = new Message("assistant", "Error: " + response.code() + " " + response.message(), conversationId);
                        messageRepository.insert(errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("API_FAILURE", "API call failed: " + t.getMessage());
                    Message errorMessage = new Message("assistant", "Error: API call failed. Check network connection.", conversationId);
                    messageRepository.insert(errorMessage);
                }
            });
        });
    }
}