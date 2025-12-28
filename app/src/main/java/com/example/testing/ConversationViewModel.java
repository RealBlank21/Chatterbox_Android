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
import com.example.testing.network.request.RequestMessage;
import com.example.testing.network.response.ChatCompletionResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class ConversationViewModel extends AndroidViewModel {

    private final MessageRepository messageRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final PersonaDao personaDao;
    private final ApiService apiService;
    private final ExecutorService executorService;

    private final SimpleDateFormat dayFormatter;
    private final SimpleDateFormat timeFormatter;

    private final MutableLiveData<Integer> conversationIdInput = new MutableLiveData<>();
    private final LiveData<List<Message>> messages;

    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;
    private LiveData<Persona> activePersona;

    // --- NEW: Loading State ---
    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        messageRepository = MessageRepository.getInstance(application);
        characterRepository = CharacterRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        conversationRepository = ConversationRepository.getInstance(application);
        personaDao = AppDatabase.getInstance(application).personaDao();

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
        executorService.shutdown();
    }

    public void loadData(int characterId, int conversationId) {
        this.currentCharacter = characterRepository.getCharacterById(characterId);
        this.currentUser = userRepository.getUser();

        // Initialize Active Persona LiveData
        this.activePersona = Transformations.switchMap(currentUser, user -> {
            if (user == null || user.getCurrentPersonaId() == -1) {
                return new MutableLiveData<>(null);
            }
            return personaDao.getPersonaByIdLive(user.getCurrentPersonaId());
        });

        conversationIdInput.setValue(conversationId);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Persona> getActivePersona() { return activePersona; }
    public LiveData<Integer> getConversationId() { return conversationIdInput; }
    public LiveData<Boolean> getIsGenerating() { return isGenerating; } // Expose state

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

    public void continueConversation(int conversationId, User user, Character character) {
        if (conversationId == -1) {
            Conversation newConversation = new Conversation(character.getId(), "New Chat");
            conversationRepository.insert(newConversation, newId -> {
                int id = newId.intValue();
                if (!TextUtils.isEmpty(character.getFirstMessage())) {
                    Message greeting = new Message("assistant", character.getFirstMessage(), id);
                    messageRepository.insert(greeting);
                }
                conversationIdInput.postValue(id);
                triggerApiCall("", null, id, user, character, true);
            });
        } else {
            triggerApiCall("", null, conversationId, user, character, true);
        }
    }

    private void sendMessageWithId(String content, String imagePath, int conversationId, User user, Character character) {
        triggerApiCall(content, imagePath, conversationId, user, character, false);
    }

    public void regenerateResponse(Message messageToRegenerate, User user, Character character) {
        executorService.execute(() -> {
            if (messageToRegenerate != null) {
                messageRepository.deleteSync(messageToRegenerate);
            }
            triggerApiCall("", null, messageToRegenerate.getConversationId(), user, character, true);
        });
    }

    public void deleteMessage(Message message) {
        if (message != null) {
            messageRepository.delete(message);
        }
    }

    public void update(Message message) {
        messageRepository.update(message);
    }

    private void triggerApiCall(String content, String imagePath, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) return;

        executorService.execute(() -> {
            // 1. Set Loading State to TRUE
            isGenerating.postValue(true);

            try {
                if (!isRegeneration) {
                    Message userMessage = new Message("user", content, conversationId, imagePath);
                    messageRepository.insertSync(userMessage);
                    conversationRepository.updateLastUpdatedSync(conversationId, System.currentTimeMillis());
                }

                List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);
                List<RequestMessage> requestMessages = new ArrayList<>();

                long creationTimestamp = !messageHistory.isEmpty() ? messageHistory.get(0).getTimestamp() : System.currentTimeMillis();
                Date creationDate = new Date(creationTimestamp);
                String formattedDay = dayFormatter.format(creationDate);
                String formattedTime = timeFormatter.format(creationDate);

                int limit = character.getContextLimit() != null && character.getContextLimit() > 0
                        ? character.getContextLimit() : user.getDefaultContextLimit();

                List<Message> messagesToSend = messageHistory;
                if (limit > 0) {
                    int keepCount = limit * 2;
                    if (messageHistory.size() > keepCount) {
                        messagesToSend = messageHistory.subList(messageHistory.size() - keepCount, messageHistory.size());
                    }
                }

                String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
                String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";

                // --- Append Persona Info ---
                StringBuilder personaPromptBuilder = new StringBuilder();
                if (user.getCurrentPersonaId() != -1) {
                    Persona persona = personaDao.getPersonaById(user.getCurrentPersonaId());
                    if (persona != null) {
                        personaPromptBuilder.append("User Persona:\n");
                        if (persona.getName() != null && !persona.getName().isEmpty()) {
                            personaPromptBuilder.append("Name: ").append(persona.getName()).append("\n");
                        }
                        if (persona.getDescription() != null && !persona.getDescription().isEmpty()) {
                            personaPromptBuilder.append("Description: ").append(persona.getDescription()).append("\n");
                        }
                        personaPromptBuilder.append("\n");
                    }
                }
                String personaPrompt = personaPromptBuilder.toString();
                // ---------------------------

                globalPrompt = globalPrompt.replace("{day}", formattedDay).replace("{time}", formattedTime);
                characterPersonality = characterPersonality.replace("{day}", formattedDay).replace("{time}", formattedTime);

                String finalSystemPrompt = globalPrompt + "\n" + personaPrompt + characterPersonality;
                if (character.isTimeAware()) {
                    finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
                }
                if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
                    requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
                }

                for (Message msg : messagesToSend) {
                    if (character.isTimeAware() && "user".equals(msg.getRole())) {
                        Date msgDate = new Date(msg.getTimestamp());
                        String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                        requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
                    }
                    requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
                }

                if (!messagesToSend.isEmpty()) {
                    Message lastMessage = messagesToSend.get(messagesToSend.size() - 1);
                    if ("assistant".equals(lastMessage.getRole())) {
                        requestMessages.add(new RequestMessage("system", "Continue from where you stopped."));
                    }
                }

                String model = !TextUtils.isEmpty(character.getModel()) ? character.getModel() : user.getPreferredModel();
                if (TextUtils.isEmpty(model)) return;

                String apiKey = "Bearer " + user.getApiKey();

                ApiRequest apiRequest = new ApiRequest(model, requestMessages, character.getTemperature(), character.getMaxTokens(), true);
                Call<ResponseBody> call = apiService.getChatCompletionStream(apiKey, apiRequest);

                Message aiMessage = new Message("assistant", "", conversationId);
                messageRepository.insertSync(aiMessage);

                List<Message> updatedHistory = messageRepository.getMessagesForConversationSync(conversationId);
                Message currentAiMessage = updatedHistory.get(updatedHistory.size() - 1);

                try {
                    Response<ResponseBody> response = call.execute();
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
                                    ChatCompletionResponse chunk = new com.google.gson.Gson().fromJson(jsonPart, ChatCompletionResponse.class);

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
                                            conversationRepository.updateLastUpdatedSync(conversationId, System.currentTimeMillis());
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
                } catch (IOException e) {
                    currentAiMessage.setContent("Error: Stream interrupted. " + e.getMessage());
                    messageRepository.updateSync(currentAiMessage);
                }

            } finally {
                // 2. Set Loading State to FALSE (Always runs)
                isGenerating.postValue(false);
            }
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
            return Base64.encodeToString(byteArray , Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}