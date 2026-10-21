package com.example.githubstats.service;

import com.example.githubstats.model.GithubProfile;
import com.example.githubstats.model.GithubRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class GithubService {
    private final RestClient restClient;

    public GithubService(
            RestClient.Builder builder,
            @Value("${github.api-token:}") String apiToken) {

        RestClient.Builder configured = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "GitHub-Profile-Stats-Viewer");

        if (apiToken != null && !apiToken.isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken.trim());
        }
        this.restClient = configured.build();
    }

    public GithubProfile getProfile(String username) {
        try {
            GithubProfile profile = restClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .body(GithubProfile.class);

            if (profile == null) {
                throw new GithubServiceException("GitHub returned an empty profile response.");
            }
            return profile;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException("GitHub username was not found. Check the spelling and try again.");
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException("GitHub API rate limit may have been reached. Add a GitHub token in environment settings or try again later.");
        } catch (RestClientException ex) {
            throw new GithubServiceException("Unable to connect to GitHub right now. Please try again later.");
        }
    }

    public List<GithubRepository> getRepositories(String username) {
        try {
            GithubRepository[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/{username}/repos")
                            .queryParam("sort", "updated")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", 100)
                            .build(username))
                    .retrieve()
                    .body(GithubRepository[].class);

            List<GithubRepository> repositories = response == null
                    ? new ArrayList<>()
                    : new ArrayList<>(Arrays.asList(response));

            repositories.sort(Comparator.comparingInt(GithubRepository::getStars).reversed());
            return repositories;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException("GitHub username was not found. Check the spelling and try again.");
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException("GitHub API rate limit may have been reached. Add a GitHub token in environment settings or try again later.");
        } catch (RestClientException ex) {
            throw new GithubServiceException("Unable to load repositories from GitHub. Please try again later.");
        }
    }

    public static class GithubServiceException extends RuntimeException {
        public GithubServiceException(String message) {
            super(message);
        }
    }
}
