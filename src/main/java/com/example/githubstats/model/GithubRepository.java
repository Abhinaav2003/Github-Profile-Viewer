package com.example.githubstats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubRepository {
    private long id;
    private String name;
    private String description;
    private String language;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("stargazers_count")
    private int stars;

    @JsonProperty("forks_count")
    private int forks;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("fork")
    private boolean fork;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getForks() { return forks; }
    public void setForks(int forks) { this.forks = forks; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public boolean isFork() { return fork; }
    public void setFork(boolean fork) { this.fork = fork; }
}
