package com.example.glazzy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Adapter untuk menampilkan tab kategori secara horizontal di RecyclerView
public class CategoryPagerAdapter extends RecyclerView.Adapter<CategoryPagerAdapter.ViewHolder> {

    Context context;
    List<CategoryModel> categories;
    OnCategoryClickListener listener;
    int selectedPosition = 0; // Menyimpan posisi tab kategori yang sedang aktif

    // Interface callback — dipanggil saat user menekan salah satu tab kategori
    interface OnCategoryClickListener {
        void onClick(CategoryModel category, int position);
    }

    // Konstruktor — menerima daftar kategori & listener dari Activity/Fragment
    public CategoryPagerAdapter(Context context, List<CategoryModel> categories,
                                OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    // Inflate layout item_category untuk setiap tab kategori
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel cat = categories.get(position);

        // Tampilkan nama saja, tanpa emoji
        holder.catName.setText(cat.name);
        holder.catName.setBackground(null);

        if (selectedPosition == position) {
            holder.catName.setTextColor(context.getResources().getColor(R.color.primary, null));
            holder.catName.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.catName.setTextColor(context.getResources().getColor(R.color.text_secondary, null));
            holder.catName.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            int prev = selectedPosition;
            selectedPosition = pos;
            notifyItemChanged(prev);
            notifyItemChanged(pos);
            listener.onClick(categories.get(pos), pos);
        });
    }

    // Mengembalikan jumlah total tab kategori
    @Override
    public int getItemCount() { return categories.size(); }

    // ViewHolder — menyimpan referensi TextView nama kategori
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView catName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            catName = itemView.findViewById(R.id.categoryName);
        }
    }
}