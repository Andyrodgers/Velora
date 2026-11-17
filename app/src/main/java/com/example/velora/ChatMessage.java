package com.example.velora;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String remoteId;
    private String senderId;
    private String receiverId;
    private String senderEmail;
    private String receiverEmail;
    private String message;
    private long timestamp;
    private boolean isSynced;
    private String chatType; // "community" or "partner"
    private int status; // 0:PENDING, 1:SENT, 2:DELIVERED, 3:READ
    private boolean seen; // Legacy compatibility
    private boolean received; // Legacy compatibility
    
    // Reply fields
    private String repliedMessageId;
    private String repliedText;
    private String repliedSenderName;

    public ChatMessage() {
    }

    public ChatMessage(String remoteId, String senderId, String receiverId, String message, long timestamp, String chatType) {
        this.remoteId = remoteId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.chatType = chatType;
        this.isSynced = false;
        this.status = 0; // PENDING
        this.seen = false;
        this.received = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }
    
    public String getMessageId() { return remoteId; }
    public void setMessageId(String messageId) { this.remoteId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public boolean isSeen() { return status == 3 || seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public boolean isReceived() { return status >= 2 || received; }
    public void setReceived(boolean received) { this.received = received; }

    public String getRepliedMessageId() { return repliedMessageId; }
    public void setRepliedMessageId(String repliedMessageId) { this.repliedMessageId = repliedMessageId; }

    public String getRepliedText() { return repliedText; }
    public void setRepliedText(String repliedText) { this.repliedText = repliedText; }

    public String getRepliedSenderName() { return repliedSenderName; }
    public void setRepliedSenderName(String repliedSenderName) { this.repliedSenderName = repliedSenderName; }
}
