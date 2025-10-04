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
        // We'll handle loading the profile image from the path later.
        // For now, we use a placeholder.
        holder.imageViewProfile.setImageResource(R.mipmap.ic_launcher_round);
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    // ViewHolder class
    static class CharacterViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final ImageView imageViewProfile;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            imageViewProfile = itemView.findViewById(R.id.image_view_profile);
        }
    }
}