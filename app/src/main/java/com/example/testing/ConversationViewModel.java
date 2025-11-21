package com.example.testing;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
import com.example.testing.network.request.ApiRequest;
import com.example.testing.network.request.ContentPart;
import com.example.testing.network.request.ImageUrl;
import com.example.testing.network.request.RequestMessage;
import com.example.testing.network.response.ChatCompletionResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private final MutableLiveData<Integer> conversationIdInput = new MutableLiveData<>();
    private final LiveData<List<Message>> messages;

    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        // Use Singletons to prevent thread leaks
        messageRepository = MessageRepository.getInstance(application);
        characterRepository = CharacterRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        conversationRepository = ConversationRepository.getInstance(application);

        apiService = ApiClient.getClient().create(ApiService.class);
        executorService = Executors.newSingleThreadExecutor();

        dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

        messages = Transformations.switchMap(conversationIdInput, id -> {
            if (id == -1) {
                return new MutableLiveData<>(new ArrayList<>());
            } else {
                return messageRepository.getMessagesForConversation(id);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown(); // Prevent memory/thread leaks
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

    public void createConversationAndSendMessage(String content, String imagePath, User user, Character character) {
        String title = "New Chat";
        if (content != null && !content.trim().isEmpty()) {
            String cleanContent = content.trim().replace("\n", " ");
            title = cleanContent.length() > 50 ? cleanContent.substring(0, 50) + "..." : cleanContent;
        } else if (imagePath != null) {
            title = "Image sent";
        }

        Conversation newConversation = new Conversation(character.getId(), title);

        conversationRepository.insert(newConversation, newId -> {
            int id = newId.intValue();
            if (!TextUtils.isEmpty(character.getFirstMessage())) {
                Message greeting = new Message("assistant", character.getFirstMessage(), id);
                messageRepository.insert(greeting);
            }
            conversationIdInput.postValue(id);
            sendMessageWithId(content, imagePath, id, user, character);
        });
    }

    public void sendMessage(String content, String imagePath, int conversationId, User user, Character character) {
        sendMessageWithId(content, imagePath, conversationId, user, character);
    }

    private void sendMessageWithId(String content, String imagePath, int conversationId, User user, Character character) {
        triggerApiCall(content, imagePath, conversationId, user, character, false);
    }

    public void regenerateResponse(Message messageToRegenerate, User user, Character character) {
        executorService.execute(() -> {
            if (messageToRegenerate != null) {
                // Use Sync delete to ensure it is gone BEFORE we read history for the API call
                messageRepository.deleteSync(messageToRegenerate);
            }
            // We call triggerApiCall, but since we are already in the executor,
            // and triggerApiCall also submits to the executor, it will run sequentially.
            triggerApiCall("", null, messageToRegenerate.getConversationId(), user, character, true);
        });
    }

    public void update(Message message) {
        messageRepository.update(message);
    }

    private void triggerApiCall(String content, String imagePath, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) return;

        executorService.execute(() -> {
            // 1. Insert User Message Synchronously on this thread (if not regenerating)
            // This guarantees the DB has the message before we read the history below.
            if (!isRegeneration) {
                Message userMessage = new Message("user", content, conversationId, imagePath);
                messageRepository.insertSync(userMessage);
                conversationRepository.updateLastUpdatedSync(conversationId, System.currentTimeMillis());
            }

            // 2. Fetch FULL history
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
            List<RequestMessage> requestMessages = new ArrayList<>();

            // 3. Time/Date Calculation
            long creationTimestamp = !messageHistory.isEmpty() ? messageHistory.get(0).getTimestamp() : System.currentTimeMillis();
            Date creationDate = new Date(creationTimestamp);
            String formattedDay = dayFormatter.format(creationDate);
            String formattedTime = timeFormatter.format(creationDate);

            // 4. Sliding Window Logic
            int limit = character.getContextLimit() != null && character.getContextLimit() > 0
                    ? character.getContextLimit() : user.getDefaultContextLimit();

            List<Message> messagesToSend = messageHistory;
            if (limit > 0) {
                int keepCount = limit * 2;
                if (messageHistory.size() > keepCount) {
                    messagesToSend = messageHistory.subList(messageHistory.size() - keepCount, messageHistory.size());
                }
            }

            // 5. Construct System Prompt
            String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
            String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";

            globalPrompt = globalPrompt.replace("{day}", formattedDay).replace("{time}", formattedTime);
            characterPersonality = characterPersonality.replace("{day}", formattedDay).replace("{time}", formattedTime);

            String finalSystemPrompt = globalPrompt + "\n" + characterPersonality;
            if (character.isTimeAware()) {
                finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
            }
            if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
            }

            // 6. Add History
            for (Message msg : messagesToSend) {
                if (character.isTimeAware() && "user".equals(msg.getRole())) {
                    Date msgDate = new Date(msg.getTimestamp());
                    String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                    requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
                }
                requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
            }

            // Note: We no longer need the "alreadyInHistory" check here because we
            // guaranteed insertion via insertSync at the start of this thread.

            // 7. API Call Construction
            String model = !TextUtils.isEmpty(character.getModel()) ? character.getModel() : user.getPreferredModel();
            if (TextUtils.isEmpty(model)) return;

            ApiRequest apiRequest = new ApiRequest(model, requestMessages, character.getTemperature(), character.getMaxTokens());
            String apiKey = "Bearer " + user.getApiKey();

            Call<ChatCompletionResponse> call = apiService.getChatCompletion(apiKey, apiRequest);

            // Retrofit callback runs on Main Thread usually, or background depending on config.
            // Safe to leave as is, but we must handle the DB insert off-thread.
            call.enqueue(new Callback<ChatCompletionResponse>() {
                @Override
                public void onResponse(Call<ChatCompletionResponse> call, Response<ChatCompletionResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                        ChatCompletionResponse fullResponse = response.body();
                        String aiResponseContent = fullResponse.getChoices().get(0).getMessage().getContent();

                        // Handle potential empty response from API
                        if (TextUtils.isEmpty(aiResponseContent)) {
                            aiResponseContent = "..."; // Placeholder or handle error
                        }

                        ChatCompletionResponse.Usage usage = fullResponse.getUsage();
                        String finishReason = fullResponse.getChoices().get(0).getFinishReason();

                        if ("length".equals(finishReason)) aiResponseContent += "\n\n[Message cut off]";

                        Message aiMessage = new Message("assistant", aiResponseContent, conversationId);
                        if (usage != null) {
                            aiMessage.setTokenCount(usage.getTotalTokens());
                            aiMessage.setPromptTokens(usage.getPromptTokens());
                            aiMessage.setCompletionTokens(usage.getCompletionTokens());
                        }
                        if (finishReason != null) aiMessage.setFinishReason(finishReason);

                        messageRepository.insert(aiMessage); // Async is fine here
                        conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
                    } else {
                        messageRepository.insert(new Message("assistant", "Error: " + response.code() + " " + response.message(), conversationId));
                    }
                }

                @Override
                public void onFailure(Call<ChatCompletionResponse> call, Throwable t) {
                    messageRepository.insert(new Message("assistant", "Error: API call failed. " + t.getMessage(), conversationId));
                }
            });
        });
    }

    private String encodeImageToBase64(String imagePath) {
        // (Keep existing implementation)
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) return null;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) return null;
            int maxDimension = 1024;
            if (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension) {
                float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();
                int newWidth = maxDimension;
                int newHeight = maxDimension;
                if (bitmap.getWidth() > bitmap.getHeight()) {
                    newHeight = Math.round(maxDimension / aspectRatio);
                } else {
                    newWidth = Math.round(maxDimension * aspectRatio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}