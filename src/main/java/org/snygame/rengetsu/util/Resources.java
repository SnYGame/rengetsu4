package org.snygame.rengetsu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Resources {
    /**
     * Gets a specific resource file as String
     *
     * @param fileName The file path omitting "resources/"
     * @return The contents of the file as a String, otherwise throws an exception
     */
    public static String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream resourceAsStream = getResourceFileAsStream(fileName)) {
            if (resourceAsStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    public static InputStream getResourceFileAsStream(String fileName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }
}
