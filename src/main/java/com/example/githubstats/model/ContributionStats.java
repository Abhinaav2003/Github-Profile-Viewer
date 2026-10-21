package com.example.githubstats.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ContributionStats {

    private int totalContributions;
    private int currentStreak;
    private int longestStreak;
    private int activeDays;

    private List<ContributionDay> calendarDays = new ArrayList<>();

    public ContributionStats() {
    }

    public static ContributionStats calculate(
            List<ContributionDay> days,
            int totalContributions) {

        ContributionStats stats = new ContributionStats();

        stats.calendarDays = days;
        stats.totalContributions = totalContributions;

        int currentRun = 0;
        int longestRun = 0;

        LocalDate today = LocalDate.now();

        for (ContributionDay day : days) {
            if (day.getCount() > 0) {
                stats.activeDays++;
                currentRun++;
                longestRun = Math.max(longestRun, currentRun);
            } else {
                currentRun = 0;
            }
        }

        stats.longestStreak = longestRun;

        // A streak can remain current if the last active day
        // was today or yesterday.
        LocalDate lastActiveDate = null;

        for (int i = days.size() - 1; i >= 0; i--) {
            ContributionDay day = days.get(i);

            if (day.getCount() > 0) {
                lastActiveDate = LocalDate.parse(day.getDate());
                break;
            }
        }

        if (lastActiveDate != null
                && !lastActiveDate.isBefore(today.minusDays(1))) {

            int streak = 0;

            for (int i = days.size() - 1; i >= 0; i--) {
                ContributionDay day = days.get(i);

                if (day.getCount() > 0) {
                    streak++;
                } else {
                    // Ignore trailing inactive days after today.
                    if (LocalDate.parse(day.getDate())
                            .isBefore(lastActiveDate)) {
                        break;
                    }
                }
            }

            stats.currentStreak = streak;
        }

        return stats;
    }

    public int getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(int totalContributions) {
        this.totalContributions = totalContributions;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = activeDays;
    }

    public List<ContributionDay> getCalendarDays() {
        return calendarDays;
    }

    public void setCalendarDays(List<ContributionDay> calendarDays) {
        this.calendarDays = calendarDays;
    }
}