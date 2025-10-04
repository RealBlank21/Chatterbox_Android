package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ConversationActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private Button buttonSend;

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
        // This is the correct conversation ID from the new flow
        conversationId = intent.getIntExtra("CONVERSATION_ID", -1);

        if (characterId == -1 || conversationId == -1) {
            Toast.makeText(this, "Error: Invalid Character or Conversation ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        buttonSend.setEnabled(false);

        RecyclerView recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(layoutManager);
        MessageAdapter messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        // --- THIS IS THE FIX ---
        // The ViewModel must be initialized BEFORE you use it.
        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        // -----------------------

        // Now that the ViewModel exists, we can call its methods.
        conversationViewModel.loadData(characterId, conversationId);

        // --- Observers ---
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
                Toast.makeText(this, "API Key not set. Please configure it in Settings.", Toast.LENGTH_LONG).show();
            }
        });

        conversationViewModel.getMessages().observe(this, messages -> {
            messageAdapter.setMessages(messages);
            if (messages.size() > 0) {
                layoutManager.scrollToPosition(messages.size() - 1);
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
            conversationViewModel.sendMessage(messageContent, conversationId, currentUser, currentCharacter);
            editTextMessage.setText("");
        }
    }
}