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

// --- IMPORTS FOR TIME ARE STILL NEEDED ---
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// ------------------------------------

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
    private final ApiService apiService;
    private final ExecutorService executorService;

    // --- WE NOW NEED TWO FORMATTERS ---
    private final SimpleDateFormat dayFormatter;
    private final SimpleDateFormat timeFormatter;
    // ---------------------------------

    private LiveData<List<Message>> messages;
    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
        characterRepository = new CharacterRepository(application);
        userRepository = new UserRepository(application);
        apiService = ApiClient.getClient().create(ApiService.class);
        executorService = Executors.newSingleThreadExecutor();

        // --- INITIALIZE BOTH FORMATTERS ---
        dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        // ----------------------------------
    }

    public void loadData(int characterId, int conversationId) {
        this.currentCharacter = characterRepository.getCharacterById(characterId);
        this.currentUser = userRepository.getUser();
        this.messages = messageRepository.getMessagesForConversation(conversationId);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }

    // --- Public method for the UI to call ---
    public void sendMessage(String content, int conversationId, User user, Character character) {
        // This is a new message from the user, so isRegeneration is false.
        triggerApiCall(content, conversationId, user, character, false);
    }

    // --- New method for regeneration ---
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
                // Delete the last AI message from the database
                messageRepository.delete(lastAssistantMessage);
                // Trigger a new API call, indicating it's a regeneration
                triggerApiCall("", conversationId, user, character, true);
            }
        });
    }

    // --- This method is for the "Edit" feature ---
    public void update(Message message) {
        messageRepository.update(message);
    }

    // --- PRIVATE method that contains the core API logic ---
    private void triggerApiCall(String content, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) {
            Log.e("VIEW_MODEL_ERROR", "User or Character is null.");
            return;
        }

        // Only save a new user message if this is NOT a regeneration.
        if (!isRegeneration) {
            Message userMessage = new Message("user", content, conversationId);
            messageRepository.insert(userMessage);
        }

        executorService.execute(() -> {
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            List<RequestMessage> requestMessages = new ArrayList<>();

            // --- THIS IS THE NEW LOGIC YOU REQUESTED ---
            // 1. Get the conversation creation timestamp
            long creationTimestamp;
            if (!messageHistory.isEmpty()) {
                // Get timestamp from the *first* message in history
                creationTimestamp = messageHistory.get(0).getTimestamp();
            } else {
                // Fallback for a brand new conversation (this msg is the first)
                creationTimestamp = System.currentTimeMillis();
            }
            Date creationDate = new Date(creationTimestamp);

            // 2. Format the Day and Time
            String formattedDay = dayFormatter.format(creationDate);
            String formattedTime = timeFormatter.format(creationDate);

            // 3. Get the prompts
            String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
            String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";

            // 4. Perform replacements if the tags exist
            // (Using String.replace(), not regex, for simplicity)
            if (globalPrompt.contains("{day}")) {
                globalPrompt = globalPrompt.replace("{day}", formattedDay);
            }
            if (globalPrompt.contains("{time}")) {
                globalPrompt = globalPrompt.replace("{time}", formattedTime);
            }
            if (characterPersonality.contains("{day}")) {
                characterPersonality = characterPersonality.replace("{day}", formattedDay);
            }
            if (characterPersonality.contains("{time}")) {
                characterPersonality = characterPersonality.replace("{time}", formattedTime);
            }

            // 5. Build the final prompt
            String finalSystemPrompt = globalPrompt + "\n" + characterPersonality;
            // -------------------------------------------

            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            // --- THIS LOOP IS BACK TO NORMAL ---
            // It does not add timestamps to each message
            for (Message msg : messageHistory) {
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
            }
            // ---------------------------------

            // --- THIS BLOCK IS BACK TO NORMAL ---
            // It does not add a timestamp to the new message
            if (!isRegeneration && !content.isEmpty()) {
                requestMessages.add(new RequestMessage("user", content));
            }
            // ----------------------------------

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
                    } else {
                        Log.e("API_ERROR", "Response not successful: " + response.code() + " - " + response.message());
                        // Optional: Save an error message to display in the chat
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