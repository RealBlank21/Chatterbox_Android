package com.example.testing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testing.network.request.RequestMessage;
import com.example.testing.network.response.Model;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConversationActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private ImageButton buttonSend; // Changed to ImageButton
    private ImageButton buttonAttachImage;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;

    // Preview UI
    private CardView cardViewImagePreview;
    private ImageView imageViewPreview;
    private ImageButton buttonRemoveImage;

    private ConversationViewModel conversationViewModel;
    private int conversationId = -1;

    private User currentUser;
    private Character currentCharacter;

    private List<Message> currentMessages = new ArrayList<>();

    private String selectedImagePath = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveImageToInternalStorage(uri);
                }
            });

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
        buttonAttachImage = findViewById(R.id.button_attach_image);

        cardViewImagePreview = findViewById(R.id.card_view_image_preview);
        imageViewPreview = findViewById(R.id.image_view_preview);
        buttonRemoveImage = findViewById(R.id.button_remove_image);

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

        conversationViewModel.getCurrentCharacter().observe(this, character -> {
            if (character != null) {
                this.currentCharacter = character;
                setTitle(character.getName());
                checkIfReadyToSend();

                // Show/Hide Attach Button
                if (character.isAllowImageInput()) {
                    buttonAttachImage.setVisibility(View.VISIBLE);
                } else {
                    buttonAttachImage.setVisibility(View.GONE);
                }

                if (conversationId == -1 && !TextUtils.isEmpty(character.getFirstMessage())) {
                    List<Message> transientList = new ArrayList<>();
                    transientList.add(new Message("assistant", character.getFirstMessage(), -1));
                    messageAdapter.setMessages(transientList);
                    currentMessages = transientList;
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

        // Text Watcher to enable/disable send button
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkIfReadyToSend(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSend.setOnClickListener(v -> handleSendAction());

        buttonAttachImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        buttonRemoveImage.setOnClickListener(v -> clearSelectedImage());
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File directory = new File(getFilesDir(), "chat_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, UUID.randomUUID().toString() + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            selectedImagePath = file.getAbsolutePath();
            showImagePreview();
            checkIfReadyToSend();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreview() {
        if (selectedImagePath != null) {
            cardViewImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImagePath).centerCrop().into(imageViewPreview);
        }
    }

    private void clearSelectedImage() {
        selectedImagePath = null;
        cardViewImagePreview.setVisibility(View.GONE);
        checkIfReadyToSend();
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

        List<RequestMessage> requestMessages = new ArrayList<>();

        SimpleDateFormat dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());

        long creationTimestamp = currentMessages.get(0).getTimestamp();
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

        for (Message msg : currentMessages) {
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
        String text = editTextMessage.getText().toString().trim();
        boolean hasContent = !TextUtils.isEmpty(text) || selectedImagePath != null;
        boolean hasCreds = currentUser != null && !TextUtils.isEmpty(currentUser.getApiKey()) && currentCharacter != null;

        // Toggle Icon
        if (hasContent) {
            buttonSend.setImageResource(android.R.drawable.ic_menu_send);
        } else {
            buttonSend.setImageResource(android.R.drawable.ic_media_play); // Continue Icon
        }

        // Enable if credentials exist (Continue is valid even without content)
        buttonSend.setEnabled(hasCreds);
    }

    private void handleSendAction() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getApiKey())) {
            Toast.makeText(this, "API Key is missing. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        String messageContent = editTextMessage.getText().toString().trim();

        if (!TextUtils.isEmpty(messageContent) || selectedImagePath != null) {
            // SEND MODE
            String imageToSend = selectedImagePath;

            if (conversationId == -1) {
                conversationViewModel.createConversationAndSendMessage(messageContent, imageToSend, currentUser, currentCharacter);
            } else {
                conversationViewModel.sendMessage(messageContent, imageToSend, conversationId, currentUser, currentCharacter);
            }
            // Clear UI
            editTextMessage.setText("");
            clearSelectedImage();

        } else {
            // CONTINUE MODE (No text, no image)
            conversationViewModel.continueConversation(conversationId, currentUser, currentCharacter);

            // No UI to clear, but maybe show a toast?
            Toast.makeText(this, "Continuing conversation...", Toast.LENGTH_SHORT).show();
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