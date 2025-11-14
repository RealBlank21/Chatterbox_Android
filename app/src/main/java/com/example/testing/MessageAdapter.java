package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_CHARACTER = 2;
    private Markwon markwon;
    private OnMessageLongClickListener longClickListener;

    // --- ADD NEW FIELDS ---
    private int editingPosition = -1; // -1 means no item is being edited
    private OnMessageEditListener editListener;

    // --- New listener for saving edits ---
    public interface OnMessageEditListener {
        void onMessageEdited(Message message);
    }

    public void setOnMessageEditListener(OnMessageEditListener listener) {
        this.editListener = listener;
    }

    // --- New method to trigger edit mode ---
    public void setEditingPosition(int position) {
        int oldPosition = this.editingPosition;
        this.editingPosition = position;

        // Notify the old item to refresh (go back to TextView)
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        // Notify the new item to refresh (go to EditText)
        if (this.editingPosition != -1) {
            notifyItemChanged(this.editingPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // ... (no changes here)
        Message message = messages.get(position);
        if ("user".equals(message.getRole())) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_CHARACTER;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ... (no changes here)
        if (markwon == null) {
            markwon = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .build();
        }
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item_character, parent, false);
        }
        return new MessageViewHolder(view);
    }

    // --- UPDATE ONBINDVIEWHOLDER ---
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        if (position == editingPosition) {
            holder.showEditMode(message);

            holder.buttonSaveEdit.setOnClickListener(v -> {
                String newContent = holder.editTextMessageContent.getText().toString().trim();
                if (!newContent.isEmpty()) {
                    message.setContent(newContent);
                    if (editListener != null) {
                        editListener.onMessageEdited(message); // Send to ViewModel
                    }
                    setEditingPosition(-1); // Exit edit mode
                } else {
                    setEditingPosition(-1); // Cancel if empty
                }
            });

        } else {
            holder.showDisplayMode(message, markwon);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> messages) {
        // ... (no changes here)
        this.messages = messages;
        notifyDataSetChanged();
    }

    // --- UPDATE VIEWHOLDER CLASS ---
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        // --- Add new views ---
        View layoutEditMessage;
        EditText editTextMessageContent;
        Button buttonSaveEdit;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_message);

            // --- Find new views ---
            layoutEditMessage = itemView.findViewById(R.id.layout_edit_message);
            editTextMessageContent = itemView.findViewById(R.id.edit_text_message_content);
            buttonSaveEdit = itemView.findViewById(R.id.button_save_edit);

            // --- Update long click listener to pass position ---
            OnMessageLongClickListener listener = longClickListener;
            View.OnLongClickListener longClick = v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onMessageLongClick(messages.get(position), v, position); // Pass position
                }
                return true;
            };

            itemView.setOnLongClickListener(longClick);
            textViewMessage.setOnLongClickListener(longClick);
        }

        // --- New method to show normal bubble ---
        public void showDisplayMode(Message message, Markwon markwon) {
            markwon.setMarkdown(textViewMessage, message.getContent());
            textViewMessage.setVisibility(View.VISIBLE);
            layoutEditMessage.setVisibility(View.GONE);
        }

        // --- New method to show edit view ---
        public void showEditMode(Message message) {
            textViewMessage.setVisibility(View.GONE);
            layoutEditMessage.setVisibility(View.VISIBLE);
            editTextMessageContent.setText(message.getContent());
            editTextMessageContent.requestFocus();
        }
    }

    // --- UPDATE INTERFACE ---
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, View anchorView, int position); // Add position
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
}