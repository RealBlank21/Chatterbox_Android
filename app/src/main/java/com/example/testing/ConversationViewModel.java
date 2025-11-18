package com.example.testing;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

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
    private final ConversationRepository conversationRepository;
    private final ApiService apiService;
    private final ExecutorService executorService;

    private final SimpleDateFormat dayFormatter;
    private final SimpleDateFormat timeFormatter;

    // Use SwitchMap for dynamic conversation ID
    private final MutableLiveData<Integer> conversationIdInput = new MutableLiveData<>();
    private final LiveData<List<Message>> messages;

    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
        characterRepository = new CharacterRepository(application);
        userRepository = new UserRepository(application);
        conversationRepository = new ConversationRepository(application);
        apiService = ApiClient.getClient().create(ApiService.class);
        executorService = Executors.newSingleThreadExecutor();

        dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

        // Setup SwitchMap
        messages = Transformations.switchMap(conversationIdInput, id -> {
            if (id == -1) {
                return new MutableLiveData<>(new ArrayList<>());
            } else {
                return messageRepository.getMessagesForConversation(id);
            }
        });
    }

    public void loadData(int characterId, int conversationId) {
        this.currentCharacter = characterRepository.getCharacterById(characterId);
        this.currentUser = userRepository.getUser();
        conversationIdInput.setValue(conversationId);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Integer> getConversationId() { return conversationIdInput; }

    // --- UPDATED: Create Conversation with Title from Content ---
    public void createConversationAndSendMessage(String content, User user, Character character) {
        // 1. Determine the title from the first message content
        String title = "New Chat";
        if (content != null && !content.trim().isEmpty()) {
            // Flatten newlines to spaces
            String cleanContent = content.trim().replace("\n", " ");
            // Truncate to 50 characters
            if (cleanContent.length() > 50) {
                title = cleanContent.substring(0, 50) + "...";
            } else {
                title = cleanContent;
            }
        }

        // 2. Create the conversation with the dynamic title
        Conversation newConversation = new Conversation(character.getId(), title);

        conversationRepository.insert(newConversation, newId -> {
            int id = newId.intValue();

            // Insert Greeting if exists
            if (!TextUtils.isEmpty(character.getFirstMessage())) {
                Message greeting = new Message("assistant", character.getFirstMessage(), id);
                messageRepository.insert(greeting);
            }

            // Update LiveData
            conversationIdInput.postValue(id);

            // Proceed with sending
            sendMessageWithId(content, id, user, character);
        });
    }

    public void sendMessage(String content, int conversationId, User user, Character character) {
        sendMessageWithId(content, conversationId, user, character);
    }

    private void sendMessageWithId(String content, int conversationId, User user, Character character) {
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
        if (user == null || character == null) return;

        if (!isRegeneration) {
            Message userMessage = new Message("user", content, conversationId);
            messageRepository.insert(userMessage);
            conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
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

            // --- NEW: Time Awareness - Initial System Prompt Injection ---
            if (character.isTimeAware()) {
                finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
            }

            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            for (Message msg : messageHistory) {
                // --- NEW: Time Awareness - Historical User Message Injection ---
                if (character.isTimeAware() && "user".equals(msg.getRole())) {
                    Date msgDate = new Date(msg.getTimestamp());
                    String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                    requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
                }
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
            }

            if (!isRegeneration && !content.isEmpty()) {
                // --- NEW: Time Awareness - Current User Message Injection ---
                if (character.isTimeAware()) {
                    long now = System.currentTimeMillis();
                    Date nowDate = new Date(now);
                    String nowTime = dayFormatter.format(nowDate) + " at " + timeFormatter.format(nowDate);
                    requestMessages.add(new RequestMessage("system", "Current time: " + nowTime));
                }
                requestMessages.add(new RequestMessage("user", content));
            }

            String model = character.getModel();
            if (TextUtils.isEmpty(model)) model = user.getPreferredModel();
            if (TextUtils.isEmpty(model)) return;

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
                        conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
                    } else {
                        Message errorMessage = new Message("assistant", "Error: " + response.code() + " " + response.message(), conversationId);
                        messageRepository.insert(errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Message errorMessage = new Message("assistant", "Error: API call failed. " + t.getMessage(), conversationId);
                    messageRepository.insert(errorMessage);
                }
            });
        });
    }
}