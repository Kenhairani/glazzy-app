package com.example.glazzy;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    // rvCategory       → RecyclerView tab kategori horizontal
    // viewPager        → swipe antar halaman kategori
    // categoryIndicatorLine → garis bawah yang ikut gerak saat swipe
    // notifBadge       → titik merah penanda ada notif belum dibaca
    // etSearch         → input pencarian artikel
    RecyclerView rvCategory;
    ViewPager2 viewPager;
    View categoryIndicatorLine;
    View notifBadge;
    EditText etSearch;
    ArrayList<CategoryModel> categoryList = new ArrayList<>();
    CategoryPagerAdapter categoryAdapter;
    CategoryFragmentAdapter fragmentAdapter;
    RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvCategory            = view.findViewById(R.id.rvCategory);
        viewPager             = view.findViewById(R.id.viewPager);
        categoryIndicatorLine = view.findViewById(R.id.categoryIndicatorLine);
        etSearch              = view.findViewById(R.id.etSearch);
        notifBadge            = view.findViewById(R.id.notifBadge);
        requestQueue          = Volley.newRequestQueue(requireContext());

        // Klik judul "Glazzy" → buka halaman About
        TextView tvGlazzyTitle = view.findViewById(R.id.tvGlazzyTitle);
        tvGlazzyTitle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openAbout();
            }
        });

        // Setiap karakter yang diketik → panggil filterPosts()
        // Tombol search di keyboard → tutup keyboard
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (etSearch != null && etSearch.isFocused()) {
                    dismissKeyboard();
                }
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPosts(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                dismissKeyboard();
                return true;
            }
            return false;
        });

        // Buka FragmentNotifications via MainActivity
        View btnNotif = view.findViewById(R.id.btnNotif);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openNotifications();
                }
            });
        }

        // Cek badge + artikel baru saat fragment pertama dibuat
        // Tambah tab "All" sebagai kategori pertama (hardcoded)
        // Fetch kategori dari API setelah ViewPager siap (viewPager.post)
        updateBadge();
        checkNewPosts();

        categoryList.add(new CategoryModel(0, "All"));
        setupAdapters();
        viewPager.post(() -> fetchCategories());

        return view;
    }

    // Tampilkan/sembunyikan badge notifikasi sesuai status hasUnread
    public void updateBadge() {
        if (notifBadge != null) {
            notifBadge.setVisibility(
                    NotificationHelper.hasUnread(requireContext())
                            ? View.VISIBLE : View.GONE);
        }
    }

    // Fetch 5 artikel terbaru dari WordPress API
    void checkNewPosts() {
        String url = "https://glazzy.web.id/wp-json/wp/v2/posts?per_page=5&orderby=date&order=desc&_embed&_fields=id,title,excerpt,content,link,date,_embedded,jetpack_featured_media_url";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) return;
                        int latestId = response.getJSONObject(0).getInt("id");
                        int savedId  = NotificationHelper.getLastPostId(requireContext());

                        // Pertama kali buka app → simpan ID, skip notif
                        if (savedId == -1) {
                            NotificationHelper.saveLastPostId(requireContext(), latestId);
                            return;
                        }

                        // Ada artikel baru → kumpulkan semua yang ID-nya > savedId
                        if (latestId > savedId) {
                            ArrayList<PostModel> newPosts = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                int id = obj.getInt("id");
                                if (id <= savedId) break;

                                String title   = obj.getJSONObject("title").getString("rendered");
                                String content = obj.getJSONObject("content").getString("rendered");
                                String excerpt = obj.getJSONObject("excerpt").getString("rendered");
                                String link    = obj.optString("link", "https://glazzy.web.id");

                                String imageUrl = obj.optString("jetpack_featured_media_url", "");
                                if (imageUrl.isEmpty() && obj.has("_embedded")) {
                                    try {
                                        JSONObject embedded = obj.getJSONObject("_embedded");
                                        if (embedded.has("wp:featuredmedia")) {
                                            JSONObject media = embedded
                                                    .getJSONArray("wp:featuredmedia")
                                                    .getJSONObject(0);
                                            imageUrl = media.optString("source_url", "");
                                        }
                                    } catch (Exception ignored) {}
                                }

                                String date = obj.optString("date", "");
                                PostModel post = new PostModel(title, content, excerpt, imageUrl);
                                post.link = link;
                                post.date = date;
                                newPosts.add(post);
                            }

                            if (!newPosts.isEmpty()) {
                                NotificationHelper.saveNewPosts(requireContext(), newPosts);
                                NotificationHelper.saveLastPostId(requireContext(), latestId);
                                updateBadge();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("GLAZZY", "Check new posts error: " + e.getMessage());
                    }
                },
                error -> Log.e("GLAZZY", "Check new posts error: " + error.toString())
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));
        requestQueue.add(request);
    }

    // Tutup keyboard dan hapus fokus dari search bar
    private void dismissKeyboard() {
        if (etSearch == null) return;
        etSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    // Teruskan query pencarian ke CategoryPostsFragment yang sedang aktif
    // Fragment ditemukan lewat tag "f{itemId}" yang dibuat oleh fragmentAdapter
    void filterPosts(String query) {
        int currentPos = viewPager.getCurrentItem();
        long itemId = fragmentAdapter.getItemId(currentPos);
        String tag = "f" + itemId;
        Fragment frag = getChildFragmentManager().findFragmentByTag(tag);
        if (frag instanceof CategoryPostsFragment) {
            ((CategoryPostsFragment) frag).filterByQuery(query);
        }
    }

    void setupAdapters() {
        categoryAdapter = new CategoryPagerAdapter(requireContext(), categoryList, (cat, pos) -> {
            viewPager.setCurrentItem(pos, true);
            updateIndicator(pos, 0f);
        });

        rvCategory.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategory.setAdapter(categoryAdapter);

        fragmentAdapter = new CategoryFragmentAdapter(
                getChildFragmentManager(), getLifecycle(), categoryList);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(fragmentAdapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                categoryAdapter.selectedPosition = position;
                categoryAdapter.notifyDataSetChanged();
                rvCategory.smoothScrollToPosition(position);
                if (etSearch != null) {
                    etSearch.setText("");
                    dismissKeyboard();
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updateIndicator(position, positionOffset); // gerakkan garis indikator
            }
        });

        rvCategory.post(() -> updateIndicator(0, 0f));
    }

    // Animasikan garis bawah tab agar ikut bergerak saat swipe
    // Posisi & lebar garis dihitung dari lebar teks tab,
    // lalu di-interpolasi antara tab sekarang dan tab berikutnya
    void updateIndicator(int position, float offset) {
        LinearLayoutManager lm = (LinearLayoutManager) rvCategory.getLayoutManager();
        if (lm == null) return;
        View currentTab = lm.findViewByPosition(position);
        View nextTab    = lm.findViewByPosition(position + 1);
        if (currentTab == null) return;
        android.widget.TextView currentText = currentTab.findViewById(R.id.categoryName);
        if (currentText == null) return;
        float currentTextWidth = currentText.getPaint().measureText(currentText.getText().toString());
        float currentCenter    = currentTab.getLeft() + currentTab.getWidth() / 2f;
        float currentLeft      = currentCenter - currentTextWidth / 2f;
        if (nextTab != null && offset > 0f) {
            android.widget.TextView nextText = nextTab.findViewById(R.id.categoryName);
            if (nextText == null) return;
            float nextTextWidth = nextText.getPaint().measureText(nextText.getText().toString());
            float nextCenter    = nextTab.getLeft() + nextTab.getWidth() / 2f;
            float nextLeft      = nextCenter - nextTextWidth / 2f;
            float interpolatedLeft  = currentLeft  + (nextLeft  - currentLeft)  * offset;
            float interpolatedWidth = currentTextWidth + (nextTextWidth - currentTextWidth) * offset;
            categoryIndicatorLine.setTranslationX(interpolatedLeft);
            ViewGroup.LayoutParams params = categoryIndicatorLine.getLayoutParams();
            params.width = (int) interpolatedWidth;
            categoryIndicatorLine.setLayoutParams(params);
        } else {
            categoryIndicatorLine.setTranslationX(currentLeft);
            ViewGroup.LayoutParams params = categoryIndicatorLine.getLayoutParams();
            params.width = (int) currentTextWidth;
            categoryIndicatorLine.setLayoutParams(params);
        }
    }

    // Fetch daftar kategori dari WordPress API
    // Tambahkan ke categoryList (skip "Uncategorized")
    // Notify adapter agar tab baru muncul
    void fetchCategories() {
        String url = "https://glazzy.web.id/wp-json/wp/v2/categories?per_page=20";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            int id = obj.getInt("id");
                            String name = obj.getString("name");
                            if (!name.equalsIgnoreCase("uncategorized")) {
                                categoryList.add(new CategoryModel(id, name));
                            }
                        }
                        categoryAdapter.notifyDataSetChanged();
                        fragmentAdapter.notifyDataSetChanged();
                        rvCategory.post(() -> updateIndicator(categoryAdapter.selectedPosition, 0f));
                    } catch (Exception e) {
                        Log.e("GLAZZY", "Category error: " + e.getMessage());
                    }
                },
                error -> Log.e("GLAZZY", "Category fetch error: " + error.toString())
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 2, 1.0f));
        requestQueue.add(request);
    }

    // onResume → refresh badge & cek artikel baru setiap kali fragment aktif kembali
    @Override
    public void onResume() {
        super.onResume();
        updateBadge();
        checkNewPosts();
    }

    // onDestroyView → batalkan semua request Volley yang pending
    // Mencegah callback jalan saat fragment sudah tidak ada (NullPointerException)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) requestQueue.cancelAll(this);
    }
}