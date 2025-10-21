package org.example;

import java.util.*;

public class ProductSearchManager {

    public static void handleSearch(StoreBot bot, Long userId, String chatId, String text) {
        text = text.trim();

        // Якщо користувач вводить номер
        if (text.matches("\\d+")) {
            List<Map<String, Object>> products = bot.getSearchResults().get(userId);
            if (products != null) {
                int index = Integer.parseInt(text) - 1;
                if (index >= 0 && index < products.size()) {
                    Map<String, Object> product = products.get(index);
                    bot.getLastShownProduct().put(userId, product);

                    // ✅ Показуємо деталі товару
                    bot.sendProductDetailsWithButtons(userId, product);

                    bot.getSearchResults().remove(userId);
                    return;
                } else {
                    bot.sendText(chatId, "⚠️ Неправильний номер товару. Спробуйте ще раз.");
                    return;
                }
            }
        }

        // Якщо користувач просто ввів назву
        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено.");
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

            // ✅ Показуємо деталі товару
            bot.sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }
}