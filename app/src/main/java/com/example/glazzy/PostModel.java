package com.example.glazzy;

public class PostModel {
    String title, content, excerpt, imageUrl, link, date;
    long savedAt;
    int postId;

    public PostModel(String title, String content, String excerpt, String imageUrl) {
        this.title = title;
        this.content = content;
        this.excerpt = excerpt;
        this.imageUrl = imageUrl;
        this.savedAt = System.currentTimeMillis();
    }
}