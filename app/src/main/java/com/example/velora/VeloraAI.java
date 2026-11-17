package com.example.velora;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vela Local AI Engine
 * Dynamically loads knowledge from all JSON files in the assets folder.
 */
public class VeloraAI {
    private static final String TAG = "VeloraAI";
    private final String aiName = "Vela";
    private final List<KnowledgeItem> knowledgeBase = new ArrayList<>();
    private final Random random = new Random();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<String> fallbackResponses = new ArrayList<>();

    public static class Response {
        public String text;
        public List<String> followUps;
        public boolean isFallback;

        public Response(String text, List<String> followUps, boolean isFallback) {
            this.text = text;
            this.followUps = followUps;
            this.isFallback = isFallback;
        }
    }

    public VeloraAI(Context context) {
        loadAllKnowledgeFiles(context);
    }

    private void loadAllKnowledgeFiles(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] files = assetManager.list("");
            if (files != null) {
                for (String fileName : files) {
                    if (fileName.endsWith(".json")) {
                        loadKnowledgeFromFile(context, fileName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing asset files", e);
        }
        
        if (fallbackResponses.isEmpty()) {
            fallbackResponses.add("I'm here to help, but I'm still learning. Try asking about your cycle or symptoms!");
            fallbackResponses.add("That's a good question. I'm here to support your health journey!");
        }
    }

    private void loadKnowledgeFromFile(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            if (size == 0) {
                is.close();
                return;
            }
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            
            if (json.trim().equals("[]") || json.trim().isEmpty()) return;

            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                
                List<String> keyList = new ArrayList<>();
                if (obj.has("keywords")) {
                    JSONArray keywords = obj.getJSONArray("keywords");
                    for (int j = 0; j < keywords.length(); j++) {
                        keyList.add(keywords.getString(j).toLowerCase());
                    }
                }
                
                if (obj.has("question")) {
                    keyList.add(obj.getString("question").toLowerCase());
                }
                if (obj.has("alternate_questions")) {
                    JSONArray alts = obj.getJSONArray("alternate_questions");
                    for (int j = 0; j < alts.length(); j++) {
                        keyList.add(alts.getString(j).toLowerCase());
                    }
                }

                List<String> respList = new ArrayList<>();
                if (obj.has("responses")) {
                    JSONArray responses = obj.getJSONArray("responses");
                    for (int j = 0; j < responses.length(); j++) {
                        respList.add(responses.getString(j));
                    }
                } else if (obj.has("answer")) {
                    respList.add(obj.getString("answer"));
                }

                List<String> followUps = new ArrayList<>();
                if (obj.has("follow_up")) {
                    JSONArray fu = obj.getJSONArray("follow_up");
                    for (int j = 0; j < fu.length(); j++) {
                        followUps.add(fu.getString(j));
                    }
                }

                if (keyList.contains("default_fallback") && !respList.isEmpty()) {
                    fallbackResponses.addAll(respList);
                } else if (!keyList.isEmpty() && !respList.isEmpty()) {
                    knowledgeBase.add(new KnowledgeItem(keyList, respList, followUps));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading " + fileName + ": " + e.getMessage());
        }
    }

    public void processMessage(String userMessage, final AiResponseCallback callback) {
        final String input = userMessage.toLowerCase().trim();
        
        mainHandler.postDelayed(() -> {
            Response response = findBestMatch(input);
            callback.onResponse(response);
        }, 300);
    }

    private Response findBestMatch(String input) {
        KnowledgeItem bestItem = null;
        int maxMatchCount = 0;

        for (KnowledgeItem item : knowledgeBase) {
            int matchCount = 0;
            for (String keyword : item.keywords) {
                if (input.contains(keyword)) {
                    matchCount += keyword.length();
                }
            }
            
            if (matchCount > maxMatchCount) {
                maxMatchCount = matchCount;
                bestItem = item;
            }
        }

        if (bestItem != null && maxMatchCount > 2) {
            return new Response(bestItem.getRandomResponse(), bestItem.followUps, false);
        }

        String fallbackText = fallbackResponses.get(random.nextInt(fallbackResponses.size()));
        return new Response(fallbackText, null, true);
    }

    public String getAiName() {
        return aiName;
    }

    private static class KnowledgeItem {
        List<String> keywords;
        List<String> responses;
        List<String> followUps;
        Random random = new Random();

        KnowledgeItem(List<String> keywords, List<String> responses, List<String> followUps) {
            this.keywords = keywords;
            this.responses = responses;
            this.followUps = followUps;
        }

        String getRandomResponse() {
            return responses.get(random.nextInt(responses.size()));
        }
    }

    public interface AiResponseCallback {
        void onResponse(Response response);
        void onError(String error);
    }
}
