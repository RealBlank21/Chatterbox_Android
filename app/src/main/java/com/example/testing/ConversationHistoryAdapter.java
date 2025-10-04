package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationHistoryAdapter extends RecyclerView.Adapter<ConversationHistoryAdapter.ConversationViewHolder> {

    private List<ConversationWithCharacter> conversations = new ArrayList<>();
    private OnItemClickListener listener;

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversation_history_item, parent, false);
        return new ConversationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationWithCharacter currentItem = conversations.get(position);
        holder.textViewCharacterName.setText(currentItem.characterName);

        // Format the timestamp into a readable date and time
        Date date = new Date(currentItem.conversation.getLastUpdated());
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        holder.textViewTimestamp.setText(formatter.format(date));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setConversations(List<ConversationWithCharacter> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewCharacterName;
        private final TextView textViewTimestamp;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCharacterName = itemView.findViewById(R.id.text_view_character_name);
            textViewTimestamp = itemView.findViewById(R.id.text_view_timestamp);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // Pass the conversation object from our combined data holder
                    listener.onItemClick(conversations.get(position).conversation);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}