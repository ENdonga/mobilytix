package io.mobilytix.utils;

import java.util.List;

public class DataUtils {
    private DataUtils() {
    }

    public static List<String> sortedAlphabetically(List<String> names) {
        return names.stream()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    public static List<String> sortedAlphabeticallyReversed(List<String> names) {
        return names.stream()
                .sorted((a, b) -> b.compareToIgnoreCase(a))
                .toList();
    }

    public static List<Double> sortedLowestToHighest(List<Double> prices) {
        return prices.stream()
                .sorted()
                .toList();
    }

    public static List<Double> sortedHighestToLowest(List<Double> prices) {
        return prices.stream()
                .sorted((a, b) -> Double.compare(b, a))
                .toList();
    }
}
