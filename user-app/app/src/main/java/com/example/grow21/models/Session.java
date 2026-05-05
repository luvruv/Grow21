package com.example.grow21.models;

public class Session {
    private int id;
    private String gameType;
    private int score;
    private int total;
    private String date;

    public Session() {
    }

    public Session(int id, String gameType, int score, int total, String date) {
        this.id = id;
        this.gameType = gameType;
        this.score = score;
        this.total = total;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
