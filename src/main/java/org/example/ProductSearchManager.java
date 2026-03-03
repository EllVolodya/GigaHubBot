package org.example;

import java.util.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ProductSearchManager {

    private final StoreBot bot;

    public ProductSearchManager(StoreBot bot) {
        this.bot = bot;
    }

    // 🔹 Основний метод пошуку
    public void performSearch(Long userId, String chatId, String text) throws TelegramApiException {

        text = text.trim();
        System.out.println("[performSearch] User " + userId + " input: '" + text + "'");

        // 🛍️ Перейти в кошик
        if (text.equalsIgnoreCase("🛍️ Перейти в кошик") || text.equalsIgnoreCase("Перейти в кошик")) {
            bot.getUserStates().remove(userId);
            bot.openCartForUser(userId);
            System.out.println("[performSearch] User " + userId + " opened the cart.");
            return;
        }

        // ⬅️ Назад
        if (text.equalsIgnoreCase("⬅️ Назад") || text.equalsIgnoreCase("Назад")) {
            bot.sendUserMenu(userId);
            System.out.println("[performSearch] User " + userId + " exited search mode.");
            return;
        }

        // Якщо користувач нічого не ввів
        if (text.isEmpty()) {
            bot.sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            // ✅ Використовуємо новий SmartSearch
            List<Map<String, Object>> foundProducts = SmartSearch.search(text);

            System.out.println("[performSearch] Found products by SmartSearch: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId,
                        "❌ Товар не знайдено.\n" +
                                "🔎 Митрофан уже шукає кращі варіанти — спробуйте інший запит!");

                bot.sendUserMenu(userId);
                return;
            }

            // Зберігаємо результати
            bot.getSearchResults().put(userId, foundProducts);

            // Якщо знайдено декілька — показуємо список
            if (foundProducts.size() > 1) {

                StringBuilder sb = new StringBuilder("🔎 Митрофан знайшов кілька варіантів:\n\n");
                int idx = 1;

                for (Map<String, Object> product : foundProducts) {
                    sb.append(idx++)
                            .append(". ")
                            .append(product.get("name"))
                            .append("\n");
                }

                sb.append("\n👉 Введіть номер товару, щоб переглянути деталі.");

                bot.sendText(chatId, sb.toString());
                return;
            }

            // Якщо один товар → показуємо одразу
            Map<String, Object> product = foundProducts.get(0);

            bot.getLastShownProduct().put(userId, product);

            // Формуємо текст товару
            String productText = String.format(
                    "📦 %s\n💰 Ціна: %s грн за %s\n📂 Категорія: %s → %s",
                    product.get("name"),
                    product.get("price"),
                    product.get("unit"),
                    product.get("category"),
                    product.get("subcategory")
            );

            // Показуємо список кнопок "Додати в кошик"
            bot.sendProductWithAddToCartRow(userId, chatId, productText);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка під час пошуку товару 😢");
            bot.sendUserMenu(userId);
        }
    }

    // 🔹 Метод для обробки введення номера товару зі списку
    public void handleSearchNumber(Long userId, String chatId, String text) {

        // Якщо це не число — нічого не робимо
        if (!text.matches("\\d+")) {
            return;
        }

        List<Map<String, Object>> products = bot.getSearchResults().get(userId);

        if (products == null || products.isEmpty()) {
            bot.sendText(chatId, "❌ Список товарів порожній.");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(text) - 1;
        } catch (NumberFormatException e) {
            bot.sendText(chatId, "❌ Введіть номер товару цифрою.");
            return;
        }

        if (index < 0 || index >= products.size()) {
            bot.sendText(chatId, "⚠️ Неправильний номер товару. Спробуйте ще раз.");
            return;
        }

        Map<String, Object> product = products.get(index);

        // 🔥 ЗБЕРІГАЄМО ОБРАНИЙ ТОВАР
        bot.getLastShownProduct().put(userId, product);

        try {
            // 🔥 Показуємо товар через chatId (для відправки)
            bot.sendProductFromMap(Long.valueOf(chatId), product);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            bot.sendText(chatId, "⚠️ Помилка при показі товару.");
        }

        // 🔥 Міняємо стан — тепер товар вибраний
        bot.getUserStates().put(userId, "product_shown");
    }
}
