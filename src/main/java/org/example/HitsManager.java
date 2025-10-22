package org.example;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HitsManager {

    // --- Cloudinary ---
    private static final Cloudinary cloudinary;
    static {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dru76f7hj",
                "api_key", "512831249592811",
                "api_secret", "FHs737cSf4akCFLuJQJcK70p9VU",
                "secure", true
        ));
    }

    // --- Завантаження медіа з Telegram у Cloudinary ---
    public static String uploadFromTelegram(TelegramLongPollingBot bot, Message message) {
        try {
            String fileId = null;

            if (message.hasPhoto()) {
                List<PhotoSize> photos = message.getPhoto();
                fileId = photos.get(photos.size() - 1).getFileId(); // найкраща якість
            } else if (message.hasVideo()) {
                fileId = message.getVideo().getFileId();
            }

            if (fileId == null) return null;

            // Отримуємо Telegram File
            File tgFile = bot.execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
            String filePath = tgFile.getFilePath();

            // Завантажуємо файл у тимчасову папку
            java.io.File tempFile = java.io.File.createTempFile("tgfile_", filePath.replaceAll("[^a-zA-Z0-9\\.]", "_"));
            try (InputStream is = new URL("https://api.telegram.org/file/bot" + bot.getBotToken() + "/" + filePath).openStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = is.read(buffer)) > 0) fos.write(buffer, 0, n);
            }

            // Завантажуємо на Cloudinary
            String cloudUrl = uploadToCloudinary(tempFile);

            // Видаляємо тимчасовий файл
            tempFile.delete();

            return cloudUrl;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[HitsManager] Failed to upload from Telegram: " + e.getMessage());
            return null;
        }
    }

    // --- Збереження медіа на Cloudinary ---
    public static String uploadToCloudinary(java.io.File file) {
        try {
            var result = cloudinary.uploader().upload(file, ObjectUtils.asMap("overwrite", true));
            return result.get("secure_url").toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[Cloudinary] Upload failed: " + e.getMessage());
            return null;
        }
    }

    // --- Збереження нового хіта у MySQL ---
    public static void saveHit(String title, String description, String mediaUrl) {
        String sql = "INSERT INTO hits (title, description, media_url) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title != null ? title : "немає");
            ps.setString(2, description != null ? description : "немає");
            ps.setString(3, mediaUrl != null ? mediaUrl : "немає");

            ps.executeUpdate();
            System.out.println("[HitsManager] Hit saved successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[HitsManager] Failed to save hit: " + e.getMessage());
        }
    }

    // --- Отримати всі хіти ---
    public static List<Hit> loadHits() {
        List<Hit> hits = new ArrayList<>();
        String sql = "SELECT * FROM hits ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Hit hit = new Hit();
                hit.id = rs.getInt("id");
                hit.title = rs.getString("title");
                hit.description = rs.getString("description");
                hit.media_url = rs.getString("media");
                hits.add(hit);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("[HitsManager] Failed to load hits: " + e.getMessage());
        }
        return hits;
    }

    // --- Клас для зручності ---
    public static class Hit {
        public int id;
        public String title;
        public String description;
        public String media_url;
    }
}
