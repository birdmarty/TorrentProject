package com.example.myapplication.repository;

import android.os.AsyncTask;
import android.util.Log;
import com.example.myapplication.parsing.SearchResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;

public class TorrentRepository {
    private static final String TAG = "TorrentRepo";
    private static final String BASE_URL = "https://1337x.to";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final int TIMEOUT = 15000;

    public interface TorrentListener {
        void onResultsReady(ArrayList<SearchResult> results);
        void onError(String message);
    }

    public void getTrendingTorrents(TorrentListener listener) {
        new TorrentTask(listener).execute(BASE_URL + "/trending");
    }

    public void searchTorrents(String searchUrl, TorrentListener listener) {
        new TorrentTask(listener).execute(searchUrl);
    }

    private static class TorrentTask extends AsyncTask<String, Void, ArrayList<SearchResult>> {
        private final TorrentListener listener;

        TorrentTask(TorrentListener listener) {
            this.listener = listener;
        }

        @Override
        protected ArrayList<SearchResult> doInBackground(String... urls) {
            ArrayList<SearchResult> results = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(urls[0])
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT)
                        .ignoreHttpErrors(true)
                        .get();

                Elements rows = doc.select("tbody tr");
                for (Element row : rows) {
                    Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
                    if (titleLink == null) continue;

                    String title = titleLink.text();
                    String seeds = row.select("td:nth-child(2)").text();
                    String leeches = row.select("td:nth-child(3)").text();
                    String size = row.select("td.coll-4").first().ownText();
                    String link = BASE_URL + titleLink.attr("href");

                    results.add(new SearchResult(
                            title,
                            "Seeds: " + seeds,
                            "Leeches: " + leeches,
                            "Size: " + size,
                            "1337x",
                            link,
                            null  // InfoHash will be fetched later when needed
                    ));
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<SearchResult> results) {
            if (listener != null) {
                if (results.isEmpty()) {
                    listener.onError("No results found");
                } else {
                    listener.onResultsReady(results);
                }
            }
        }
    }
}