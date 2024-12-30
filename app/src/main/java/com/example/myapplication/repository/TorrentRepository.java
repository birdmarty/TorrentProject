package com.example.myapplication.repository;

import com.example.myapplication.parsing.SearchResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class TorrentRepository {

    private static final String BASE_URL = "https://1337x.to";

    // Fetch trending torrents
    public ArrayList<SearchResult> getTrendingTorrents() throws IOException {
        return fetchTorrents(BASE_URL + "/trending");
    }

    // Fetch search results based on a keyword and sorting
    public ArrayList<SearchResult> searchTorrents(String searchUrl) throws IOException {
        return fetchTorrents(searchUrl);
    }


    // Common method to fetch torrents from a given URL
    private ArrayList<SearchResult> fetchTorrents(String url) throws IOException {
        ArrayList<SearchResult> results = new ArrayList<>();
        Document document = Jsoup.connect(url).get();
        Elements rows = document.select("tbody > tr");

        for (Element row : rows) {
            Element titleLink = row.select("td.coll-1 a:not(.icon)").first();
            String title = titleLink.text();
            String seeds = row.select("td:nth-child(2)").text();
            String leeches = row.select("td:nth-child(3)").text();
            String size = row.select("td.coll-4").first().ownText();
            String link = BASE_URL + titleLink.attr("href");
            String infohash = fetchInfohash(link);

            results.add(new SearchResult(title, "Seeds: " + seeds, "Leeches: " + leeches, "Size: " + size, "1337x", link, infohash));
        }

        return results;
    }

    // Fetch the infohash from the torrent page
    private String fetchInfohash(String link) {
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
}
