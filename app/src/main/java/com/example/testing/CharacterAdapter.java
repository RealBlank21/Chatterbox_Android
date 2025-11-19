package com.example.testing;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private List<Character> characters = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.character_list_item, parent, false);
        return new CharacterViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Character currentCharacter = characters.get(position);

        // Name with Favorite Star
        String displayName = currentCharacter.getName();
        if (currentCharacter.isFavorite()) {
            displayName = "★ " + displayName;
        }
        holder.textViewName.setText(displayName);

        // First Message Preview
        String firstMessage = currentCharacter.getFirstMessage();
        if (!TextUtils.isEmpty(firstMessage)) {
            holder.textViewFirstMessage.setText(firstMessage);
            holder.textViewFirstMessage.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFirstMessage.setVisibility(View.GONE);
        }

        // Conversation Count
        int count = currentCharacter.getConversationCount();
        String countText = count + (count == 1 ? " Conversation" : " Conversations");
        holder.textViewConversationCount.setText(countText);

        // Image
        String imagePath = currentCharacter.getCharacterProfileImagePath();
        if (!TextUtils.isEmpty(imagePath)) {
            Glide.with(holder.itemView.getContext())
                    .load(imagePath)
                    .circleCrop()
                    .into(holder.imageViewProfile);
        } else {
            holder.imageViewProfile.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
        notifyDataSetChanged();
    }

    class CharacterViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final TextView textViewFirstMessage;
        private final TextView textViewConversationCount;
        private final ImageView imageViewProfile;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewFirstMessage = itemView.findViewById(R.id.text_view_first_message);
            textViewConversationCount = itemView.findViewById(R.id.text_view_conversation_count);
            imageViewProfile = itemView.findViewById(R.id.image_view_profile);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(characters.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(characters.get(position), v);
                }
                return true;
            });
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Character character, View anchorView);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Character character);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
}