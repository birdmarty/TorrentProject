package com.example.myapplication.fragments;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.myapplication.fragments.SearchResultsFragment;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TorrentAdapter;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.parsing.SortList;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;


public class DiscoverFragment extends Fragment  implements TorrentAdapter.RecyclerviewListener, TorrentListener {

    private EditText inputSearch;
    private String keyword;
    private String sortItem = "Seeds DESC";
    public TorrentStream torrentStream;
    private static final String TAG = "SearchResultsFragment";

    private TorrentAdapter torrentAdapter;
    private ArrayList<SearchResult> TorrentsList = new ArrayList<>();
    private String infohash;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        torrentAdapter = new TorrentAdapter(this, TorrentsList);
        recyclerView.setAdapter(torrentAdapter);

        inputSearch = rootView.findViewById(R.id.inputSearch);
        inputSearch.setOnEditorActionListener(editorActionListener);

        // Load trending torrents
        new LoadTrendingTorrentsTask().execute("https://1337x.to/trending");

        return rootView;
    }



//    Load the trending on startup and save it in device storage/have a database and load the trending shit from there idk
    public class LoadTrendingTorrentsTask extends AsyncTask<String, Void, ArrayList<SearchResult>> {
        @Override

//        protected void onPreExecute() {
////            super.onPreExecute();
////            // Add a progress indicator
////            // progressBar.setVisibility(View.VISIBLE);
////        }

        protected ArrayList<SearchResult> doInBackground(String... params) {
            ArrayList<SearchResult> results = new ArrayList<>();
            try {
                Document document = Jsoup.connect(params[0]).get();
                Elements rows = document.select("tbody > tr");
                for (Element row : rows) {
                    Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
                    String title = titleLink.text();
                    String seeds = row.select("td:nth-child(2)").text();
                    String leeches = row.select("td:nth-child(3)").text();
                    String size = row.select("td.coll-4").first().ownText();
                    String link = "https://www.1337x.to" + titleLink.attr("href");
                    String infohash = fetchInfohash(link);

                    results.add(new SearchResult(title, "Seeds: " + seeds, "Leeches: " + leeches, "Size: " + size, "1337x", link, infohash));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return results;
        }

        public String fetchInfohash(String link) {
            try {
                Document torrentPage = Jsoup.connect(link).get();
                Element infohashElement = torrentPage.select("div.infohash-box span").first();
                if (infohashElement != null) {
                    return infohashElement.text();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<SearchResult> results) {
            TorrentsList.clear();
            TorrentsList.addAll(results);
            torrentAdapter.notifyDataSetChanged();
        }
    }


    private final TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Get the keyword from the search bar
                keyword = inputSearch.getText().toString().trim();

                // Check if the keyword is not empty
                if (!keyword.isEmpty()) {
                    // Perform the search here
                    new LoadSearchResultsTask().execute(new SortList(sortItem).urlOneThree(keyword));
                    return true;
                }
            }
            return false;
        }
    };

    public class LoadSearchResultsTask extends AsyncTask<String, Void, ArrayList<SearchResult>> {
        @Override

//        protected void onPreExecute() {
//            super.onPreExecute();
//            // Add a progress indicator
//            // progressBar.setVisibility(View.VISIBLE);
//        }

        protected ArrayList<SearchResult> doInBackground(String... params) {
            ArrayList<SearchResult> results = new ArrayList<>();
            try {
                Document document = Jsoup.connect(params[0]).get();
                Elements rows = document.select("tbody > tr");
                for (Element row : rows) {
                    Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
                    String title = titleLink.text();
                    String seeds = row.select("td:nth-child(2)").text();
                    String leeches = row.select("td:nth-child(3)").text();
                    String size = row.select("td.coll-4").first().ownText();
                    String link = "https://www.1337x.to" + titleLink.attr("href");
                    String infohash = fetchInfohash(link);

                    results.add(new SearchResult(title, "Seeds: " + seeds, "Leeches: " + leeches, "Size: " + size, "1337x", link, infohash));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return results;
        }

        public String fetchInfohash(String link) {
            try{
                Document torrentPage = Jsoup.connect(link).get();
                Element infohashElement = torrentPage.select("div.infohash-box span").first();
                if (infohashElement!= null) {
                    return infohashElement.text();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null; // Return null if the infohash is not found
        }

        @Override
        protected void onPostExecute(ArrayList<SearchResult> results) {
            TorrentsList.clear();
            TorrentsList.addAll(results);
            torrentAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onStreamPrepared(Torrent torrent) {

    }

    @Override
    public void onStreamStarted(Torrent torrent) {

    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {

    }

    @Override
    public void onStreamReady(Torrent torrent) {

    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {

    }

    @Override
    public void onStreamStopped() {

    }
}