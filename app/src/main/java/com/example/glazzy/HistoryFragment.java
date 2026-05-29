package com.example.glazzy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HistoryFragment extends Fragment {

    RecyclerView rvHistory;
    TextView tvEmpty, tvSelectedCount;
    ImageButton btnCancelSelect, btnSelectAll, btnDeleteSelected;
    LinearLayout headerNormal, headerSelection;
    View emptyState;
    SwipeRefreshLayout swipeRefresh;
    PostAdapter adapter;
    ArrayList<PostModel> historyList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory         = view.findViewById(R.id.rvHistory);
        tvEmpty           = view.findViewById(R.id.tvEmpty);
        emptyState        = view.findViewById(R.id.emptyState);
        headerNormal      = view.findViewById(R.id.headerNormal);
        headerSelection   = view.findViewById(R.id.headerSelection);
        tvSelectedCount   = view.findViewById(R.id.tvSelectedCount);
        btnCancelSelect   = view.findViewById(R.id.btnCancelSelect);
        btnSelectAll      = view.findViewById(R.id.btnSelectAll);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        swipeRefresh      = view.findViewById(R.id.swipeRefreshHistory);

        // Warna spinner loading saat pull-to-refresh
        swipeRefresh.setColorSchemeColors(
                android.graphics.Color.parseColor("#2D3561"),
                android.graphics.Color.parseColor("#4A5299")
        );

        // User tarik ke bawah → validasi artikel ke server dulu,
        // baru reload list (artikel yang sudah dihapus di server ikut hilang)
        swipeRefresh.setOnRefreshListener(() -> {
            HistoryManager.validateHistory(requireContext(), () -> {
                loadHistory();
                swipeRefresh.setRefreshing(false);
            });
        });

        // Cancel → keluar dari selection mode, kembali ke header normal
        btnCancelSelect.setOnClickListener(v -> {
            adapter.exitSelectionMode();
            headerNormal.setVisibility(View.VISIBLE);
            headerSelection.setVisibility(View.GONE);
        });

        // Select All → centang semua item, update teks jumlah terpilih
        btnSelectAll.setOnClickListener(v -> {
            adapter.selectAll();
            tvSelectedCount.setText(historyList.size() + " selected");
        });

        // Delete Selected → hapus item terpilih dari list
        btnDeleteSelected.setOnClickListener(v -> {
            Set<Integer> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) return;
            List<Integer> sortedList = new ArrayList<>(selected);
            sortedList.sort((a, b) -> b - a); // urut dari index terbesar dulu
            for (int index : sortedList) historyList.remove(index);
            HistoryManager.saveHistory(requireContext(), historyList);
            adapter.exitSelectionMode();
            headerNormal.setVisibility(View.VISIBLE);
            headerSelection.setVisibility(View.GONE);
            loadHistory();
        });

        loadHistory();
        return view;
    }

    void loadHistory() {
        historyList = HistoryManager.getHistory(requireContext());

        if (historyList.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);

            // Klik item → buka PostDetailActivity dengan data artikel
            adapter = new PostAdapter(requireContext(), historyList, post -> {
                Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                intent.putExtra("title",   post.title);
                intent.putExtra("content", post.content  != null ? post.content  : "");
                intent.putExtra("image",   post.imageUrl != null ? post.imageUrl : "");
                intent.putExtra("link",    post.link     != null ? post.link     : "");
                intent.putExtra("excerpt", post.excerpt  != null ? post.excerpt  : "");
                startActivity(intent);
            });

            adapter.setShowDate(false); // History tampilkan waktu terakhir dibuka, bukan tanggal terbit

            adapter.setOnSelectionChangeListener(count -> {
                if (count > 0) { // tampilkan header selection
                    headerNormal.setVisibility(View.GONE);
                    headerSelection.setVisibility(View.VISIBLE);
                    tvSelectedCount.setText(count + " selected");
                } else { // kembali ke header normal
                    headerNormal.setVisibility(View.VISIBLE);
                    headerSelection.setVisibility(View.GONE);
                }
            });

            rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvHistory.setAdapter(adapter);

            // Swipe kiri atau kanan → hapus item dari list & storage
            new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView rv,
                                      @NonNull RecyclerView.ViewHolder vh,
                                      @NonNull RecyclerView.ViewHolder target) { return false; }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int pos = viewHolder.getBindingAdapterPosition();
                    historyList.remove(pos);
                    HistoryManager.saveHistory(requireContext(), historyList);
                    adapter.notifyItemRemoved(pos);
                    if (historyList.isEmpty()) {
                        rvHistory.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    }
                }
            }).attachToRecyclerView(rvHistory);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tampilkan data lokal dulu langsung
        loadHistory();
        // Validasi ke WordPress di background, update UI kalau ada yang dihapus
        ArticleValidator.validateAndClean(requireContext(), () -> {
            if (isAdded()) requireActivity().runOnUiThread(this::loadHistory);
        });
    }
}