package com.example.velora;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatMessage.class}, version = 2, exportSchema = false)
public abstract class VeloraDatabase extends RoomDatabase {
    private static volatile VeloraDatabase INSTANCE;

    public abstract MessageDao messageDao();

    public static VeloraDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VeloraDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            VeloraDatabase.class, "velora_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
