package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelToMySqlImporter {

    public static List<Long> importProducts() throws Exception {

        List<Long> importedIds = new ArrayList<>();

        // 🔥 правильне читання з resources
        try (InputStream is = ExcelToMySqlImporter.class
                .getClassLoader()
                .getResourceAsStream("products.xlsx");
             Workbook workbook = new XSSFWorkbook(is)) {

            if (is == null) {
                throw new RuntimeException("❌ products.xlsx not found in resources");
            }

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getString(row, 0);
                if (name == null || name.isBlank()) continue;

                Double price = getDouble(row, 1);
                String description = getString(row, 2);
                String unit = getString(row, 3);
                String manufacturer = getString(row, 4);

                long productId = insertOrUpdate(
                        name, price, description, unit, manufacturer
                );

                importedIds.add(productId);
            }
        }

        return importedIds;
    }

    // ================= DB =================

    private static long insertOrUpdate(String name, Double price,
                                       String description, String unit,
                                       String manufacturer) throws SQLException {

        String checkSql = "SELECT id FROM products WHERE name = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setString(1, name);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                long id = rs.getLong("id");
                update(conn, id, price, description, unit, manufacturer);
                return id;
            }

            return insert(conn, name, price, description, unit, manufacturer);
        }
    }

    private static long insert(Connection conn, String name, Double price,
                               String description, String unit,
                               String manufacturer) throws SQLException {

        String sql = """
            INSERT INTO products (name, price, description, unit, manufacturer)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setObject(2, price);
            ps.setString(3, description);
            ps.setString(4, unit);
            ps.setString(5, manufacturer);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        }

        throw new SQLException("Insert failed");
    }

    private static void update(Connection conn, long id, Double price,
                               String description, String unit,
                               String manufacturer) throws SQLException {

        String sql = """
            UPDATE products
            SET price=?, description=?, unit=?, manufacturer=?
            WHERE id=?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, price);
            ps.setString(2, description);
            ps.setString(3, unit);
            ps.setString(4, manufacturer);
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    // ================= HELPERS =================

    private static String getString(Row row, int i) {
        Cell c = row.getCell(i);
        return c == null ? null : c.toString().trim();
    }

    private static Double getDouble(Row row, int i) {
        Cell c = row.getCell(i);
        return c == null ? null : c.getNumericCellValue();
    }
}
