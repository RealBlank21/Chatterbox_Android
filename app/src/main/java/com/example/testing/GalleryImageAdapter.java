package com.example.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class GalleryImageAdapter extends RecyclerView.Adapter<GalleryImageAdapter.GalleryImageViewHolder> {

    private List<GalleryImage> images = new ArrayList<>();
    private OnImageActionListener listener;

    public interface OnImageActionListener {
        void onEdit(GalleryImage image);
        void onDelete(GalleryImage image);
    }

    public void setListener(OnImageActionListener listener) {
        this.listener = listener;
    }

    public void setImages(List<GalleryImage> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
        return new GalleryImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryImageViewHolder holder, int position) {
        GalleryImage image = images.get(position);
        holder.bind(image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class GalleryImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textLabel, textDescription, textUuid;
        ImageButton btnMenu;

        public GalleryImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view_thumbnail);
            textLabel = itemView.findViewById(R.id.text_view_label);
            textDescription = itemView.findViewById(R.id.text_view_description);
            textUuid = itemView.findViewById(R.id.text_view_uuid);
            btnMenu = itemView.findViewById(R.id.button_menu);
        }

        void bind(GalleryImage image) {
            textLabel.setText(image.getLabel());
            textDescription.setText(image.getDescription());
            textUuid.setText("ID: $image:" + image.getUuid() + "$");

            Glide.with(itemView.getContext())
                    .load(image.getImagePath())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imageView);

            btnMenu.setOnClickListener(v -> {
                // Simple popup menu could be added here, for now just trigger edit
                if (listener != null) listener.onEdit(image);
            });

            // Long press to delete
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onDelete(image);
                return true;
            });
        }
    }
}