package com.example.testing;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class ConversationHistoryViewModel extends AndroidViewModel {

    private final ConversationRepository repository;
    private final LiveData<List<ConversationWithCharacter>> allConversations; // Change the type here

    public ConversationHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new ConversationRepository(application);
        allConversations = repository.getAllConversationsWithCharacter(); // Call the new method
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversations() { // Change the return type here
        return allConversations;
    }
}