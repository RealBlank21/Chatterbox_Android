package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private CharacterViewModel characterViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab_add_character);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditCharacterActivity.class);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.character_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final CharacterAdapter adapter = new CharacterAdapter();
        recyclerView.setAdapter(adapter);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        characterViewModel.getAllCharacters().observe(this, adapter::setCharacters);

        // --- SETUP LISTENERS ---
        adapter.setOnItemLongClickListener(this::showCharacterOptionsMenu);

        adapter.setOnItemClickListener(character -> {
            // Tell the ViewModel to start creating a new conversation
            characterViewModel.startNewConversation(character);
        });

        // --- OBSERVE FOR NAVIGATION ---
        characterViewModel.getNavigateToConversation().observe(this, conversationInfo -> {
            if (conversationInfo != null) {
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                intent.putExtra("CHARACTER_ID", conversationInfo.characterId);
                intent.putExtra("CONVERSATION_ID", conversationInfo.conversationId);
                startActivity(intent);

                // Reset the event so it doesn't fire again on screen rotation
                characterViewModel.doneNavigating();
            }
        });
    }

    // ... The rest of your MainActivity methods (onCreateOptionsMenu, showCharacterOptionsMenu, etc.) remain the same
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_history) { // ADD THIS ELSE-IF BLOCK
            Intent intent = new Intent(this, ConversationHistoryActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCharacterOptionsMenu(Character character, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.character_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_edit) {
                editCharacter(character);
                return true;
            } else if (itemId == R.id.option_delete) {
                showDeleteConfirmationDialog(character);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void editCharacter(Character character) {
        Intent intent = new Intent(MainActivity.this, AddEditCharacterActivity.class);
        intent.putExtra("CHARACTER_ID", character.getId());
        startActivity(intent);
    }

    private void showDeleteConfirmationDialog(Character character) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirmation)
                .setMessage(R.string.are_you_sure_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    characterViewModel.delete(character);
                    Toast.makeText(this, "Character deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}