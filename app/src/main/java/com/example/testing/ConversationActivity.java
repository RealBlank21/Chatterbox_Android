package com.example.testing;

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
import android.widget.PopupMenu;
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
import com.example.testing.network.response.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

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

        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.loadData(characterId, conversationId);

        conversationViewModel.getConversationId().observe(this, id -> {
            this.conversationId = id;
        });

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
        });

        conversationViewModel.getConversationScenario().observe(this, scenario -> {
            if (scenario != null) {
                if (!TextUtils.isEmpty(scenario.getImagePath())) {
                    updateActionBarImage(scenario.getImagePath());
                }
                updateActionBarTag(scenario.getName());
            } else {
                updateActionBarTag(null);
                if (currentCharacter != null) {
                    if (conversationId != -1) {
                        updateActionBarImage(currentCharacter.getCharacterProfileImagePath());
                    }
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

                recyclerViewMessages.post(() ->
                        recyclerViewMessages.scrollToPosition(messages.size() - 1)
                );
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
            if (conversationId != -1) {
                conversationViewModel.update(message);
            }
        });

        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkIfReadyToSend(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSend.setOnClickListener(v -> handleSendAction());
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
        if (currentMessages == null || currentMessages.isEmpty() || currentCharacter == null) {
            Toast.makeText(this, "No information available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        int userMsgCount = 0;
        int botMsgCount = 0;

        long totalInputTokens = 0;
        long totalOutputTokens = 0;

        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = 0;
        String lastFinishReason = "N/A";

        for (Message msg : currentMessages) {
            if ("user".equals(msg.getRole())) {
                userMsgCount++;
            } else if ("assistant".equals(msg.getRole())) {
                botMsgCount++;
                totalInputTokens += msg.getPromptTokens();
                totalOutputTokens += msg.getCompletionTokens();

                if (msg.getFinishReason() != null) {
                    lastFinishReason = msg.getFinishReason();
                }
            }

            if (msg.getTimestamp() < firstTimestamp) firstTimestamp = msg.getTimestamp();
            if (msg.getTimestamp() > lastTimestamp) lastTimestamp = msg.getTimestamp();
        }

        String durationStr = "N/A";
        if (firstTimestamp != Long.MAX_VALUE && lastTimestamp != 0) {
            long diffInMillis = lastTimestamp - firstTimestamp;
            if (diffInMillis < 0) diffInMillis = 0;

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

            if (hours > 0) {
                durationStr = hours + "h " + (minutes % 60) + "m";
            } else {
                durationStr = minutes + "m";
            }
        }

        String modelId = currentCharacter.getModel();
        if (TextUtils.isEmpty(modelId) && currentUser != null) {
            modelId = currentUser.getPreferredModel();
        }

        String costStr = "Unknown Model Price";
        String inputPriceStr = "N/A";
        String outputPriceStr = "N/A";

        if (modelId != null) {
            Model model = ModelRepository.getInstance().getModelById(modelId);
            if (model != null && model.getPricing() != null) {
                try {
                    double promptPrice = Double.parseDouble(model.getPricing().getPrompt());
                    double completionPrice = Double.parseDouble(model.getPricing().getCompletion());

                    double totalCost = (totalInputTokens * promptPrice) + (totalOutputTokens * completionPrice);

                    if (totalCost < 0.0001) {
                        costStr = String.format(Locale.getDefault(), "$%.6f", totalCost);
                    } else {
                        costStr = String.format(Locale.getDefault(), "$%.4f", totalCost);
                    }

                    inputPriceStr = String.format(Locale.getDefault(), "$%.2f/1M", promptPrice * 1000000);
                    outputPriceStr = String.format(Locale.getDefault(), "$%.2f/1M", completionPrice * 1000000);

                } catch (Exception e) {
                    costStr = "Pricing Error";
                }
            }
        }

        StringBuilder info = new StringBuilder();
        info.append("Model: ").append(modelId != null ? modelId : "Default").append("\n");
        info.append("Input Price: ").append(inputPriceStr).append("\n");
        info.append("Output Price: ").append(outputPriceStr).append("\n\n");

        info.append("User Messages: ").append(userMsgCount).append("\n");
        info.append("Bot Messages: ").append(botMsgCount).append("\n");
        info.append("Duration: ").append(durationStr).append("\n\n");

        info.append("--- Token Usage ---\n");
        info.append("Total Input Tokens: ").append(totalInputTokens).append("\n");
        info.append("Total Output Tokens: ").append(totalOutputTokens).append("\n");
        info.append("Last Finish Reason: ").append(lastFinishReason).append("\n\n");

        info.append("Total Cost: ").append(costStr);

        new AlertDialog.Builder(this)
                .setTitle("Conversation Stats")
                .setMessage(info.toString())
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
        if (Boolean.TRUE.equals(conversationViewModel.getIsGenerating().getValue())) {
            return;
        }

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
                // Modified to pass null for image
                conversationViewModel.createConversationAndSendMessage(messageContent, null, currentUser, currentCharacter, selectedPersonaId, selectedScenarioId);
            } else {
                // Modified to pass null for image
                conversationViewModel.sendMessage(messageContent, null, conversationId, currentUser, currentCharacter);
            }
            editTextMessage.setText("");

        } else {
            conversationViewModel.continueConversation(conversationId, currentUser, currentCharacter);
            Toast.makeText(this, "Continuing conversation...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCharacterMessageOptions(Message message, View anchorView, int position) {
        if (conversationId == -1) return;

        if (Boolean.TRUE.equals(conversationViewModel.getIsGenerating().getValue())) {
            Toast.makeText(this, "Please wait for generation to finish", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_regenerate) {
                conversationViewModel.regenerateResponse(message, currentUser, currentCharacter);
                return true;
            } else if (itemId == R.id.option_copy_message) {
                copyMessageToClipboard(message);
                return true;
            } else if (itemId == R.id.option_edit_message) {
                messageAdapter.setEditingPosition(position);
                return true;
            } else if (itemId == R.id.option_delete_message) {
                conversationViewModel.deleteMessage(message);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showUserMessageOptions(Message message, View anchorView, int position) {
        if (conversationId == -1) return;

        if (Boolean.TRUE.equals(conversationViewModel.getIsGenerating().getValue())) {
            Toast.makeText(this, "Please wait for generation to finish", Toast.LENGTH_SHORT).show();
            return;
        }

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
            } else if (itemId == R.id.option_delete_message) {
                conversationViewModel.deleteMessage(message);
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