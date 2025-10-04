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
    }

    // --- MODIFIED loadData METHOD ---
    // The buggy observer logic has been removed.
    public void loadData(int characterId, int conversationId) {
        this.currentCharacter = characterRepository.getCharacterById(characterId);
        this.currentUser = userRepository.getUser();
        this.messages = messageRepository.getMessagesForConversation(conversationId);
    }

    // --- The rest of the file is IDENTICAL to before ---
    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }

    public void sendMessage(String content, int conversationId, User user, Character character) {
        // ... (This method is unchanged)
        if (user == null || character == null) {
            Log.e("VIEW_MODEL_ERROR", "User or Character is null.");
            return;
        }

        Message userMessage = new Message("user", content, conversationId);
        messageRepository.insert(userMessage);

        executorService.execute(() -> {
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            List<RequestMessage> requestMessages = new ArrayList<>();

            String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
            String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";
            String finalSystemPrompt = globalPrompt + "\n" + characterPersonality;

            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            for (Message msg : messageHistory) {
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
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
                    } else {
                        Log.e("API_ERROR", "Response not successful: " + response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("API_FAILURE", "API call failed: " + t.getMessage());
                }
            });
        });
    }
}