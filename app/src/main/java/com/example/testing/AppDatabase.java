package com.example.testing;

import android.content.Context;
import android.database.Cursor;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class, Character.class, Conversation.class, Message.class, Persona.class, Scenario.class}, version = 19, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract CharacterDao characterDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract PersonaDao personaDao();
    public abstract ScenarioDao scenarioDao();

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

    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_config ADD COLUMN character_list_mode TEXT DEFAULT 'list'");
        }
    };

    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `persona` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `description` TEXT)");

            boolean columnExists = false;
            try (Cursor cursor = database.query("SELECT * FROM user_config LIMIT 0")) {
                if (cursor != null && cursor.getColumnIndex("current_persona_id") != -1) {
                    columnExists = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!columnExists) {
                database.execSQL("ALTER TABLE user_config ADD COLUMN current_persona_id INTEGER NOT NULL DEFAULT -1");
            }
        }
    };

    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `scenario` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`character_id` INTEGER NOT NULL, " +
                    "`name` TEXT, " +
                    "`description` TEXT, " +
                    "`first_message` TEXT, " +
                    "`is_default` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`character_id`) REFERENCES `character`(`character_id`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            database.execSQL("CREATE INDEX IF NOT EXISTS `index_scenario_character_id` ON `scenario` (`character_id`)");

            boolean scenarioColExists = false;
            boolean personaColExists = false;
            try (Cursor cursor = database.query("SELECT * FROM conversation LIMIT 0")) {
                if (cursor != null) {
                    if (cursor.getColumnIndex("scenario_id") != -1) scenarioColExists = true;
                    if (cursor.getColumnIndex("persona_id") != -1) personaColExists = true;
                }
            } catch (Exception e) { e.printStackTrace(); }

            if (!scenarioColExists) {
                database.execSQL("ALTER TABLE conversation ADD COLUMN scenario_id INTEGER");
            }
            if (!personaColExists) {
                database.execSQL("ALTER TABLE conversation ADD COLUMN persona_id INTEGER");
            }
        }
    };

    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE scenario ADD COLUMN image_path TEXT");
        }
    };

    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE character ADD COLUMN default_scenario TEXT DEFAULT ''");
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_6_7, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}