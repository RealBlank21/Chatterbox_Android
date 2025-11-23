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

        // Trigger model refresh on app start with Toast feedback
        ModelRepository.getInstance().refreshModels(isSuccess -> {
            // Retrofit callbacks run on Main Thread by default on Android, but purely for safety:
            runOnUiThread(() -> {
                if (isSuccess) {
                    Toast.makeText(MainActivity.this, "Models updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to update models", Toast.LENGTH_SHORT).show();
                }
            });
        });

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

        characterViewModel.getDisplayedCharacters().observe(this, adapter::setCharacters);

        adapter.setOnItemLongClickListener(this::showCharacterOptionsMenu);

        adapter.setOnItemClickListener(character -> {
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("CHARACTER_ID", character.getId());
            intent.putExtra("CONVERSATION_ID", -1);
            startActivity(intent);
        });

        characterViewModel.getNavigateToConversation().observe(this, conversationInfo -> {
            if (conversationInfo != null) {
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                intent.putExtra("CHARACTER_ID", conversationInfo.characterId);
                intent.putExtra("CONVERSATION_ID", conversationInfo.conversationId);
                startActivity(intent);
                characterViewModel.doneNavigating();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        MenuItem hiddenItem = menu.findItem(R.id.action_hidden_bots);
        if (characterViewModel.isShowingHidden()) {
            hiddenItem.setTitle("Show All Bots");
        } else {
            hiddenItem.setTitle("Hidden Bots");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_history) {
            Intent intent = new Intent(this, ConversationHistoryActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_hidden_bots) {
            boolean currentlyHidden = characterViewModel.isShowingHidden();
            characterViewModel.setShowHidden(!currentlyHidden);
            invalidateOptionsMenu();

            String msg = !currentlyHidden ? "Showing Hidden Bots" : "Showing All Bots";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCharacterOptionsMenu(Character character, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.character_options_menu, popup.getMenu());

        MenuItem favItem = popup.getMenu().findItem(R.id.option_favorite);
        favItem.setTitle(character.isFavorite() ? "Unfavorite" : "Favorite");

        MenuItem hideItem = popup.getMenu().findItem(R.id.option_hide);
        hideItem.setTitle(character.isHidden() ? "Unhide" : "Hide");

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_edit) {
                editCharacter(character);
                return true;
            } else if (itemId == R.id.option_delete) {
                showDeleteConfirmationDialog(character);
                return true;
            } else if (itemId == R.id.option_favorite) {
                character.setFavorite(!character.isFavorite());
                characterViewModel.update(character);
                return true;
            } else if (itemId == R.id.option_hide) {
                character.setHidden(!character.isHidden());
                characterViewModel.update(character);

                String msg = character.isHidden() ? "Bot Hidden" : "Bot Unhidden";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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