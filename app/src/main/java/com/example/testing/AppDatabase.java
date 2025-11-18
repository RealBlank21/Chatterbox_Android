package com.example.testing;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Incremented version to 3 to support new columns
@Database(entities = {User.class, Character.class, Conversation.class, Message.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract CharacterDao characterDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "chatterbox-db";

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration() // WARNING: This wipes data on schema change
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}