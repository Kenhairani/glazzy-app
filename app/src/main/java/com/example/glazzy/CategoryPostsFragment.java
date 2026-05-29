package com.example.glazzy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

// Fragment untuk menampilkan daftar postingan berdasarkan kategori
public class CategoryPostsFragment extends Fragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private static final int PER_PAGE = 10; // Jumlah postingan per halaman (pagination)
    private int categoryId;
    private int currentPage = 1;
    private boolean isLoading = false; // Mencegah request ganda saat sedang load
    private boolean hasMore = true; // Menandai apakah masih ada halaman berikutnya

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefresh;
    ShimmerFrameLayout shimmerLayout;
    View tvEmpty;
    ArrayList<PostModel> postList = new ArrayList<>(); // Semua data dari API
    ArrayList<PostModel> filteredList = new ArrayList<>(); // Data setelah filter pencarian
    PostAdapter postAdapter;
    RequestQueue requestQueue;
    String currentQuery = "";

    // Membuat instance fragment dengan categoryId sebagai argumen
    public static CategoryPostsFragment newInstance(int categoryId) {
        CategoryPostsFragment fragment = new CategoryPostsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY_ID, categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    // Dipanggil pertama kali — ambil categoryId dari argumen & inisialisasi Volley
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getInt(ARG_CATEGORY_ID, 0);
        }
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    // Inflate layout fragment & setup semua komponen UI
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_posts, container, false);

        recyclerView  = view.findViewById(R.id.recyclerView);
        swipeRefresh  = view.findViewById(R.id.swipeRefresh);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        tvEmpty       = view.findViewById(R.id.tvEmpty);

        // Setup adapter — saat item diklik, buka PostDetailActivity
        postAdapter = new PostAdapter(getContext(), filteredList, post -> {
            HistoryManager.addHistory(requireContext(), post);
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("title",   post.title);
            intent.putExtra("content", post.content);
            intent.putExtra("image",   post.imageUrl);
            intent.putExtra("link",    post.link);
            intent.putExtra("date",    post.date != null ? post.date : "");
            intent.putExtra("postId",  post.postId);
            intent.putExtra("excerpt", post.excerpt != null ? post.excerpt : "");
            startActivity(intent);
        });
        postAdapter.setShowDate(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(postAdapter);

        // Infinite scroll — otomatis load halaman berikutnya saat mendekati akhir list
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                int total = layoutManager.getItemCount();
                if (!isLoading && hasMore && lastVisible >= total - 3) {
                    fetchPosts(false); // Load halaman berikutnya
                }
            }
        });

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::refreshPosts);

        fetchPosts(true); // Load halaman pertama saat fragment dibuka
        return view;
    }

    // Reset semua data & mulai ulang dari halaman 1
    void refreshPosts() {
        currentPage = 1;
        hasMore = true;
        currentQuery = "";
        postList.clear();
        filteredList.clear();
        if (postAdapter != null) postAdapter.notifyDataSetChanged();
        fetchPosts(true);
    }

    // Filter postingan berdasarkan kata kunci pencarian (judul / excerpt)
    public void filterByQuery(String query) {
        currentQuery = query;
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(postList); // Tampilkan semua jika kosong
        } else {
            String lower = query.toLowerCase();
            for (PostModel post : postList) {
                if (post.title.toLowerCase().contains(lower)
                        || (post.excerpt != null && post.excerpt.toLowerCase().contains(lower))) {
                    filteredList.add(post);
                }
            }
        }
        updateEmptyState(filteredList.isEmpty() && !query.isEmpty());
        if (postAdapter != null) postAdapter.notifyDataSetChanged();
    }

    // Tampilkan animasi shimmer (skeleton loading) saat data belum tersedia
    void showShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
    }

    // Sembunyikan shimmer setelah data selesai dimuat
    void hideShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
    }

    // Tampilkan/sembunyikan pesan "tidak ada postingan"
    void updateEmptyState(boolean isEmpty) {
        if (tvEmpty == null) return;
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerView != null)
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // Ambil data postingan dari WordPress REST API menggunakan Volley
    void fetchPosts(boolean isFirstPage) {
        if (isLoading || !hasMore) return;
        isLoading = true;

        if (isFirstPage) showShimmer();

        // URL berbeda tergantung categoryId (0 = semua postingan, selainnya = filter kategori)
        String url = categoryId == 0
                ? "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=" + PER_PAGE
                + "&page=" + currentPage + "&orderby=date&order=desc&_embed"
                : "https://glazzy.web.id/wp-json/wp/v2/posts?categories=" + categoryId
                + "&per_page=" + PER_PAGE + "&page=" + currentPage + "&_embed";

        Log.d("GLAZZY", "Fetching page " + currentPage + ": " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("GLAZZY", "Got " + response.length() + " posts (page " + currentPage + ")");
                    try {
                        int startIndex = postList.size();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            // Parsing field utama dari response JSON
                            String title   = obj.getJSONObject("title").getString("rendered");
                            String content = obj.getJSONObject("content").getString("rendered");
                            String excerpt = obj.getJSONObject("excerpt").getString("rendered");
                            String link    = obj.optString("link", "https://glazzy.web.id");
                            String date    = obj.optString("date", "");
                            int id         = obj.getInt("id");

                            // Ambil URL gambar dari _embedded (featured image)
                            String imageUrl = "";
                            if (obj.has("_embedded")) {
                                JSONObject embedded = obj.getJSONObject("_embedded");
                                if (embedded.has("wp:featuredmedia")) {
                                    JSONObject media = embedded
                                            .getJSONArray("wp:featuredmedia")
                                            .getJSONObject(0);
                                    imageUrl = media.optString("source_url", "");
                                }
                            }

                            PostModel post = new PostModel(title, content, excerpt, imageUrl);
                            post.link   = link;
                            post.postId = id;
                            post.date   = date;
                            postList.add(post);
                        }

                        if (response.length() < PER_PAGE) hasMore = false; // Tandai sudah halaman terakhir
                        currentPage++;

                        // Update filteredList sesuai kondisi pencarian aktif atau tidak
                        if (currentQuery.isEmpty()) {
                            filteredList.clear();
                            filteredList.addAll(postList);
                            if (postAdapter != null) postAdapter.notifyDataSetChanged();
                        } else {
                            String lower = currentQuery.toLowerCase();
                            int added = 0;
                            for (int i = startIndex; i < postList.size(); i++) {
                                PostModel p = postList.get(i);
                                if (p.title.toLowerCase().contains(lower)
                                        || (p.excerpt != null && p.excerpt.toLowerCase().contains(lower))) {
                                    filteredList.add(p);
                                    added++;
                                }
                            }
                            if (added > 0 && postAdapter != null) postAdapter.notifyDataSetChanged();
                        }

                        updateEmptyState(filteredList.isEmpty());
                    } catch (Exception e) {
                        Log.e("GLAZZY", "Parsing error: " + e.getMessage());
                    }
                    hideShimmer();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    isLoading = false;
                },
                error -> {
                    Log.e("GLAZZY", "Fetch error: " + error.toString());
                    hasMore = false;
                    hideShimmer();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    isLoading = false;
                    // Kalau gagal dan list masih kosong → load dari backup lokal
                    if (postList.isEmpty() && getContext() != null) {
                        loadFromBackup();
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f)); // Timeout 15 detik
        requestQueue.add(request);
    }

    // Baca file JSON dari folder assets
    private String loadJSONFromAssets(String fileName) {
        try {
            java.io.InputStream is = requireContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Load artikel dari file backup lokal saat API tidak bisa diakses
    void loadFromBackup() {
        try {
            String json = loadJSONFromAssets("artikel_backup.json");
            if (json == null) return;

            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title   = obj.getJSONObject("title").getString("rendered");
                String content = obj.getJSONObject("content").getString("rendered");
                String excerpt = obj.getJSONObject("excerpt").getString("rendered");
                String link    = obj.optString("link", "");
                String date    = obj.optString("date", "");

                String imageUrl = "";
                if (obj.has("_embedded")) {
                    JSONObject embedded = obj.getJSONObject("_embedded");
                    if (embedded.has("wp:featuredmedia")) {
                        imageUrl = embedded.getJSONArray("wp:featuredmedia")
                                .getJSONObject(0)
                                .optString("source_url", "");
                    }
                }

                PostModel post = new PostModel(title, content, excerpt, imageUrl);
                post.link = link;
                post.date = date;
                postList.add(post);
            }

            filteredList.clear();
            filteredList.addAll(postList);
            if (postAdapter != null) postAdapter.notifyDataSetChanged();
            updateEmptyState(filteredList.isEmpty());

        } catch (Exception e) {
            Log.e("GLAZZY", "Backup error: " + e.getMessage());
            updateEmptyState(true);
        }
    }

    // Batalkan semua request Volley saat fragment dihancurkan (cegah memory leak)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) requestQueue.cancelAll(this);
    }
}