package com.example.glazzy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;

public class CategoryFragmentAdapter extends FragmentStateAdapter {

    ArrayList<CategoryModel> categories;

    public CategoryFragmentAdapter(FragmentManager fm, Lifecycle lifecycle,
                                   ArrayList<CategoryModel> categories) {
        super(fm, lifecycle);
        this.categories = categories;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return CategoryPostsFragment.newInstance(categories.get(position).id);
    }

    @Override
    public int getItemCount() { return categories.size(); }

    @Override
    public long getItemId(int position) { return categories.get(position).id; }

    @Override
    public boolean containsItem(long itemId) {
        for (CategoryModel c : categories) {
            if (c.id == itemId) return true;
        }
        return false;
    }
}