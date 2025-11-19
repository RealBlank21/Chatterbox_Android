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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
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
    private final LiveData<List<Message>> dbMessages; // Messages from DB

    // UI States
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // Holds the temporary content of the message currently being streamed
    private final MutableLiveData<String> streamingContent = new MutableLiveData<>(null);

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

        // SwitchMap for DB messages
        dbMessages = Transformations.switchMap(conversationIdInput, id -> {
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

    public LiveData<List<Message>> getDbMessages() { return dbMessages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Integer> getConversationId() { return conversationIdInput; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStreamingContent() { return streamingContent; }

    // --- Create Conversation with Title from Content ---
    public void createConversationAndSendMessage(String content, User user, Character character) {
        String title = "New Chat";
        if (content != null && !content.trim().isEmpty()) {
            String cleanContent = content.trim().replace("\n", " ");
            if (cleanContent.length() > 50) {
                title = cleanContent.substring(0, 50) + "...";
            } else {
                title = cleanContent;
            }
        }

        Conversation newConversation = new Conversation(character.getId(), title);

        conversationRepository.insert(newConversation, newId -> {
            int id = newId.intValue();
            if (!TextUtils.isEmpty(character.getFirstMessage())) {
                Message greeting = new Message("assistant", character.getFirstMessage(), id);
                messageRepository.insert(greeting);
            }
            conversationIdInput.postValue(id);
            sendMessageWithId(content, id, user, character);
        });
    }

    public void sendMessage(String content, int conversationId, User user, Character character) {
        sendMessageWithId(content, conversationId, user, character);
    }

    private void sendMessageWithId(String content, int conversationId, User user, Character character) {
        triggerApiCall(content, conversationId, user, character, false);
    }

    public void regenerateResponse(Message messageToRegenerate, User user, Character character) {
        executorService.execute(() -> {
            if (messageToRegenerate != null) {
                messageRepository.delete(messageToRegenerate);
            }
            triggerApiCall("", messageToRegenerate.getConversationId(), user, character, true);
        });
    }

    public void update(Message message) {
        messageRepository.update(message);
    }

    private void triggerApiCall(String content, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) return;

        isLoading.postValue(true); // Start loading

        // 1. Insert User Message if not regen
        if (!isRegeneration) {
            Message userMessage = new Message("user", content, conversationId);
            messageRepository.insert(userMessage);
            conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
        }

        executorService.execute(() -> {
            // 2. Fetch History
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            List<RequestMessage> requestMessages = new ArrayList<>();

            long creationTimestamp = !messageHistory.isEmpty() ? messageHistory.get(0).getTimestamp() : System.currentTimeMillis();
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

            if (character.isTimeAware()) {
                finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
            }

            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            for (Message msg : messageHistory) {
                if (character.isTimeAware() && "user".equals(msg.getRole())) {
                    Date msgDate = new Date(msg.getTimestamp());
                    String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                    requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
                }
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
            }

            // Handle Double Message Send Race Condition
            if (!isRegeneration && !content.isEmpty()) {
                boolean alreadyInHistory = false;
                if (!messageHistory.isEmpty()) {
                    Message lastMsg = messageHistory.get(messageHistory.size() - 1);
                    if ("user".equals(lastMsg.getRole()) && content.equals(lastMsg.getContent())) {
                        alreadyInHistory = true;
                    }
                }
                if (!alreadyInHistory) {
                    if (character.isTimeAware()) {
                        long now = System.currentTimeMillis();
                        Date nowDate = new Date(now);
                        String nowTime = dayFormatter.format(nowDate) + " at " + timeFormatter.format(nowDate);
                        requestMessages.add(new RequestMessage("system", "Current time: " + nowTime));
                    }
                    requestMessages.add(new RequestMessage("user", content));
                }
            }

            String model = character.getModel();
            if (TextUtils.isEmpty(model)) model = user.getPreferredModel();
            if (TextUtils.isEmpty(model)) {
                isLoading.postValue(false);
                return;
            }

            // Request Streaming
            ApiRequest apiRequest = new ApiRequest(model, requestMessages, character.getTemperature(), character.getMaxTokens(), true);
            String apiKey = "Bearer " + user.getApiKey();

            Call<ResponseBody> call = apiService.getChatCompletionStream(apiKey, apiRequest);

            try {
                Response<ResponseBody> response = call.execute(); // Execute synchronously in this background thread

                if (response.isSuccessful() && response.body() != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    String line;
                    StringBuilder fullContent = new StringBuilder();

                    // Start streaming UI
                    streamingContent.postValue("");

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;

                            try {
                                JSONObject json = new JSONObject(data);
                                JSONObject delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
                                if (delta.has("content")) {
                                    String contentChunk = delta.getString("content");
                                    fullContent.append(contentChunk);
                                    // Update UI with partial content
                                    streamingContent.postValue(fullContent.toString());
                                }
                            } catch (Exception e) {
                                Log.e("Stream", "Parsing error", e);
                            }
                        }
                    }

                    // Streaming finished: Save to DB
                    Message aiMessage = new Message("assistant", fullContent.toString(), conversationId);
                    messageRepository.insert(aiMessage);
                    conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());

                } else {
                    Message errorMessage = new Message("assistant", "Error: " + response.code() + " " + response.message(), conversationId);
                    messageRepository.insert(errorMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message errorMessage = new Message("assistant", "Error: Network request failed. " + e.getMessage(), conversationId);
                messageRepository.insert(errorMessage);
            } finally {
                // Reset states
                isLoading.postValue(false);
                streamingContent.postValue(null);
            }
        });
    }
}