package com.example.myapplication.fragments;

import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.bumptech.glide.Glide;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.example.myapplication.parsing.CategoryList;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.SortList;
import com.example.myapplication.repository.TorrentRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoverFragment extends Fragment implements TorrentAdapter.RecyclerviewListener {

    private ImageView loadingIcon;
    private EditText inputSearch;
    private Spinner categorySpinner, sortSpinner;
    private String  searchUrl;
    private String keyword = "";
    private String sortItem = "Seeds DESC";
    private String category = "All";
    private int current = 0;
    private String TAG = "101";

    private TorrentAdapter torrentAdapter;
    private ArrayList<SearchResult> TorrentsList = new ArrayList<>();

    private TorrentRepository torrentRepository;
    private ExecutorService executorService;
    private Handler uiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        torrentRepository = new TorrentRepository();
        executorService = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerview);
        loadingIcon = rootView.findViewById(R.id.loadingIcon);
        categorySpinner = rootView.findViewById(R.id.categorySpinner);
        sortSpinner = rootView.findViewById(R.id.sortSpinner);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        torrentAdapter = new TorrentAdapter(this, TorrentsList);
        recyclerView.setAdapter(torrentAdapter);

        inputSearch = rootView.findViewById(R.id.inputSearch);
        inputSearch.setOnEditorActionListener(editorActionListener);

        // Set up the spinners
        setUpSpinners();

        // Load trending torrents
        loadTrendingTorrents();

        return rootView;
    }

    private void setUpSpinners() {
        // Set up category spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.category_options, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set up sort spinner
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // Handle category spinner selection
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                category = parentView.getItemAtPosition(position).toString();
                if (!category.equals("All") && !keyword.isEmpty())
                {
                    searchTorrents(keyword);  // Trigger search with the selected category
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                category = "All";
            }
        });

        // Handle sort spinner selection
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                sortItem = parentView.getItemAtPosition(position).toString();
                if (!sortItem.equals("Sort By...") && !keyword.isEmpty()) {
                    searchTorrents(keyword);  // Trigger search with the selected sorting option
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                sortItem = "Sort By...";
            }
        });
    }

    private void loadTrendingTorrents() {
        showLoading(); // Show loading icon

        executorService.execute(() -> {
            try {
                ArrayList<SearchResult> results = torrentRepository.getTrendingTorrents();
                uiHandler.post(() -> {
                    TorrentsList.clear();
                    TorrentsList.addAll(results);
                    torrentAdapter.notifyDataSetChanged();
                    hideLoading(); // Hide loading icon when done
                });
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading(); // Hide loading icon if an error occurs
            }
        });
    }

    private void searchTorrents(String keyword) {
        showLoading(); // Show loading icon
        TorrentsList.clear();
        torrentAdapter.notifyDataSetChanged();

        if (!category.equals("All") && !sortItem.equals("Sort by...")) {
            searchUrl = "https://1337x.to/sort-category-search/" + keyword + new CategoryList(category).getCategory() + new SortList(sortItem).getSort() + "1/";
        }

        else if (category.equals("All") && !sortItem.equals("Sort By...")) {
            searchUrl = new SortList(sortItem).urlSortSearch(keyword);
        }

        else {
            searchUrl = new CategoryList(category).urlCategorySearch(keyword);
        }

        Log.d(TAG, searchUrl);
        executorService.execute(() -> {
            try {
                ArrayList<SearchResult> results = torrentRepository.searchTorrents(searchUrl);
                uiHandler.post(() -> {
                    TorrentsList.clear();
                    TorrentsList.addAll(results);
                    torrentAdapter.notifyDataSetChanged();
                    hideLoading(); // Hide loading icon when done
                });
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading(); // Hide loading icon if an error occurs
            }
        });
    }

    private void showLoading() {
        uiHandler.post(() -> {
            loadingIcon.setVisibility(View.VISIBLE);
            // Load the GIF using Glide
            Glide.with(getContext())
                    .load(R.drawable.loading)  // This is your GIF file
                    .into(loadingIcon);
        });
    }

    private void hideLoading() {
        uiHandler.post(() -> loadingIcon.setVisibility(View.GONE));
    }

    private final TextView.OnEditorActionListener editorActionListener = (v, actionId, event) -> {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
            keyword = inputSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                searchTorrents(keyword);
                return true;
            }
        }
        return false;
    };

    @Override
    public void onItemClick(int position) {

    }
}
