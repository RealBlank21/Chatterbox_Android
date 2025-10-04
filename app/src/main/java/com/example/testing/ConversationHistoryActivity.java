package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ConversationHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConversationHistoryViewModel viewModel;
    private ConversationHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_history);

        setTitle("Conversation History");

        // --- Setup RecyclerView ---
        recyclerView = findViewById(R.id.recycler_view_conversation_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // --- Setup Adapter ---
        adapter = new ConversationHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // --- Setup ViewModel and Observer ---
        viewModel = new ViewModelProvider(this).get(ConversationHistoryViewModel.class);
        viewModel.getAllConversations().observe(this, conversationWithCharacters -> {
            adapter.setConversations(conversationWithCharacters);
        });

        // --- Setup Click Listener ---
        adapter.setOnItemClickListener(conversation -> {
            Intent intent = new Intent(ConversationHistoryActivity.this, ConversationActivity.class);
            // Pass the IDs needed to resume the conversation
            intent.putExtra("CONVERSATION_ID", conversation.getId());

            // The character ID is stored in the conversation object
            intent.putExtra("CHARACTER_ID", conversation.getCharacterId());

            startActivity(intent);
        });
    }
}