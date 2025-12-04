package org.example;
import org.example.CatalogEditor;

import java.util.*;

public class SmartSearch {

    // 🔥 Словник синоніми (можна легко розширити)
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("бойлер", List.of("водонагрівач")),
            Map.entry("водонагрівач", List.of("бойлер")),
            Map.entry("кафель", List.of("плитка")),
            Map.entry("плитка", List.of("кафель")),
            Map.entry("грунт", List.of("грунтовка")),
            Map.entry("грунтовка", List.of("грунт")),
            Map.entry("гвозді", List.of("цвяхи")),
            Map.entry("цвяхи", List.of("гвозді")),
            Map.entry("шурупи", List.of("саморізи")),
            Map.entry("саморізи", List.of("шурупи"))
    );

    /** Головний метод пошуку */
    public static List<Map<String, Object>> search(String query) {

        query = query.toLowerCase().trim();
        List<Map<String, Object>> allProducts = CatalogEditor.getAllProducts();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> p : allProducts) {

            String name = p.get("name").toString().toLowerCase();

            // 🔹 1. Точне входження
            if (name.contains(query)) {
                results.add(p);
                continue;
            }

            // 🔹 2. Перевірка словника синонімів
            if (matchesSynonym(query, name)) {
                results.add(p);
                continue;
            }

            // 🔹 3. Пошук по кореню (водонагрі → водонагрівач)
            if (rootMatch(query, name)) {
                results.add(p);
                continue;
            }

            // 🔹 4. Fuzzy search (бойлер → болер)
            if (levenshteinDistance(query, name) <= 2) {
                results.add(p);
            }
        }

        return results;
    }

    /** Перевірка синонімів */
    private static boolean matchesSynonym(String query, String name) {
        for (var entry : SYNONYMS.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (query.contains(key)) {
                for (String v : values) {
                    if (name.contains(v)) return true;
                }
            }

            if (values.stream().anyMatch(query::contains) && name.contains(key))
                return true;
        }
        return false;
    }

    /** Пошук по кореню слова */
    private static boolean rootMatch(String q, String name) {
        if (q.length() < 3) return false;
        return name.contains(q.substring(0, q.length() - 1));
    }

    /** Fuzzy search (Levenshtein distance) */
    public static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(
                        1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}

