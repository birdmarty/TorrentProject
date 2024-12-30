package com.example.myapplication.parsing;


public class SortList {
    private String sortOneThree;

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
            case "Leeches DESC":
                sortOneThree = "/leechers/desc/";
                break;
            case "Leeches ASC":
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
        return sortOneThree;
    }

    public String urlSortSearch(String keyword) {
        return "https://www.1337x.to/" + "sort-search/" + keyword + sortOneThree + "1/";
    }
}

