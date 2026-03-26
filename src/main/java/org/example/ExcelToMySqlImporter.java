package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelToMySqlImporter {

    public static class ExcelProduct {
        public String name;
        public Double price;
        public String unit;
        public String manufacturer;
        public String group;
    }

    public static List<ExcelProduct> searchInExcel(String keyword) throws Exception {

        List<ExcelProduct> results = new ArrayList<>();
        String search = keyword.toLowerCase();

        InputStream is = ExcelToMySqlImporter.class
                .getClassLoader()
                .getResourceAsStream("products.xlsx");

        if (is == null) {
            throw new RuntimeException("❌ products.xlsx не знайдено в resources");
        }

        try (Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String group = getString(row, 0); // 🔥 A ГРУПА
                String name = getString(row, 2);  // C Назва

                if (name == null) continue;

                String nameLower = name.toLowerCase();
                String groupLower = group != null ? group.toLowerCase() : "";

                // 🔥 ПОШУК ПО NAME + GROUP
                if (nameLower.contains(search) || groupLower.contains(search)) {

                    ExcelProduct p = new ExcelProduct();

                    p.name = name;
                    p.group = group; // 🔥 ЗБЕРЕГЛИ
                    p.price = getDouble(row, 7);
                    p.unit = getString(row, 3);
                    p.manufacturer = getString(row, 4);

                    results.add(p);
                }
            }
        }

        return results;
    }

    public static long importSingleProduct(ExcelProduct p) throws SQLException {

        String checkSql = "SELECT id FROM products WHERE name = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setString(1, p.name);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                long id = rs.getLong("id");
                updateProduct(conn, id, p.price, p.unit, p.manufacturer);
                return id;
            }

            return insertProduct(conn, p.name, p.price, p.unit, p.manufacturer);
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
            if (rs.next()) return rs.getLong(1);
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
