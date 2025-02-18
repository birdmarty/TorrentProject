package com.example.myapplication.parsing;

public class CategoryList {
    private String category1;


    public CategoryList(String category) {
        switch (category) {
            case "Movies":
                category1 = "/Movies";
                break;
            case "All":
                category1 = "/Movies";
                break;
            case "TV Shows":
                category1 = "/TV";
                break;
            case "Games":
                category1 = "/Games";
                break;
            case "Music":
                category1 = "/Music";
                break;
            case "Applications":
                category1 = "/Applications";
                break;
            case "Documentaries":
                category1= "/Documentaries";
                break;
            case "Anime":
                category1 = "/Aninme";
                break;
            case "Other":
                category1 = "/Other";
                break;
            case "XXX":
                category1 = "/XXX";
                break;
        }
    }

    public String getCategory() {
        return category1;
    }

    public String urlCategorySearch(String keyword) {
        return "https://www.1337x.to/category-search/" + keyword + category1 + "/1/";
    }
}
