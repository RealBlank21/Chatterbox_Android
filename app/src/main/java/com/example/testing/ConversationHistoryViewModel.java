package com.example.testing;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

public class ConversationHistoryViewModel extends AndroidViewModel {

    private final ConversationRepository repository;

    // MutableLiveData to hold the growing list
    private final MutableLiveData<List<ConversationWithCharacter>> conversations = new MutableLiveData<>();
    // Local list to append data to
    private final List<ConversationWithCharacter> currentList = new ArrayList<>();

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public ConversationHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = ConversationRepository.getInstance(application);

        // Load initial page
        loadNextPage();
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversations() {
        return conversations;
    }

    public void loadNextPage() {
        if (isLoading || isLastPage) return;

        isLoading = true;
        int offset = currentPage * PAGE_SIZE;

        repository.loadConversationsPaged(PAGE_SIZE, offset, data -> {
            isLoading = false;
            if (data != null) {
                if (data.size() < PAGE_SIZE) {
                    isLastPage = true;
                }

                // If it's the first page, clear current list (in case of reload/refresh logic)
                if (currentPage == 0) {
                    currentList.clear();
                }

                currentList.addAll(data);
                // Post a copy to trigger observers
                conversations.setValue(new ArrayList<>(currentList));

                if (!data.isEmpty()) {
                    currentPage++;
                }
            }
        });
    }

    public void deleteConversations(List<Integer> conversationIds) {
        repository.deleteConversations(conversationIds);

        // Manually update the local list to reflect changes immediately
        // (Since we are not observing the DB directly anymore)
        boolean changed = false;
        // Using standard loop to be safe with older Java versions if needed, though removeIf is Java 8+
        List<ConversationWithCharacter> toRemove = new ArrayList<>();
        for (ConversationWithCharacter item : currentList) {
            if (conversationIds.contains(item.getId())) {
                toRemove.add(item);
            }
        }

        if (!toRemove.isEmpty()) {
            currentList.removeAll(toRemove);
            conversations.setValue(new ArrayList<>(currentList));
        }
    }
}