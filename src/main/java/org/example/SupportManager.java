package org.example;

import java.sql.*;

public class SupportManager {


    public static ResultSet getOpenTickets() {

        try {

            Connection connection = DatabaseManager.getConnection();

            PreparedStatement ps = connection.prepareStatement("""
                SELECT *
                FROM support_tickets
                WHERE status <> 'CLOSED'
                ORDER BY id DESC
                """);

            return ps.executeQuery();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ResultSet getTicket(long id) {

        try {

            Connection connection = DatabaseManager.getConnection();

            PreparedStatement ps = connection.prepareStatement("""
                SELECT *
                FROM support_tickets
                WHERE id = ?
                """);

            ps.setLong(1, id);

            return ps.executeQuery();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static long createTicket(long userId,
                                    String username,
                                    String productName,
                                    String message) {

        String sql = """
                INSERT INTO support_tickets
                (user_id, username, product_name, message)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            ps.setLong(1, userId);
            ps.setString(2, username);
            ps.setString(3, productName);
            ps.setString(4, message);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static void saveAnswer(long ticketId,
                                  long consultantId,
                                  String answer) {

        String sql = """
                UPDATE support_tickets
                SET
                    answer = ?,
                    consultant_id = ?,
                    answered_at = CURRENT_TIMESTAMP,
                    status = 'ANSWERED'
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, answer);
            ps.setLong(2, consultantId);
            ps.setLong(3, ticketId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeTicket(long ticketId) {

        String sql = """
                UPDATE support_tickets
                SET status='CLOSED'
                WHERE id=?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteTicket(long ticketId) {

        String sql = "DELETE FROM support_tickets WHERE id=?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}