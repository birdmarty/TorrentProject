package com.example.myapplication.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.WatchedTorrentAdapter;
import com.example.myapplication.models.WatchedTorrent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvContinueWatching;
    private ProgressBar progressBar;
    private WatchedTorrentAdapter adapter;
    private final List<WatchedTorrent> watchedTorrents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        rvContinueWatching = view.findViewById(R.id.rv_continue_watching);
        progressBar = view.findViewById(R.id.progress_bar);

        // Setup RecyclerView
        adapter = new WatchedTorrentAdapter(requireContext(), watchedTorrents, torrent -> {
            // Handle item click - you can implement torrent playback here
        });

        rvContinueWatching.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        rvContinueWatching.setAdapter(adapter);

        // Load data
        loadWatchedContent();

        return view;
    }

    private void loadWatchedContent() {
        progressBar.setVisibility(View.VISIBLE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("watched_torrents")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .orderBy("__name__") // Add this line
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WatchedTorrent> torrents = queryDocumentSnapshots.toObjects(WatchedTorrent.class);
                    adapter.updateData(torrents);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading watched content", e);
                    progressBar.setVisibility(View.GONE);
                });
    }
}