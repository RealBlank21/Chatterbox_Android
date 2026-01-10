package com.example.testing.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.testing.R;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Message;
import com.example.testing.data.local.entity.User;
import com.example.testing.ui.conversation.ConversationViewModel;
import com.example.testing.ui.conversation.MessageAdapter;

public class MessagePopupHelper {

    private final Context context;
    private final ConversationViewModel viewModel;
    private final MessageAdapter adapter;

    public MessagePopupHelper(Context context, ConversationViewModel viewModel, MessageAdapter adapter) {
        this.context = context;
        this.viewModel = viewModel;
        this.adapter = adapter;
    }

    public void showCharacterOptions(Message message, View anchorView, int position, User currentUser, Character currentCharacter) {
        if (Boolean.TRUE.equals(viewModel.getIsGenerating().getValue())) {
            Toast.makeText(context, "Please wait for generation to finish", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_regenerate) {
                viewModel.regenerateResponse(message, currentUser, currentCharacter);
                return true;
            } else if (itemId == R.id.option_copy_message) {
                copyToClipboard(message.getContent());
                return true;
            } else if (itemId == R.id.option_edit_message) {
                adapter.setEditingPosition(position);
                return true;
            } else if (itemId == R.id.option_delete_message) {
                viewModel.deleteMessage(message);
                return true;
            }
            return false;
        });
        popup.show();
    }

    public void showUserOptions(Message message, View anchorView, int position) {
        if (Boolean.TRUE.equals(viewModel.getIsGenerating().getValue())) {
            Toast.makeText(context, "Please wait for generation to finish", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());
        // User messages generally cannot be "regenerated" in the same way, so hide that option
        popup.getMenu().findItem(R.id.option_regenerate).setVisible(false);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_copy_message) {
                copyToClipboard(message.getContent());
                return true;
            } else if (itemId == R.id.option_edit_message) {
                adapter.setEditingPosition(position);
                return true;
            } else if (itemId == R.id.option_delete_message) {
                viewModel.deleteMessage(message);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void copyToClipboard(String content) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Chat Message", content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}