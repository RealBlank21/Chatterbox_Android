package com.example.testing;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private List<Character> characters = new ArrayList<>();
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;
    private boolean isCardView = false;

    public void setViewMode(String mode) {
        boolean newIsCardView = "card".equalsIgnoreCase(mode);
        if (this.isCardView != newIsCardView) {
            this.isCardView = newIsCardView;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isCardView ? 1 : 0;
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == 1) ? R.layout.character_grid_item : R.layout.character_list_item;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new CharacterViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Character currentCharacter = characters.get(position);

        String displayName = currentCharacter.getName();
        if (currentCharacter.isFavorite()) {
            displayName = "★ " + displayName;
        }
        holder.textViewName.setText(displayName);

        String firstMessage = currentCharacter.getFirstMessage();
        if (!TextUtils.isEmpty(firstMessage)) {
            holder.textViewFirstMessage.setText(firstMessage);
            holder.textViewFirstMessage.setVisibility(View.VISIBLE);
        } else {
            if (isCardView) {
                holder.textViewFirstMessage.setText("");
                holder.textViewFirstMessage.setVisibility(View.INVISIBLE);
            } else {
                holder.textViewFirstMessage.setVisibility(View.GONE);
            }
        }

        int count = currentCharacter.getConversationCount();
        String countText = count + (count == 1 ? " Conversation" : " Conversations");
        holder.textViewConversationCount.setText(countText);

        String imagePath = currentCharacter.getCharacterProfileImagePath();
        if (isCardView) {
            if (!TextUtils.isEmpty(imagePath)) {
                Glide.with(holder.itemView.getContext())
                        .load(imagePath)
                        .centerCrop()
                        .into(holder.imageViewProfile);
            } else {
                holder.imageViewProfile.setImageResource(R.mipmap.ic_launcher);
                holder.imageViewProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        } else {
            if (!TextUtils.isEmpty(imagePath)) {
                Glide.with(holder.itemView.getContext())
                        .load(imagePath)
                        .circleCrop()
                        .into(holder.imageViewProfile);
            } else {
                holder.imageViewProfile.setImageResource(R.mipmap.ic_launcher_round);
            }
        }

        // --- Tag Handling (ChipGroup) ---
        // Logic applies to both Card and List view now
        if (holder.tagsContainer != null) {
            String tagsStr = currentCharacter.getTags();
            if (tagsStr != null && !tagsStr.trim().isEmpty()) {
                holder.tagsContainer.setVisibility(View.VISIBLE);
                holder.tagsContainer.removeAllViews();

                String[] tags = tagsStr.split("[,|]");
                for (String tag : tags) {
                    String cleanTag = tag.trim();
                    if (!cleanTag.isEmpty()) {
                        addTagView(holder.itemView.getContext(), holder.tagsContainer, cleanTag);
                    }
                }
            } else {
                holder.tagsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void addTagView(Context context, ChipGroup container, String tag) {
        TextView tagView = new TextView(context);
        tagView.setText(tag);
        tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tagView.setPadding(dpToPx(context, 6), dpToPx(context, 2), dpToPx(context, 6), dpToPx(context, 2));
        tagView.setGravity(Gravity.CENTER);

        ChipGroup.LayoutParams params = new ChipGroup.LayoutParams(
                ChipGroup.LayoutParams.WRAP_CONTENT,
                ChipGroup.LayoutParams.WRAP_CONTENT
        );
        tagView.setLayoutParams(params);

        int color = getTagColor(tag);
        tagView.setTextColor(color);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(context, 4));
        shape.setColor(Color.TRANSPARENT);
        shape.setStroke(dpToPx(context, 1), color);

        tagView.setBackground(shape);

        container.addView(tagView);
    }

    private int getTagColor(String tag) {
        int hash = tag.hashCode();
        float[] hsv = new float[3];
        hsv[0] = Math.abs(hash) % 360;
        hsv[1] = 0.4f + (Math.abs(hash * 7) % 40) / 100f;
        hsv[2] = 1.0f;
        return Color.HSVToColor(hsv);
    }

    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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
        private final View imageBadge;
        private final TextView textViewFirstMessage;
        private final TextView textViewConversationCount;
        private final ImageView imageViewProfile;

        // ChipGroup for tags (Present in both layouts now)
        private final ChipGroup tagsContainer;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            imageBadge = itemView.findViewById(R.id.card_view_image_badge);
            textViewFirstMessage = itemView.findViewById(R.id.text_view_first_message);
            textViewConversationCount = itemView.findViewById(R.id.text_view_conversation_count);
            imageViewProfile = itemView.findViewById(R.id.image_view_profile);

            tagsContainer = itemView.findViewById(R.id.chip_group_tags);

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