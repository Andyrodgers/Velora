package com.example.velora;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/**
 * AI Chat System for Velora App
 * Android Activity for interacting with Velora AI (Vela)
 */
public class ChatActivity extends BaseActivity {

    private VeloraAI ai;
    private AIService remoteAi;
    private LinearLayout llChatMessages;
    private EditText etChatMessage;
    private Button btnSendChat;
    private ScrollView svChat;
    private LinearLayout llSuggestions;
    private View hsvSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Local AI (Vela)
        this.ai = new VeloraAI(this);
        // Initialize Remote AI (OpenRouter) as fallback/advanced engine
        this.remoteAi = new AIService();

        // Bind Views
        llChatMessages = findViewById(R.id.llChatMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendChat = findViewById(R.id.btnSendChat);
        svChat = findViewById(R.id.svChat);
        llSuggestions = findViewById(R.id.llSuggestions);
        hsvSuggestions = findViewById(R.id.hsvSuggestions);

        // Set Listeners
        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                onSendMessage(message);
                etChatMessage.setText("");
            }
        });

        // Add Initial Greeting
        addMessageToUI("Hi! I'm " + ai.getAiName() + ". I'm here to support you with your cycle and health. How are you feeling today?", false);
    }

    public void onSendMessage(String userMessage) {
        // Clear old suggestions
        hideSuggestions();
        
        // Show user message in UI
        addMessageToUI(userMessage, true);

        // 1. Try Local AI first (fast, accurate for specific knowledge)
        ai.processMessage(userMessage, new VeloraAI.AiResponseCallback() {
            @Override
            public void onResponse(VeloraAI.Response response) {
                if (response.isFallback) {
                    // 2. If Local AI doesn't know, ask the Remote AI (LLM)
                    callRemoteAI(userMessage);
                } else {
                    addMessageToUI(response.text, false);
                    if (response.followUps != null && !response.followUps.isEmpty()) {
                        showSuggestions(response.followUps);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // If local fails (unlikely), try remote
                callRemoteAI(userMessage);
            }
        });
    }

    private void callRemoteAI(String userMessage) {
        remoteAi.sendMessage(userMessage, new AIService.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                addMessageToUI(response, false);
            }

            @Override
            public void onError(String error) {
                addMessageToUI("I'm having a little trouble connecting right now. Try asking me something about your symptoms or cycle!", false);
            }
        });
    }

    private void showSuggestions(List<String> suggestions) {
        llSuggestions.removeAllViews();
        for (String suggestion : suggestions) {
            Button btn = new Button(this);
            btn.setText(suggestion);
            btn.setAllCaps(false);
            btn.setTextSize(14);
            btn.setTextColor(0xFFFF69B4); // Pink text
            btn.setBackgroundResource(R.drawable.bg_suggestion_pill);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            btn.setLayoutParams(params);
            
            btn.setOnClickListener(v -> {
                onSendMessage(suggestion);
            });
            
            llSuggestions.addView(btn);
        }
        hsvSuggestions.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        hsvSuggestions.setVisibility(View.GONE);
        llSuggestions.removeAllViews();
    }

    private void addMessageToUI(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(36, 24, 36, 24);
        textView.setTextSize(16);
        textView.setTextColor(isUser ? 0xFFFFFFFF : 0xFF000000);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16);

        if (isUser) {
            params.gravity = Gravity.END;
            textView.setBackgroundResource(R.drawable.chat_bubble_sent);
            textView.getBackground().setTint(getResources().getColor(R.color.colorPrimary));
        } else {
            params.gravity = Gravity.START;
            textView.setBackgroundResource(R.drawable.chat_bubble_received);
            textView.getBackground().setTint(0xFFFFFFFF);
        }

        textView.setLayoutParams(params);
        llChatMessages.addView(textView);

        // Scroll to bottom
        svChat.post(() -> svChat.fullScroll(View.FOCUS_DOWN));
    }
}
