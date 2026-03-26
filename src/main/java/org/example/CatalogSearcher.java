package org.example;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.sql.*;

import java.util.*;
import java.util.List;                       // для List
import java.util.Map;                        // для Map
import java.util.ArrayList;                  // для ArrayList

public class CatalogSearcher {

    private static final String CATALOG_PATH = "src/main/resources/catalog.yml";
    public CatalogSearcher() {
        System.out.println("✅ CatalogSearcher ready to query DB.");
    }


    public static List<Map<String, Object>> loadProducts() {
        List<Map<String, Object>> products = new ArrayList<>();

        try (InputStream input = new FileInputStream(CATALOG_PATH)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data != null && data.containsKey("products")) {
                products = (List<Map<String, Object>>) data.get("products");
                System.out.println("✅ Loaded " + products.size() + " products from catalog.yml");
            } else {
                System.out.println("⚠️ catalog.yml is empty or missing 'products' key");
            }

        } catch (Exception e) {
            System.err.println("❌ Error loading catalog.yml: " + e.getMessage());
        }

        return products;
    }

    public List<Map<String, Object>> searchMixedFromYAML(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Нормалізація рядків для порівняння
        String normKeyword = normalize(keyword);

        // 1️⃣ Завантаження catalog.yml через ClassLoader
        List<Map<String, Object>> yamlProducts = new ArrayList<>();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("catalog.yml")) {
            if (input == null) {
                System.err.println("❌ catalog.yml not found in resources!");
                return results;
            }

            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data != null && data.containsKey("products")) {
                yamlProducts = (List<Map<String, Object>>) data.get("products");
                System.out.println("✅ Loaded " + yamlProducts.size() + " products from catalog.yml");
            } else {
                System.out.println("⚠️ catalog.yml is empty or missing 'products' key");
            }

        } catch (Exception e) {
            System.err.println("❌ Error loading catalog.yml: " + e.getMessage());
            e.printStackTrace();
            return results;
        }

        // 2️⃣ Фільтруємо товари по ключовому слову
        for (Map<String, Object> p : yamlProducts) {
            String name = p.get("name") != null ? p.get("name").toString() : "";
            String price = p.get("price") != null ? p.get("price").toString() : "?";

            if (normalize(name).contains(normKeyword)) {
                // 3️⃣ Отримуємо категорію та підкатегорію (якщо є в БД)
                String category = "❓";
                String subcategory = "❓";

                List<Map<String, Object>> dbMatches = findProductsByName(name);
                if (!dbMatches.isEmpty()) {
                    Map<String, Object> match = dbMatches.get(0);
                    category = match.get("category") != null ? match.get("category").toString() : "❓";
                    subcategory = match.get("subcategory") != null ? match.get("subcategory").toString() : "❓";
                }

                // 4️⃣ Формуємо текст для Telegram
                String formattedText = String.format(
                        "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s",
                        name, price, category, subcategory
                );

                // 5️⃣ Створюємо Map для результатів
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("text", formattedText);
                productMap.put("name", name);
                productMap.put("price", price);
                productMap.put("category", category);
                productMap.put("subcategory", subcategory);

                results.add(productMap);
            }
        }

        System.out.println("[searchMixedFromYAML] Keyword: '" + keyword + "' -> Found: " + results.size() + " products");

        return results;
    }

    public List<Map<String, Object>> searchByKeywordsAdmin(String keywords) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (keywords == null || keywords.isEmpty()) return results;

        String sql = """
            SELECT p.*, s.name AS subcategory, c.name AS category
            FROM products p
            JOIN subcategories s ON p.subcategory_id = s.id
            JOIN categories c ON s.category_id = c.id
            WHERE LOWER(p.name) LIKE ?
            """;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection(); // ⚡ бере вже активне з’єднання
            ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + keywords.toLowerCase() + "%");

            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getInt("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getString("price"));
                product.put("subcategory", rs.getString("subcategory"));
                product.put("category", rs.getString("category"));
                results.add(product);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Error executing product search query: " + e.getMessage());
        } finally {
            // Закриваємо тільки statement і resultset — з’єднання залишається активним
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    // ---------------- Пошук по назві ----------------
    public List<Map<String, Object>> findProductsByName(String name) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) return results;

        String sql = """
            SELECT p.*, s.name AS subcategory, c.name AS category
            FROM products p
            JOIN subcategories s ON p.subcategory_id = s.id
            JOIN categories c ON s.category_id = c.id
            WHERE LOWER(p.name) LIKE ?
            ORDER BY p.name;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name.toLowerCase() + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) results.add(mapProduct(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    // ---------------- Категорії ----------------
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT name FROM categories ORDER BY id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) categories.add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
    }

    // ---------------- Підкатегорії ----------------
    public List<String> getSubcategories(String categoryName) {
        List<String> subcategories = new ArrayList<>();
        String sql = """
            SELECT s.name
            FROM subcategories s
            JOIN categories c ON s.category_id = c.id
            WHERE c.name = ?
            ORDER BY s.name;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) subcategories.add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return subcategories;
    }

    // ---------------- Продукти ----------------
    public List<Map<String, Object>> getProducts(String categoryName, String subcategoryName) {
        List<Map<String, Object>> products = new ArrayList<>();

        String sql = """
        SELECT p.id, p.name, p.price, p.unit, p.quantity, p.description, p.photo, p.manufacturer,
            s.name AS subcategory, c.name AS category
        FROM products p
        JOIN subcategories s ON p.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        WHERE c.name = ? AND s.name = ?
        ORDER BY p.id;
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);
            stmt.setString(2, subcategoryName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("id", rs.getInt("id"));
                    product.put("name", rs.getString("name"));
                    product.put("price", rs.getObject("price"));
                    product.put("unit", rs.getString("unit"));
                    product.put("quantity", rs.getString("quantity"));
                    product.put("description", rs.getString("description"));
                    product.put("photo", rs.getString("photo"));
                    product.put("subcategory", rs.getString("subcategory"));
                    product.put("category", rs.getString("category"));

                    // 🏭 Обробка manufacturer
                    Object manufacturerObj = rs.getObject("manufacturer");
                    String manufacturer = "";
                    if (manufacturerObj instanceof byte[] bytes) {
                        manufacturer = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    } else if (manufacturerObj != null) {
                        manufacturer = manufacturerObj.toString();
                    }
                    product.put("manufacturer", manufacturer);

                    products.add(product);

                    System.out.println("DEBUG: Loaded product '" + rs.getString("name") + "', manufacturer='" + manufacturer + "'");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ getProducts SQL error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DEBUG: getProducts() -> found " + products.size() + " products for category=" + categoryName + ", subcategory=" + subcategoryName);
        return products;
    }

    public List<Map<String, Object>> getProductsByCategory(String category) {

        List<Map<String, Object>> products = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM products WHERE category = ? ORDER BY id")) {

            stmt.setString(1, category);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                Map<String, Object> product = new HashMap<>();

                product.put("id", rs.getInt("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getString("price"));
                product.put("quantity", rs.getString("quantity"));

                products.add(product);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }

    public List<String> searchMixed(String keyword) {
        List<String> results = new ArrayList<>();

        try {
            // 1️⃣ Завантажуємо продукти з catalog.yml
            String catalogPath = "src/main/resources/catalog.yml";
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> data;

            try (InputStream input = new FileInputStream(catalogPath)) {
                data = yaml.load(input);
            }

            if (data == null || !data.containsKey("products")) {
                System.out.println("⚠️ catalog.yml is empty or missing 'products' key");
                return results;
            }

            List<Map<String, Object>> yamlProducts = (List<Map<String, Object>>) data.get("products");

            // 2️⃣ Фільтруємо по ключовому слову
            yamlProducts.stream()
                    .filter(p -> p.get("name").toString().toLowerCase().contains(keyword.toLowerCase()))
                    .forEach(p -> {
                        String name = p.get("name").toString();
                        String price = p.get("price").toString();

                        // 3️⃣ Отримуємо з MySQL підкатегорію/категорію
                        String category = "❓";
                        String subcategory = "❓";

                        List<Map<String, Object>> dbMatches = findProductsByName(name);
                        if (!dbMatches.isEmpty()) {
                            Map<String, Object> match = dbMatches.get(0);
                            category = match.get("category") != null ? match.get("category").toString() : "❓";
                            subcategory = match.get("subcategory") != null ? match.get("subcategory").toString() : "❓";
                        }

                        // 4️⃣ Формуємо текст результату
                        String formatted = String.format("""
                        📦 %s
                        💰 Ціна: %s грн за шт
                        📂 %s → %s
                        """, name, price, category, subcategory);

                        results.add(formatted);
                    });

        } catch (Exception e) {
            System.err.println("❌ Error in searchMixed: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\p{L}\\p{Nd}]", "").toLowerCase();
    }

    // ---------------- Допоміжний метод ----------------
    private Map<String, Object> mapProduct(ResultSet rs) throws SQLException {
        Map<String, Object> product = new HashMap<>();
        product.put("id", rs.getInt("id"));
        product.put("name", rs.getString("name"));
        product.put("price", rs.getDouble("price"));
        product.put("unit", rs.getString("unit"));

        // 🔥 ТУТ ТЕЖ
        product.put("quantity", rs.getString("quantity"));

        product.put("description", rs.getString("description"));
        product.put("photo", rs.getString("photo"));
        product.put("subcategory", rs.getString("subcategory"));
        product.put("category", rs.getString("category"));
        return product;
    }

    public List<String> getFilters(String categoryName, String subcategoryName) {

        List<String> filters = new ArrayList<>();

        String sql = """
        SELECT f.name
        FROM filters f
        JOIN subcategories s ON f.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        WHERE c.name = ? AND s.name = ?
        ORDER BY f.id
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoryName);
            ps.setString(2, subcategoryName);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                filters.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return filters;
    }

    public List<String> getFiltersAdmin(){

        List<String> filters = new ArrayList<>();

        String sql = "SELECT name FROM filters ORDER BY id";

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){

            while(rs.next()){
                filters.add(rs.getString("name"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return filters;
    }

    public void insertFilter(String filterName, String subcategoryName){

        String sql = """
        INSERT INTO filters (name, subcategory_id)
        SELECT ?, s.id
        FROM subcategories s
        WHERE s.name = ?
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, filterName);
            ps.setString(2, subcategoryName);

            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public List<String> getAllSubcategories() {

        List<String> subcategories = new ArrayList<>();

        String sql = "SELECT name FROM subcategories ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                subcategories.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return subcategories;
    }

    public void addFilterToSubcategory(String subcategoryName,String filterName){

        String sql = """
    INSERT INTO subcategory_filters(subcategory_id,filter_id)
    SELECT s.id,f.id
    FROM subcategories s,filters f
    WHERE s.name=? AND f.name=?
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1,subcategoryName);
            ps.setString(2,filterName);

            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Показ фільтрів підкатегорії
    public List<String> getFiltersBySubcategory(String subcategoryName) {

        List<String> filters = new ArrayList<>();

        String sql = """
        SELECT f.name
        FROM filters f
        JOIN subcategories s ON f.subcategory_id = s.id
        WHERE s.name = ?
        ORDER BY f.id
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, subcategoryName);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                filters.add(rs.getString("name"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return filters;
    }

    public String getProductSubcategory(String productName){

        String sql = """
        SELECT s.name
        FROM products p
        JOIN subcategories s ON p.subcategory_id = s.id
        WHERE p.name = ?
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, productName);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getString("name");
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasSubfilters(String filterName){

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement ps = conn.prepareStatement("""
            SELECT COUNT(*)
            FROM subfilters sf
            JOIN filters f ON sf.filter_id = f.id
            WHERE f.name=?
        """);

            ps.setString(1, filterName);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt(1) > 0;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public List<String> getSubfilters(String filterName){

        List<String> subfilters = new ArrayList<>();

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement ps = conn.prepareStatement("""
            SELECT sf.name
            FROM subfilters sf
            JOIN filters f ON sf.filter_id = f.id
            WHERE f.name=?
            ORDER BY sf.name
        """);

            ps.setString(1, filterName);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                subfilters.add(rs.getString("name"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return subfilters;
    }

    public List<Map<String,Object>> getProductsBySubfilter(String subfilterName){

        List<Map<String,Object>> products = new ArrayList<>();

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement ps = conn.prepareStatement("""
            SELECT p.*
            FROM products p
            JOIN subfilters sf ON p.subfilter_id = sf.id
            WHERE sf.name = ?
        """);

            ps.setString(1, subfilterName);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> product = new HashMap<>();

                product.put("id", rs.getInt("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getDouble("price"));
                product.put("description", rs.getString("description"));
                product.put("photo", rs.getString("photo"));
                product.put("manufacturer", rs.getString("manufacturer"));
                product.put("quantity", rs.getInt("quantity"));

                products.add(product);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return products;
    }

    public List<Map<String,Object>> getProductsByFilter(String filterName){

        List<Map<String,Object>> products = new ArrayList<>();

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement ps = conn.prepareStatement("""
            SELECT p.*
            FROM products p
            JOIN filters f ON p.filter_id = f.id
            WHERE f.name = ?
            AND p.subfilter_id IS NULL
        """);

            ps.setString(1, filterName);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                Map<String,Object> product = new HashMap<>();

                product.put("id", rs.getInt("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getDouble("price"));
                product.put("description", rs.getString("description"));
                product.put("photo", rs.getString("photo"));
                product.put("manufacturer", rs.getString("manufacturer"));
                product.put("quantity", rs.getInt("quantity"));
                product.put("unit", rs.getString("unit"));

                products.add(product);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return products;
    }

    public boolean hasProductsInFilter(String filterName) {

        String sql = """
        SELECT COUNT(*)
        FROM products p
        JOIN filters f ON p.filter_id = f.id
        WHERE f.name = ?
        AND p.subfilter_id IS NULL
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, filterName);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt(1) > 0;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public void addSubfilterToProduct(Map<String, Object> product, String filterName, String subfilterName) {

        if (product == null) return;

        int productId = (int) product.get("id");

        String sql = """
        UPDATE products
        SET subfilter_id = (
            SELECT sf.id
            FROM subfilters sf
            JOIN filters f ON sf.filter_id = f.id
            WHERE sf.name = ? AND f.name = ?
            LIMIT 1
        )
        WHERE id = ?
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subfilterName);
            ps.setString(2, filterName);
            ps.setInt(3, productId);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                System.out.println("✅ Subfilter added to product ID: " + productId);
            } else {
                System.out.println("⚠️ Product not updated ID: " + productId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean addProductToFilter(Map<String, Object> product, String filterName, String subfilterName) {

        if (product == null) return false;

        try (Connection conn = DatabaseManager.getConnection()) {

            int productId = ((Number) product.get("id")).intValue();

            // 🔹 знайти filter_id
            PreparedStatement psFilter = conn.prepareStatement(
                    "SELECT id FROM filters WHERE name = ? LIMIT 1"
            );
            psFilter.setString(1, filterName);

            ResultSet rsFilter = psFilter.executeQuery();

            if (!rsFilter.next()) {
                System.out.println("❌ Filter not found");
                return false;
            }

            int filterId = rsFilter.getInt("id");

            Integer subfilterId = null;

            // 🔹 якщо є підфільтр
            if (subfilterName != null) {

                PreparedStatement psSub = conn.prepareStatement("""
                SELECT sf.id
                FROM subfilters sf
                JOIN filters f ON sf.filter_id = f.id
                WHERE sf.name = ? AND f.name = ?
                LIMIT 1
            """);

                psSub.setString(1, subfilterName);
                psSub.setString(2, filterName);

                ResultSet rsSub = psSub.executeQuery();

                if (!rsSub.next()) {
                    System.out.println("❌ Subfilter not found");
                    return false;
                }

                subfilterId = rsSub.getInt("id");
            }

            // 🔥 UPDATE
            PreparedStatement psUpdate = conn.prepareStatement("""
            UPDATE products
            SET filter_id = ?, subfilter_id = ?
            WHERE id = ?
        """);

            psUpdate.setInt(1, filterId);

            if (subfilterId != null) {
                psUpdate.setInt(2, subfilterId);
            } else {
                psUpdate.setNull(2, Types.INTEGER);
            }

            psUpdate.setInt(3, productId);

            int updated = psUpdate.executeUpdate();

            System.out.println("UPDATED ROWS: " + updated);

            return updated > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> findProducts(
            String category,
            String subcategory,
            String filter,
            String subfilter
    ) {

        List<Map<String, Object>> products = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT p.*
        FROM products p
        JOIN subcategories s ON p.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        LEFT JOIN filters f ON p.filter_id = f.id
        LEFT JOIN subfilters sf ON p.subfilter_id = sf.id
        WHERE 1=1
    """);

        List<Object> params = new ArrayList<>();

        if (category != null) {
            sql.append(" AND c.name = ?");
            params.add(category);
        }

        if (subcategory != null) {
            sql.append(" AND s.name = ?");
            params.add(subcategory);
        }

        if (filter != null) {
            sql.append(" AND f.name = ?");
            params.add(filter);
        }

        if (subfilter != null) {
            sql.append(" AND sf.name = ?");
            params.add(subfilter);
        }

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(sql.toString());

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i).toString());
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Map<String, Object> product = new HashMap<>();

                product.put("id", rs.getLong("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getString("price"));
                product.put("unit", rs.getString("unit"));

                byte[] descBytes = rs.getBytes("description");
                if (descBytes != null) {
                    product.put("description", new String(descBytes, java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    product.put("description", "");
                }

                // 🔥 фото
                product.put("photo", rs.getString("photo"));

                products.add(product);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }
}