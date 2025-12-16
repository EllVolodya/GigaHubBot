package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelToMySqlImporter {

    /**
     * Excel структура:
     * A - Група товарів
     * B - Код
     * C - Название
     * D - Ед.
     * E - Производитель
     * F - Код поставщика
     * G - Штрих код
     * H - Розничная цена
     * I - Количество
     */

    public static List<Long> importProducts() throws Exception {

        List<Long> importedIds = new ArrayList<>();

        InputStream is = ExcelToMySqlImporter.class
                .getClassLoader()
                .getResourceAsStream("products.xlsx");

        if (is == null) {
            throw new RuntimeException("❌ products.xlsx не знайдено в resources");
        }

        try (Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 🔥 Дані починаються з 3-го рядка (index = 2)
            for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String name = getString(row, 2);          // C Название
                if (name == null || name.isBlank()) continue;

                Double price = getDouble(row, 7);         // H Розничная цена
                String unit = getString(row, 3);          // D Ед.
                String manufacturer = getString(row, 4); // E Производитель

                long productId = insertOrUpdateProduct(
                        name,
                        price,
                        unit,
                        manufacturer
                );

                importedIds.add(productId);
            }
        }

        return importedIds;
    }

    // ======================= DATABASE =======================

    private static long insertOrUpdateProduct(
            String name,
            Double price,
            String unit,
            String manufacturer
    ) throws SQLException {

        String checkSql = "SELECT id FROM products WHERE name = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setString(1, name);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                long id = rs.getLong("id");
                updateProduct(conn, id, price, unit, manufacturer);
                return id;
            }

            return insertProduct(conn, name, price, unit, manufacturer);
        }
    }

    private static long insertProduct(
            Connection conn,
            String name,
            Double price,
            String unit,
            String manufacturer
    ) throws SQLException {

        String sql = """
            INSERT INTO products (name, price, unit, manufacturer)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setObject(2, price);
            ps.setString(3, unit);
            ps.setString(4, manufacturer);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        throw new SQLException("❌ Не вдалося вставити товар: " + name);
    }

    private static void updateProduct(
            Connection conn,
            long id,
            Double price,
            String unit,
            String manufacturer
    ) throws SQLException {

        String sql = """
            UPDATE products
            SET price = ?, unit = ?, manufacturer = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, price);
            ps.setString(2, unit);
            ps.setString(3, manufacturer);
            ps.setLong(4, id);

            ps.executeUpdate();
        }
    }

    // ======================= EXCEL HELPERS =======================

    private static String getString(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> null;
        };
    }

    private static Double getDouble(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {

                case NUMERIC -> cell.getNumericCellValue();

                case STRING -> {
                    String text = cell.getStringCellValue()
                            .replace(",", ".")
                            .replaceAll("[^0-9.]", "")
                            .trim();

                    if (text.isEmpty()) yield null;
                    yield Double.parseDouble(text);
                }

                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}
