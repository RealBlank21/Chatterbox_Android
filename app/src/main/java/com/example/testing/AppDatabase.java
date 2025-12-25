package com.example.testing;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class, Character.class, Conversation.class, Message.class}, version = 14, exportSchema = false)
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

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_config ADD COLUMN default_context_limit INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE character ADD COLUMN context_limit INTEGER");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_config ADD COLUMN theme_color_primary INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE user_config ADD COLUMN theme_color_secondary INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE character ADD COLUMN tags TEXT DEFAULT ''");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_last_updated ON conversation(last_updated)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_message_conversation_fk ON chat_message(conversation_fk)");
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_6_7, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}