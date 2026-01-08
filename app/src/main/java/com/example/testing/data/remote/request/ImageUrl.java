package com.example.testing.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class ImageUrl {
    @SerializedName("url")
    private String url;

    public ImageUrl(String url) {
        this.url = url;
    }
}