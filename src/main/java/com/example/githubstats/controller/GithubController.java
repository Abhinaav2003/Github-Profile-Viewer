package com.example.githubstats.controller;

import com.example.githubstats.model.ContributionStats;
import com.example.githubstats.model.GithubProfile;
import com.example.githubstats.model.GithubRepository;
import com.example.githubstats.service.GithubService;
import com.example.githubstats.service.GithubService.GithubServiceException;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Controller
@Validated
public class GithubController {
    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$");

    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/profile")
    public String profile(
            @RequestParam("username") String username,
            @RequestParam(name = "sort", defaultValue = "stars") String sort,
            Model model) {

        String cleanUsername = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            model.addAttribute("error", "Enter a valid GitHub username (letters, numbers, and hyphens).");
            model.addAttribute("searchedUsername", cleanUsername);
            return "index";
        }

        try {
            GithubProfile profile = githubService.getProfile(cleanUsername);
            List<GithubRepository> repositories = githubService.getRepositories(cleanUsername);

            if ("updated".equalsIgnoreCase(sort)) {
                repositories.sort((a, b) -> safe(b.getUpdatedAt()).compareTo(safe(a.getUpdatedAt())));
            } else if ("name".equalsIgnoreCase(sort)) {
                repositories.sort((a, b) -> safe(a.getName()).compareToIgnoreCase(safe(b.getName())));
            } else {
                repositories.sort((a, b) -> Integer.compare(b.getStars(), a.getStars()));
                sort = "stars";
            }

            int totalStars = repositories.stream().mapToInt(GithubRepository::getStars).sum();
            int totalForks = repositories.stream().mapToInt(GithubRepository::getForks).sum();

            Map<String, Long> languages = repositories.stream()
                    .map(GithubRepository::getLanguage)
                    .filter(language -> language != null && !language.isBlank())
                    .collect(Collectors.groupingBy(language -> language, LinkedHashMap::new, Collectors.counting()));

            // Contribution data is optional: a missing token or GraphQL error must not break the page.
            ContributionStats contributionStats = null;
            String contributionError = null;
            try {
                contributionStats = githubService.getContributionStats(cleanUsername);
            } catch (GithubServiceException ex) {
                contributionError = ex.getMessage();
            }

            model.addAttribute("contributionStats", contributionStats);
            model.addAttribute("contributionError", contributionError);
            model.addAttribute("profile", profile);
            model.addAttribute("repositories", repositories);
            model.addAttribute("totalStars", totalStars);
            model.addAttribute("totalForks", totalForks);
            model.addAttribute("languages", languages);
            model.addAttribute("sort", sort);
            model.addAttribute("searchedUsername", cleanUsername);
            return "profile";
        } catch (GithubServiceException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("searchedUsername", cleanUsername);
            return "index";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
