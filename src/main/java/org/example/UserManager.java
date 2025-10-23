package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class UserManager {

    private Connection connection;

    // Конструктор ініціалізує з'єднання
    public UserManager() {
        try {
            this.connection = DatabaseManager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Не вдалося підключитися до БД", e);
        }
    }

    // Реєстрація нового користувача
    public SendMessage registerUser(Long telegramId, String name, String chatId) {
        String selectSql = "SELECT id FROM users WHERE telegram_id = ?";
        // Зверни увагу: просто вставляємо значення, без DEFAULT
        String insertSql = "INSERT INTO users (telegram_id, name, is_admin, is_developer, number_carts) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, telegramId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    // Новий користувач → реєструємо
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setLong(1, telegramId);
                        insertStmt.setString(2, name);
                        insertStmt.setString(3, "NO"); // is_admin
                        insertStmt.setString(4, "NO"); // is_developer
                        insertStmt.setInt(5, 0);       // number_carts
                        insertStmt.executeUpdate();

                        System.out.println("✅ New user registered: " + telegramId);

                        // Відправляємо стартове повідомлення одразу
                        return SendMessage.builder()
                                .chatId(chatId)
                                .text(getStartMessageText())
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Користувач вже є → нічого не надсилаємо
    }

    // Інкремент кількості замовлень
    public void incrementOrders(Long telegramId) {
        String sql = "UPDATE users SET number_carts = number_carts + 1 WHERE telegram_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, telegramId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Список всіх користувачів
    public List<Long> getRegisteredUsers() {
        List<Long> users = new ArrayList<>();
        String sql = "SELECT telegram_id FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getLong("telegram_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Надсилає стартове повідомлення, якщо користувач новий
    public SendMessage sendStartMessageIfNewUser(String chatId, Long telegramId) {
        String sql = "SELECT start_sent FROM users WHERE telegram_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, telegramId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && "NO".equals(rs.getString("start_sent"))) {
                    String text = getStartMessageText();
                    // Оновлюємо статус start_sent
                    String updateSql = "UPDATE users SET start_sent = 'YES' WHERE telegram_id = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, telegramId);
                        updateStmt.executeUpdate();
                    }
                    return SendMessage.builder()
                            .chatId(chatId)
                            .text(text)
                            .build();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Інформаційне повідомлення користувачу
    public SendMessage getInfoMessage(String chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(getStartMessageText())
                .build();
    }

    // Текст стартового повідомлення
    private String getStartMessageText() {
        return "👋 Привіт, друже!\nМитрофан 🤖 — твій помічник у телеграм-магазині 🛍️\n\n" +
                "✨ Каталог товарів, 🔎 Пошук, 🧺 Кошик і доставка, ⭐ Відгуки, 🔥 Хіти продажів, 💡 Допомога, 🌐 Соцмережі.\n" +
                "🫶 Я радий, що ти тут!";
    }
}
