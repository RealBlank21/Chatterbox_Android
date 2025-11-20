package com.example.testing;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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
import java.io.FileInputStream;
import java.io.IOException;
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

    public void createConversationAndSendMessage(String content, String imagePath, User user, Character character) {
        String title = "New Chat";
        if (content != null && !content.trim().isEmpty()) {
            String cleanContent = content.trim().replace("\n", " ");
            if (cleanContent.length() > 50) {
                title = cleanContent.substring(0, 50) + "...";
            } else {
                title = cleanContent;
            }
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
                messageRepository.delete(messageToRegenerate);
            }
            triggerApiCall("", null, messageToRegenerate.getConversationId(), user, character, true);
        });
    }

    public void update(Message message) {
        messageRepository.update(message);
    }

    private void triggerApiCall(String content, String imagePath, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) return;

        if (!isRegeneration) {
            Message userMessage = new Message("user", content, conversationId, imagePath);
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

            if (!isRegeneration) {
                boolean alreadyInHistory = false;
                if (!messageHistory.isEmpty()) {
                    Message lastMsg = messageHistory.get(messageHistory.size() - 1);
                    if ("user".equals(lastMsg.getRole())
                            && TextUtils.equals(content, lastMsg.getContent())
                            && TextUtils.equals(imagePath, lastMsg.getImagePath())) {
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

                    if (imagePath != null) {
                        String base64Image = encodeImageToBase64(imagePath);
                        if (base64Image != null) {
                            List<ContentPart> parts = new ArrayList<>();
                            if (!TextUtils.isEmpty(content)) {
                                parts.add(new ContentPart("text", content));
                            }
                            String mimeType = "image/jpeg";
                            if (imagePath.endsWith(".png")) mimeType = "image/png";
                            if (imagePath.endsWith(".webp")) mimeType = "image/webp";

                            parts.add(new ContentPart("image_url", new ImageUrl("data:" + mimeType + ";base64," + base64Image)));
                            requestMessages.add(new RequestMessage("user", parts));
                        } else {
                            requestMessages.add(new RequestMessage("user", content + " [Image Upload Failed]"));
                        }
                    } else {
                        requestMessages.add(new RequestMessage("user", content));
                    }
                }
            }

            String model = character.getModel();
            if (TextUtils.isEmpty(model)) model = user.getPreferredModel();
            if (TextUtils.isEmpty(model)) return;

            ApiRequest apiRequest = new ApiRequest(model, requestMessages, character.getTemperature(), character.getMaxTokens());
            String apiKey = "Bearer " + user.getApiKey();

            Call<ChatCompletionResponse> call = apiService.getChatCompletion(apiKey, apiRequest);

            call.enqueue(new Callback<ChatCompletionResponse>() {
                @Override
                public void onResponse(Call<ChatCompletionResponse> call, Response<ChatCompletionResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                        ChatCompletionResponse fullResponse = response.body();

                        String aiResponseContent = fullResponse.getChoices().get(0).getMessage().getContent();

                        ChatCompletionResponse.Usage usage = fullResponse.getUsage();
                        String finishReason = fullResponse.getChoices().get(0).getFinishReason();

                        if ("length".equals(finishReason)) {
                            aiResponseContent += "\n\n[Message cut off due to token limit]";
                        }

                        Message aiMessage = new Message("assistant", aiResponseContent, conversationId);

                        // --- SAVE DETAILED TOKEN USAGE ---
                        if (usage != null) {
                            aiMessage.setTokenCount(usage.getTotalTokens());
                            aiMessage.setPromptTokens(usage.getPromptTokens());
                            aiMessage.setCompletionTokens(usage.getCompletionTokens());
                        }
                        if (finishReason != null) {
                            aiMessage.setFinishReason(finishReason);
                        }

                        messageRepository.insert(aiMessage);
                        conversationRepository.updateLastUpdated(conversationId, System.currentTimeMillis());
                    } else {
                        Message errorMessage = new Message("assistant", "Error: " + response.code() + " " + response.message(), conversationId);
                        messageRepository.insert(errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ChatCompletionResponse> call, Throwable t) {
                    Message errorMessage = new Message("assistant", "Error: API call failed. " + t.getMessage(), conversationId);
                    messageRepository.insert(errorMessage);
                }
            });
        });
    }

    private String encodeImageToBase64(String imagePath) {
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