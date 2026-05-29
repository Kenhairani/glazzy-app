package com.example.glazzy;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class BookmarkManager {

    private static final String PREF_NAME = "glazzy_bookmark";
    private static final String KEY_BOOKMARK = "bookmark_list";

    public static void addBookmark(Context context, PostModel post) {
        ArrayList<PostModel> list = getBookmarks(context);
        list.removeIf(p -> p.title.equals(post.title));
        list.add(0, post);
        saveBookmarks(context, list);
    }

    public static void removeBookmark(Context context, PostModel post) {
        ArrayList<PostModel> list = getBookmarks(context);
        list.removeIf(p -> p.title.equals(post.title));
        saveBookmarks(context, list);
    }

    public static boolean isBookmarked(Context context, PostModel post) {
        ArrayList<PostModel> list = getBookmarks(context);
        for (PostModel p : list) {
            if (p.title.equals(post.title)) return true;
        }
        return false;
    }

    public static ArrayList<PostModel> getBookmarks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_BOOKMARK, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<PostModel>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    public static void saveBookmarks(Context context, ArrayList<PostModel> list) {
        String json = new Gson().toJson(list);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_BOOKMARK, json).apply();
    }
}