package com.example.grow21.models;

public class CategoryProgress {
    private String category;
    private int attempted;
    private int correct;
    private float accuracy;

    public CategoryProgress() {
    }

    public CategoryProgress(String category, int attempted, int correct, float accuracy) {
        this.category = category;
        this.attempted = attempted;
        this.correct = correct;
        this.accuracy = accuracy;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAttempted() {
        return attempted;
    }

    public void setAttempted(int attempted) {
        this.attempted = attempted;
    }

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
