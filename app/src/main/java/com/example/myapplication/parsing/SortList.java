package com.example.myapplication.parsing;


import android.util.Log;

public class SortList {
    private String sortOneThree;
    private String sortItem;

    public SortList(String sortItem) {
        switch (sortItem) {
            case "Seeds DESC":
                sortOneThree = "/seeders/desc/";
                break;
            case "Sort by...":
                sortOneThree = "/seeders/desc/";
                break;
            case "Seeds ASC":
                sortOneThree = "/seeders/asc/";
                break;
            case "Leechers DESC":
                sortOneThree = "/leechers/desc/";
                break;
            case "Leechers ASC":
                sortOneThree = "/leechers/asc/";
                break;
            case "Size DESC":
                sortOneThree = "/size/desc/";
                break;
            case "Size ASC":
                sortOneThree = "/size/asc/";
                break;
            case "Time DESC":
                sortOneThree = "/time/desc/";
                break;
            case "Time ASC":
                sortOneThree = "/time/asc/";
                break;
        }
    }

    public String getSort() {
        Log.d("sort",sortItem + "," + sortOneThree);
        return sortOneThree;
    }

    public String urlSortSearch(String keyword) {
        return "https://www.1337x.to/" + "sort-search/" + keyword + sortOneThree + "1/";
    }
}

