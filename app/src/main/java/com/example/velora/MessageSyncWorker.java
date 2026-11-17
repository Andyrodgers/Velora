package com.example.velora;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import kotlin.Unit;

public class MessageSyncWorker extends Worker {
    private static final String TAG = "MessageSyncWorker";
    private final MessageDao messageDao;

    public MessageSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        messageDao = VeloraDatabase.getInstance(context).messageDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync work...");
        List<ChatMessage> unsyncedMessages = messageDao.getUnsyncedMessages();
        
        if (unsyncedMessages.isEmpty()) {
            Log.d(TAG, "No unsynced messages found.");
            return Result.success();
        }

        boolean allSynced = true;
        for (ChatMessage message : unsyncedMessages) {
            boolean success = syncMessage(message);
            if (!success) {
                allSynced = false;
            }
        }

        return allSynced ? Result.success() : Result.retry();
    }

    private boolean syncMessage(ChatMessage localMsg) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        // ChatMessageSupabase constructor expects 12 arguments:
        // id, senderId, receiverId, message, timestamp, chatType, isSeen, isReceived, status, repliedMessageId, repliedText, repliedSenderName
        ChatMessageSupabase supabaseMsg = new ChatMessageSupabase(
                null, 
                localMsg.getSenderId(), 
                localMsg.getReceiverId(), 
                localMsg.getMessage(), 
                localMsg.getTimestamp(), 
                localMsg.getChatType(), 
                false, // isSeen
                false, // isReceived
                localMsg.getStatus(), // Added status argument (Integer)
                localMsg.getRepliedMessageId(),
                localMsg.getRepliedText(),
                localMsg.getRepliedSenderName()
        );

        SupabaseHelper.sendMessage(supabaseMsg, (result) -> {
            if (result != null) {
                localMsg.setRemoteId(result.getId());
                localMsg.setSynced(true);
                localMsg.setStatus(1); // SENT (1 grey tick)

                new Thread(() -> {
                    messageDao.updateMessage(localMsg);
                    success[0] = true;
                    latch.countDown();
                }).start();
            } else {
                latch.countDown();
            }
            return Unit.INSTANCE;
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync interrupted", e);
            return false;
        }

        return success[0];
    }
}
