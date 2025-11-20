package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationHistoryAdapter extends RecyclerView.Adapter<ConversationHistoryAdapter.ConversationViewHolder> {

    private List<ConversationWithCharacter> conversations = new ArrayList<>();
    private OnItemClickListener listener;

    // --- Selection Fields ---
    private boolean isDeleteMode = false;
    private final Set<Integer> selectedIds = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

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

        String title = currentItem.conversation.getTitle();
        if (title == null || title.isEmpty()) {
            holder.textViewTitle.setText("New Chat");
        } else {
            holder.textViewTitle.setText(title);
        }

        Date date = new Date(currentItem.conversation.getLastUpdated());
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        String formattedDate = formatter.format(date);

        // --- UPDATED FORMATTING ---
        // Format: <Timestamp> | (<Total Messages>)
        String infoText = formattedDate + " | (" + currentItem.messageCount + " messages)";
        holder.textViewTimestamp.setText(infoText);
        // --------------------------

        // --- Handle Selection Visuals ---
        if (isDeleteMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedIds.contains(currentItem.conversation.getId()));
        } else {
            holder.checkBox.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setConversations(List<ConversationWithCharacter> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    public void setDeleteMode(boolean enabled) {
        this.isDeleteMode = enabled;
        this.selectedIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionChanged(0);
    }

    public boolean isDeleteMode() {
        return isDeleteMode;
    }

    public void toggleSelectAll() {
        if (selectedIds.size() == conversations.size()) {
            selectedIds.clear();
        } else {
            for (ConversationWithCharacter c : conversations) {
                selectedIds.add(c.conversation.getId());
            }
        }
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionChanged(selectedIds.size());
    }

    public List<Integer> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewCharacterName;
        private final TextView textViewTitle;
        private final TextView textViewTimestamp;
        private final CheckBox checkBox;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCharacterName = itemView.findViewById(R.id.text_view_character_name);
            textViewTitle = itemView.findViewById(R.id.text_view_conversation_title);
            textViewTimestamp = itemView.findViewById(R.id.text_view_timestamp);
            checkBox = itemView.findViewById(R.id.checkbox_select);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (isDeleteMode) {
                        int id = conversations.get(position).conversation.getId();
                        if (selectedIds.contains(id)) {
                            selectedIds.remove(id);
                        } else {
                            selectedIds.add(id);
                        }
                        notifyItemChanged(position);
                        if (selectionListener != null) selectionListener.onSelectionChanged(selectedIds.size());
                    } else {
                        if (listener != null) {
                            listener.onItemClick(conversations.get(position).conversation);
                        }
                    }
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