package com.example.glazzy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class AboutFragment extends Fragment {

    RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView statArticles = view.findViewById(R.id.statArticles);
        TextView statCategories = view.findViewById(R.id.statCategories);
        View btnWebsite = view.findViewById(R.id.btnWebsite);
        ImageView btnBack = view.findViewById(R.id.btnBack);

        // Pakai 1 RequestQueue yang sama, simpan di field biar tidak GC
        requestQueue = Volley.newRequestQueue(requireContext().getApplicationContext());

        // Tombol back → kembali ke Home
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).closeAbout();
            }
        });

        // Buka website saat diklik
        btnWebsite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://glazzy.web.id"));
            startActivity(intent);
        });

        // Fetch total artikel via header X-WP-Total (lebih akurat & ringan)
        String articleUrl = "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=1&page=1";
        JsonArrayRequest articleReq = new JsonArrayRequest(Request.Method.GET, articleUrl, null,
                response -> {
                    // Tidak bisa baca header dari JsonArrayRequest langsung,
                    // pakai fallback hitung dari semua post
                    fetchArticleCount(statArticles);
                },
                error -> {
                    Log.e("GLAZZY_ABOUT", "Article error: " + error.toString());
                    statArticles.setText("—");
                }
        );
        articleReq.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));

        // Fetch total kategori
        String catUrl = "https://glazzy.web.id/wp-json/wp/v2/categories?per_page=100";
        JsonArrayRequest catReq = new JsonArrayRequest(Request.Method.GET, catUrl, null,
                response -> {
                    // Kurangi 1 untuk kategori "Uncategorized"
                    int count = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String name = response.getJSONObject(i).getString("name");
                            if (!name.equalsIgnoreCase("uncategorized")) count++;
                        } catch (Exception e) {
                            Log.e("GLAZZY_ABOUT", "Cat parse error: " + e.getMessage());
                        }
                    }
                    statCategories.setText(String.valueOf(count));
                },
                error -> {
                    Log.e("GLAZZY_ABOUT", "Category error: " + error.toString());
                    statCategories.setText("—");
                }
        );
        catReq.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));

        fetchArticleCount(statArticles);
        requestQueue.add(catReq);

        return view;
    }

    // Ambil total artikel dari semua halaman
    void fetchArticleCount(TextView statArticles) {
        // WP REST API: ambil semua post, hitung total
        // Gunakan per_page=100 lalu cek berapa yang kembali
        String url = "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=100&page=1";
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 100) {
                        // Kemungkinan ada lebih dari 100, fetch halaman berikutnya
                        fetchArticleCountPage(statArticles, 2, 100);
                    } else {
                        statArticles.setText(String.valueOf(response.length()));
                    }
                },
                error -> {
                    Log.e("GLAZZY_ABOUT", "Article page error: " + error.toString());
                    statArticles.setText("—");
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));
        if (requestQueue != null) requestQueue.add(req);
    }

    void fetchArticleCountPage(TextView statArticles, int page, int accumulated) {
        String url = "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=100&page=" + page;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    int total = accumulated + response.length();
                    if (response.length() == 100) {
                        fetchArticleCountPage(statArticles, page + 1, total);
                    } else {
                        statArticles.setText(String.valueOf(total));
                    }
                },
                error -> {
                    // Halaman tidak ada = sudah habis, tampilkan yang sudah terkumpul
                    statArticles.setText(String.valueOf(accumulated));
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));
        if (requestQueue != null) requestQueue.add(req);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) requestQueue.cancelAll(this);
    }
}