package org.snygame.rengetsu.util;

public class TimeStrings {
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
            return 0;
        }
    }
}
