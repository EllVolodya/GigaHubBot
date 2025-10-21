package org.example;

import java.util.*;

public class ProductSearchManager {

    private final StoreBot bot;

    // Конструктор вже існує
    public ProductSearchManager(StoreBot bot) {
        this.bot = bot;
    }

    // Основний метод для обробки пошуку
    public void handleSearch(Long userId, String chatId, String text) {
        text = text.trim();
        System.out.println("[handleSearch] User " + userId + " input: '" + text + "'");

        // Якщо користувач ввів номер товару
        if (text.matches("\\d+")) {
            List<Map<String, Object>> products = bot.getSearchResults().get(userId);
            System.out.println("[handleSearch] searchResults for user " + userId + ": " + products);

            if (products != null) {
                int index = Integer.parseInt(text) - 1;
                if (index >= 0 && index < products.size()) {
                    Map<String, Object> product = products.get(index);
                    System.out.println("[handleSearch] Selected product: " + product);

                    bot.getLastShownProduct().put(userId, product);
                    System.out.println("[handleSearch] lastShownProduct updated: " + bot.getLastShownProduct().get(userId));

                    bot.sendProductDetailsWithButtons(userId, product);
                    return;
                } else {
                    bot.sendText(chatId, "⚠️ Неправильний номер товару. Спробуйте ще раз.");
                    System.out.println("[handleSearch] Invalid index " + index + " for searchResults size " + products.size());
                    return;
                }
            } else {
                System.out.println("[handleSearch] No searchResults found for user " + userId);
            }
        }

        // Якщо користувач ввів назву
        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            System.out.println("[handleSearch] Empty input for user " + userId);
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[handleSearch] Found products: " + foundProducts);

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");
                System.out.println("[handleSearch] No products found for '" + text + "'");
                return;
            }

            if (foundProducts.size() > 1) {
                StringBuilder sb = new StringBuilder("🔎 Знайдено кілька товарів:\n\n");
                int idx = 1;
                for (Map<String, Object> p : foundProducts) {
                    sb.append(idx++).append(". ").append(p.get("name")).append("\n");
                }
                sb.append("\nВведіть номер товару, щоб побачити деталі.");

                bot.getSearchResults().put(userId, foundProducts);
                bot.sendText(chatId, sb.toString());
                System.out.println("[handleSearch] Multiple products found, waiting for user selection. userId=" + userId);
                return;
            }

            // Якщо знайдено один товар
            Map<String, Object> product = foundProducts.get(0);
            bot.getLastShownProduct().put(userId, product);
            System.out.println("[handleSearch] Single product found: " + product);

            bot.sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
            System.out.println("[handleSearch] Exception for user " + userId + ": " + e.getMessage());
        }
    }
}