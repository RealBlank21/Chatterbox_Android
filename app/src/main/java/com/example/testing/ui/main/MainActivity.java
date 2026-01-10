package com.example.testing.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testing.ui.base.BaseActivity;
import com.example.testing.data.repository.ModelRepository;
import com.example.testing.R;
import com.example.testing.ui.base.ThemeUtils;
import com.example.testing.ui.character.AddEditCharacterActivity;
import com.example.testing.ui.character.CharacterAdapter;
import com.example.testing.ui.character.CharacterViewModel;
import com.example.testing.utils.TagViewManager;
import com.example.testing.ui.conversation.ConversationActivity;
import com.example.testing.ui.history.ConversationHistoryActivity;
import com.example.testing.utils.ChatStartFlowHelper;
import com.example.testing.ui.settings.SettingsActivity;
import com.example.testing.ui.settings.SettingsViewModel;
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
    private ChatStartFlowHelper chatStartFlowHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        chatStartFlowHelper = new ChatStartFlowHelper(this, this, characterViewModel);

        // Check/Update Models
        if (!ModelRepository.getInstance().isModelsCached()) {
            ModelRepository.getInstance().refreshModels(isSuccess -> {
                runOnUiThread(() -> {
                    if (isSuccess) {
                        Toast.makeText(MainActivity.this, "Models updated successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        initViews();
        setupRecyclerView();
        setupObservers();
    }

    private void initViews() {
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                characterViewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new CharacterAdapter();
        recyclerView.setAdapter(adapter);

        // Click to Chat
        adapter.setOnItemClickListener(character -> {
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("CHARACTER_ID", character.getId());
            intent.putExtra("CONVERSATION_ID", -1);
            startActivity(intent);
        });

        // Long Click for Options
        adapter.setOnItemLongClickListener((character, anchorView) -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, anchorView);
            popup.getMenuInflater().inflate(R.menu.character_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.option_edit) {
                    Intent intent = new Intent(MainActivity.this, AddEditCharacterActivity.class);
                    intent.putExtra("CHARACTER_ID", character.getId());
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.option_custom_option) {
                    chatStartFlowHelper.startCustomChatFlow(character);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupObservers() {
        characterViewModel.getDisplayedCharacters().observe(this, adapter::setCharacters);

        // Switch Layout Manager based on User Settings
        settingsViewModel.getUser().observe(this, user -> {
            String mode = (user != null && user.getCharacterListMode() != null) ? user.getCharacterListMode() : "list";
            adapter.setViewMode(mode);
            updateLayoutManager(mode);
        });

        characterViewModel.getAllTags().observe(this, this::populateTagChips);

        // Navigation from ViewModel events
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

    private void updateLayoutManager(String mode) {
        boolean isCard = "card".equals(mode);
        RecyclerView.LayoutManager current = recyclerView.getLayoutManager();

        if (isCard) {
            if (!(current instanceof GridLayoutManager)) {
                recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            }
        } else {
            if (!(current instanceof LinearLayoutManager) || (current instanceof GridLayoutManager)) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
        }
    }

    private void populateTagChips(List<String> tags) {
        String currentSelection = characterViewModel.getCurrentTagFilter();
        chipGroupTags.removeAllViews();

        for (String tag : tags) {
            boolean isSelected = tag.equals(currentSelection);

            Chip chip = TagViewManager.createFilterChip(this, tag, isSelected, (buttonView, isChecked) -> {
                if (isChecked) {
                    characterViewModel.setTagFilter(tag);
                } else {
                    if (tag.equals(characterViewModel.getCurrentTagFilter())) {
                        characterViewModel.setTagFilter("");
                    }
                }
            });

            chipGroupTags.addView(chip);
        }
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_history) {
            startActivity(new Intent(this, ConversationHistoryActivity.class));
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
}