package com.example.testing;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditGalleryImageDialog extends DialogFragment {

    private ImageView imageViewPreview;
    private Button buttonSelectImage;
    private TextInputEditText editTextLabel, editTextDescription;
    private String currentImagePath;
    private GalleryImage editingImage;

    private OnDialogInteractionListener listener;

    public interface OnDialogInteractionListener {
        void onSelectImageClicked(AddEditGalleryImageDialog dialog);
        void onSaveClicked(String label, String description, String imagePath, GalleryImage editingImage);
    }

    public static AddEditGalleryImageDialog newInstance(GalleryImage image) {
        AddEditGalleryImageDialog frag = new AddEditGalleryImageDialog();
        if (image != null) {
            frag.editingImage = image;
            frag.currentImagePath = image.getImagePath();
        }
        return frag;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDialogInteractionListener) {
            listener = (OnDialogInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDialogInteractionListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_edit_gallery_image, null);

        imageViewPreview = view.findViewById(R.id.image_view_preview);
        buttonSelectImage = view.findViewById(R.id.button_select_image);
        editTextLabel = view.findViewById(R.id.edit_text_label);
        editTextDescription = view.findViewById(R.id.edit_text_description);

        if (editingImage != null) {
            editTextLabel.setText(editingImage.getLabel());
            editTextDescription.setText(editingImage.getDescription());
            if (currentImagePath != null) {
                Glide.with(this).load(currentImagePath).into(imageViewPreview);
            }
        }

        buttonSelectImage.setOnClickListener(v -> listener.onSelectImageClicked(this));

        builder.setView(view)
                .setTitle(editingImage == null ? "Add Image" : "Edit Image Details")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String label = editTextLabel.getText().toString();
                    String desc = editTextDescription.getText().toString();
                    listener.onSaveClicked(label, desc, currentImagePath, editingImage);
                });

        return builder.create();
    }

    public void setImage(Uri uri) {
        this.currentImagePath = uri.toString(); // Or handle file path
        if (imageViewPreview != null) {
            Glide.with(this).load(uri).into(imageViewPreview);
        }
    }

    // Helper to update image path after cropping (called by Activity)
    public void updateImagePath(String path) {
        this.currentImagePath = path;
        if (imageViewPreview != null) {
            Glide.with(this).load(path).into(imageViewPreview);
        }
    }
}