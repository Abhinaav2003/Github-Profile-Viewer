
package com.example.githubstats.service;

import com.example.githubstats.model.ContributionDay;
import com.example.githubstats.model.ContributionStats;
import com.example.githubstats.model.GithubProfile;
import com.example.githubstats.model.GithubRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class GithubService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GithubService.class);

    private final RestClient restClient;
    private final RestClient publicClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "GitHub-Profile-Stats-Viewer")
            .defaultHeader(HttpHeaders.ACCEPT, "text/html")
            .build();
    private final String apiToken;

    public GithubService(
            RestClient.Builder builder,
            @Value("${github.api-token:}") String apiToken) {

        this.apiToken = apiToken == null ? "" : apiToken.trim();

        RestClient.Builder configured = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "GitHub-Profile-Stats-Viewer");

        if (!this.apiToken.isBlank()) {
            configured.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + this.apiToken
            );
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
                throw new GithubServiceException(
                        "GitHub returned an empty profile response."
                );
            }

            return profile;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException(
                    "GitHub username was not found. Check the spelling."
            );
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException(
                    "GitHub API rate limit may have been reached. Try again later."
            );
        } catch (RestClientException ex) {
            throw new GithubServiceException(
                    "Unable to connect to GitHub. Please try again later."
            );
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

            repositories.sort(
                    Comparator.comparingInt(GithubRepository::getStars)
                            .reversed()
            );

            return repositories;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException(
                    "GitHub username was not found. Check the spelling."
            );
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException(
                    "GitHub API rate limit may have been reached. Try again later."
            );
        } catch (RestClientException ex) {
            throw new GithubServiceException(
                    "Unable to load repositories from GitHub."
            );
        }
    }

    /**
     * Fetches the user's contribution calendar using GitHub GraphQL.
     * A token with permission to read the user's data is required.
     */
    public ContributionStats getContributionStats(String username) {
        if (!apiToken.isBlank()) {
            try {
                return getContributionStatsGraphQl(username);
            } catch (GithubServiceException ex) {
                log.warn("GraphQL contributions failed ({}); trying public calendar", ex.getMessage());
                try {
                    return getContributionStatsPublic(username);
                } catch (GithubServiceException fallbackEx) {
                    throw ex; // report the original, more specific error
                }
            }
        }
        return getContributionStatsPublic(username);
    }

    /** Token-free fallback: reads GitHub's public contribution calendar. */
    private ContributionStats getContributionStatsPublic(String username) {
        try {
            String html = publicClient.get()
                    .uri("https://github.com/users/{username}/contributions", username)
                    .retrieve()
                    .body(String.class);

            List<ContributionDay> days = ContributionHtmlParser.parse(html);
            if (days.isEmpty()) {
                throw new GithubServiceException(
                        "Couldn't read the public contribution calendar. Set GITHUB_API_TOKEN to use the GitHub API instead."
                );
            }
            days.sort(Comparator.comparing(ContributionDay::getDate));
            int total = days.stream().mapToInt(ContributionDay::getCount).sum();
            return new ContributionStats(total, days);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException("GitHub username was not found.");
        } catch (RestClientException ex) {
            log.warn("Public contribution request failed", ex);
            throw new GithubServiceException(
                    "Unable to load the public contribution calendar: " + ex.getMessage());
        }
    }

    private ContributionStats getContributionStatsGraphQl(String username) {

        String query = """
                query($username: String!) {
                  user(login: $username) {
                    contributionsCollection {
                      contributionCalendar {
                        totalContributions
                        weeks {
                          contributionDays {
                            date
                            contributionCount
                          }
                        }
                      }
                    }
                  }
                }
                """;

        try {
            JsonNode response = restClient.post()
                    .uri("https://api.github.com/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of(
                            "query", query,
                            "variables", java.util.Map.of("username", username)
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("errors")) {
                String detail = response.path("errors").path(0).path("message").asText("unknown error");
                log.warn("GitHub GraphQL error for '{}': {}", username, detail);
                throw new GithubServiceException("GitHub returned an error: " + detail);
            }

            JsonNode user = response == null
                    ? null
                    : response.path("data").path("user");

            if (user == null || user.isMissingNode() || user.isNull()) {
                throw new GithubServiceException(
                        "Unable to load contribution data. Check the username and token permissions."
                );
            }

            JsonNode calendar = user.path("contributionsCollection")
                    .path("contributionCalendar");

            int totalContributions = calendar.path("totalContributions").asInt(0);
            List<ContributionDay> days = new ArrayList<>();

            for (JsonNode week : calendar.path("weeks")) {
                for (JsonNode dayNode : week.path("contributionDays")) {
                    LocalDate date = LocalDate.parse(
                            dayNode.path("date").asText()
                    );

                    int count = dayNode.path("contributionCount").asInt(0);

                    ContributionDay day = new ContributionDay();
                    day.setDate(date.toString());
                    day.setCount(count);
                    day.setLevel(getContributionLevel(count));

                    days.add(day);
                }
            }

            days.sort(Comparator.comparing(ContributionDay::getDate));

            return new ContributionStats(totalContributions, days);

        } catch (HttpClientErrorException.Unauthorized ex) {
            log.warn("GitHub rejected the token (401)");
            throw new GithubServiceException(
                    "GitHub rejected the token (invalid or expired). Generate a new token and update GITHUB_API_TOKEN."
            );
        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("GitHub denied the contribution request (403): {}", ex.getMessage());
            throw new GithubServiceException(
                    "GitHub denied the contribution request. Check your token permissions or rate limit."
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException(
                    "GitHub username was not found."
            );
        } catch (RestClientException ex) {
            log.warn("Contribution request failed", ex);
            throw new GithubServiceException(
                    "Unable to fetch contribution statistics from GitHub: " + ex.getMessage()
            );
        }
    }

    private int getContributionLevel(int count) {
        if (count == 0) return 0;
        if (count <= 3) return 1;
        if (count <= 6) return 2;
        if (count <= 9) return 3;
        return 4;
    }

    public static class GithubServiceException extends RuntimeException {
        public GithubServiceException(String message) {
            super(message);
        }
    }
}