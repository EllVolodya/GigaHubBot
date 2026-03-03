package org.example;

import java.util.*;

public class ProductSearchManager {

    private final StoreBot bot;

    public ProductSearchManager(StoreBot bot) {
        this.bot = bot;
    }

    // 🔹 ОСНОВНИЙ ПОШУК
    public void performSearch(Long userId, String chatId, String text) {

        text = text.trim();

        // 🛍️ Перейти в кошик
        if (text.equalsIgnoreCase("🛍️ Перейти в кошик")) {
            bot.getUserStates().remove(userId);
            bot.openCartForUser(userId);
            return;
        }

        // ⬅️ Назад
        if (text.equalsIgnoreCase("⬅️ Назад")) {
            bot.getUserStates().remove(userId);
            bot.sendUserMenu(userId);
            return;
        }

        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            List<Map<String, Object>> foundProducts = SmartSearch.search(text);

            if (foundProducts == null || foundProducts.isEmpty()) {
                bot.sendText(chatId,
                        "❌ Товар не знайдено.\nСпробуйте інший запит.");
                return;
            }

            bot.getSearchResults().put(userId, foundProducts);

            // якщо кілька товарів
            if (foundProducts.size() > 1) {

                StringBuilder sb = new StringBuilder();
                sb.append("🔎 Знайдено кілька варіантів:\n\n");

                for (int i = 0; i < foundProducts.size(); i++) {
                    sb.append(i + 1)
                            .append(". ")
                            .append(foundProducts.get(i).get("name"))
                            .append("\n");
                }

                sb.append("\n👉 Введіть номер товару для перегляду.");

                bot.sendText(chatId, sb.toString());
                return;
            }

            // якщо один товар
            showTextProduct(userId, chatId, foundProducts.get(0));

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку.");
        }
    }

    // 🔹 ОБРОБКА НОМЕРА
    public void handleSearchNumber(Long userId, String chatId, String text) {

        if (!text.matches("\\d+")) return;

        List<Map<String, Object>> results =
                bot.getSearchResults().get(userId);

        if (results == null || results.isEmpty()) {
            bot.sendText(chatId, "❌ Список товарів порожній.");
            return;
        }

        int index = Integer.parseInt(text) - 1;

        if (index < 0 || index >= results.size()) {
            bot.sendText(chatId, "⚠️ Невірний номер товару.");
            return;
        }

        showTextProduct(userId, chatId, results.get(index));
    }

    // 🔹 ПОКАЗ ТОВАРУ (БЕЗ ФОТО)
    private void showTextProduct(Long userId, String chatId,
                                 Map<String, Object> product) {

        bot.getLastShownProduct().put(userId, product);

        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "0"));
        String unit = String.valueOf(product.getOrDefault("unit", "шт"));
        String category = String.valueOf(product.getOrDefault("category", ""));
        String subcategory = String.valueOf(product.getOrDefault("subcategory", ""));
        String description = String.valueOf(product.getOrDefault("description", ""));

        StringBuilder sb = new StringBuilder();
        sb.append("📦 ").append(name).append("\n")
                .append("💰 Ціна: ").append(price)
                .append(" грн за ").append(unit);

        if (!category.isEmpty()) {
            sb.append("\n📂 ").append(category);
            if (!subcategory.isEmpty()) {
                sb.append(" → ").append(subcategory);
            }
        }

        if (!description.isEmpty()) {
            sb.append("\n📖 ").append(description);
        }

        bot.sendProductWithAddToCartRow(userId, chatId, sb.toString());
    }
}