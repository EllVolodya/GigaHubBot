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

        // Якщо користувач ввів номер товару
        if (text.matches("\\d+")) {
            List<Map<String, Object>> products = bot.getSearchResults().get(userId);
            if (products != null) {
                int index = Integer.parseInt(text) - 1;
                if (index >= 0 && index < products.size()) {
                    Map<String, Object> product = products.get(index);
                    bot.getLastShownProduct().put(userId, product);

                    // Показуємо деталі
                    bot.sendProductDetailsWithButtons(userId, product);

                    // Відчищаємо пошуковий список
                    bot.getSearchResults().remove(userId);
                    return;
                } else {
                    bot.sendText(chatId, "⚠️ Неправильний номер товару. Спробуйте ще раз.");
                    return;
                }
            }
        }

        // Якщо користувач ввів назву
        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");
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
                return;
            }

            // Якщо знайдено один товар
            Map<String, Object> product = foundProducts.get(0);
            bot.getLastShownProduct().put(userId, product);
            bot.sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }

    // Додати останній показаний товар у кошик
    public void addToCart(Long userId) {
        Map<String, Object> product = bot.getLastShownProduct().get(userId);
        String chatId = String.valueOf(userId);

        if (product == null) {
            bot.sendText(chatId, "❌ Товар не знайдено для додавання в кошик.");
            return;
        }

        bot.getUserCart().computeIfAbsent(userId, k -> new ArrayList<>());
        bot.getUserCart().get(userId).add(product);

        bot.sendText(chatId, "✅ Товар додано до кошика: " + product.get("name"));
        bot.sendText(chatId, "🔎 Введіть назву нового товару або оберіть інший товар з попереднього списку:");
    }
}