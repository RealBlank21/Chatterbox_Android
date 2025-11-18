package com.example.testing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

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

        if (characterId == -1) {
            Toast.makeText(this, "Error: Invalid Character ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        buttonSend.setEnabled(false);

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);

        // Load data (fetches char/user, prepares messages LiveData)
        conversationViewModel.loadData(characterId, conversationId);

        // Observe Conversation ID to handle creation
        conversationViewModel.getConversationId().observe(this, id -> {
            this.conversationId = id;
        });

        conversationViewModel.getCurrentCharacter().observe(this, character -> {
            if (character != null) {
                this.currentCharacter = character;
                setTitle(character.getName());
                checkIfReadyToSend();

                // --- NEW: Show transient greeting if new conversation ---
                if (conversationId == -1 && !TextUtils.isEmpty(character.getFirstMessage())) {
                    List<Message> transientList = new ArrayList<>();
                    transientList.add(new Message("assistant", character.getFirstMessage(), -1));
                    messageAdapter.setMessages(transientList);
                }
            }
        });

        conversationViewModel.getCurrentUser().observe(this, user -> {
            if (user != null && !TextUtils.isEmpty(user.getApiKey())) {
                this.currentUser = user;
                checkIfReadyToSend();
            } else {
                buttonSend.setEnabled(false);
            }
        });

        conversationViewModel.getMessages().observe(this, messages -> {
            // If we are in "new chat" mode (-1), we prefer the transient greeting.
            // But once the DB has data (messages not empty), we show that.
            if (messages != null && !messages.isEmpty()) {
                messageAdapter.setMessages(messages);
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });

        messageAdapter.setOnMessageLongClickListener((message, anchorView, position) -> {
            if ("assistant".equals(message.getRole())) {
                showCharacterMessageOptions(message, anchorView, position);
            } else {
                showUserMessageOptions(message, anchorView, position);
            }
        });

        messageAdapter.setOnMessageEditListener(message -> {
            // Can only update if saved in DB
            if (conversationId != -1) {
                conversationViewModel.update(message);
            }
        });

        buttonSend.setOnClickListener(v -> sendMessage());
    }

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
            if (conversationId == -1) {
                // Create first, then send
                conversationViewModel.createConversationAndSendMessage(messageContent, currentUser, currentCharacter);
            } else {
                // Normal send
                conversationViewModel.sendMessage(messageContent, conversationId, currentUser, currentCharacter);
            }
            editTextMessage.setText("");
        }
    }

    private void showCharacterMessageOptions(Message message, View anchorView, int position) {
        // Prevent actions on transient message (id 0/-1)
        if (conversationId == -1) return;

        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_regenerate) {
                conversationViewModel.regenerateLastResponse(conversationId, currentUser, currentCharacter);
                return true;
            } else if (itemId == R.id.option_copy_message) {
                copyMessageToClipboard(message);
                return true;
            } else if (itemId == R.id.option_edit_message) {
                messageAdapter.setEditingPosition(position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showUserMessageOptions(Message message, View anchorView, int position) {
        if (conversationId == -1) return;

        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());
        popup.getMenu().findItem(R.id.option_regenerate).setVisible(false);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_copy_message) {
                copyMessageToClipboard(message);
                return true;
            } else if (itemId == R.id.option_edit_message) {
                messageAdapter.setEditingPosition(position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void copyMessageToClipboard(Message message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Chat Message", message.getContent());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}