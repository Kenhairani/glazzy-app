package com.example.glazzy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HistoryManager {

    private static final String PREF_NAME   = "glazzy_history";
    private static final String KEY_HISTORY = "history_list";
    private static final int    MAX_HISTORY = 20;
    private static final long   EXPIRED_MS  = 30L * 24 * 60 * 60 * 1000; // 30 hari dalam ms

    public static void addHistory(Context context, PostModel post) {
        ArrayList<PostModel> history = getHistory(context);
        history.removeIf(p -> p.title.equals(post.title)); // cegah duplikat
        post.savedAt = System.currentTimeMillis();
        history.add(0, post); // sisip di depan
        if (history.size() > MAX_HISTORY) history.remove(history.size() - 1); // buang yang paling lama
        saveHistory(context, history);
    }

    public static ArrayList<PostModel> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<PostModel>>(){}.getType();
        ArrayList<PostModel> history = new Gson().fromJson(json, type);

        // Hapus artikel yang sudah expired (> 30 hari)
        boolean adaYangDihapus = history.removeIf(p -> {
            long selisih = System.currentTimeMillis() - p.savedAt;
            return selisih > EXPIRED_MS;
        });

        if (adaYangDihapus) saveHistory(context, history); // update storage jika ada perubahan
        return history;
    }

    // Hapus seluruh history (dipanggil dari tombol "Clear History")
    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_HISTORY).apply();
    }

    // Serialisasi ArrayList ke JSON lalu simpan ke SharedPreferences
    public static void saveHistory(Context context, ArrayList<PostModel> history) {
        String json = new Gson().toJson(history);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_HISTORY, json).apply();
    }

    // Validasi setiap artikel di history ke server WordPress
    public static void validateHistory(Context context, Runnable onComplete) {
        ArrayList<PostModel> history = getHistory(context);
        if (history.isEmpty()) {
            onComplete.run();
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(4); // 4 thread paralel
        List<PostModel> toRemove = Collections.synchronizedList(new ArrayList<>()); // thread-safe
        CountDownLatch latch = new CountDownLatch(history.size()); // tunggu semua thread
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Cek setiap artikel secara paralel
        for (PostModel post : history) {
            executor.execute(() -> {
                try {
                    boolean exists = checkPostExists(post);
                    if (!exists) toRemove.add(post);
                } catch (Exception e) {
                    // Network error → jangan hapus
                } finally {
                    latch.countDown();
                }
            });
        }

        // Thread terakhir: tunggu semua cek selesai, lalu bersihkan & callback
        executor.execute(() -> {
            try {
                latch.await(15, TimeUnit.SECONDS); // timeout 15 detik
            } catch (InterruptedException ignored) {}

            if (!toRemove.isEmpty()) {
                history.removeAll(toRemove);
                saveHistory(context, history);
            }

            executor.shutdown();
            mainHandler.post(onComplete); // kembali ke main thread
        });
    }

    private static boolean checkPostExists(PostModel post) throws Exception {
        String urlStr;
        if (post.postId > 0) {
            urlStr = "https://glazzy.com/wp-json/wp/v2/posts/" + post.postId + "?_fields=id,status";
        } else {
            String slug = extractSlug(post.link);
            if (slug == null) return true;
            urlStr = "https://glazzy.com/wp-json/wp/v2/posts?slug=" + slug + "&_fields=id,status";
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();

        if (post.postId > 0) {
            conn.disconnect();
            return code != 404;
        } else {
            if (code != 200) {
                conn.disconnect();
                return true;
            }
            String body = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
            conn.disconnect();
            return !body.trim().equals("[]");
        }
    }

    // Ambil slug (bagian terakhir URL) dari link artikel
    private static String extractSlug(String link) {
        if (link == null || link.isEmpty()) return null;
        String[] parts = link.replaceAll("/$", "").split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}