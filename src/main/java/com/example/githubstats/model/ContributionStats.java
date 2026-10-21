package com.example.githubstats.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Contribution totals, streaks and calendar for the past year.
 * All derived values are computed once in the constructor.
 */
public class ContributionStats {

    private final int totalContributions;
    private final List<ContributionDay> calendarDays;

    private int activeDays;
    private int currentStreak;
    private String currentStreakStart;
    private String currentStreakEnd;
    private int longestStreak;
    private String longestStreakStart;
    private String longestStreakEnd;
    private int busiestDayCount;
    private String busiestDay;
    private double averagePerDay;
    private double averagePerActiveDay;

    public ContributionStats(int totalContributions, List<ContributionDay> days) {
        this.totalContributions = totalContributions;
        this.calendarDays = days == null ? new ArrayList<>() : new ArrayList<>(days);
        this.calendarDays.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        calculate();
    }

    private void calculate() {
        int n = calendarDays.size();
        if (n == 0) {
            return;
        }

        // Longest streak, active days, busiest day.
        int run = 0;
        String runStart = null;
        LocalDate previous = null;

        for (ContributionDay day : calendarDays) {
            LocalDate date = LocalDate.parse(day.getDate());

            if (day.getCount() > 0) {
                activeDays++;

                // A gap in the dates (missing day) also breaks a streak.
                boolean continues = run > 0 && previous != null && previous.plusDays(1).equals(date);
                if (!continues) {
                    run = 0;
                    runStart = day.getDate();
                }
                run++;

                if (run > longestStreak) {
                    longestStreak = run;
                    longestStreakStart = runStart;
                    longestStreakEnd = day.getDate();
                }
                if (day.getCount() > busiestDayCount) {
                    busiestDayCount = day.getCount();
                    busiestDay = day.getDate();
                }
            } else {
                run = 0;
            }
            previous = date;
        }

        // Current streak: walk back from the most recent day. Like GitHub, today having no
        // contributions yet doesn't break the streak - it simply starts from yesterday.
        int i = n - 1;
        if (calendarDays.get(i).getCount() == 0) {
            i--;
        }
        if (i >= 0 && calendarDays.get(i).getCount() > 0) {
            currentStreakEnd = calendarDays.get(i).getDate();
            LocalDate expected = LocalDate.parse(currentStreakEnd);
            while (i >= 0
                    && calendarDays.get(i).getCount() > 0
                    && LocalDate.parse(calendarDays.get(i).getDate()).equals(expected)) {
                currentStreak++;
                currentStreakStart = calendarDays.get(i).getDate();
                expected = expected.minusDays(1);
                i--;
            }
        }

        int summed = calendarDays.stream().mapToInt(ContributionDay::getCount).sum();
        averagePerDay = (double) summed / n;
        averagePerActiveDay = activeDays == 0 ? 0 : (double) summed / activeDays;
    }

    public int getTotalContributions() { return totalContributions; }
    public List<ContributionDay> getCalendarDays() { return calendarDays; }
    public int getActiveDays() { return activeDays; }
    public int getCurrentStreak() { return currentStreak; }
    public String getCurrentStreakStart() { return currentStreakStart; }
    public String getCurrentStreakEnd() { return currentStreakEnd; }
    public int getLongestStreak() { return longestStreak; }
    public String getLongestStreakStart() { return longestStreakStart; }
    public String getLongestStreakEnd() { return longestStreakEnd; }
    public int getBusiestDayCount() { return busiestDayCount; }
    public String getBusiestDay() { return busiestDay; }
    public double getAveragePerDay() { return averagePerDay; }
    public double getAveragePerActiveDay() { return averagePerActiveDay; }
}
