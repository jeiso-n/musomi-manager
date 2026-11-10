package com.musomi.desktop.model;

public record GlobalSearchResult(
        Long id,
        String category,
        String title,
        String description
) {
    @Override
    public String toString() {
        return category + ": " + title + " (" + description + ")";
    }
}

