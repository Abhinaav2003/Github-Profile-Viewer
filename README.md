<h1 align="center">GitHub Profile Stats Viewer</h1>

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,hibernate,postgres,maven" alt="Tech stack icons" />
</p>

## Features

- Search any public GitHub username.
- View avatar, name, bio, location, followers, following, and public repository count.
- Browse up to 100 public repositories returned by GitHub.
- View repository descriptions, primary languages, stars, and forks.
- Calculate stars and forks across the repositories loaded by the application.
- Sort repositories by stars, recently updated, or name.
- Responsive dark interface.
- Friendly handling of invalid usernames and GitHub API errors.
- No database required.

## Technology

- Java 17
- Spring Boot 3.4.5
- Spring Web (`RestClient`)
- Thymeleaf
- Bootstrap 5.3
- Maven
- GitHub REST API

## Requirements

- JDK 17 or newer
- Git
- Maven 3.6+ (or use the included Maven Wrapper if you add one)

Verify your Java installation:

```bash
java -version
```

## Run locally

From the project root:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

To build a runnable JAR:

```bash
mvn clean package
java -jar target/github-profile-stats-viewer-0.0.1-SNAPSHOT.jar
```

## Optional GitHub token

Unauthenticated public API requests are rate-limited. For development or deployment, you can configure a personal access token with no additional scopes for public-data access. Do not commit the token.

PowerShell:

```powershell
$env:GITHUB_API_TOKEN="YOUR_GITHUB_TOKEN"
mvn spring-boot:run
```

Linux/macOS:

```bash
export GITHUB_API_TOKEN="YOUR_GITHUB_TOKEN"
mvn spring-boot:run
```

If deploying, add `GITHUB_API_TOKEN` in the hosting provider's environment-variable settings. Never place the real token in source code or frontend JavaScript.

## GitHub API endpoints

- `GET https://api.github.com/users/{username}`
- `GET https://api.github.com/users/{username}/repos?sort=updated&direction=desc&per_page=100`

The application displays public data only. The repository list is limited to the first 100 results, so calculated stars/forks and language counts represent those loaded repositories, not necessarily every repository for accounts with more than 100 public repositories. Language counts represent each repository's primary language, not byte-level language percentages.

## Project structure

```text
src/main/java/com/example/githubstats/
├── GithubStatsApplication.java
├── controller/GithubController.java
├── model/GithubProfile.java
├── model/GithubRepository.java
└── service/GithubService.java

src/main/resources/
├── application.properties
├── static/css/style.css
└── templates/
    ├── index.html
    └── profile.html
```

## Upload to GitHub

Create an empty repository on GitHub named `github-profile-stats-viewer` (do not initialize it with a README if you already have this project locally).

Then, in PowerShell or a terminal opened in the project folder:

```bash
git init
git add .
git commit -m "Build GitHub Profile Stats Viewer with Spring Boot"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/github-profile-stats-viewer.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your GitHub username. If `origin` already exists, update it instead:

```bash
git remote set-url origin https://github.com/YOUR_USERNAME/github-profile-stats-viewer.git
git push -u origin main
```

## Troubleshooting

- **Port already in use:** set `server.port=8081` in `application.properties`, or stop the process using port 8080.
- **API rate limit:** wait for the limit to reset or configure `GITHUB_API_TOKEN` as an environment variable.
- **Profile not found:** check the spelling of the username.
- **No repositories:** the profile may not have public repositories.
- **Maven not found:** install Maven or open the project in IntelliJ IDEA and run the Maven `spring-boot:run` goal.

## License

This project is available under the MIT License. Add a `LICENSE` file if you want to publish it formally under that license.
