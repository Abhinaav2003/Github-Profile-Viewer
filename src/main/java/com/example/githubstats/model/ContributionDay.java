package com.example.githubstats.model;

public class ContributionDay {

    private String date;
    private int count;
    private int level;

    public ContributionDay() {
    }

    public ContributionDay(String date, int count, int level) {
        this.date = date;
        this.count = count;
        this.level = level;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}