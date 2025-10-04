package com.example.testing;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

// This class will hold a Conversation object and the name of the character it belongs to.
public class ConversationWithCharacter {

    @Embedded
    public Conversation conversation;

    @ColumnInfo(name = "name") // This is the 'name' column from the 'character' table
    public String characterName;
}