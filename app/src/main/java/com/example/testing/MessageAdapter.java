package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // --- ADDED LISTENER ---
    private OnMessageLongClickListener longClickListener;

    @Override
    public int getItemViewType(int position) {
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

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        markwon.setMarkdown(holder.textViewMessage, message.getContent());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    // --- ViewHolder class with listener ---
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_message);

            // This listener handles long-clicks on the empty space in the row
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onMessageLongClick(messages.get(position), v);
                }
                return true;
            });

            // --- THIS IS THE FIX ---
            // Add the same listener to the TextView itself.
            // This ensures that long-clicking the bubble works,
            // as the TextView might consume the event otherwise.
            textViewMessage.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    // Pass the TextView 'v' as the anchor for the popup
                    longClickListener.onMessageLongClick(messages.get(position), v);
                }
                return true;
            });
        }
    }

    // --- Interface for the long click listener ---
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, View anchorView);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
}