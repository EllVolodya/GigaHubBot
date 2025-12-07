package org.example;

import java.util.*;

public class SmartSearch {

    // 🔹 Синоніми + пов’язані слова
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

            Map.entry("бокоріз", List.of("бокорізи", "кусачки", "кліщі")),
            Map.entry("бокорізи", List.of("бокоріз", "кусачки", "кліщі")),
            Map.entry("кусачки", List.of("бокорізи", "бокоріз")),
            Map.entry("кліщі", List.of("бокорізи", "бокоріз")),

            Map.entry("шурупи", List.of("саморізи")),
            Map.entry("саморізи", List.of("шурупи"))
    );

    // ================================
    // 🔥 УНІВЕРСАЛЬНИЙ ПОШУК
    // ================================
    public static List<Map<String, Object>> search(String query) {

        query = query.toLowerCase().trim();

        List<Map<String, Object>> all = CatalogEditor.getAllProducts();
        List<Map<String, Object>> results = new ArrayList<>();

        // 🔥 Всі варіанти слів, які підходять під запит
        Set<String> variants = getVariants(query);

        for (Map<String, Object> p : all) {

            String name = p.get("name").toString().toLowerCase();

            int weight = 0;

            // 1️⃣ Найважливіше — пряме входження
            if (name.contains(query)) weight += 100;

            // 2️⃣ Синоніми
            for (String v : variants) {
                if (name.contains(v)) weight += 70;
            }

            // 3️⃣ Root-match (підтримка множини)
            if (rootMatch(query, name)) weight += 40;

            // 4️⃣ Fuzzy-match (до 2 помилок)
            if (levenshtein(query, name) <= 2) weight += 20;

            // Додаємо товар, якщо хоч щось збіглося
            if (weight > 0) {
                p.put("weight", weight);
                results.add(p);
            }
        }

        // Сортуємо — найкраще зверху
        results.sort((a, b) -> Integer.compare((int) b.get("weight"), (int) a.get("weight")));

        return results;
    }

    // ================================
    // 🔹 Отримання всіх словоформ
    // ================================
    private static Set<String> getVariants(String word) {
        Set<String> set = new HashSet<>();
        set.add(word);

        for (var entry : SYNONYMS.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (word.contains(key) || values.contains(word)) {
                set.add(key);
                set.addAll(values);
            }
        }
        return set;
    }

    // ================================
    // 🔹 Root-match (множина → однина)
    // ================================
    private static boolean rootMatch(String q, String name) {
        if (q.length() < 4) return false;

        String r1 = q.substring(0, q.length() - 1);
        String r2 = q.substring(0, q.length() - 2);

        return name.contains(r1) || name.contains(r2);
    }

    // ================================
    // 🔹 Levenshtein (fuzzy)
    // ================================
    private static int levenshtein(String a, String b) {

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
