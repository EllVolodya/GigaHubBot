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
        String selectSql = "SELECT id FROM users WHERE telegramid = ?";
        String insertSql = "INSERT INTO users (name, city, number, number_carts, bonus, is_admin, is_developer, telegramid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, telegramId.toString());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("🔹 Registering new user: " + telegramId);
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, name);            // name
                        insertStmt.setString(2, "");              // city
                        insertStmt.setString(3, "");              // number
                        insertStmt.setInt(4, 0);                  // number_carts
                        insertStmt.setInt(5, 0);                  // bonus
                        insertStmt.setString(6, "NO");            // is_admin
                        insertStmt.setString(7, "NO");            // is_developer
                        insertStmt.setString(8, telegramId.toString()); // telegramid
                        insertStmt.executeUpdate();

                        System.out.println("✅ New user inserted: " + telegramId);

                        // Стартове повідомлення
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

    // Інкремент кількості замовлень
    public void incrementOrders(Long telegramId) {
        String sql = "UPDATE users SET number_carts = number_carts + 1 WHERE telegramid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, telegramId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Список всіх користувачів
    public List<String> getRegisteredUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT telegramid FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("telegramid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Текст стартового повідомлення
    private String getStartMessageText() {
        return "👋 Привіт, друже!\nМитрофан 🤖 — твій помічник у телеграм-магазині 🛍️\n\n" +
                "✨ Каталог товарів, 🔎 Пошук, 🧺 Кошик і доставка, ⭐ Відгуки, 🔥 Хіти продажів, 💡 Допомога, 🌐 Соцмережі.\n" +
                "🫶 Я радий, що ти тут!";
    }
}