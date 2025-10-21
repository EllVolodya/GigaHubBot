package org.example;

import java.util.*;

public class ProductSearchManager {

    private final StoreBot bot;

    public ProductSearchManager(StoreBot bot) {
        this.bot = bot;
    }

    public void handleSearch(Long userId, String chatId, String text) {
        text = text.trim();

        if (text.matches("\\d+")) {
            List<Map<String, Object>> results = bot.getSearchResults().get(userId); // Викликаємо через bot
            if (results != null && !results.isEmpty()) {
                int index = Integer.parseInt(text) - 1;
                if (index >= 0 && index < results.size()) {
                    Map<String, Object> product = results.get(index);
                    bot.getLastShownProduct().put(userId, product);

                    // Показуємо деталі
                    bot.sendProductDetailsWithButtons(userId, product);

                    bot.getSearchResults().remove(userId);
                    return;
                } else {
                    bot.sendText(chatId, "⚠️ Неправильний номер товару.");
                    return;
                }
            }
        }

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

            Map<String, Object> product = foundProducts.get(0);
            bot.getLastShownProduct().put(userId, product);
            bot.sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }
}