package org.example;

import java.util.*;

public class ProductSearchManager {

    private final StoreBot bot;

    public ProductSearchManager(StoreBot bot) {
        this.bot = bot;
    }

    // Основний метод пошуку
    public void performSearch(Long userId, String chatId, String text) {
        text = text.trim();
        System.out.println("[performSearch] User " + userId + " input: '" + text + "'");

        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[performSearch] Found products: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");
                return;
            }

            // Зберігаємо результати пошуку
            bot.getSearchResults().put(userId, foundProducts);

            if (foundProducts.size() > 1) {
                // Показуємо список товарів з номерами
                StringBuilder sb = new StringBuilder("🔎 Знайдено кілька товарів:\n\n");
                int idx = 1;
                for (Map<String, Object> p : foundProducts) {
                    sb.append(idx++).append(". ").append(p.get("name")).append("\n");
                }
                sb.append("\nВведіть номер товару, щоб побачити деталі.");
                bot.sendText(chatId, sb.toString());
            } else {
                // Якщо один товар — одразу показуємо з кнопкою
                Map<String, Object> product = foundProducts.get(0);
                bot.getLastShownProduct().put(userId, product);
                String productText = String.format(
                        "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s",
                        product.get("name"),
                        product.get("price"),
                        product.get("category"),
                        product.get("subcategory")
                );
                bot.sendProductWithAddToCartRow(userId, chatId, productText);
            }

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }

    // Метод для обробки введення номера
    public void handleSearchNumber(Long userId, String chatId, String text) {
        if (!text.matches("\\d+")) return;

        List<Map<String, Object>> products = bot.getSearchResults().get(userId);
        if (products == null || products.isEmpty()) return;

        int index = Integer.parseInt(text) - 1;
        if (index < 0 || index >= products.size()) {
            bot.sendText(chatId, "⚠️ Неправильний номер товару. Спробуйте ще раз.");
            return;
        }

        Map<String, Object> product = products.get(index);
        bot.getLastShownProduct().put(userId, product);

        String productText = String.format(
                "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s",
                product.get("name"),
                product.get("price"),
                product.get("category"),
                product.get("subcategory")
        );
        bot.sendProductWithAddToCartRow(userId, chatId, productText);
    }

    private void addToCart(Long userId, Map<String, Object> product) {
        String chatId = String.valueOf(userId);
        bot.getUserCart().computeIfAbsent(userId, k -> new ArrayList<>());
        bot.getUserCart().get(userId).add(product);

        bot.sendText(chatId, "✅ Товар додано до кошика: " + product.get("name"));
    }
}