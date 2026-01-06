package com.example.testing;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class GalleryManagementActivity extends BaseActivity implements AddEditGalleryImageDialog.OnDialogInteractionListener {

    public static final String EXTRA_OWNER_ID = "owner_id";
    public static final String EXTRA_OWNER_TYPE = "owner_type";

    private GalleryImageViewModel viewModel;
    private GalleryImageAdapter adapter;
    private int ownerId;
    private String ownerType;

    private AddEditGalleryImageDialog activeDialog; // Keep ref to update image

    // Image Picking & Cropping logic
    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri uriContent = result.getUriContent();
                    if (uriContent != null && activeDialog != null) {
                        String savedPath = saveImageToInternalStorage(uriContent);
                        activeDialog.updateImagePath(savedPath);
                    }
                } else {
                    Exception error = result.getError();
                    Toast.makeText(this, "Crop failed: " + (error != null ? error.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    CropImageOptions options = new CropImageOptions();
                    options.fixAspectRatio = false; // Allow free crop for gallery images
                    cropImage.launch(new CropImageContractOptions(uri, options));
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_management);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Gallery Management");

        ownerId = getIntent().getIntExtra(EXTRA_OWNER_ID, -1);
        ownerType = getIntent().getStringExtra(EXTRA_OWNER_TYPE);

        if (ownerId == -1 || ownerType == null) {
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_gallery);
        TextView emptyView = findViewById(R.id.text_view_empty);
        FloatingActionButton fab = findViewById(R.id.fab_add_image);

        adapter = new GalleryImageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(GalleryImageViewModel.class);

        viewModel.getImagesForOwner(ownerId, ownerType).observe(this, images -> {
            if (images == null || images.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setImages(images);
            }
        });

        fab.setOnClickListener(v -> {
            showAddEditDialog(null);
        });

        adapter.setListener(new GalleryImageAdapter.OnImageActionListener() {
            @Override
            public void onEdit(GalleryImage image) {
                showAddEditDialog(image);
            }

            @Override
            public void onDelete(GalleryImage image) {
                new AlertDialog.Builder(GalleryManagementActivity.this)
                        .setTitle("Delete Image")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (d, w) -> viewModel.delete(image))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void showAddEditDialog(GalleryImage image) {
        activeDialog = AddEditGalleryImageDialog.newInstance(image);
        activeDialog.show(getSupportFragmentManager(), "GalleryDialog");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectImageClicked(AddEditGalleryImageDialog dialog) {
        this.activeDialog = dialog; // Ensure we have the ref
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    @Override
    public void onSaveClicked(String label, String description, String imagePath, GalleryImage editingImage) {
        if (imagePath == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingImage == null) {
            // Create new
            GalleryImage newImg = new GalleryImage(imagePath, label, description, ownerId, ownerType);
            viewModel.insert(newImg);
        } else {
            // Update
            editingImage.setLabel(label);
            editingImage.setDescription(description);
            editingImage.setImagePath(imagePath);
            viewModel.update(editingImage);
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File directory = new File(getFilesDir(), "gallery_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            // Generate a simpler filename
            File file = new File(directory, UUID.randomUUID().toString() + ".jpg");

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}