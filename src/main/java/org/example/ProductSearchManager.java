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

        // 🔥 ІГНОРУЄМО ВСІ КНОПКИ (щоб не запускати пошук)
        if (text.equals("🛒 Додати в кошик") ||
                text.equalsIgnoreCase("🛍️ Перейти в кошик") ||
                text.equalsIgnoreCase("⬅️ Назад") ||
                text.equalsIgnoreCase("Назад")) {
            return;
        }

        // Порожній текст
        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {

            List<Map<String, Object>> foundProducts =
                    SmartSearch.search(text);

            if (foundProducts == null || foundProducts.isEmpty()) {

                bot.sendText(chatId,
                        "❌ Товар не знайдено.\nСпробуйте інший запит.");

                return;
            }

            // Зберігаємо результати пошуку
            bot.getSearchResults().put(userId, foundProducts);

            // Якщо знайдено кілька товарів
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

            // Якщо знайдено один товар — показуємо
            showTextProduct(userId, chatId,
                    foundProducts.get(0));

        } catch (Exception e) {

            System.out.println("Search error: " + e.getMessage());

            bot.sendText(chatId,
                    "⚠️ Помилка під час пошуку.");
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
    private void showTextProduct(Long userId,
                                 String chatId,
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

        if (!category.isBlank()) {
            sb.append("\n📂 ").append(category);
            if (!subcategory.isBlank()) {
                sb.append(" → ").append(subcategory);
            }
        }

        if (!description.isBlank()) {
            sb.append("\n\n📖 ").append(description);
        }

        bot.sendSearchProduct(userId, chatId, sb.toString());
    }
}