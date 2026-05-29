package com.example.glazzy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    Context context;
    List<PostModel> posts;
    OnPostClickListener listener;
    boolean showDate         = false;
    boolean disableLongPress = false; // set true untuk nonaktifkan selection mode
    boolean showBadgeNew     = true;
    boolean selectionMode    = false;
    Set<Integer> selectedItems = new HashSet<>();
    OnSelectionChangeListener selectionListener;

    // Interface callback: klik item biasa
    interface OnPostClickListener {
        void onClick(PostModel post);
    }

    // Interface callback: jumlah item terpilih berubah
    interface OnSelectionChangeListener {
        void onChange(int count);
    }

    public PostAdapter(Context context, List<PostModel> posts, OnPostClickListener listener) {
        this.context  = context;
        this.posts    = posts;
        this.listener = listener;
    }

    // Setter konfigurasi dari luar (dipanggil sebelum attach ke RecyclerView)
    public void setShowDate(boolean show)            { this.showDate = show; }
    public void setDisableLongPress(boolean disable) { this.disableLongPress = disable; }
    public void setShowBadgeNew(boolean show)        { this.showBadgeNew = show; }
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionListener = listener;
    }

    // Keluar dari selection mode — hapus semua centang
    public void exitSelectionMode() {
        selectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // Centang semua item sekaligus
    public void selectAll() {
        for (int i = 0; i < posts.size(); i++) selectedItems.add(i);
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedItems() { return selectedItems; }

    // Inflate layout item_post.xml untuk setiap baris list
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostModel post = posts.get(position);

        // Judul
        holder.title.setText(post.title != null ? post.title : "");

        // Gambar
        Glide.with(context)
                .load(post.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.image);

        // Excerpt
        String cleanExcerpt = android.text.Html.fromHtml(
                post.excerpt != null ? post.excerpt : "",
                android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
        holder.excerpt.setText(cleanExcerpt);

        // Tanggal relatif — hanya tampil jika showDate=true (Home/Notif)
        // Prioritas: date (dari server) → savedAt (dari local)
        // Badge "NEW" muncul jika artikel terbit < 24 jam & showBadgeNew=true
        if (holder.tvDate != null) {
            if (showDate) {
                String timeText = null;
                if (post.date != null && !post.date.isEmpty()) {
                    timeText = getRelativeTime(post.date);
                } else if (post.savedAt != 0) {
                    timeText = getRelativeTimeFromMillis(post.savedAt);
                }
                if (timeText != null) {
                    holder.tvDate.setVisibility(View.VISIBLE);
                    holder.tvDate.setText(timeText);
                } else {
                    holder.tvDate.setVisibility(View.GONE);
                }
                if (holder.tvBadgeNew != null) {
                    holder.tvBadgeNew.setVisibility(
                            showBadgeNew && post.date != null && isNew(post.date)
                                    ? View.VISIBLE : View.GONE);
                }
            } else {
                holder.tvDate.setVisibility(View.GONE);
                if (holder.tvBadgeNew != null) holder.tvBadgeNew.setVisibility(View.GONE);
            }
        }

        // Waktu terakhir dibuka - hanya di History (showDate=false & savedAt ada)
        if (holder.tvLastOpened != null) {
            if (!showDate && post.savedAt != 0) {
                holder.tvLastOpened.setVisibility(View.VISIBLE);
                holder.tvLastOpened.setText("🕐 " + getRelativeTimeFromMillis(post.savedAt));
            } else {
                holder.tvLastOpened.setVisibility(View.GONE);
            }
        }

        // Checkbox - hanya muncul saat selection mode aktif
        if (holder.checkbox != null) {
            holder.checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            holder.checkbox.setChecked(selectedItems.contains(position));
        }
        holder.itemView.setAlpha(selectionMode && selectedItems.contains(position) ? 0.6f : 1f);

        // Long press → selection mode (dinonaktifkan jika disableLongPress=true)
        holder.itemView.setOnLongClickListener(v -> {
            if (!disableLongPress && !selectionMode) {
                selectionMode = true;
                selectedItems.add(holder.getAdapterPosition());
                notifyDataSetChanged();
                if (selectionListener != null) selectionListener.onChange(selectedItems.size());
            }
            return true;
        });

        // Click
        // • selection mode aktif → toggle centang item ini
        // • selection mode nonaktif → buka artikel
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (selectionMode) {
                if (selectedItems.contains(pos)) selectedItems.remove(pos);
                else selectedItems.add(pos);
                notifyItemChanged(pos);
                if (selectionListener != null) selectionListener.onChange(selectedItems.size());
            } else {
                listener.onClick(post);
            }
        });
    }

    // Parse tanggal ISO dari server → waktu relatif ("2 hours ago", dst)
    String getRelativeTime(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return dateStr;
            return formatDiff(System.currentTimeMillis() - date.getTime(), date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    // Dari timestamp millis lokal → waktu relatif (untuk History & Bookmark)
    String getRelativeTimeFromMillis(long millis) {
        return formatDiff(System.currentTimeMillis() - millis, new Date(millis));
    }

    // Konversi selisih ms → teks: "Just now" / "X minutes ago" / tanggal
    private String formatDiff(long diff, Date fallbackDate) {
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;
        long weeks   = days / 7;

        if (seconds < 60) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        if (hours   < 24) return hours   + " hours ago";
        if (days    < 7)  return days    + " days ago";
        if (weeks   < 4)  return weeks   + " weeks ago";
        return new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(fallbackDate);
    }

    // Cek apakah artikel baru terbit dalam 24 jam → untuk badge "NEW"
    boolean isNew(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            return System.currentTimeMillis() - date.getTime() < 24L * 60 * 60 * 1000;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getItemCount() { return posts.size(); }

    // Simpan referensi semua view dalam satu item agar tidak
    // perlu findViewById berulang saat scroll (efisiensi RecyclerView)
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, excerpt, tvDate, tvLastOpened, tvBadgeNew;
        ImageView image;
        CheckBox checkbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title        = itemView.findViewById(R.id.postTitle);
            excerpt      = itemView.findViewById(R.id.postExcerpt);
            image        = itemView.findViewById(R.id.postImage);
            tvDate       = itemView.findViewById(R.id.tvDate);
            tvLastOpened = itemView.findViewById(R.id.tvLastOpened);
            tvBadgeNew   = itemView.findViewById(R.id.tvBadgeNew);
            checkbox     = itemView.findViewById(R.id.checkbox);
        }
    }
}