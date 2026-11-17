package com.example.velora;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    // API Key is retrieved from BuildConfig which is generated during build
    private static final String API_KEY = BuildConfig.OPENROUTER_API_KEY;
    
    private final OkHttpClient client;
    private final Handler mainHandler;
    
    // Maintain chat history for contextual replies
    private final JSONArray chatMessages;

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public AIService() {
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.chatMessages = new JSONArray();
        
        // Add initial system message to set personality
        try {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are Vela, a compassionate AI health companion for the Velora app. Your tone is warm and supportive.");
            chatMessages.put(systemMessage);
        } catch (JSONException ignored) {}
    }

    public void sendMessage(String userMessage, final AIResponseCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            callback.onError("API Key not configured. Please add it to your local.properties file.");
            return;
        }

        try {
            // Add user message to history
            JSONObject userJson = new JSONObject();
            userJson.put("role", "user");
            userJson.put("content", userMessage);
            chatMessages.put(userJson);

            // Prepare the request body
            JSONObject jsonBody = new JSONObject();
            // Using the requested model from OpenRouter
            jsonBody.put("model", "z-ai/glm-5-turbo"); 
            jsonBody.put("messages", chatMessages);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // Build request with OpenRouter required headers
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://velora-app.com")
                    .addHeader("X-Title", "Velora")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    postErrorMessage(callback, "Network Error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "";
                        postErrorMessage(callback, "API Error " + response.code() + ": " + errBody);
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String aiReply = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        
                        // Add assistant response to history for context in next message
                        JSONObject assistantJson = new JSONObject();
                        assistantJson.put("role", "assistant");
                        assistantJson.put("content", aiReply);
                        chatMessages.put(assistantJson);
                        
                        postSuccessMessage(callback, aiReply);
                    } catch (Exception e) {
                        postErrorMessage(callback, "Parsing Error: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("JSON Error: " + e.getMessage());
        }
    }

    private void postSuccessMessage(final AIResponseCallback callback, final String message) {
        mainHandler.post(() -> callback.onSuccess(message));
    }

    private void postErrorMessage(final AIResponseCallback callback, final String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
