package com.musomi.desktop.model;

public record GlobalSearchResult(
        Long id,
        String category,
        String title,
        String description,
        Long contextId,
        Long contextStreamId,
        String contextName,
        int contextCount
) {
    public GlobalSearchResult(Long id, String category, String title, String description) {
        this(id, category, title, description, null, null, null, 0);
    }

    @Override
    public String toString() {
        return category + ": " + title + " (" + description + ")";
    }
}
