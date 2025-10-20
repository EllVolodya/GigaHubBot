package org.example;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

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

    // === MAIN ENTRY POINT ===
    public void handleUpdate(Long userId, String chatId, Update update) {
        String state = userStates.get(userId);
        System.out.println("[DEBUG] handleUpdate() called for userId=" + userId + ", state=" + state);

        if ("awaiting_photo".equals(state)) {
            System.out.println("[DEBUG] User is in 'awaiting_photo' state. Calling handleAwaitingPhoto...");
            handleAwaitingPhoto(userId, chatId, update);
        } else {
            System.out.println("[DEBUG] handleAwaitingPhoto not called. Current state: " + state);
        }
    }

    // === START PHOTO UPLOAD ===
    public void requestPhotoUpload(Long userId, String chatId, String productName) {
        System.out.println("[DEBUG] requestPhotoUpload called: userId=" + userId + ", productName=" + productName);

        userStates.put(userId, "awaiting_photo");
        adminEditingProduct.put(userId, productName);

        bot.sendText(chatId, "📷 Please send an image URL for the product '" + productName + "'.");
    }

    // === HANDLE AWAITING PHOTO STATE ===
    public void handleAwaitingPhoto(Long userId, String chatId, Update update) {
        System.out.println("[DEBUG] handleAwaitingPhoto triggered for userId=" + userId);

        String productName = adminEditingProduct.get(userId);
        if (productName == null || productName.isEmpty()) {
            bot.sendText(chatId, "⚠️ No product found to attach the photo.");
            userStates.remove(userId);
            return;
        }

        if (!update.hasMessage()) {
            bot.sendText(chatId, "❌ Please send a valid image URL as text.");
            return;
        }

        Message msg = update.getMessage();
        if (!msg.hasText()) {
            bot.sendText(chatId, "❌ Please send an image link as text.");
            return;
        }

        String imageUrl = msg.getText().trim();

        // === Validate link type ===
        if (imageUrl.startsWith("blob:") || imageUrl.startsWith("file://") || imageUrl.matches("^[a-zA-Z]:\\\\.*")) {
            bot.sendText(chatId, "❌ Local or blob URLs are not supported. Please send an internet image link (http/https).");
            return;
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            bot.sendText(chatId, "❌ This doesn’t look like a valid link. Please send a proper image URL.");
            return;
        }

        // === Save link ===
        boolean updated = CatalogEditor.updateField(productName, "photo", imageUrl);
        if (updated) {
            bot.sendText(chatId, "✅ Photo successfully updated for product '" + productName + "'.");
        } else {
            bot.sendText(chatId, "⚠️ Received the link but failed to update the database.");
        }

        // === Cleanup ===
        userStates.remove(userId);
        adminEditingProduct.remove(userId);
    }
}