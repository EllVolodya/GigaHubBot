package org.example;

import java.sql.*;
import java.util.*;

public class InviteManager {

    private final Connection connection;

    public InviteManager() throws SQLException {
        this.connection = DatabaseManager.getConnection();
        this.connection.setAutoCommit(true);
    }

    // Створення інвайту
    public boolean addInvite(String name, String kasa, String city, String botUsername) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 8);
        inviteCode = inviteCode.toLowerCase(); // нормалізація
        String inviteLink = "https://t.me/" + botUsername + "?start=" + inviteCode;

        String sql = "INSERT INTO invites (name, kasa, city, invite, number) VALUES (?, ?, ?, ?, 0)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, kasa);
            stmt.setString(3, city);
            stmt.setString(4, inviteLink);

            stmt.executeUpdate();
            System.out.println("✅ Invite added: " + inviteLink);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Видалення інвайту
    public boolean deleteInvite(int id) {
        String sql = "DELETE FROM invites WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Редагування інвайту
    public boolean editInvite(int id, String name, String kasa, String city) {
        String sql = "UPDATE invites SET name = ?, kasa = ?, city = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, kasa);
            stmt.setString(3, city);
            stmt.setInt(4, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // 🔥 НАЙВАЖЛИВІШЕ → ОНОВЛЕННЯ ЛІЧИЛЬНИКА
    public boolean incrementInviteNumber(String inviteCode) {
        // нормалізація (видаляємо всі пробіли і приховані знаки)
        inviteCode = normalize(inviteCode);

        String sql = "UPDATE invites " +
                "SET number = number + 1 " +
                "WHERE LOWER(invite) LIKE LOWER(?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + inviteCode + "%");

            int updated = stmt.executeUpdate();
            System.out.println("🔍 incrementInviteNumber -> updated rows: " + updated);

            return updated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Отримати інвайт по коду
    public Map<String, Object> getInviteByCode(String inviteCode) {
        inviteCode = normalize(inviteCode);

        String sql = "SELECT * FROM invites WHERE LOWER(invite) LIKE LOWER(?) LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + inviteCode + "%");
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", rs.getInt("id"));
                data.put("name", rs.getString("name"));
                data.put("kasa", rs.getString("kasa"));
                data.put("city", rs.getString("city"));
                data.put("invite", rs.getString("invite"));
                data.put("number", rs.getInt("number"));
                return data;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    // 🔧 Нормалізація коду — ОБОВ'ЯЗКОВО!
    private String normalize(String code) {
        if (code == null) return "";
        return code
                .replace("\u00A0", "") // NO-BREAK space
                .replace(" ", "")       // звичайний пробіл
                .trim()
                .toLowerCase();
    }
}
