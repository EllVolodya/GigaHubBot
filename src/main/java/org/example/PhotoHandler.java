package org.example;

import java.util.Map;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class PhotoHandler {

    private final Map<Long, String> userStates;          // userId -> state
    private final Map<Long, String> adminEditingProduct; // userId -> productName

    public PhotoHandler(Map<Long, String> userStates, Map<Long, String> adminEditingProduct) {
        this.userStates = userStates;
        this.adminEditingProduct = adminEditingProduct;
    }

    // Основний метод для обробки повідомлень
    public void handleUpdate(Long userId, String chatId, Update update) {
        String state = userStates.getOrDefault(userId, "editing");

        if (update.hasMessage()) {
            Message msg = update.getMessage();

            System.out.println("[DEBUG] Message class: " + msg.getClass().getSimpleName());
            System.out.println("[DEBUG] Message content type:");
            System.out.println("  hasText=" + msg.hasText());
            System.out.println("  hasPhoto=" + msg.hasPhoto());
            System.out.println("  hasDocument=" + msg.hasDocument());
            System.out.println("  hasAnimation=" + msg.hasAnimation());
            System.out.println("  hasSticker=" + msg.hasSticker());
            System.out.println("  hasVideo=" + msg.hasVideo());
            System.out.println("  hasVideoNote=" + msg.hasVideoNote());
            System.out.println("  hasVoice=" + msg.hasVoice());

            if (msg.hasText() && isInvalidLink(msg.getText())) {
                sendText(chatId, "❌ Локальні або blob-посилання не підтримуються. Надішліть URL зображення з інтернету.");
                return;
            }
        }

        System.out.println("[DEBUG] Поточний стан користувача: " + state);

        if ("awaiting_photo".equals(state)) {
            System.out.println("[DEBUG] Стан користувача 'awaiting_photo' — викликаємо handleAwaitingPhoto");
            handleAwaitingPhoto(userId, chatId, update);
        } else {
            System.out.println("[DEBUG] handleAwaitingPhoto не викликано. Поточний стан: " + state);
        }
    }

    // Переведення користувача в стан очікування фото
    public void requestPhotoUpload(Long userId, String chatId, String productName) {
        adminEditingProduct.put(userId, productName);
        userStates.put(userId, "awaiting_photo");
        sendText(chatId, "📎 Надішліть посилання на фото для товару '" + productName + "'.");
    }

    // Обробка очікуваного фото
    private void handleAwaitingPhoto(Long userId, String chatId, Update update) {
        System.out.println("[DEBUG] Входження в handleAwaitingPhoto для користувача " + userId);

        String productName = adminEditingProduct.get(userId);
        if (productName == null || productName.isEmpty()) {
            sendText(chatId, "⚠️ Не знайдено товар для збереження фото.");
            userStates.remove(userId);
            return;
        }

        if (!update.hasMessage() || update.getMessage().getText() == null) {
            sendText(chatId, "❌ Будь ласка, надішліть посилання на фото у вигляді тексту.");
            return;
        }

        String imageUrl = update.getMessage().getText().trim();

        if (isInvalidLink(imageUrl)) {
            sendText(chatId, "❌ Локальні або blob-посилання не підтримуються. Надішліть URL зображення з інтернету.");
            return;
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            sendText(chatId, "❌ Це не виглядає як посилання на фото. Надішліть правильне URL.");
            return;
        }

        boolean updated = CatalogEditor.updateField(productName, "photo", imageUrl);
        if (updated) {
            sendText(chatId, "✅ Фото оновлено у хмарі для товару '" + productName + "'.");
        } else {
            sendText(chatId, "⚠️ Не вдалося оновити базу даних.");
        }

        userStates.put(userId, "editing");
        adminEditingProduct.remove(userId);
    }

    // Перевірка посилань
    private boolean isInvalidLink(String link) {
        if (link == null) return true;
        link = link.trim();
        return link.startsWith("blob:") || link.startsWith("file://") || link.matches("^[a-zA-Z]:\\\\.*");
    }

    // Відправка повідомлення
    private void sendText(String chatId, String text) {
        System.out.println("[SEND TO " + chatId + "]: " + text);
        // тут код для фактичної відправки через Telegram API
    }
}
