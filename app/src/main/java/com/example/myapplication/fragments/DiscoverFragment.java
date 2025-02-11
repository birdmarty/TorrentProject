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
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.example.myapplication.cache.TorrentCacheManager;
import com.example.myapplication.parsing.CategoryList;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.SortList;
import com.example.myapplication.repository.TorrentRepository;

import java.util.ArrayList;

public class DiscoverFragment extends Fragment implements TorrentAdapter.RecyclerviewListener {

    private static final String TAG = "DiscoverFragment";
    private ImageView loadingIcon;
    private EditText inputSearch;
    private Spinner categorySpinner, sortSpinner;
    private String searchUrl;
    private String keyword = "";
    private String sortItem = "Seeds DESC";
    private String category = "All";
    private TorrentCacheManager cacheManager;
    private TorrentAdapter torrentAdapter;
    private ArrayList<SearchResult> torrentsList = new ArrayList<>();
    private TorrentRepository torrentRepository;
    private Handler uiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        torrentRepository = new TorrentRepository();
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
        torrentAdapter = new TorrentAdapter(getContext(), this, torrentsList);
        recyclerView.setAdapter(torrentAdapter);

        inputSearch = rootView.findViewById(R.id.inputSearch);
        inputSearch.setOnEditorActionListener(editorActionListener);

        setupSpinners();
        initializeCache();

        return rootView;
    }

    private void initializeCache() {
        cacheManager = new TorrentCacheManager(requireContext());
        ArrayList<SearchResult> cachedResults = cacheManager.getCachedDiscoverResults();
        if (cachedResults != null && !cachedResults.isEmpty()) {
            torrentsList.clear();
            torrentsList.addAll(cachedResults);
            torrentAdapter.notifyDataSetChanged();
        } else {
            loadTrendingTorrents();
        }
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.category_options, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                category = parent.getItemAtPosition(pos).toString();
                if (!category.equals("All") && !keyword.isEmpty()) {
                    searchTorrents(keyword);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                category = "All";
            }
        });

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                sortItem = parent.getItemAtPosition(pos).toString();
                if (!sortItem.equals("Sort By...") && !keyword.isEmpty()) {
                    searchTorrents(keyword);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sortItem = "Sort By...";
            }
        });
    }

    private void loadTrendingTorrents() {
        showLoading();
        torrentRepository.getTrendingTorrents(new TorrentRepository.TorrentListener() {
            @Override
            public void onResultsReady(ArrayList<SearchResult> results) {
                uiHandler.post(() -> {
                    cacheManager.cacheDiscoverResults(results);
                    torrentsList.clear();
                    torrentsList.addAll(results);
                    torrentAdapter.notifyDataSetChanged();
                    hideLoading();
                });
            }

            @Override
            public void onError(String message) {
                uiHandler.post(() -> {
                    hideLoading();
                    showToast("Error: " + message);
                });
            }
        });
    }

    private void searchTorrents(String keyword) {
        showLoading();
        torrentsList.clear();
        torrentAdapter.notifyDataSetChanged();

        buildSearchUrl();

        torrentRepository.searchTorrents(searchUrl, new TorrentRepository.TorrentListener() {
            @Override
            public void onResultsReady(ArrayList<SearchResult> results) {
                uiHandler.post(() -> {
                    torrentsList.clear();
                    torrentsList.addAll(results);
                    torrentAdapter.notifyDataSetChanged();
                    hideLoading();
                });
            }

            @Override
            public void onError(String message) {
                uiHandler.post(() -> {
                    hideLoading();
                    showToast("Error: " + message);
                });
            }
        });
    }

    private void buildSearchUrl() {
        try {
            if (!category.equals("All") && !sortItem.equals("Sort by...")) {
                searchUrl = "https://1337x.to/sort-category-search/" + keyword
                        + new CategoryList(category).getCategory()
                        + new SortList(sortItem).getSort() + "1/";
            } else if (category.equals("All") && !sortItem.equals("Sort By...")) {
                searchUrl = new SortList(sortItem).urlSortSearch(keyword);
            } else {
                searchUrl = new CategoryList(category).urlCategorySearch(keyword);
            }
        } catch (Exception e) {
            showToast("Invalid search parameters");
        }
    }

    private void showLoading() {
        uiHandler.post(() -> {
            loadingIcon.setVisibility(View.VISIBLE);
            Glide.with(getContext())
                    .load(R.drawable.loading)
                    .into(loadingIcon);
        });
    }

    private void hideLoading() {
        uiHandler.post(() -> loadingIcon.setVisibility(View.GONE));
    }

    private void showToast(String message) {
        uiHandler.post(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
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
        // Handle item click
    }
}