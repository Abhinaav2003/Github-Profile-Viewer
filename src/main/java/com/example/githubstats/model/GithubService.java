
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
import java.util.Map;

@Service
public class GithubService {

    private final RestClient restClient;
    private final String apiToken;

    public GithubService(
            RestClient.Builder builder,
            @Value("${github.api-token:}") String apiToken) {

        this.apiToken = apiToken == null ? "" : apiToken.trim();

        RestClient.Builder configured = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2022-11-28"
                )
                .defaultHeader(
                        HttpHeaders.USER_AGENT,
                        "GitHub-Profile-Stats-Viewer"
                );

        if (!this.apiToken.isBlank()) {
            configured.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + this.apiToken
            );
        }

        this.restClient = configured.build();
    }

    // --------------------------------------------------
    // Get GitHub user profile
    // --------------------------------------------------

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
                    "GitHub API rate limit may have been reached. Check your token."
            );

        } catch (RestClientException ex) {
            throw new GithubServiceException(
                    "Unable to connect to GitHub. Please try again later."
            );
        }
    }

    // --------------------------------------------------
    // Get GitHub repositories
    // --------------------------------------------------

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

            List<GithubRepository> repositories =
                    response == null
                            ? new ArrayList<>()
                            : new ArrayList<>(Arrays.asList(response));

            repositories.sort(
                    Comparator.comparingInt(
                            GithubRepository::getStars
                    ).reversed()
            );

            return repositories;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException(
                    "GitHub username was not found. Check the spelling."
            );

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException(
                    "GitHub API rate limit may have been reached. Check your token."
            );

        } catch (RestClientException ex) {
            throw new GithubServiceException(
                    "Unable to load repositories from GitHub."
            );
        }
    }

    // --------------------------------------------------
    // Get contribution calendar, totals and streaks
    // --------------------------------------------------

    public ContributionStats getContributionStats(String username) {

        if (apiToken.isBlank()) {
            throw new GithubServiceException(
                    "A GitHub token is required to load contribution statistics."
            );
        }

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
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "variables", Map.of("username", username)
            );

            JsonNode response = restClient.post()
                    .uri("https://api.github.com/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new GithubServiceException(
                        "GitHub returned an empty contribution response."
                );
            }

            // GraphQL can return errors even when the HTTP status is 200.
            if (response.has("errors")) {
                throw new GithubServiceException(
                        "GitHub could not return contribution data. Check your token permissions and username."
                );
            }

            JsonNode user = response.path("data").path("user");

            if (user.isMissingNode() || user.isNull()) {
                throw new GithubServiceException(
                        "GitHub user was not found or contribution data is unavailable."
                );
            }

            JsonNode calendar = user
                    .path("contributionsCollection")
                    .path("contributionCalendar");

            int totalContributions =
                    calendar.path("totalContributions").asInt(0);

            List<ContributionDay> days = new ArrayList<>();

            for (JsonNode week : calendar.path("weeks")) {
                for (JsonNode dayNode : week.path("contributionDays")) {

                    String date = dayNode.path("date").asText();
                    int count = dayNode.path("contributionCount").asInt(0);

                    ContributionDay day = new ContributionDay();

                    // Your model uses String for the date.
                    day.setDate(date);
                    day.setCount(count);
                    day.setLevel(getContributionLevel(count));

                    days.add(day);
                }
            }

            days.sort(
                    Comparator.comparing(
                            day -> LocalDate.parse(day.getDate())
                    )
            );

            // Uses your existing streak-calculation method.
            return ContributionStats.calculate(
                    days,
                    totalContributions
            );

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new GithubServiceException(
                    "GitHub rejected your token. Check GITHUB_API_TOKEN."
            );

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new GithubServiceException(
                    "GitHub denied the contribution request. Check token permissions or rate limits."
            );

        } catch (HttpClientErrorException.NotFound ex) {
            throw new GithubServiceException(
                    "GitHub GraphQL endpoint or requested user was not found."
            );

        } catch (RestClientException ex) {
            throw new GithubServiceException(
                    "Unable to fetch contribution statistics from GitHub."
            );
        }
    }

    // --------------------------------------------------
    // Contribution calendar intensity: 0 to 4
    // --------------------------------------------------

    private int getContributionLevel(int count) {
        if (count == 0) {
            return 0;
        }

        if (count <= 3) {
            return 1;
        }

        if (count <= 6) {
            return 2;
        }

        if (count <= 9) {
            return 3;
        }

        return 4;
    }

    // --------------------------------------------------
    // Custom exception
    // --------------------------------------------------

    public static class GithubServiceException
            extends RuntimeException {

        public GithubServiceException(String message) {
            super(message);
        }
    }
}