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
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[performSearch] Found products: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                bot.sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");

                // Після невдалого пошуку показуємо меню користувача
                bot.sendUserMenu(userId);
                return;
            }

            // Зберігаємо результати пошуку
            bot.getSearchResults().put(userId, foundProducts);

            if (foundProducts.size() > 1) {
                // Показуємо список товарів
                StringBuilder sb = new StringBuilder("🔎 Знайдено кілька товарів:\n\n");
                int idx = 1;
                for (Map<String, Object> p : foundProducts) {
                    sb.append(idx++).append(". ").append(p.get("name")).append("\n");
                }
                sb.append("\nВведіть номер товару, щоб побачити деталі.");
                bot.sendText(chatId, sb.toString());
            } else {
                // Якщо один товар — показуємо відразу
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

            // Після помилки теж показуємо меню користувача
            bot.sendUserMenu(userId);
        }
    }

    // 🔹 Метод для обробки введення номера
    public void handleSearchNumber(Long userId, String chatId, String text) throws TelegramApiException {
        // ⬅️ Назад
        if (text.equalsIgnoreCase("⬅️ Назад") || text.equalsIgnoreCase("Назад")) {
            bot.sendUserMenu(userId);
            System.out.println("[handleSearchNumber] User " + userId + " exited search mode.");
            return;
        }

        // ⛔ Якщо користувач не у пошуку — ігноруємо
        String state = bot.getUserStates().get(userId);
        if (state == null || !state.equals("waiting_for_search")) {
            System.out.println("[handleSearchNumber] User " + userId + " not in search mode, ignoring input.");
            return;
        }

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
}
