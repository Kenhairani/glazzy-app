package com.example.glazzy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;

public class FragmentNotifications extends Fragment {

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefresh;
    View emptyNotif;
    ArrayList<PostModel> postList = new ArrayList<>();
    PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.notifRecycler);
        emptyNotif   = view.findViewById(R.id.emptyNotif);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Tombol back → tutup notifikasi via MainActivity
        // (agar badge & bottom nav ikut diupdate)
        ImageView btnBack = view.findViewById(R.id.btnBackNotif);
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).closeNotifications();
            }
        });

        // Hanya reload dari storage lokal — tidak fetch ke server
        // Data baru masuk lewat checkNewPosts() di HomeFragment
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefresh.setRefreshing(false);
        });

        adapter = new PostAdapter(getContext(), postList, post -> {
            HistoryManager.addHistory(requireContext(), post); // catat ke history
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("title",   post.title);
            intent.putExtra("content", post.content  != null ? post.content  : "");
            intent.putExtra("image",   post.imageUrl != null ? post.imageUrl : "");
            intent.putExtra("link",    post.link     != null ? post.link     : "");
            intent.putExtra("excerpt", post.excerpt  != null ? post.excerpt  : ""); // ← fix
            startActivity(intent);
        });
        adapter.setShowDate(true);          // tampilkan tanggal terbit
        adapter.setShowBadgeNew(false);
        adapter.setDisableLongPress(true);  // nonaktifkan selection mode

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadNotifications();
        return view;
    }

    // Muat ulang daftar notifikasi dari storage lokal
    void loadNotifications() {
        postList.clear();
        postList.addAll(NotificationHelper.getNewPosts(requireContext()));
        if (adapter != null) adapter.notifyDataSetChanged();

        if (postList.isEmpty()) {
            emptyNotif.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyNotif.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // onResume → reload notifikasi setiap kali fragment aktif kembali
    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }
}