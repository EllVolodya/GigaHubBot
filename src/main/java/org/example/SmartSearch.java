package org.example;

import java.util.*;

public class SmartSearch {

    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(

            // 🔥 Група бойлерів
            Map.entry("бойлер", List.of("водонагрівач", "тен", "фланець", "клапан", "анод")),
            Map.entry("водонагрівач", List.of("бойлер", "тен")),
            Map.entry("тен", List.of("бойлер", "водонагрівач")),

            // Плитка
            Map.entry("кафель", List.of("плитка")),
            Map.entry("плитка", List.of("кафель", "керамічна плитка")),

            // Грунт
            Map.entry("грунт", List.of("грунтовка")),
            Map.entry("грунтовка", List.of("грунт")),

            // Цвяхи
            Map.entry("гвозді", List.of("цвяхи")),
            Map.entry("цвяхи", List.of("гвозді")),

            Map.entry("бокоріз", List.of("бокорізи", "кусачки", "кліщі")),
            Map.entry("бокорізи", List.of("бокоріз", "кусачки", "кліщі")),
            Map.entry("кусачки", List.of("бокорізи", "бокоріз")),
            Map.entry("кліщі", List.of("бокорізи", "бокоріз")),

            // Шурупи
            Map.entry("шурупи", List.of("саморізи")),
            Map.entry("саморізи", List.of("шурупи"))
    );

    public static List<Map<String, Object>> search(String query) {

        query = query.toLowerCase().trim();

        List<Map<String, Object>> all = CatalogEditor.getAllProducts();
        List<Map<String, Object>> results = new ArrayList<>();

        // Синоніми, включаючи введене слово
        Set<String> allVariants = getVariants(query);

        for (Map<String, Object> p : all) {

            String name = p.get("name").toString().toLowerCase();
            int weight = 0;

            // ✔ 1. Точна часткова відповідність
            if (name.contains(query)) weight += 10;

            // ✔ 2. Будь-який синонім
            for (String v : allVariants) {
                if (name.contains(v)) weight += 7;
            }

            // ✔ 3. Root-match (бойл → бойлер)
            if (rootMatch(query, name)) weight += 4;

            // ✔ 4. Fuzzy (до 2 помилок)
            if (levenshteinDistance(query, name) <= 2) weight += 3;

            // Якщо хоч щось збіглося — додаємо
            if (weight > 0) {
                p.put("weight", weight);
                results.add(p);
            }
        }

        results.sort((a, b) -> Integer.compare((int) b.get("weight"), (int) a.get("weight")));

        return results;
    }

    private static Set<String> getVariants(String word) {

        Set<String> set = new HashSet<>();
        set.add(word);

        for (var entry : SYNONYMS.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            // Якщо ввели слово — додаємо всі його синоніми
            if (word.contains(key)) {
                set.add(key);
                set.addAll(values);
            }

            // Якщо ввели значення — додаємо ключ
            if (values.stream().anyMatch(word::contains)) {
                set.add(key);
                set.addAll(values);
            }
        }

        return set;
    }

    private static boolean rootMatch(String q, String name) {
        if (q.length() < 3) return false;

        String root = q.substring(0, q.length() - 1); // водонагрі ≈ водонагрівач
        return name.contains(root);
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
