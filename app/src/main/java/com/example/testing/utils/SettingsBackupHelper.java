package com.example.testing.utils;

import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.testing.ui.settings.SettingsViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsBackupHelper {

    private final ComponentActivity activity;
    private final SettingsViewModel viewModel;

    private final ActivityResultLauncher<String> createBackupLauncher;
    private final ActivityResultLauncher<String[]> openBackupLauncher;
    private final ActivityResultLauncher<String> createCharExportLauncher;
    private final ActivityResultLauncher<String[]> openCharImportLauncher;

    public SettingsBackupHelper(ComponentActivity activity, SettingsViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;

        this.createBackupLauncher = activity.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
            if (uri != null) viewModel.exportBackup(uri, activity.getContentResolver());
            else showToast("Export cancelled");
        });

        this.openBackupLauncher = activity.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) viewModel.importBackup(uri, activity.getContentResolver());
            else showToast("Import cancelled");
        });

        this.createCharExportLauncher = activity.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
            if (uri != null) viewModel.exportCharacters(uri, activity.getContentResolver());
            else showToast("Character export cancelled");
        });

        this.openCharImportLauncher = activity.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) viewModel.importCharacters(uri, activity.getContentResolver());
            else showToast("Character import cancelled");
        });
    }

    public void launchFullBackupExport() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        createBackupLauncher.launch("chatterbox_backup_" + timeStamp + ".zip");
    }

    public void launchFullBackupImport() {
        openBackupLauncher.launch(new String[]{"application/zip", "application/octet-stream"});
    }

    public void launchCharacterExport() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        createCharExportLauncher.launch("characters_export_" + timeStamp + ".zip");
    }

    public void launchCharacterImport() {
        openCharImportLauncher.launch(new String[]{"application/zip", "application/octet-stream"});
    }

    private void showToast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }
}