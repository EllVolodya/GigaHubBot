package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class UserManager {

    private final Connection connection;

    public UserManager() {
        try {
            this.connection = DatabaseManager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Не вдалося підключитися до БД", e);
        }
    }

    // Реєстрація нового користувача. Повертає стартове повідомлення, якщо користувач новий
    public SendMessage registerUser(Long telegramId, String name, String chatId) {
        String selectSql = "SELECT id FROM accounts WHERE telegramid = ?";
        String insertSql = "INSERT INTO accounts (telegramid, name, city, number, number_carts, bonus, is_admin, is_developer) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, telegramId.toString());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("🔹 Registering new user: " + telegramId);
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, telegramId.toString()); // telegramid
                        insertStmt.setString(2, name);                  // name
                        insertStmt.setString(3, "");                    // city
                        insertStmt.setString(4, "");                    // number
                        insertStmt.setInt(5, 0);                        // number_carts
                        insertStmt.setInt(6, 0);                        // bonus
                        insertStmt.setString(7, "NO");                  // is_admin
                        insertStmt.setString(8, "NO");                  // is_developer
                        insertStmt.executeUpdate();

                        System.out.println("✅ New user inserted: " + telegramId);

                        return SendMessage.builder()
                                .chatId(chatId)
                                .text(getStartMessageText())
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ SQL Error while registering user: " + telegramId);
            e.printStackTrace();
        }
        return null; // Користувач вже є
    }

    // Список всіх користувачів
    public List<String> getRegisteredUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT telegramid FROM accounts";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String idStr = rs.getString("telegramid");
                if (idStr != null && !idStr.isBlank()) {
                    users.add(idStr);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Інкремент кількості замовлень
    public void incrementOrders(String telegramId) {
        String sql = "UPDATE accounts SET number_carts = number_carts + 1 WHERE telegramid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, telegramId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Стартове повідомлення для нових пользователей
    private String getStartMessageText() {
        return "👋 Привіт, друже!\n" +
                "Мене звати Митрофан 🤖 — я твій вірний помічник у цьому чудовому телеграм-магазині 🛍️\n\n" +
                "Кажуть, я вмію знаходити все 😉 — від потрібного товару до вигідної знижки 💸\n" +
                "Тож розслабся, бери каву ☕ і дозволь мені допомогти зробити твої покупки простими та приємними 💫\n\n" +
                "✨ У нашому магазині ти знайдеш усе, що потрібно, а я допоможу розібратися крок за кроком:\n\n" +
                "🔹 Каталог товарів — переглядай категорії й підкатегорії, знаходь потрібні товари на замовлення або просто напиши мені, і я допоможу 😉\n\n" +
                "🔎 Пошук товару — введи назву або частину слова, і я миттєво покажу потрібний результат 💡\n\n" +
                "🧺 Кошик і доставка — додавай товари до кошика й обирай зручний спосіб отримання:\n" +
                "🚚 Нова пошта | 🏠 Доставка додому | 🏬 Самовивіз із наших магазинів.\n\n" +
                "⭐ Відгуки — мені дуже приємно читати ваші слова ❤️ Кожен відгук допомагає мені ставати кращим 💪\n\n" +
                "🔥 Хіти продажів та знижки — не пропусти акції, сезонні пропозиції та найпопулярніші товари 🌞❄️\n\n" +
                "💡 Допомога — маєш питання? Запитуй мене або зв’яжись із нашими консультантами 🧡\n\n" +
                "🌐 Соцмережі та адреси магазинів — дізнавайся про новинки та завітай особисто 🏪\n\n" +
                "🫶 Я радий, що ти тут!\n" +
                "Разом ми зробимо твої покупки легкими, комфортними й трішки чарівними 🌈\n\n" +
                "З повагою, твій вірний помічник — Митрофан 🤖💙";
    }
}
