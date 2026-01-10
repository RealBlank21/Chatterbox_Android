package com.example.testing.ui.conversation;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.testing.data.local.entity.Message;
import com.example.testing.ui.base.BaseActivity;
import com.example.testing.data.local.entity.Character;
import com.example.testing.R;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.ui.base.ThemeUtils;
import com.example.testing.data.local.entity.User;
import com.example.testing.ui.conversation.utils.ConversationStatsFormatter;
import com.example.testing.ui.conversation.utils.MessagePopupHelper;

import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends BaseActivity {

    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ProgressBar progressBarGenerating;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;

    private ImageView actionBarImage;
    private TextView actionBarName;
    private TextView actionBarTag;

    private ConversationViewModel conversationViewModel;
    private int conversationId = -1;
    private Integer selectedPersonaId = null;
    private Integer selectedScenarioId = null;

    private User currentUser;
    private Character currentCharacter;

    private List<Message> currentMessages = new ArrayList<>();
    private String currentActionBarImagePath = null;
    private MessagePopupHelper messagePopupHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        setupActionBar();

        Intent intent = getIntent();
        int characterId = intent.getIntExtra("CHARACTER_ID", -1);
        conversationId = intent.getIntExtra("CONVERSATION_ID", -1);

        if (intent.hasExtra("PERSONA_ID")) {
            selectedPersonaId = intent.getIntExtra("PERSONA_ID", -1);
        }
        if (intent.hasExtra("SCENARIO_ID")) {
            selectedScenarioId = intent.getIntExtra("SCENARIO_ID", -1);
        }

        if (characterId == -1) {
            Toast.makeText(this, "Error: Invalid Character ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupViewModel(characterId);
        setupObservers();

        // Initialize the helper
        messagePopupHelper = new MessagePopupHelper(this, conversationViewModel, messageAdapter);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);

            LayoutInflater inflater = LayoutInflater.from(this);
            View customView = inflater.inflate(R.layout.action_bar_title, null);

            ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.START
            );
            actionBar.setCustomView(customView, params);

            actionBarImage = customView.findViewById(R.id.image_view_title_profile);
            actionBarName = customView.findViewById(R.id.text_view_title_name);
            actionBarTag = customView.findViewById(R.id.text_view_title_tag);

            actionBarImage.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(currentActionBarImagePath)) {
                    showFullScreenImage(currentActionBarImagePath);
                }
            });
        }
    }

    private void initViews() {
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        progressBarGenerating = findViewById(R.id.progress_bar_generating);

        buttonSend.setEnabled(false);

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setItemAnimator(null);

        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        // Click listeners
        buttonSend.setOnClickListener(v -> handleSendAction());

        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkIfReadyToSend(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        messageAdapter.setOnMessageLongClickListener((message, anchorView, position) -> {
            if ("assistant".equals(message.getRole())) {
                messagePopupHelper.showCharacterOptions(message, anchorView, position, currentUser, currentCharacter);
            } else {
                messagePopupHelper.showUserOptions(message, anchorView, position);
            }
        });

        messageAdapter.setOnMessageEditListener(message -> {
            if (conversationId != -1) {
                conversationViewModel.update(message);
            }
        });
    }

    private void setupViewModel(int characterId) {
        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.loadData(characterId, conversationId);
    }

    private void setupObservers() {
        conversationViewModel.getConversationId().observe(this, id -> this.conversationId = id);

        conversationViewModel.getIsGenerating().observe(this, isGenerating -> {
            if (isGenerating) {
                buttonSend.setVisibility(View.GONE);
                progressBarGenerating.setVisibility(View.VISIBLE);
            } else {
                buttonSend.setVisibility(View.VISIBLE);
                progressBarGenerating.setVisibility(View.GONE);
                checkIfReadyToSend();
            }
        });

        conversationViewModel.getCurrentCharacter().observe(this, character -> {
            if (character != null) {
                this.currentCharacter = character;
                if (actionBarName != null) actionBarName.setText(character.getName());
                updateActionBarImage(character.getCharacterProfileImagePath());
                checkIfReadyToSend();
                handleInitialScenarioLoad(character);
            }
        });

        conversationViewModel.getConversationScenario().observe(this, scenario -> {
            if (scenario != null) {
                if (!TextUtils.isEmpty(scenario.getImagePath())) {
                    updateActionBarImage(scenario.getImagePath());
                }
                updateActionBarTag(scenario.getName());
            } else {
                updateActionBarTag(null);
                if (currentCharacter != null && conversationId != -1) {
                    updateActionBarImage(currentCharacter.getCharacterProfileImagePath());
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

        conversationViewModel.getActivePersona().observe(this, persona -> {
            if (persona != null) {
                editTextMessage.setHint("Chatting as " + persona.getName());
            } else {
                editTextMessage.setHint("Type a message");
            }
        });

        conversationViewModel.getMessages().observe(this, messages -> {
            if (messages != null && !messages.isEmpty()) {
                messageAdapter.setMessages(messages);
                currentMessages = messages;
                recyclerViewMessages.post(() -> recyclerViewMessages.scrollToPosition(messages.size() - 1));
            }
        });
    }

    private void handleInitialScenarioLoad(Character character) {
        if (conversationId == -1) {
            androidx.lifecycle.LiveData<Scenario> scenarioLiveData;
            if (selectedScenarioId != null && selectedScenarioId != -1) {
                scenarioLiveData = conversationViewModel.getScenarioByIdLive(selectedScenarioId);
            } else {
                scenarioLiveData = conversationViewModel.getDefaultScenarioLive(character.getId());
            }

            scenarioLiveData.observe(this, scenario -> {
                String firstMsg = character.getFirstMessage();
                if (scenario != null) {
                    if (!TextUtils.isEmpty(scenario.getFirstMessage())) {
                        firstMsg = scenario.getFirstMessage();
                    }
                    if (!TextUtils.isEmpty(scenario.getImagePath())) {
                        updateActionBarImage(scenario.getImagePath());
                    }
                    updateActionBarTag(scenario.getName());
                } else {
                    updateActionBarTag(null);
                }

                if (!TextUtils.isEmpty(firstMsg)) {
                    List<Message> transientList = new ArrayList<>();
                    transientList.add(new Message("assistant", firstMsg, -1));
                    messageAdapter.setMessages(transientList);
                    currentMessages = transientList;
                }
            });
        }
    }

    private void updateActionBarImage(String imagePath) {
        currentActionBarImagePath = imagePath;
        if (actionBarImage != null) {
            if (!TextUtils.isEmpty(imagePath)) {
                Glide.with(this).load(imagePath).circleCrop().into(actionBarImage);
            } else {
                actionBarImage.setImageResource(R.mipmap.ic_launcher_round);
            }
        }
    }

    private void showFullScreenImage(String imagePath) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView view = new ImageView(this);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(this).load(imagePath).into(view);
        view.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    private void updateActionBarTag(String tagName) {
        if (actionBarTag != null) {
            if (!TextUtils.isEmpty(tagName)) {
                actionBarTag.setText(tagName);
                actionBarTag.setVisibility(View.VISIBLE);
                int secondaryColor = ThemeUtils.getSecondaryColor(this);
                float radius = 8 * getResources().getDisplayMetrics().density;
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(radius);
                shape.setColor(secondaryColor);
                actionBarTag.setBackground(shape);
            } else {
                actionBarTag.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_copy_history) {
            copyConversationHistoryToClipboard();
            return true;
        } else if (id == R.id.action_conversation_info) {
            showConversationInfo();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConversationInfo() {
        String info = ConversationStatsFormatter.generateStatsInfo(currentMessages, currentCharacter, currentUser);
        new AlertDialog.Builder(this)
                .setTitle("Conversation Stats")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private void copyConversationHistoryToClipboard() {
        if (currentMessages == null || currentMessages.isEmpty() || currentCharacter == null || currentUser == null) {
            Toast.makeText(this, "Nothing to copy or data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        conversationViewModel.getDebugConversationHistory(conversationId, currentUser, currentCharacter, jsonHistory -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Conversation History JSON", jsonHistory);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Full API History copied to clipboard (JSON)", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkIfReadyToSend() {
        if (Boolean.TRUE.equals(conversationViewModel.getIsGenerating().getValue())) return;

        String text = editTextMessage.getText().toString().trim();
        boolean hasContent = !TextUtils.isEmpty(text);
        boolean hasCreds = currentUser != null && !TextUtils.isEmpty(currentUser.getApiKey()) && currentCharacter != null;

        if (hasContent) {
            buttonSend.setImageResource(android.R.drawable.ic_menu_send);
        } else {
            buttonSend.setImageResource(android.R.drawable.ic_media_play);
        }
        buttonSend.setEnabled(hasCreds);
    }

    private void handleSendAction() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getApiKey())) {
            Toast.makeText(this, "API Key is missing. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        String messageContent = editTextMessage.getText().toString().trim();

        if (!TextUtils.isEmpty(messageContent)) {
            if (conversationId == -1) {
                conversationViewModel.createConversationAndSendMessage(messageContent, null, currentUser, currentCharacter, selectedPersonaId, selectedScenarioId);
            } else {
                conversationViewModel.sendMessage(messageContent, null, conversationId, currentUser, currentCharacter);
            }
            editTextMessage.setText("");
        } else {
            conversationViewModel.continueConversation(conversationId, currentUser, currentCharacter);
            Toast.makeText(this, "Continuing conversation...", Toast.LENGTH_SHORT).show();
        }
    }
}