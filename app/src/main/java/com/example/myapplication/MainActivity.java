package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.frostwire.jlibtorrent.TorrentInfo;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;


import com.example.myapplication.fragments.SearchResultsFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public TorrentStream torrentStream;
    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static String keyword;
    private static String sortItem = "Seeds DESC";
    public static int current;
    private EditText inputSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        inputSearch = findViewById(R.id.inputSearch);
        inputSearch.setOnEditorActionListener(editorActionListener);

        requestPermissions();
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 and above
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, STORAGE_PERMISSION_CODE);
            } else {
                initTorrentStream();
            }
        } else {
            // For Android 10 and below
            String[] permissions = {
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
            };

            boolean allPermissionsGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE);
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void initTorrentStream() {
        try {
            // Create download directory
            File saveLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            boolean dirCreated = saveLocation.mkdirs();
            Log.d(TAG, "Directory created: " + dirCreated + ", Path: " + saveLocation.getAbsolutePath());

            // Initialize TorrentStream with options
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(saveLocation)
                    .removeFilesAfterStop(false)
                    .maxConnections(0)
                    .maxDownloadSpeed(0) // No limit
                    .maxUploadSpeed(0) // No limit
                    .build();

            torrentStream = TorrentStream.init(torrentOptions);

            if (torrentStream != null) {
                torrentStream.addListener((TorrentListener) this);
                Toast.makeText(this, "TorrentStream initialized successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "TorrentStream initialized successfully");
            } else {
                Toast.makeText(this, "Failed to initialize TorrentStream", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TorrentStream initialization returned null");
            }
        } catch (Exception e) {
            String errorMsg = "Error initializing TorrentStream: " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg, e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initTorrentStream();
                    Toast.makeText(this, "Permissions granted, initializing TorrentStream", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Manage External Storage permission required", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initTorrentStream();
                Toast.makeText(this, "Permissions granted, initializing TorrentStream", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required for downloading", Toast.LENGTH_LONG).show();
            }
        }
    }


    private final TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                keyword = inputSearch.getText().toString();
                if (!keyword.isEmpty()){
                    loadSearchResultsFragment();
                    return true;
                }
                current = 0;
            }

            return false;
        }
    };

    public static String getKeyword() {
        return keyword;
    }

    public static String getSortItem() {
        return sortItem;
    }

    private void loadSearchResultsFragment() {
        // Create an instance of SearchResultsFragment
        SearchResultsFragment fragment = new SearchResultsFragment();

        // Begin a fragment transaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace the fragmentContainer with the SearchResultsFragment
        transaction.replace(R.id.fragmentContainer, fragment);

        // Optionally add the transaction to the back stack
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }
}