package com.example.testing;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends BaseActivity {

    private CharacterViewModel characterViewModel;
    private SettingsViewModel settingsViewModel;
    private FloatingActionButton fab;
    private SearchView searchView;
    private ChipGroup chipGroupTags;
    private RecyclerView recyclerView;
    private CharacterAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        if (!ModelRepository.getInstance().isModelsCached()) {
            ModelRepository.getInstance().refreshModels(isSuccess -> {
                runOnUiThread(() -> {
                    if (isSuccess) {
                        Toast.makeText(MainActivity.this, "Models updated successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        fab = findViewById(R.id.fab_add_character);
        searchView = findViewById(R.id.search_view);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        recyclerView = findViewById(R.id.character_recyclerview);

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditCharacterActivity.class);
            startActivity(intent);
        });
        int secondaryColor = ThemeUtils.getSecondaryColor(this);
        ThemeUtils.tintFab(fab, secondaryColor);

        adapter = new CharacterAdapter();
        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                characterViewModel.setSearchQuery(newText);
                return true;
            }
        });

        characterViewModel.getDisplayedCharacters().observe(this, adapter::setCharacters);

        settingsViewModel.getUser().observe(this, user -> {
            if (user != null) {
                String mode = user.getCharacterListMode();
                if (mode == null) mode = "list";

                adapter.setViewMode(mode);

                if ("card".equals(mode)) {
                    if (!(recyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                    }
                } else {
                    if (!(recyclerView.getLayoutManager() instanceof LinearLayoutManager) || (recyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    }
                }
            } else {
                if (!(recyclerView.getLayoutManager() instanceof LinearLayoutManager) || (recyclerView.getLayoutManager() instanceof GridLayoutManager)) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                }
            }
        });

        characterViewModel.getAllTags().observe(this, tags -> {
            populateTagChips(tags);
        });

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

    private void populateTagChips(List<String> tags) {
        String currentSelection = characterViewModel.getCurrentTagFilter();
        chipGroupTags.removeAllViews();

        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setClickable(true);

            int color = getTagColor(tag);
            chip.setTextColor(color);
            chip.setChipStrokeColor(ColorStateList.valueOf(color));
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
            chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#20" + Integer.toHexString(color).substring(2))));

            if (tag.equals(currentSelection)) {
                chip.setChecked(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setTextColor(Color.WHITE);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    characterViewModel.setTagFilter(tag);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                    chip.setTextColor(Color.WHITE);
                } else {
                    if (tag.equals(characterViewModel.getCurrentTagFilter())) {
                        characterViewModel.setTagFilter("");
                    }
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
                    chip.setTextColor(color);
                }
            });
            chipGroupTags.addView(chip);
        }
    }

    private int getTagColor(String tag) {
        int hash = tag.hashCode();
        float[] hsv = new float[3];
        hsv[0] = Math.abs(hash) % 360; // Random Hue
        // Saturation 0.4 - 0.8
        hsv[1] = 0.4f + (Math.abs(hash * 7) % 40) / 100f;
        // Value 1.0 (Brightest)
        hsv[2] = 1.0f;
        return Color.HSVToColor(hsv);
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
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