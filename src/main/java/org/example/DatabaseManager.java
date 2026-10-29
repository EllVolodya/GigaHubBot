package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseManager {

    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private static void validateDatabaseConfig() {
        if (URL == null || URL.isBlank()
                || USER == null || USER.isBlank()
                || PASSWORD == null || PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "Missing database configuration. Check DB_URL, DB_USER and DB_PASSWORD."
            );
        }
    }

    public static Connection getConnection() throws SQLException {
        validateDatabaseConfig();

        return DriverManager.getConnection(URL, USER, PASSWORD);
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
        // Кожне з'єднання закривається у try-with-resources.
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
