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
            System.out.println("[performSearch] Empty input for user " + userId);
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[performSearch] Found products: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");
                System.out.println("[performSearch] No products found for '" + text + "'");
                return;
            }

            bot.getSearchResults().put(userId, foundProducts);

            for (Map<String, Object> product : foundProducts) {
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
            System.out.println("[performSearch] Exception for user " + userId + ": " + e.getMessage());
        }
    }

    private void addToCart(Long userId, Map<String, Object> product) {
        String chatId = String.valueOf(userId);
        bot.getUserCart().computeIfAbsent(userId, k -> new ArrayList<>());
        bot.getUserCart().get(userId).add(product);

        bot.sendText(chatId, "✅ Товар додано до кошика: " + product.get("name"));
    }
}