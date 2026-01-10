package com.example.testing.ui.character.utils;

import android.net.Uri;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.function.Consumer;

public class CharacterImageHandler {

    private final ComponentActivity activity;
    private final Consumer<String> onImageReady;
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    public CharacterImageHandler(ComponentActivity activity, Consumer<String> onImageReady) {
        this.activity = activity;
        this.onImageReady = onImageReady;

        this.cropImageLauncher = activity.registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri uriContent = result.getUriContent();
                if (uriContent != null) {
                    saveImageToInternalStorage(uriContent);
                }
            } else {
                Exception error = result.getError();
                Toast.makeText(activity, "Crop failed: " + (error != null ? error.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
            }
        });

        this.pickMediaLauncher = activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                CropImageOptions options = new CropImageOptions();
                options.fixAspectRatio = true;
                options.aspectRatioX = 1;
                options.aspectRatioY = 1;
                cropImageLauncher.launch(new CropImageContractOptions(uri, options));
            } else {
                Toast.makeText(activity, "No image selected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void launchImagePicker() {
        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            File directory = new File(activity.getFilesDir(), "profile_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, UUID.randomUUID().toString() + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            // Notify callback with the new path
            onImageReady.accept(file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}