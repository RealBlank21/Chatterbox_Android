package com.example.testing.ui.conversation;

import android.app.Application;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.testing.data.local.AppDatabase;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Message;
import com.example.testing.data.repository.CharacterRepository;
import com.example.testing.data.local.entity.Conversation;
import com.example.testing.data.repository.ConversationRepository;
import com.example.testing.data.repository.MessageRepository;
import com.example.testing.data.local.entity.Persona;
import com.example.testing.data.local.dao.PersonaDao;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.data.repository.ScenarioRepository;
import com.example.testing.data.local.entity.User;
import com.example.testing.data.repository.UserRepository;
import com.example.testing.data.remote.api.ApiClient;
import com.example.testing.data.remote.api.ApiService;
import com.example.testing.data.remote.request.ApiRequest;
import com.example.testing.data.remote.request.RequestMessage;
import com.example.testing.utils.ChatStreamHandler;
import com.example.testing.utils.ChatPromptGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class ConversationViewModel extends AndroidViewModel {

    private final MessageRepository messageRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ScenarioRepository scenarioRepository;
    private final PersonaDao personaDao;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final ChatPromptGenerator chatPromptGenerator;

    private final MutableLiveData<Integer> conversationIdInput = new MutableLiveData<>();
    private final LiveData<List<Message>> messages;

    private LiveData<Character> currentCharacter;
    private LiveData<User> currentUser;
    private LiveData<Persona> activePersona;

    private final LiveData<Conversation> currentConversation;
    private final LiveData<Scenario> conversationScenario;

    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        messageRepository = MessageRepository.getInstance(application);
        characterRepository = CharacterRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        conversationRepository = ConversationRepository.getInstance(application);
        scenarioRepository = ScenarioRepository.getInstance(application);
        personaDao = AppDatabase.getInstance(application).personaDao();

        apiService = ApiClient.getClient().create(ApiService.class);
        executorService = Executors.newSingleThreadExecutor();
        chatPromptGenerator = new ChatPromptGenerator(personaDao, scenarioRepository);

        messages = Transformations.switchMap(conversationIdInput, id -> {
            if (id == -1) {
                return new MutableLiveData<>(new ArrayList<>());
            } else {
                return messageRepository.getMessagesForConversation(id);
            }
        });

        currentConversation = Transformations.switchMap(conversationIdInput, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            } else {
                return conversationRepository.getConversationById(id);
            }
        });

        conversationScenario = Transformations.switchMap(currentConversation, conversation -> {
            if (conversation == null || conversation.getScenarioId() == null) {
                return new MutableLiveData<>(null);
            }
            return scenarioRepository.getScenarioByIdLive(conversation.getScenarioId());
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

        this.activePersona = Transformations.switchMap(currentUser, user -> {
            if (user == null || user.getCurrentPersonaId() == -1) {
                return new MutableLiveData<>(null);
            }
            return personaDao.getPersonaByIdLive(user.getCurrentPersonaId());
        });

        conversationIdInput.setValue(conversationId);
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<com.example.testing.data.local.entity.Character> getCurrentCharacter() { return currentCharacter; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Persona> getActivePersona() { return activePersona; }
    public LiveData<Integer> getConversationId() { return conversationIdInput; }
    public LiveData<Boolean> getIsGenerating() { return isGenerating; }
    public LiveData<Conversation> getCurrentConversation() { return currentConversation; }
    public LiveData<Scenario> getConversationScenario() { return conversationScenario; }

    public ScenarioRepository getScenarioRepository() { return scenarioRepository; }

    public LiveData<Scenario> getScenarioByIdLive(int id) {
        return scenarioRepository.getScenarioByIdLive(id);
    }

    public LiveData<Scenario> getDefaultScenarioLive(int characterId) {
        return scenarioRepository.getDefaultScenarioLive(characterId);
    }

    public void createConversationAndSendMessage(String content, String imagePath, User user, Character character, Integer personaId, Integer scenarioId) {
        executorService.execute(() -> {
            String title = "New Chat";
            if (content != null && !content.trim().isEmpty()) {
                String cleanContent = content.trim().replace("\n", " ");
                title = cleanContent.length() > 50 ? cleanContent.substring(0, 50) + "..." : cleanContent;
            }

            Conversation newConversation = new Conversation(character.getId(), title);
            String greetingText = character.getFirstMessage();

            if (scenarioId == null || scenarioId == -1) {
                Scenario defaultScenario = scenarioRepository.getDefaultScenarioSync(character.getId());
                if (defaultScenario != null) {
                    newConversation.setScenarioId(defaultScenario.getId());
                    if (!TextUtils.isEmpty(defaultScenario.getFirstMessage())) {
                        greetingText = defaultScenario.getFirstMessage();
                    }
                } else {
                    newConversation.setScenarioId(null);
                }
            } else {
                newConversation.setScenarioId(scenarioId);
                Scenario selected = scenarioRepository.getScenarioByIdSync(scenarioId);
                if (selected != null && !TextUtils.isEmpty(selected.getFirstMessage())) {
                    greetingText = selected.getFirstMessage();
                }
            }

            if (personaId != null && personaId != -1) {
                newConversation.setPersonaId(personaId);
            } else {
                newConversation.setPersonaId(null);
            }

            final String finalGreetingText = greetingText;

            conversationRepository.insert(newConversation, newId -> {
                int id = newId.intValue();

                conversationIdInput.postValue(id);

                if (!TextUtils.isEmpty(finalGreetingText)) {
                    executorService.execute(() -> {
                        Message greeting = new Message("assistant", finalGreetingText, id);
                        messageRepository.insertSync(greeting);
                    });
                }

                sendMessageWithId(content, id, user, character);
            });
        });
    }

    public void createConversationAndSendMessage(String content, String imagePath, User user, com.example.testing.data.local.entity.Character character) {
        createConversationAndSendMessage(content, imagePath, user, character, null, null);
    }

    public void sendMessage(String content, String imagePath, int conversationId, User user, Character character) {
        sendMessageWithId(content, conversationId, user, character);
    }

    public void continueConversation(int conversationId, User user, Character character) {
        if (conversationId == -1) {
            createConversationAndSendMessage("", null, user, character, null, null);
        } else {
            triggerApiCall("", conversationId, user, character, true);
        }
    }

    private void sendMessageWithId(String content, int conversationId, User user, Character character) {
        triggerApiCall(content, conversationId, user, character, false);
    }

    public void regenerateResponse(Message messageToRegenerate, User user, Character character) {
        executorService.execute(() -> {
            if (messageToRegenerate != null) {
                messageRepository.deleteSync(messageToRegenerate);
            }
            triggerApiCall("", messageToRegenerate.getConversationId(), user, character, true);
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

    public void getDebugConversationHistory(int conversationId, User user, Character character, Consumer<String> callback) {
        executorService.execute(() -> {
            Conversation conversation = conversationRepository.getConversationByIdSync(conversationId);
            List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);

            List<RequestMessage> requestMessages = chatPromptGenerator.buildApiRequestMessages(conversation, user, character, messageHistory);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(requestMessages);

            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.accept(jsonOutput));
            }
        });
    }

    private void triggerApiCall(String content, int conversationId, User user, Character character, boolean isRegeneration) {
        if (user == null || character == null) return;

        executorService.execute(() -> {
            isGenerating.postValue(true);

            try {
                if (!isRegeneration) {
                    Message userMessage = new Message("user", content, conversationId);
                    messageRepository.insertSync(userMessage);
                    conversationRepository.updateLastUpdatedSync(conversationId, System.currentTimeMillis());
                }

                Conversation conversation = conversationRepository.getConversationByIdSync(conversationId);
                List<Message> messageHistory = messageRepository.getMessagesForConversationSync(conversationId);

                List<RequestMessage> requestMessages = chatPromptGenerator.buildApiRequestMessages(conversation, user, character, messageHistory);

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
                    ChatStreamHandler.handleStream(response, currentAiMessage, messageRepository, conversationRepository);
                } catch (IOException e) {
                    currentAiMessage.setContent("Error: Stream interrupted. " + e.getMessage());
                    messageRepository.updateSync(currentAiMessage);
                }

            } finally {
                isGenerating.postValue(false);
            }
        });
    }
}