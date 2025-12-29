package com.example.testing;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.List;
import java.util.Objects;

public class CharacterViewModel extends AndroidViewModel {

    private final CharacterRepository characterRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ScenarioRepository scenarioRepository;
    private final PersonaDao personaDao;

    private final MutableLiveData<Boolean> showHiddenInput = new MutableLiveData<>(false);
    private final MutableLiveData<String> searchQueryInput = new MutableLiveData<>("");
    private final MutableLiveData<String> tagFilterInput = new MutableLiveData<>("");

    private final LiveData<List<Character>> displayedCharacters;
    private final LiveData<List<String>> allTags;

    private final MutableLiveData<ConversationInfo> navigateToConversation = new MutableLiveData<>();

    public CharacterViewModel(Application application) {
        super(application);
        characterRepository = CharacterRepository.getInstance(application);
        conversationRepository = ConversationRepository.getInstance(application);
        messageRepository = MessageRepository.getInstance(application);
        scenarioRepository = ScenarioRepository.getInstance(application);
        personaDao = AppDatabase.getInstance(application).personaDao();

        MediatorLiveData<FilterParams> filterParams = new MediatorLiveData<>();
        filterParams.setValue(new FilterParams(false, "", ""));

        filterParams.addSource(showHiddenInput, v ->
                filterParams.setValue(new FilterParams(v, searchQueryInput.getValue(), tagFilterInput.getValue())));
        filterParams.addSource(searchQueryInput, v ->
                filterParams.setValue(new FilterParams(showHiddenInput.getValue(), v, tagFilterInput.getValue())));
        filterParams.addSource(tagFilterInput, v ->
                filterParams.setValue(new FilterParams(showHiddenInput.getValue(), searchQueryInput.getValue(), v)));

        displayedCharacters = Transformations.switchMap(filterParams, params ->
                characterRepository.getFilteredCharacters(params.isHidden, params.searchQuery, params.tagFilter));

        allTags = characterRepository.getAllTags();
    }

    public LiveData<List<Scenario>> getScenariosForCharacter(int characterId) {
        return scenarioRepository.getScenariosForCharacter(characterId);
    }

    public void insertScenario(Scenario scenario) {
        scenarioRepository.insert(scenario);
    }

    public void updateScenario(Scenario scenario) {
        scenarioRepository.update(scenario);
    }

    public void deleteScenario(Scenario scenario) {
        scenarioRepository.delete(scenario);
    }

    public LiveData<List<Persona>> getAllPersonas() {
        return personaDao.getAllPersonas();
    }

    private static class FilterParams {
        final boolean isHidden;
        final String searchQuery;
        final String tagFilter;

        FilterParams(boolean isHidden, String searchQuery, String tagFilter) {
            this.isHidden = isHidden;
            this.searchQuery = searchQuery == null ? "" : searchQuery;
            this.tagFilter = tagFilter == null ? "" : tagFilter;
        }
    }

    public static class ConversationInfo {
        public final int characterId;
        public final int conversationId;

        public ConversationInfo(int characterId, int conversationId) {
            this.characterId = characterId;
            this.conversationId = conversationId;
        }
    }

    public LiveData<List<Character>> getDisplayedCharacters() { return displayedCharacters; }
    public LiveData<List<String>> getAllTags() { return allTags; }
    public LiveData<Character> getCharacterById(int id) { return characterRepository.getCharacterById(id); }
    public LiveData<ConversationInfo> getNavigateToConversation() { return navigateToConversation; }

    public void setShowHidden(boolean show) {
        if (!Objects.equals(showHiddenInput.getValue(), show)) {
            showHiddenInput.setValue(show);
        }
    }

    public boolean isShowingHidden() {
        return Boolean.TRUE.equals(showHiddenInput.getValue());
    }

    public void setSearchQuery(String query) {
        if (!Objects.equals(searchQueryInput.getValue(), query)) {
            searchQueryInput.setValue(query);
        }
    }

    public void setTagFilter(String tag) {
        if (!Objects.equals(tagFilterInput.getValue(), tag)) {
            tagFilterInput.setValue(tag);
        }
    }

    public String getCurrentTagFilter() {
        return tagFilterInput.getValue();
    }

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