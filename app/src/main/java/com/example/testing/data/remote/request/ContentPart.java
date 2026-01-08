package com.example.testing.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class ContentPart {
    @SerializedName("type")
    private String type;

    @SerializedName("text")
    private String text;

    @SerializedName("image_url")
    private ImageUrl imageUrl;

    public ContentPart(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public ContentPart(String type, ImageUrl imageUrl) {
        this.type = type;
        this.imageUrl = imageUrl;
    }
}