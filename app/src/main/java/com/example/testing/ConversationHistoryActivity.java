package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ConversationHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConversationHistoryViewModel viewModel;
    private ConversationHistoryAdapter adapter;

    private LinearLayout layoutDeleteControls;
    private Button buttonSelectAll;
    private Button buttonDeleteSelected;
    private MenuItem deleteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_history);

        setTitle("Conversation History");

        recyclerView = findViewById(R.id.recycler_view_conversation_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Controls
        layoutDeleteControls = findViewById(R.id.layout_delete_controls);
        buttonSelectAll = findViewById(R.id.button_select_all);
        buttonDeleteSelected = findViewById(R.id.button_delete_selected);

        adapter = new ConversationHistoryAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ConversationHistoryViewModel.class);
        viewModel.getAllConversations().observe(this, conversationWithCharacters -> {
            adapter.setConversations(conversationWithCharacters);
            // If we were selecting all, and list changed (deleted), reset selection
            if (adapter.isDeleteMode() && conversationWithCharacters.isEmpty()) {
                toggleDeleteMode(false);
            }
        });

        // Click logic for navigation
        adapter.setOnItemClickListener(conversation -> {
            Intent intent = new Intent(ConversationHistoryActivity.this, ConversationActivity.class);
            intent.putExtra("CONVERSATION_ID", conversation.getId());
            intent.putExtra("CHARACTER_ID", conversation.getCharacterId());
            startActivity(intent);
        });

        // Selection logic for UI updates
        adapter.setOnSelectionChangedListener(count -> {
            buttonDeleteSelected.setText("Delete (" + count + ")");
            buttonDeleteSelected.setEnabled(count > 0);
            if (count > 0) {
                buttonDeleteSelected.setAlpha(1.0f);
            } else {
                buttonDeleteSelected.setAlpha(0.5f);
            }
        });

        buttonSelectAll.setOnClickListener(v -> adapter.toggleSelectAll());

        buttonDeleteSelected.setOnClickListener(v -> {
            List<Integer> selected = adapter.getSelectedIds();
            if (!selected.isEmpty()) {
                showDeleteConfirmation(selected);
            }
        });
    }

    // --- Menu Logic ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        deleteMenuItem = menu.findItem(R.id.action_delete_mode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_mode) {
            boolean isCurrentlyMode = adapter.isDeleteMode();
            toggleDeleteMode(!isCurrentlyMode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleDeleteMode(boolean enable) {
        adapter.setDeleteMode(enable);
        if (enable) {
            layoutDeleteControls.setVisibility(View.VISIBLE);
            deleteMenuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel); // Change icon to 'X'
        } else {
            layoutDeleteControls.setVisibility(View.GONE);
            deleteMenuItem.setIcon(android.R.drawable.ic_menu_delete); // Change icon back to trash
        }
    }

    private void showDeleteConfirmation(List<Integer> selectedIds) {
        int count = selectedIds.size();
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversations")
                .setMessage("Are you sure you want to delete " + count + " conversation(s)? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteConversations(selectedIds);
                    Toast.makeText(this, "Deleted " + count + " conversations", Toast.LENGTH_SHORT).show();
                    toggleDeleteMode(false); // Exit delete mode after deletion
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (adapter.isDeleteMode()) {
            toggleDeleteMode(false);
        } else {
            super.onBackPressed();
        }
    }
}