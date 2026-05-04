package org.example;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.util.Map;

public class PhotoHandler {

    private final StoreBot bot;
    private final Map<Long, String> userStates;
    private final Map<Long, String> adminEditingProduct;
    private final Map<Long, Map<String, Object>> adminSelectedProduct;

    public PhotoHandler(
            StoreBot bot,
            Map<Long, String> userStates,
            Map<Long, String> adminEditingProduct,
            Map<Long, Map<String, Object>> adminSelectedProduct
    ) {
        this.bot = bot;
        this.userStates = userStates;
        this.adminEditingProduct = adminEditingProduct;
        this.adminSelectedProduct = adminSelectedProduct;
    }

    // === ENTRY POINT ===
    public void handleUpdate(Long userId, String chatId, Update update) {

        String state = userStates.get(userId);

        System.out.println("[DEBUG] handleUpdate userId=" + userId + " state=" + state);

        if ("awaiting_photo".equals(state)) {
            handleAwaitingPhoto(userId, chatId, update);
        }
    }

    // === START PHOTO UPLOAD ===
    public void requestPhotoUpload(Long userId, String chatId, String productName) {

        System.out.println("[DEBUG] requestPhotoUpload userId=" + userId + " product=" + productName);

        userStates.put(userId, "awaiting_photo");
        adminEditingProduct.put(userId, productName);

        bot.sendText(chatId,
                "📷 Надішліть фото, документ або URL зображення для товару:\n" +
                        "\"" + productName + "\"");
    }

    // === HANDLE PHOTO ===
    public void handleAwaitingPhoto(Long userId, String chatId, Update update) {

        System.out.println("[DEBUG] handleAwaitingPhoto userId=" + userId);

        Map<String, Object> product = adminSelectedProduct.get(userId);

        if (product == null) {
            bot.sendText(chatId, "❌ Товар не знайдено.");
            userStates.remove(userId);
            return;
        }

        int productId = (int) product.get("id");
        String productName = String.valueOf(product.get("name"));

        if (!update.hasMessage()) {
            bot.sendText(chatId, "❌ Надішліть фото або URL.");
            return;
        }

        Message msg = update.getMessage();

        try {

            String imageUrl = null;

            // === PHOTO ===
            if (msg.hasPhoto()) {

                var photos = msg.getPhoto();
                var largest = photos.get(photos.size() - 1);

                File file = bot.downloadTelegramFile(largest.getFileId());

                imageUrl = CloudinaryManager.uploadImage(file, "products");

                System.out.println("[DEBUG] PHOTO uploaded");
            }

            // === DOCUMENT ===
            else if (msg.hasDocument()) {

                File file = bot.downloadTelegramFile(msg.getDocument().getFileId());

                imageUrl = CloudinaryManager.uploadImage(file, "products");

                System.out.println("[DEBUG] DOCUMENT uploaded");
            }

            // === URL ===
            else if (msg.hasText()) {

                imageUrl = msg.getText().trim();

                if (imageUrl.isEmpty()) {
                    bot.sendText(chatId, "❌ Пустий URL.");
                    return;
                }

                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    bot.sendText(chatId, "❌ Невірний URL.");
                    return;
                }

                System.out.println("[DEBUG] URL = " + imageUrl);
            }

            else {
                bot.sendText(chatId, "📎 Надішліть фото, документ або URL.");
                return;
            }

            // === SAVE ===
            if (imageUrl != null) {

                System.out.println("DEBUG ID = " + productId);

                boolean updated =
                        CatalogEditor.updateFieldById(productId, "photo", imageUrl);

                if (updated) {
                    bot.sendText(chatId, "✅ Фото збережено для \"" + productName + "\"");
                } else {
                    bot.sendText(chatId, "⚠️ Не вдалося зберегти фото.");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
            bot.sendText(chatId, "❌ Помилка: " + e.getMessage());

        } finally {

            userStates.remove(userId);
            adminEditingProduct.remove(userId);
        }
    }
}