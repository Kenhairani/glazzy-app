package com.example.glazzy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;

public class BookmarkFragment extends Fragment {

    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefresh;
    View emptyState;
    ArrayList<PostModel> postList = new ArrayList<>();
    PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);

        recyclerView = view.findViewById(R.id.bookmarkRecycler);
        emptyState   = view.findViewById(R.id.emptyState);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.primary);

        adapter = new PostAdapter(getContext(), postList, post -> {
            HistoryManager.addHistory(requireContext(), post);
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("title",   post.title);
            intent.putExtra("content", post.content  != null ? post.content  : "");
            intent.putExtra("image",   post.imageUrl != null ? post.imageUrl : "");
            intent.putExtra("link",    post.link     != null ? post.link     : "");
            intent.putExtra("excerpt", post.excerpt  != null ? post.excerpt  : "");
            intent.putExtra("postId",  post.postId);
            startActivity(intent);
        });

        adapter.setShowDate(true);
        adapter.setDisableLongPress(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadBookmarks();
            swipeRefresh.setRefreshing(false);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tampilkan data lokal dulu langsung
        loadBookmarks();
        // Validasi ke WordPress di background, update UI kalau ada yang dihapus
        ArticleValidator.validateAndClean(requireContext(), () -> {
            if (isAdded()) requireActivity().runOnUiThread(this::loadBookmarks);
        });
    }

    void loadBookmarks() {
        postList.clear();
        postList.addAll(BookmarkManager.getBookmarks(requireContext()));
        adapter.notifyDataSetChanged();
        if (postList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}