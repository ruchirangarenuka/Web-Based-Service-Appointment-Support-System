package com.example.support;

import java.time.LocalDateTime;

public class Feedback {
    private final int rating; // 1-5
    private final String comments;
    private final LocalDateTime createdAt;

    public Feedback(int rating, String comments) {
        this.rating = rating;
        this.comments = comments;
        this.createdAt = LocalDateTime.now();
    }

    public int getRating() {
        return rating;
    }

    public String getComments() {
        return comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Rating: " + rating + "/5, Comments: " + comments;
    }
}

