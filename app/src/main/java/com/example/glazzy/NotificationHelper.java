package com.example.glazzy;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

// NotificationHelper — manajemen notifikasi artikel baru
// Semua data disimpan lokal di SharedPreferences (bukan server)
public class NotificationHelper {

    private static final String PREF_NAME = "glazzy_notif"; // nama file penyimpanan lokal
    private static final String KEY_LAST_POST_ID = "last_post_id"; // ID artikel terakhir yang sudah diketahui
    private static final String KEY_NEW_POSTS = "new_posts"; // daftar artikel baru (JSON)
    private static final String KEY_HAS_UNREAD = "has_unread"; // flag: ada notifikasi yang belum dibaca?

    // Simpan ID artikel terbaru yang sudah diketahui app
    // → next check: jika ada artikel dengan ID lebih besar = artikel baru
    public static void saveLastPostId(Context context, int id) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_LAST_POST_ID, id).apply();
    }

    // Ambil ID artikel terakhir (default -1 jika belum pernah disimpan)
    public static int getLastPostId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_POST_ID, -1);
    }

    // Simpan daftar artikel baru + tandai sebagai UNREAD
    // → savedAt diisi timestamp sekarang agar urutan waktu bisa ditampilkan
    // → KEY_HAS_UNREAD = true → badge notifikasi muncul di HomeFragment
    public static void saveNewPosts(Context context, ArrayList<PostModel> posts) {
        long now = System.currentTimeMillis();
        for (PostModel post : posts) {
            post.savedAt = now;
        }
        String json = new Gson().toJson(posts);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_NEW_POSTS, json)
                .putBoolean(KEY_HAS_UNREAD, true).apply();
    }

    // Simpan daftar artikel baru TANPA mengubah status unread
    // → dipakai saat cleanup/validasi artikel (ArticleValidator)
    //   agar badge tidak reset saat data diperbarui di background
    public static void saveNewPostsSilent(Context context, ArrayList<PostModel> posts) {
        String json = new Gson().toJson(posts);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_NEW_POSTS, json).apply();
    }

    // Ambil daftar artikel baru dari storage lokal
    // → Gson deserialize JSON → ArrayList<PostModel>
    // → return list kosong jika belum ada data
    public static ArrayList<PostModel> getNewPosts(Context context) {
        String json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NEW_POSTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<PostModel>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    // Cek apakah ada notifikasi yang belum dibaca
    // → dipakai di HomeFragment untuk tampilkan/sembunyikan badge
    public static boolean hasUnread(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HAS_UNREAD, false);
    }

    // Tandai semua notifikasi sudah dibaca
    // → dipanggil di MainActivity.closeNotifications()
    // → badge langsung hilang setelah user buka halaman notifikasi
    public static void markAsRead(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_HAS_UNREAD, false).apply();
    }
}