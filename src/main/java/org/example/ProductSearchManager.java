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

            bot.getSearchResults().put(userId, foundProducts);

            for (Map<String, Object> product : foundProducts) {
                // Формуємо текст товару
                String productText = String.format(
                        "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s",
                        product.get("name"),
                        product.get("price"),
                        product.get("category"),
                        product.get("subcategory")
                );

                // Зберігаємо останній показаний товар для додавання в кошик
                bot.getLastShownProduct().put(userId, product);

                // Надсилаємо повідомлення з кнопкою в рядку
                bot.sendProductWithAddToCartRow(userId, chatId, productText);
            }

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }

    private void addToCart(Long userId, Map<String, Object> product) {
        String chatId = String.valueOf(userId);
        bot.getUserCart().computeIfAbsent(userId, k -> new ArrayList<>());
        bot.getUserCart().get(userId).add(product);

        bot.sendText(chatId, "✅ Товар додано до кошика: " + product.get("name"));
    }
}