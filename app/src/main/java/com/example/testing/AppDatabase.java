package com.example.testing;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class, Character.class, Conversation.class, Message.class}, version = 11, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract CharacterDao characterDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "chatterbox-db";

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE character ADD COLUMN allow_image_input INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE chat_message ADD COLUMN image_path TEXT");
        }
    };

    // NEW MIGRATION
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add column to user_config with default 0 (Unlimited)
            database.execSQL("ALTER TABLE user_config ADD COLUMN default_context_limit INTEGER NOT NULL DEFAULT 0");
            // Add column to character (nullable)
            database.execSQL("ALTER TABLE character ADD COLUMN context_limit INTEGER");
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_6_7, MIGRATION_10_11)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}