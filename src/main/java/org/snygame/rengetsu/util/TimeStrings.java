package org.snygame.rengetsu.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeStrings {
    private static final Pattern ALNUM_RE = Pattern.compile("\\d+(\\.\\d*)?|[a-zA-Z]+");

    public static final int DAY_MILLI = 1000 * 60 * 60 * 24;

    public static String secondsToEnglish(int seconds) {
        int w = seconds / (60 * 60 * 24 * 7);
        seconds %= (60 * 60 * 24 * 7);
        int d = seconds / (60 * 60 * 24);
        seconds %= (60 * 60 * 24);
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (w > 0) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("%d week%s".formatted(w, w != 1 ? "s" : ""));
        }

        if (d > 0) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("%d day%s".formatted(d, d != 1 ? "s" : ""));
        }

        if (h > 0) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("%d hour%s".formatted(h, h != 1 ? "s" : ""));
        }

        if (m > 0) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("%d minute%s".formatted(m, m != 1 ? "s" : ""));
        }

        if (s > 0 || (h == 0 && m == 0)) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("%d second%s".formatted(s, s != 1 ? "s" : ""));
        }

        return sb.toString();
    }

    public static int readDuration(String duration) {
        try {
            return Integer.parseInt(duration);
        } catch (NumberFormatException e) {
            Matcher matcher = ALNUM_RE.matcher(duration);

            float seconds = 0;

            while (matcher.find()) {
                String match = matcher.group();
                try {
                    float value = Float.parseFloat(match);

                    if (matcher.find()) {
                        switch (matcher.group()) {
                            case "s", "sec", "secs", "second", "seconds" -> seconds += value;
                            case "m", "min", "mins", "minute", "minutes" -> seconds += value * 60;
                            case "h", "hour", "hours" -> seconds += value * 60 * 60;
                            case "d", "day", "days" -> seconds += value * 60 * 60 * 24;
                            case "w", "week", "weeks" -> seconds += value * 60 * 60 * 24 * 7;
                            default -> {
                                return -1;
                            }
                        }

                        continue;
                    }
                } catch (NumberFormatException ignored) {}

                return -1;
            }

            return Math.round(seconds);
        }
    }
}
