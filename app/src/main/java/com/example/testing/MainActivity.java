package com.example.testing;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private CharacterViewModel characterViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Setup RecyclerView ---
        RecyclerView recyclerView = findViewById(R.id.character_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        final CharacterAdapter adapter = new CharacterAdapter();
        recyclerView.setAdapter(adapter);

        // --- Setup ViewModel and LiveData Observer ---
        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        characterViewModel.getAllCharacters().observe(this, characters -> {
            // This code runs every time the character list in the database changes.
            adapter.setCharacters(characters);
        });

        // --- Setup FAB ---
        FloatingActionButton fab = findViewById(R.id.fab_add_character);
        fab.setOnClickListener(view -> {
            // For now, let's just add a sample character to test.
            // Later, this will open the "Character Creation" screen.
            Character testCharacter = new Character("Gandalf", "A wise and powerful wizard.", "You shall not pass!", "gpt-4", null, null, null);
            characterViewModel.insert(testCharacter);
            Toast.makeText(this, "Test Character Added!", Toast.LENGTH_SHORT).show();
        });
    }
}