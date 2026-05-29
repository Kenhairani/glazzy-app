package com.example.glazzy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {

    TextView contentText;
    String postTitle, htmlContent, imageUrl, postLink, postExcerpt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // Hubungkan variabel Java ke komponen XML layout
        TextView title        = findViewById(R.id.titleDetail);
        contentText           = findViewById(R.id.contentDetail);
        ImageView image       = findViewById(R.id.imageDetail);
        ImageView btnBack     = findViewById(R.id.btnBack);
        ImageView btnBookmark = findViewById(R.id.btnBookmark);
        ImageView btnShare    = findViewById(R.id.btnShare);
        TextView tvDateDetail = findViewById(R.id.tvDateDetail);

        // Data dikirim dari fragment/activity sebelumnya via Intent.putExtra()
        postTitle   = getIntent().getStringExtra("title");
        htmlContent = getIntent().getStringExtra("content");
        imageUrl    = getIntent().getStringExtra("image");
        postLink    = getIntent().getStringExtra("link");
        postExcerpt = getIntent().getStringExtra("excerpt");
        String postDate = getIntent().getStringExtra("date");
        int postId      = getIntent().getIntExtra("postId", 0);

        // Handle deep link
        Uri data = getIntent().getData();
        if (data != null) {
            postLink    = data.toString();
            postTitle   = data.getLastPathSegment();
            htmlContent = "Opening article...";
        }

        title.setText(postTitle != null ? postTitle : "");

        // Tampilkan tanggal
        // Jika date sudah ada di Intent → langsung tampil
        // Jika tidak → fetch ke WordPress API berdasarkan:
        //   • postId  → endpoint single  (/posts/{id})
        //   • postLink → endpoint by slug (/posts?slug=...)
        if (postDate != null && !postDate.isEmpty()) {
            showDate(tvDateDetail, postDate);
        } else {
            tvDateDetail.setVisibility(View.GONE);
            String apiUrl = null;
            boolean isSingle = false;

            // Tentukan URL API yang akan di-fetch
            if (postId > 0) {
                apiUrl = "https://glazzy.web.id/wp-json/wp/v2/posts/" + postId + "?_fields=id,date";
                isSingle = true;
            } else if (postLink != null && !postLink.isEmpty()) {
                String slug = Uri.parse(postLink).getLastPathSegment();
                if (slug != null && !slug.isEmpty()) {
                    apiUrl = "https://glazzy.web.id/wp-json/wp/v2/posts?slug=" + slug + "&_fields=id,date";
                }
            }

            if (apiUrl != null) {
                if (isSingle) {
                    // Response: JsonObject (1 artikel langsung)
                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.GET, apiUrl, null,
                            response -> showDate(tvDateDetail, response.optString("date", "")),
                            error -> tvDateDetail.setVisibility(View.GONE)
                    );
                    Volley.newRequestQueue(this).add(req);
                } else {
                    // Response: JsonArray (ambil index 0 — hasil pencarian slug)
                    JsonArrayRequest req = new JsonArrayRequest(
                            Request.Method.GET, apiUrl, null,
                            response -> {
                                try {
                                    if (response.length() > 0) {
                                        showDate(tvDateDetail,
                                                response.getJSONObject(0).optString("date", ""));
                                    }
                                } catch (Exception e) {
                                    tvDateDetail.setVisibility(View.GONE);
                                }
                            },
                            error -> tvDateDetail.setVisibility(View.GONE)
                    );
                    Volley.newRequestQueue(this).add(req);
                }
            }
        }

        // Glide handle load async + placeholder jika gagal
        Glide.with(this).load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(image);

        // GlideImageGetter → render gambar inline di dalam konten HTML
        // LinkMovementMethod → buat link dalam teks bisa diklik
        contentText.setMovementMethod(LinkMovementMethod.getInstance());
        if (htmlContent != null) {
            Spanned htmlSpanned = Html.fromHtml(htmlContent,
                    Html.FROM_HTML_MODE_LEGACY,
                    new GlideImageGetter(contentText, this), null);
            contentText.setText(htmlSpanned);
        }

        // Setiap artikel yang dibuka otomatis tercatat di HistoryManager
        PostModel currentPost = new PostModel(
                postTitle,
                htmlContent,
                postExcerpt != null ? postExcerpt : "",
                imageUrl);
        currentPost.link   = postLink;
        currentPost.postId = postId;
        currentPost.date   = postDate != null ? postDate : "";
        HistoryManager.addHistory(this, currentPost);

        // Sinkronkan ikon bookmark sesuai status tersimpan atau tidak
        updateBookmarkIcon(btnBookmark, currentPost);

        // Back — tutup activity, kembali ke halaman sebelumnya
        btnBack.setOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Bookmark — toggle simpan/hapus, update ikon setelahnya
        btnBookmark.setOnClickListener(v -> {
            if (BookmarkManager.isBookmarked(this, currentPost)) {
                BookmarkManager.removeBookmark(this, currentPost);
            } else {
                BookmarkManager.addBookmark(this, currentPost);
            }
            updateBookmarkIcon(btnBookmark, currentPost);
        });

        // Share — buka share sheet sistem dengan judul + link artikel
        btnShare.setOnClickListener(v -> {
            String shareUrl = postLink != null ? postLink : "https://glazzy.web.id";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, postTitle);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "📖 " + postTitle + "\n\n" +
                            "Read more:\n" + shareUrl +
                            "\n\nShared via Glazzy App");
            startActivity(Intent.createChooser(shareIntent, "Share article via"));
        });
    }

    // Parse tanggal ISO → teks relatif ("2 hours ago", dst)
    // Jika gagal parse, sembunyikan TextView tanggal
    void showDate(TextView tv, String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(isoDate);
            if (date == null) { tv.setVisibility(View.GONE); return; }

            long diff    = System.currentTimeMillis() - date.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours   = minutes / 60;
            long days    = hours / 24;
            long weeks   = days / 7;

            String relative;
            if (seconds < 60)      relative = "Just now";
            else if (minutes < 60) relative = minutes + " minutes ago";
            else if (hours < 24)   relative = hours + " hours ago";
            else if (days < 7)     relative = days + " days ago";
            else if (weeks < 4)    relative = weeks + " weeks ago";
            else {
                java.text.SimpleDateFormat display = new java.text.SimpleDateFormat(
                        "dd MMM yyyy", new java.util.Locale("en"));
                relative = display.format(date);
            }

            tv.setText(relative);
            tv.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tv.setVisibility(View.GONE);
        }
    }

    // Update ikon bookmark
    void updateBookmarkIcon(ImageView btn, PostModel post) {
        if (BookmarkManager.isBookmarked(this, post)) {
            btn.setImageResource(R.drawable.ic_bookmark_filled);
            btn.setColorFilter(getResources().getColor(R.color.primary, null));
        } else {
            btn.setImageResource(R.drawable.ic_bookmark_outline);
            btn.setColorFilter(getResources().getColor(R.color.text_secondary, null));
        }
    }
}