package com.example.testing;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class ConversationHistoryViewModel extends AndroidViewModel {

    private final ConversationRepository repository;
    private final LiveData<List<ConversationWithCharacter>> allConversations;

    public ConversationHistoryViewModel(@NonNull Application application) {
        super(application);
        // FIX: Use getInstance()
        repository = ConversationRepository.getInstance(application);
        allConversations = repository.getAllConversationsWithCharacter();
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversations() {
        return allConversations;
    }

    public void deleteConversations(List<Integer> conversationIds) {
        repository.deleteConversations(conversationIds);
    }
}