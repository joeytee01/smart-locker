package com.example.smartlockersolution;

public class ExhibitItem {
    private String title;
    private int imageResId; // Store only one image resource ID
    private String description;

    public ExhibitItem(String title, int imageResId, String description) {
        this.title = title;
        this.imageResId = imageResId;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getDescription() {
        return description;
    }
}
