package org.example;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class CatalogEditor {

    // --- Додати категорію
    public static boolean addCategory(String categoryName) {
        String checkSql = "SELECT id FROM categories WHERE name = ?";
        String insertSql = "INSERT INTO categories (name) VALUES (?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, categoryName);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return false;

            insertStmt.setString(1, categoryName);
            insertStmt.executeUpdate();
            System.out.println("✅ Категорію додано: " + categoryName);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ addCategory error: " + e.getMessage());
            return false;
        }
    }

    // --- Видалити категорію
    public static boolean deleteCategory(String categoryName) {
        String sql = "DELETE FROM categories WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ deleteCategory error: " + e.getMessage());
            return false;
        }
    }

    // --- Додати підкатегорію
    public static boolean addSubcategory(String categoryName, String subcategoryName) {
        String findCategorySql = "SELECT id FROM categories WHERE name = ?";
        String checkSubSql = "SELECT id FROM subcategories WHERE name = ? AND category_id = ?";
        String insertSql = "INSERT INTO subcategories (name, category_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement findCategoryStmt = conn.prepareStatement(findCategorySql);
             PreparedStatement checkStmt = conn.prepareStatement(checkSubSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            findCategoryStmt.setString(1, categoryName);
            ResultSet categoryRs = findCategoryStmt.executeQuery();
            if (!categoryRs.next()) return false;
            int categoryId = categoryRs.getInt("id");

            checkStmt.setString(1, subcategoryName);
            checkStmt.setInt(2, categoryId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return false;

            insertStmt.setString(1, subcategoryName);
            insertStmt.setInt(2, categoryId);
            insertStmt.executeUpdate();

            System.out.println("✅ Підкатегорію '" + subcategoryName + "' додано до '" + categoryName + "'");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ addSubcategory error: " + e.getMessage());
            return false;
        }
    }

    // --- Додати товар у підкатегорію
    public static boolean addProductToSubcategory(String productName, double price, String subcategoryName) {
        String findSubSql = "SELECT id FROM subcategories WHERE name = ?";
        String checkProductSql = "SELECT id FROM products WHERE name = ? AND subcategory_id = ?";
        String insertSql = """
            INSERT INTO products (name, price, unit, description, photo, created_at, subcategory_id)
            VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement findSubStmt = conn.prepareStatement(findSubSql);
             PreparedStatement checkStmt = conn.prepareStatement(checkProductSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            findSubStmt.setString(1, subcategoryName);
            ResultSet subRs = findSubStmt.executeQuery();
            if (!subRs.next()) {
                System.out.println("❌ Підкатегорію '" + subcategoryName + "' не знайдено");
                return false;
            }
            int subcategoryId = subRs.getInt("id");

            checkStmt.setString(1, productName);
            checkStmt.setInt(2, subcategoryId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("⚠️ Товар '" + productName + "' вже існує");
                return false;
            }

            insertStmt.setString(1, productName);
            insertStmt.setDouble(2, price);
            insertStmt.setString(3, "шт");
            insertStmt.setString(4, "");
            insertStmt.setString(5, "");
            insertStmt.setInt(6, subcategoryId);

            insertStmt.executeUpdate();
            System.out.println("✅ Додано товар '" + productName + "' у '" + subcategoryName + "'");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ addProductToSubcategory error: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateProductManufacturer(String productName, String manufacturer) {
        if (productName == null || productName.trim().isEmpty()) {
            System.out.println("❌ Назва продукту пуста");
            return false;
        }

        productName = productName.trim();
        boolean clearManufacturer = manufacturer == null
                || manufacturer.trim().isEmpty()
                || manufacturer.equalsIgnoreCase("❌");

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(true);

            // --- Дебаг: перевіримо, чи рядок взагалі існує ---
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT name FROM products WHERE LOWER(name) = LOWER(?)"
            )) {
                checkStmt.setString(1, productName);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    System.out.println("⚠️ DEBUG: Рядок з назвою '" + productName + "' не знайдено у базі.");
                    return false;
                } else {
                    System.out.println("DEBUG: Рядок знайдено у базі: '" + rs.getString("name") + "'");
                }
            }

            // --- Основне оновлення ---
            String sql = clearManufacturer
                    ? "UPDATE products SET manufacturer = NULL WHERE LOWER(name) = LOWER(?)"
                    : "UPDATE products SET manufacturer = ? WHERE LOWER(name) = LOWER(?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (clearManufacturer) {
                    stmt.setString(1, productName);
                    System.out.println("DEBUG: Setting parameter 1 -> productName='" + productName + "'");
                } else {
                    byte[] manufacturerBytes = manufacturer.trim().getBytes(StandardCharsets.UTF_8);
                    stmt.setBytes(1, manufacturerBytes);
                    stmt.setString(2, productName);
                    System.out.println("DEBUG: Setting parameter 1 -> manufacturer='" + manufacturer + "'");
                    System.out.println("DEBUG: Setting parameter 2 -> productName='" + productName + "'");
                }

                int rows = stmt.executeUpdate();
                System.out.println("DEBUG: Rows affected = " + rows);

                if (rows == 0) {
                    System.out.println("⚠️ Товар '" + productName + "' не вдалося оновити.");
                    return false;
                }

                if (clearManufacturer) {
                    System.out.println("✅ Виробника видалено для товару: " + productName);
                } else {
                    System.out.println("✅ Виробника збережено для товару: " + productName);
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ updateProductManufacturer SQL error: " + e.getMessage());
            return false;
        }
    }

    // --- Оновити будь-яке поле товару
    public static boolean updateField(String productName, String field, Object value) {

        if (productName == null || productName.trim().isEmpty()) {
            System.out.println("❌ Назва товару пуста.");
            return false;
        }

        if (field == null || field.trim().isEmpty()) {
            System.out.println("❌ Поле не вказано.");
            return false;
        }

        // Нормалізація назви
        productName = productName
                .replaceAll("\\s+", " ")
                .trim();

        field = field.trim().toLowerCase();

        System.out.println("========== updateField() ==========");
        System.out.println("DEBUG Product = " + productName);
        System.out.println("DEBUG Field   = " + field);
        System.out.println("DEBUG Value   = " + value);

        // Дозволені поля
        Set<String> allowedFields = Set.of(
                "price",
                "description",
                "photo",
                "unit",
                "name",
                "manufacturer"
        );

        if (!allowedFields.contains(field)) {
            System.out.println("❌ Заборонене поле: " + field);
            return false;
        }

        boolean isBlobField = field.equals("manufacturer");

        String checkSql = """
        SELECT id, name
        FROM products
        WHERE BINARY TRIM(name) = TRIM(?)
        LIMIT 1
        """;

        String updateSql =
                "UPDATE products SET " + field + " = ? " +
                        "WHERE BINARY TRIM(name) = TRIM(?)";

        try (Connection conn = DatabaseManager.getConnection()) {

            conn.setAutoCommit(true);

            int productId = -1;
            String dbName = null;

            // Перевіряємо чи товар існує
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                checkStmt.setString(1, productName);

                try (ResultSet rs = checkStmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("⚠️ Товар не знайдено: " + productName);
                        return false;
                    }

                    productId = rs.getInt("id");
                    dbName = rs.getString("name");

                    System.out.println("DEBUG Знайдено ID   = " + productId);
                    System.out.println("DEBUG Знайдено Name = " + dbName);
                }
            }

            // Оновлення
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {

                if (isBlobField) {

                    if (value == null ||
                            value.toString().trim().isEmpty() ||
                            value.toString().equals("❌")) {

                        stmt.setNull(1, Types.BLOB);
                        System.out.println("DEBUG manufacturer = NULL");

                    } else {

                        byte[] bytes = value.toString()
                                .trim()
                                .getBytes(StandardCharsets.UTF_8);

                        stmt.setBytes(1, bytes);
                        System.out.println("DEBUG manufacturer bytes saved");
                    }

                } else {

                    if (field.equals("price")) {

                        if (value instanceof Number) {
                            stmt.setDouble(1, ((Number) value).doubleValue());
                        } else {
                            stmt.setDouble(1,
                                    Double.parseDouble(
                                            value.toString()
                                                    .replace(",", ".")
                                                    .trim()
                                    )
                            );
                        }

                    } else {

                        stmt.setString(1,
                                value == null ? "" : value.toString().trim());
                    }
                }

                stmt.setString(2, productName);

                int rows = stmt.executeUpdate();

                System.out.println("DEBUG Rows updated = " + rows);

                if (rows > 0) {
                    System.out.println("✅ Поле '" + field +
                            "' успішно оновлено для товару ID=" + productId);
                    return true;
                } else {
                    System.out.println("⚠️ Оновлення не виконано.");
                    return false;
                }
            }

        } catch (Exception e) {

            System.out.println("❌ updateField ERROR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Перевірити існування категорії
    public static boolean categoryExists(String categoryName) {
        String sql = "SELECT id FROM categories WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ categoryExists error: " + e.getMessage());
            return false;
        }
    }

    // --- Перевірити існування підкатегорії
    public static boolean subcategoryExists(String subcategoryName) {
        String sql = "SELECT id FROM subcategories WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, subcategoryName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ subcategoryExists error: " + e.getMessage());
            return false;
        }
    }

    // --- Отримати ціну продукту з YAML
    public static double getProductPriceFromYAML(String productName) {
        try (InputStream inputStream = CatalogEditor.class.getClassLoader().getResourceAsStream("catalog.yml")) {
            if (inputStream == null) return 0.0;

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);
            Object productsObj = data.get("products");
            if (!(productsObj instanceof List<?> products)) return 0.0;

            for (Object obj : products) {
                if (obj instanceof Map<?, ?> product) {
                    Object nameObj = product.get("name");
                    if (nameObj == null) continue;

                    String name = nameObj.toString().trim();
                    if (name.equalsIgnoreCase(productName.trim())) {
                        Object priceObj = product.get("price");
                        if (priceObj != null) {
                            return Double.parseDouble(priceObj.toString().replace(",", ".").trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ getProductPriceFromYAML error: " + e.getMessage());
        }
        return 0.0;
    }

    public static List<Map<String, Object>> getAllProducts() {
        List<Map<String, Object>> out = new ArrayList<>();

        String sql = """
            SELECT p.id, p.name, p.price, p.unit, p.description, p.photo,
                   c.name AS category, s.name AS subcategory
            FROM products p
            LEFT JOIN subcategories s ON p.subcategory_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", rs.getInt("id"));
                p.put("name", rs.getString("name"));
                p.put("price", rs.getObject("price"));
                p.put("unit", rs.getString("unit"));
                p.put("description", rs.getString("description"));
                p.put("photo", rs.getString("photo"));
                p.put("category", rs.getString("category"));
                p.put("subcategory", rs.getString("subcategory"));
                out.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    public static boolean updateFieldById(
            int productId,
            String field,
            Object value
    ) {

        try (Connection conn =
                     DatabaseManager.getConnection()) {

            String sql =
                    "UPDATE products SET "
                            + field
                            + "=? WHERE id=?";

            try (PreparedStatement stmt =
                         conn.prepareStatement(sql)) {

                stmt.setString(1,
                        value == null ? "" : value.toString());

                stmt.setInt(2, productId);

                int rows = stmt.executeUpdate();

                return rows > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
