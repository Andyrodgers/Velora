package com.example.velora;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE ((senderId = :userId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :userId)) AND chatType = :chatType ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesForConversation(String userId, String otherId, String chatType);

    @Query("SELECT * FROM chat_messages WHERE isSynced = 0")
    List<ChatMessage> getUnsyncedMessages();

    @Update
    void updateMessage(ChatMessage message);
    
    @Query("SELECT * FROM chat_messages WHERE remoteId = :remoteId LIMIT 1")
    ChatMessage getMessageByRemoteId(String remoteId);

    @Query("SELECT * FROM chat_messages WHERE (senderId = :userId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :userId) ORDER BY timestamp DESC LIMIT 1")
    ChatMessage getLastMessage(String userId, String otherId);
}
