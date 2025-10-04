package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private List<Character> characters = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener; // ADDED THIS LINE

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
        holder.textViewName.setText(currentCharacter.getName());
        holder.imageViewProfile.setImageResource(R.mipmap.ic_launcher_round);
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
        notifyDataSetChanged();
    }

    // ViewHolder class
    class CharacterViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final ImageView imageViewProfile;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            imageViewProfile = itemView.findViewById(R.id.image_view_profile);

            // --- START OF ADDED CODE ---
            // Set the simple click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(characters.get(position));
                }
            });
            // --- END OF ADDED CODE ---

            // Set the long click listener
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(characters.get(position), v);
                }
                return true;
            });
        }
    }

    // --- Interface for the long click listener ---
    public interface OnItemLongClickListener {
        void onItemLongClick(Character character, View anchorView);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    // --- START OF ADDED CODE ---
    // --- Interface for the simple click listener ---
    public interface OnItemClickListener {
        void onItemClick(Character character);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    // --- END OF ADDED CODE ---
}