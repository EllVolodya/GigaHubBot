package org.example;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.util.Map;

public class PhotoHandler {

    private final StoreBot bot;
    private final Map<Long, String> userStates;
    private final Map<Long, String> adminEditingProduct;

    public PhotoHandler(StoreBot bot, Map<Long, String> userStates, Map<Long, String> adminEditingProduct) {
        this.bot = bot;
        this.userStates = userStates;
        this.adminEditingProduct = adminEditingProduct;
    }

    // === ENTRY POINT ===
    public void handleUpdate(Long userId, String chatId, Update update) {
        String state = userStates.get(userId);
        System.out.println("[DEBUG] handleUpdate() called for userId=" + userId + ", state=" + state);

        if ("awaiting_photo".equals(state)) {
            System.out.println("[DEBUG] User is in 'awaiting_photo' state, delegating to handleAwaitingPhoto...");
            handleAwaitingPhoto(userId, chatId, update);
        } else {
            System.out.println("[DEBUG] User not in 'awaiting_photo', waiting for other actions.");
        }
    }

    // === START PHOTO UPLOAD ===
    public void requestPhotoUpload(Long userId, String chatId, String productName) {
        System.out.println("[DEBUG] requestPhotoUpload called: userId=" + userId + ", productName=" + productName);

        userStates.put(userId, "awaiting_photo");
        adminEditingProduct.put(userId, productName);

        bot.sendText(chatId, "📷 Please send a photo, document, or an image URL for the product '" + productName + "'.");
    }

    // === HANDLE AWAITING PHOTO ===
    public void handleAwaitingPhoto(Long userId, String chatId, Update update) {
        System.out.println("[DEBUG] handleAwaitingPhoto triggered for userId=" + userId);

        String productName = adminEditingProduct.get(userId);
        if (productName == null || productName.isEmpty()) {
            bot.sendText(chatId, "⚠️ No product selected.");
            userStates.remove(userId);
            return;
        }

        if (!update.hasMessage()) {
            bot.sendText(chatId, "❌ Please send a valid photo, document, or image URL.");
            return;
        }

        Message msg = update.getMessage();

        try {
            String imageUrl = null;

            // === PHOTO ===
            if (msg.hasPhoto()) {
                var photos = msg.getPhoto();
                var largestPhoto = photos.get(photos.size() - 1);
                File file = bot.downloadTelegramFile(largestPhoto.getFileId());
                imageUrl = CloudinaryManager.uploadImage(file, "products");
                System.out.println("[DEBUG] Photo uploaded for userId=" + userId);

            }
            // === DOCUMENT ===
            else if (msg.hasDocument()) {
                File file = bot.downloadTelegramFile(msg.getDocument().getFileId());
                imageUrl = CloudinaryManager.uploadImage(file, "products");
                System.out.println("[DEBUG] Document uploaded for userId=" + userId);

            }
            // === TEXT URL ===
            else if (msg.hasText()) {
                imageUrl = msg.getText().trim();

                if (imageUrl.isEmpty()) {
                    bot.sendText(chatId, "❌ Please send a non-empty image URL.");
                    return;
                }

                if (imageUrl.startsWith("blob:") || imageUrl.startsWith("file://") || imageUrl.matches("^[a-zA-Z]:\\\\.*")) {
                    bot.sendText(chatId, "❌ Local or blob URLs are not supported. Please send a proper internet URL.");
                    return;
                }

                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    bot.sendText(chatId, "❌ This does not look like a valid URL. Please send a proper image URL.");
                    return;
                }

                System.out.println("[DEBUG] Text URL received: " + imageUrl);
            }
            // === NOTHING VALID ===
            else {
                bot.sendText(chatId, "📎 Please send a photo, document, or an image URL.");
                return;
            }

            // === SAVE IMAGE ===
            if (imageUrl != null) {
                boolean updated = CatalogEditor.updateField(productName, "photo", imageUrl);
                if (updated) {
                    bot.sendText(chatId, "✅ Photo successfully updated for product '" + productName + "'.");
                } else {
                    bot.sendText(chatId, "⚠️ Received the image, but failed to update the database.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendText(chatId, "❌ Error processing the image: " + e.getMessage());
        } finally {
            userStates.remove(userId);
            adminEditingProduct.remove(userId);
        }
    }
}