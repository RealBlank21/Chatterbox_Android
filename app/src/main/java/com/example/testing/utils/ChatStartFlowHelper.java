package com.example.testing.ui.main.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;

import com.example.testing.R;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Persona;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.ui.character.CharacterViewModel;
import com.example.testing.ui.conversation.ConversationActivity;

import java.util.ArrayList;
import java.util.List;

public class ChatStartFlowHelper {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final CharacterViewModel characterViewModel;

    public ChatStartFlowHelper(Context context, LifecycleOwner lifecycleOwner, CharacterViewModel characterViewModel) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.characterViewModel = characterViewModel;
    }

    public void startCustomChatFlow(Character character) {
        showPersonaSelectionDialog(character);
    }

    private void showPersonaSelectionDialog(Character character) {
        characterViewModel.getAllPersonas().observe(lifecycleOwner, personas -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_selection, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView title = dialogView.findViewById(R.id.text_view_dialog_title);
            title.setText("Select Persona");

            ListView listView = dialogView.findViewById(R.id.list_view_selection);
            Button btnCancel = dialogView.findViewById(R.id.button_dialog_cancel);

            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            names.add("Current");
            ids.add(-1);

            if (personas != null) {
                for (Persona p : personas) {
                    names.add(p.getName());
                    ids.add(p.getId());
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_selection_row, names);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                int selectedPersonaId = ids.get(position);
                dialog.dismiss();
                showScenarioSelectionDialog(character, selectedPersonaId);
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        });
    }

    private void showScenarioSelectionDialog(Character character, int personaId) {
        characterViewModel.getScenariosForCharacter(character.getId()).observe(lifecycleOwner, scenarios -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_selection, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView title = dialogView.findViewById(R.id.text_view_dialog_title);
            title.setText("Select Scenario");

            ListView listView = dialogView.findViewById(R.id.list_view_selection);
            Button btnCancel = dialogView.findViewById(R.id.button_dialog_cancel);

            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            names.add("Default");
            ids.add(-1);

            if (scenarios != null) {
                for (Scenario s : scenarios) {
                    names.add(s.getName());
                    ids.add(s.getId());
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_selection_row, names);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                int selectedScenarioId = ids.get(position);
                dialog.dismiss();
                launchConversation(character, personaId, selectedScenarioId);
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        });
    }

    private void launchConversation(Character character, int personaId, int scenarioId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("CHARACTER_ID", character.getId());
        intent.putExtra("CONVERSATION_ID", -1);
        intent.putExtra("PERSONA_ID", personaId);
        intent.putExtra("SCENARIO_ID", scenarioId);
        context.startActivity(intent);
    }
}