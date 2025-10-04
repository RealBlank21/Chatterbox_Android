package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ConversationActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;

    private ConversationViewModel conversationViewModel;
    private int conversationId = -1;

    private User currentUser;
    private Character currentCharacter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Intent intent = getIntent();
        int characterId = intent.getIntExtra("CHARACTER_ID", -1);
        conversationId = intent.getIntExtra("CONVERSATION_ID", -1);

        if (characterId == -1 || conversationId == -1) {
            Toast.makeText(this, "Error: Invalid IDs", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        buttonSend.setEnabled(false);

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);

        // --- THIS IS THE DEFINITIVE SETUP FOR A CHAT UI ---
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // This makes the list bottom-aligned
        recyclerViewMessages.setLayoutManager(layoutManager);
        // ----------------------------------------------------

        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        // --- ViewModel and Observers ---
        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.loadData(characterId, conversationId);

        conversationViewModel.getCurrentCharacter().observe(this, character -> {
            if (character != null) {
                this.currentCharacter = character;
                setTitle(character.getName());
                checkIfReadyToSend();
            }
        });

        conversationViewModel.getCurrentUser().observe(this, user -> {
            if (user != null && !TextUtils.isEmpty(user.getApiKey())) {
                this.currentUser = user;
                checkIfReadyToSend();
            } else {
                buttonSend.setEnabled(false);
                // We don't show a toast here to prevent it from showing every time
            }
        });

        // This observer now has one job: scroll to the bottom when the list size changes.
        conversationViewModel.getMessages().observe(this, messages -> {
            messageAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });

        messageAdapter.setOnMessageLongClickListener((message, anchorView) -> {
            // Check the role of the message
            if ("assistant".equals(message.getRole())) {
                showCharacterMessageOptions(message, anchorView);
            } else {
                // For now, we'll just show a toast for user messages
                Toast.makeText(this, "Long-pressed your message!", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    // ... (checkIfReadyToSend and sendMessage methods are unchanged)
    private void checkIfReadyToSend() {
        if (currentUser != null && currentCharacter != null) {
            buttonSend.setEnabled(true);
        }
    }

    private void sendMessage() {
        String messageContent = editTextMessage.getText().toString().trim();

        if (currentUser == null || TextUtils.isEmpty(currentUser.getApiKey())) {
            Toast.makeText(this, "API Key is missing. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(messageContent)) {
            conversationViewModel.sendMessage(messageContent, conversationId, currentUser, currentCharacter);
            editTextMessage.setText("");
        }
    }

    private void showCharacterMessageOptions(Message message, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_regenerate) {
                conversationViewModel.regenerateLastResponse(conversationId, currentUser, currentCharacter);
                return true;
            } else if (itemId == R.id.option_edit_message) {
                Toast.makeText(this, "Edit feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }
}