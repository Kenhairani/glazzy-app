package com.example.glazzy;

import android.content.Context;
import android.util.Log;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ArticleValidator {

    public interface OnValidationDone {
        void onDone();
    }

    public static void validateAndClean(Context context, OnValidationDone callback) {
        String url = "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=100&_fields=id,link,title";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Set<String> validLinks = new HashSet<>();
                        Set<String> validTitles = new HashSet<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            String link = obj.optString("link", "");
                            String title = "";
                            if (obj.has("title")) {
                                title = obj.getJSONObject("title").optString("rendered", "");
                            }
                            if (!link.isEmpty()) validLinks.add(link);
                            if (!title.isEmpty()) validTitles.add(title);
                        }

                        Log.d("VALIDATOR", "Valid links: " + validLinks.size());
                        Log.d("VALIDATOR", "Valid titles: " + validTitles.size());

                        cleanHistory(context, validLinks, validTitles);
                        cleanBookmarks(context, validLinks, validTitles);
                        cleanNotifications(context, validLinks, validTitles);

                    } catch (Exception e) {
                        Log.e("VALIDATOR", "Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                    if (callback != null) callback.onDone();
                },
                error -> {
                    Log.e("VALIDATOR", "Fetch error: " + error.toString());
                    if (callback != null) callback.onDone();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1.0f));
        Volley.newRequestQueue(context).add(request);
    }

    static void cleanHistory(Context context, Set<String> validLinks, Set<String> validTitles) {
        ArrayList<PostModel> history = HistoryManager.getHistory(context);
        ArrayList<PostModel> cleaned = new ArrayList<>();
        for (PostModel post : history) {
            if (validLinks.contains(post.link) || validTitles.contains(post.title)) {
                cleaned.add(post);
            }
        }
        Log.d("VALIDATOR", "History before: " + history.size() + " after: " + cleaned.size());
        HistoryManager.saveHistory(context, cleaned);
    }

    static void cleanBookmarks(Context context, Set<String> validLinks, Set<String> validTitles) {
        ArrayList<PostModel> bookmarks = BookmarkManager.getBookmarks(context);
        ArrayList<PostModel> cleaned = new ArrayList<>();
        for (PostModel post : bookmarks) {
            if (validLinks.contains(post.link) || validTitles.contains(post.title)) {
                cleaned.add(post);
            }
        }
        Log.d("VALIDATOR", "Bookmarks before: " + bookmarks.size() + " after: " + cleaned.size());
        BookmarkManager.saveBookmarks(context, cleaned);
    }

    static void cleanNotifications(Context context, Set<String> validLinks, Set<String> validTitles) {
        ArrayList<PostModel> notifs = NotificationHelper.getNewPosts(context);
        ArrayList<PostModel> cleaned = new ArrayList<>();
        for (PostModel post : notifs) {
            if (validLinks.contains(post.link) || validTitles.contains(post.title)) {
                cleaned.add(post);
            }
        }
        Log.d("VALIDATOR", "Notifs before: " + notifs.size() + " after: " + cleaned.size());
        NotificationHelper.saveNewPostsSilent(context, cleaned);
    }
}