package com.example.testing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar; // Added
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testing.network.request.RequestMessage;
import com.example.testing.network.response.Model;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ConversationActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewMessages;
    private ProgressBar progressBarLoading; // Added
    private MessageAdapter messageAdapter;

    private ConversationViewModel conversationViewModel;
    private int conversationId = -1;

    private User currentUser;
    private Character currentCharacter;

    // We keep track of DB messages and streaming content separately
    private List<Message> dbMessagesList = new ArrayList<>();
    private String currentStreamingText = null;

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
        progressBarLoading = findViewById(R.id.progress_bar_loading); // Added
        buttonSend.setEnabled(false);

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.loadData(characterId, conversationId);

        conversationViewModel.getConversationId().observe(this, id -> {
            this.conversationId = id;
        });

        // Observe Loading State
        conversationViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBarLoading.setVisibility(View.VISIBLE);
                buttonSend.setEnabled(false);
            } else {
                progressBarLoading.setVisibility(View.GONE);
                buttonSend.setEnabled(true);
            }
        });

        // Observe Streaming Content
        conversationViewModel.getStreamingContent().observe(this, content -> {
            currentStreamingText = content;
            updateMessageList();
        });

        conversationViewModel.getCurrentCharacter().observe(this, character -> {
            if (character != null) {
                this.currentCharacter = character;
                setTitle(character.getName());
                checkIfReadyToSend();

                // Initial greeting logic for new conversations
                if (conversationId == -1 && !TextUtils.isEmpty(character.getFirstMessage()) && dbMessagesList.isEmpty()) {
                    List<Message> transientList = new ArrayList<>();
                    transientList.add(new Message("assistant", character.getFirstMessage(), -1));
                    dbMessagesList = transientList;
                    updateMessageList();
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

        conversationViewModel.getDbMessages().observe(this, messages -> {
            if (messages != null) {
                dbMessagesList = messages;
                updateMessageList();
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

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void updateMessageList() {
        List<Message> displayList = new ArrayList<>(dbMessagesList);

        // If we are streaming, create a temporary message and append it
        if (currentStreamingText != null) {
            Message streamingMsg = new Message("assistant", currentStreamingText, conversationId);
            displayList.add(streamingMsg);
        }

        messageAdapter.setMessages(displayList);
        if (!displayList.isEmpty()) {
            recyclerViewMessages.scrollToPosition(displayList.size() - 1);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConversationInfo() {
        // Use the adapter's list (which includes streaming content if any, though unlikely during info check)
        List<Message> currentMessages = dbMessagesList;

        if (currentMessages == null || currentMessages.isEmpty() || currentCharacter == null) {
            Toast.makeText(this, "No information available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        int userMsgCount = 0;
        int botMsgCount = 0;
        int totalTokens = 0;
        long firstTimestamp = Long.MAX_VALUE;
        long lastTimestamp = 0;

        for (Message msg : currentMessages) {
            if ("user".equals(msg.getRole())) {
                userMsgCount++;
            } else if ("assistant".equals(msg.getRole())) {
                botMsgCount++;
            }

            if (msg.getContent() != null) {
                totalTokens += msg.getContent().length() / 4;
            }

            if (msg.getTimestamp() < firstTimestamp) firstTimestamp = msg.getTimestamp();
            if (msg.getTimestamp() > lastTimestamp) lastTimestamp = msg.getTimestamp();
        }

        String personality = currentCharacter.getPersonality();
        if (personality != null) totalTokens += personality.length() / 4;

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
        if (modelId != null) {
            Model model = ModelRepository.getInstance().getModelById(modelId);
            if (model != null && model.getPricing() != null) {
                try {
                    double promptPrice = Double.parseDouble(model.getPricing().getPrompt());
                    double completionPrice = Double.parseDouble(model.getPricing().getCompletion());
                    double estimatedCost = totalTokens * Math.max(promptPrice, completionPrice);

                    if (estimatedCost < 0.01) {
                        costStr = String.format(Locale.getDefault(), "$%.6f", estimatedCost);
                    } else {
                        costStr = String.format(Locale.getDefault(), "$%.4f", estimatedCost);
                    }
                    costStr += " (Est. context snapshot)";
                } catch (Exception e) {
                    costStr = "Pricing Error";
                }
            }
        }

        StringBuilder info = new StringBuilder();
        info.append("Model: ").append(modelId != null ? modelId : "Default").append("\n\n");
        info.append("User Messages: ").append(userMsgCount).append("\n");
        info.append("Bot Messages: ").append(botMsgCount).append("\n");
        info.append("Total Messages: ").append(currentMessages.size()).append("\n\n");
        info.append("Duration: ").append(durationStr).append("\n");
        info.append("Est. Token Count: ~").append(totalTokens).append("\n");
        info.append("Est. Cost (Current Context): ").append(costStr);

        new AlertDialog.Builder(this)
                .setTitle("Conversation Info")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void copyConversationHistoryToClipboard() {
        if (dbMessagesList == null || dbMessagesList.isEmpty() || currentCharacter == null || currentUser == null) {
            Toast.makeText(this, "Nothing to copy or data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        List<RequestMessage> requestMessages = new ArrayList<>();

        SimpleDateFormat dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

        long creationTimestamp = dbMessagesList.get(0).getTimestamp();
        Date creationDate = new Date(creationTimestamp);
        String formattedDay = dayFormatter.format(creationDate);
        String formattedTime = timeFormatter.format(creationDate);

        String globalPrompt = currentUser.getGlobalSystemPrompt() != null ? currentUser.getGlobalSystemPrompt() : "";
        String characterPersonality = currentCharacter.getPersonality() != null ? currentCharacter.getPersonality() : "";

        if (globalPrompt.contains("{day}")) globalPrompt = globalPrompt.replace("{day}", formattedDay);
        if (globalPrompt.contains("{time}")) globalPrompt = globalPrompt.replace("{time}", formattedTime);
        if (characterPersonality.contains("{day}")) characterPersonality = characterPersonality.replace("{day}", formattedDay);
        if (characterPersonality.contains("{time}")) characterPersonality = characterPersonality.replace("{time}", formattedTime);

        String finalSystemPrompt = globalPrompt + "\n" + characterPersonality;

        if (currentCharacter.isTimeAware()) {
            finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
        }

        if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
            requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
        }

        for (Message msg : dbMessagesList) {
            if (currentCharacter.isTimeAware() && "user".equals(msg.getRole())) {
                Date msgDate = new Date(msg.getTimestamp());
                String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
            }
            requestMessages.add(new RequestMessage(msg.getRole(), msg.getContent()));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonHistory = gson.toJson(requestMessages);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Conversation History JSON", jsonHistory);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "History copied to clipboard (JSON)", Toast.LENGTH_SHORT).show();
    }

    private void checkIfReadyToSend() {
        if (currentUser != null && currentCharacter != null) {
            // Only enable if not currently loading
            if (progressBarLoading.getVisibility() != View.VISIBLE) {
                buttonSend.setEnabled(true);
            }
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
                conversationViewModel.createConversationAndSendMessage(messageContent, currentUser, currentCharacter);
            } else {
                conversationViewModel.sendMessage(messageContent, conversationId, currentUser, currentCharacter);
            }
            editTextMessage.setText("");
        }
    }

    private void showCharacterMessageOptions(Message message, View anchorView, int position) {
        if (conversationId == -1) return;

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