package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class UserManager {

    private final Map<Long, Long> userChatMap = new HashMap<>();
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
        return "\uD83D\uDC4B Вітаю, друже!\n" +
                "\n" +
                "Я — Митрофан \uD83E\uDD16, офіційний цифровий помічник будівельної мережі GigaHub.  \n" +
                "Моя робота — зробити твої покупки швидкими, зручними та зрозумілими, без зайвої метушні.\n" +
                "\n" +
                "Тут ти можеш:\n" +
                "\uD83E\uDDF1 Переглядати будівельні матеріали  \n" +
                "\uD83D\uDD0E Шукати товари за назвою  \n" +
                "\uD83E\uDDFA Додавати їх у свій кошик  \n" +
                "\uD83D\uDE9A Оформлювати замовлення за хвилини  \n" +
                "\uD83D\uDCAC Задати питання консультанту  \n" +
                "\uD83D\uDCF8 Надсилати фото товару, щоб ми підказали, чи є він у наявності\n" +
                "\n" +
                "Я завжди поруч, готовий допомогти з вибором цементу, фарби, інструментів, сантехніки, господарських товарів та всього, що може знадобитися на будові або в ремонті.\n" +
                "\n" +
                "Обирай у меню, що саме хочеш зробити,  \n" +
                "і давай разом збудуємо щось круте \uD83E\uDDE9\uD83D\uDC9B";
    }

    public void saveChatId(long userId, long chatId) {
        userChatMap.put(userId, chatId);
    }

    // отримуємо chatId
    public Long getChatId(long userId) {
        return userChatMap.get(userId);
    }
}
