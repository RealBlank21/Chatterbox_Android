package com.example.testing;

import android.content.Context; // Import Context
import androidx.room.Database;
import androidx.room.Room;       // Import Room
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Character.class, Conversation.class, Message.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // --- DAOs ---
    public abstract UserDao userDao();
    public abstract CharacterDao characterDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();

    // --- Singleton Implementation ---

    // 1. The single instance, marked as 'volatile'
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "chatterbox-db";

    // 2. The public static getInstance() method
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            // 3. The synchronized block
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 4. Building the database
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}