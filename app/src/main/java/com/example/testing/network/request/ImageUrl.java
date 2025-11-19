package com.example.testing.network.request;

import com.google.gson.annotations.SerializedName;

public class ImageUrl {
    @SerializedName("url")
    private String url;

    public ImageUrl(String url) {
        this.url = url;
    }
}