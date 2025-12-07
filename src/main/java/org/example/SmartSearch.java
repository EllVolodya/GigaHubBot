package org.example;

import java.util.*;

public class SmartSearch {

    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(

            Map.entry("бойлер", List.of("водонагрівач", "тен", "фланець", "клапан", "анод")),
            Map.entry("водонагрівач", List.of("бойлер", "тен")),
            Map.entry("тен", List.of("бойлер", "водонагрівач")),

            Map.entry("кафель", List.of("плитка")),
            Map.entry("плитка", List.of("кафель", "керамічна плитка")),

            Map.entry("грунт", List.of("грунтовка")),
            Map.entry("грунтовка", List.of("грунт")),

            Map.entry("гвозді", List.of("цвяхи")),
            Map.entry("цвяхи", List.of("гвозді")),

            Map.entry("шурупи", List.of("саморізи")),
            Map.entry("саморізи", List.of("шурупи"))
    );

    public static List<Map<String, Object>> search(String query) {

        query = query.toLowerCase().trim();

        List<Map<String, Object>> all = CatalogEditor.getAllProducts();
        List<Map<String, Object>> results = new ArrayList<>();

        Set<String> variants = getVariants(query);

        for (Map<String, Object> p : all) {

            String name = p.get("name").toString().toLowerCase();
            int weight = 0;

            // 1️⃣ Точний збіг по name (найважливіше)
            if (name.contains(query)) weight += 50;

            // 2️⃣ Синоніми
            for (String v : variants) {
                if (name.contains(v)) weight += 35;
            }

            // 3️⃣ Root-match
            if (rootMatch(query, name)) weight += 20;

            // 4️⃣ Fuzzy-match
            if (levenshteinDistance(query, name) <= 2) weight += 10;

            if (weight > 0) {
                p.put("weight", weight);
                results.add(p);
            }
        }

        // Сортуємо — найкраще зверху
        results.sort((a, b) -> Integer.compare((int) b.get("weight"), (int) a.get("weight")));

        return results;
    }

    private static Set<String> getVariants(String word) {
        Set<String> set = new HashSet<>();
        set.add(word);

        for (var entry : SYNONYMS.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (word.contains(key)) {
                set.add(key);
                set.addAll(values);
            }
            if (values.stream().anyMatch(word::contains)) {
                set.add(key);
                set.addAll(values);
            }
        }
        return set;
    }

    private static boolean rootMatch(String q, String name) {
        if (q.length() < 4) return false;

        String root1 = q.substring(0, q.length() - 1);
        String root2 = q.substring(0, q.length() - 2);

        return name.contains(root1) || name.contains(root2);
    }

    public static int levenshteinDistance(String a, String b) {

        a = a.toLowerCase();
        b = b.toLowerCase();

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
