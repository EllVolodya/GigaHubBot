package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseManager {

    private static final String URL = "jdbc:mysql://crossover.proxy.rlwy.net:21254/railway?useUnicode=true&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=5000";
    private static final String USER = "root";
    private static final String PASSWORD = "ByZkOlzbofgNZSBVlPCdjayWsDBJfEcP";

    private static Connection connection;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Database connected successfully!");
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
        String category = "❓";
        String subcategory = "❓";

        String query = """
            SELECT c.name AS category, s.name AS subcategory
            FROM categories c
            JOIN subcategories s ON s.category_id = c.id
            JOIN products p ON p.subcategory_id = s.id
            WHERE p.name = ? LIMIT 1
        """;

        try (Connection conn = getConnection();
             var ps = conn.prepareStatement(query)) {

            ps.setString(1, name);
            var rs = ps.executeQuery();

            if (rs.next()) {
                category = rs.getString("category");
                subcategory = rs.getString("subcategory");
            } else {
                System.out.println("⚠️ Category info not found for product: " + name);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error while getting category info for " + name);
            e.printStackTrace();
        }

        return "📂 " + category + " → " + subcategory;
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
}
