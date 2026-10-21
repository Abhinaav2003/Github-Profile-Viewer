package com.example.githubstats.service;

import com.example.githubstats.model.ContributionDay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the public contribution calendar fragment served at
 * https://github.com/users/{username}/contributions (no token required).
 */
final class ContributionHtmlParser {

    private static final Pattern CELL = Pattern.compile("<td\\b[^>]*\\bdata-date=\"[^\"]+\"[^>]*>");
    private static final Pattern DATE = Pattern.compile("data-date=\"(\\d{4}-\\d{2}-\\d{2})\"");
    private static final Pattern ID = Pattern.compile("\\bid=\"([^\"]+)\"");
    private static final Pattern LEVEL = Pattern.compile("data-level=\"(\\d)\"");
    private static final Pattern TIP = Pattern.compile(
            "<tool-tip\\b[^>]*\\bfor=\"([^\"]+)\"[^>]*>([^<]*)</tool-tip>");
    private static final Pattern COUNT = Pattern.compile("^\\s*(\\d[\\d,]*)\\s+contribution");

    private ContributionHtmlParser() { }

    static List<ContributionDay> parse(String html) {
        List<ContributionDay> days = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return days;
        }

        Map<String, Integer> countsById = new HashMap<>();
        Matcher tip = TIP.matcher(html);
        while (tip.find()) {
            Matcher c = COUNT.matcher(tip.group(2));
            countsById.put(tip.group(1), c.find() ? Integer.parseInt(c.group(1).replace(",", "")) : 0);
        }

        Matcher cell = CELL.matcher(html);
        while (cell.find()) {
            String tag = cell.group();
            Matcher date = DATE.matcher(tag);
            if (!date.find()) continue;
            Matcher id = ID.matcher(tag);
            Matcher level = LEVEL.matcher(tag);

            int count = id.find() ? countsById.getOrDefault(id.group(1), 0) : 0;
            int lvl = level.find() ? Integer.parseInt(level.group(1)) : (count > 0 ? 1 : 0);
            // A cell with a non-zero level but no tooltip still counts as active.
            if (count == 0 && lvl > 0) count = 1;

            days.add(new ContributionDay(date.group(1), count, lvl));
        }
        return days;
    }
}
