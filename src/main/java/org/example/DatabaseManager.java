package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseManager {

    private static final String URL =
            "jdbc:postgresql://postgres.yrpfrdiqnqegfjebshtc:J/M#KL8Pc!+2uZT@aws-0-eu-west-1.pooler.supabase.com:5432/postgres";

    private static final String USER = "postgres";
    private static final String PASSWORD = System.getenv("SUPABASE_DB_PASSWORD");

    private static Connection connection;

    static {
        try {
            Class.forName("org.postgresql.Driver");

            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("✅ Supabase PostgreSQL connected successfully!");

        } catch (Exception e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                // Перепідключення лише якщо з’єднання мертве
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("🔄 Database reconnected!");
            }
        } catch (SQLException e) {
            throw new SQLException("❌ Не вдалося підключитися до БД", e);
        }
        return connection;
    }

    public static String getCategoryInfoForProduct(Map<String, Object> product) {
        String name = String.valueOf(product.get("name"));

        String query = """
        SELECT c.name AS category, s.name AS subcategory
        FROM public.products p
        LEFT JOIN public.subcategories s ON s.id = p.subcategory_id
        LEFT JOIN public.categories c ON c.id = s.category_id
        WHERE p.name = ?
        LIMIT 1
        """;

        try (Connection conn = getConnection();
             var ps = conn.prepareStatement(query)) {

            ps.setString(1, name);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String category = rs.getString("category");
                    String subcategory = rs.getString("subcategory");

                    return "📂 "
                            + (category != null ? category : "❓")
                            + " → "
                            + (subcategory != null ? subcategory : "❓");
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "❌ Error while getting category info for " + name
            );
            e.printStackTrace();
        }

        return "📂 ❓ → ❓";
    }

    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Database disconnected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteUser(long telegramId) {
        String sql = "DELETE FROM users WHERE telegram_id = ?";

        try (Connection conn = getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, telegramId);

            int rows = ps.executeUpdate();

            System.out.println("🗑 Видалено користувачів: " + rows);

        } catch (SQLException e) {
            System.err.println("❌ Помилка видалення користувача");
            e.printStackTrace();
        }
    }

    public static long getUsersCount(boolean onlyCurrentMonth) {
        String sql = onlyCurrentMonth
                ? """
              SELECT COUNT(*)
              FROM public.accounts
              WHERE registered_at IS NOT NULL
                AND registered_at >= DATE_TRUNC('month', CURRENT_DATE)
                AND registered_at < DATE_TRUNC('month', CURRENT_DATE)
                                     + INTERVAL '1 month'
              """
                : "SELECT COUNT(*) FROM public.accounts";

        try (Connection conn = getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                long count = rs.getLong(1);
                System.out.println("📊 Users count (" +
                        (onlyCurrentMonth ? "місяць" : "весь час") +
                        "): " + count);
                return count;
            }
        } catch (SQLException e) {
            System.err.println("❌ Помилка підрахунку користувачів");
            e.printStackTrace();
        }

        return 0;
    }

    public static long getOrdersCount(boolean onlyCurrentMonth) {
        String sql = onlyCurrentMonth
                ? """
              SELECT COUNT(*)
              FROM public.orders
              WHERE date >= DATE_TRUNC('month', CURRENT_DATE)
                AND date < DATE_TRUNC('month', CURRENT_DATE)
                               + INTERVAL '1 month'
              """
                : "SELECT COUNT(*) FROM public.orders";

        try (Connection conn = getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Помилка підрахунку замовлень");
            e.printStackTrace();
        }

        return 0;
    }
}
