package com.example.testing;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;

public class CharacterViewModel extends AndroidViewModel {

    private final CharacterRepository characterRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LiveData<List<Character>> allCharacters;

    private final MutableLiveData<ConversationInfo> navigateToConversation = new MutableLiveData<>();

    public CharacterViewModel(Application application) {
        super(application);
        characterRepository = new CharacterRepository(application);
        conversationRepository = new ConversationRepository(application);
        messageRepository = new MessageRepository(application);
        allCharacters = characterRepository.getAllCharacters();
    }

    // --- THIS IS THE FIX ---
    // The constructor was missing from this inner class.
    public static class ConversationInfo {
        public final int characterId;
        public final int conversationId;

        public ConversationInfo(int characterId, int conversationId) {
            this.characterId = characterId;
            this.conversationId = conversationId;
        }
    }
    // -----------------------

    public LiveData<List<Character>> getAllCharacters() { return allCharacters; }
    public LiveData<Character> getCharacterById(int id) { return characterRepository.getCharacterById(id); }
    public LiveData<ConversationInfo> getNavigateToConversation() { return navigateToConversation; }

    public void insert(Character character) { characterRepository.insert(character); }
    public void update(Character character) { characterRepository.update(character); }
    public void delete(Character character) { characterRepository.delete(character); }

    public void startNewConversation(Character character) {
        Conversation newConversation = new Conversation(character.getId(), "New Chat (" + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date()) + ")");
        conversationRepository.insert(newConversation, newId -> {
            String firstMessageContent = character.getFirstMessage();
            if (firstMessageContent != null && !firstMessageContent.trim().isEmpty()) {
                Message firstMessage = new Message("assistant", firstMessageContent, newId.intValue());
                messageRepository.insert(firstMessage);
            }
            navigateToConversation.postValue(new ConversationInfo(character.getId(), newId.intValue()));
        });
    }

    public void doneNavigating() {
        navigateToConversation.setValue(null);
    }
}