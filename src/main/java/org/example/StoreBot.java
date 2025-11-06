package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;

import java.io.InputStream;
import java.io.IOException;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDateTime;

public class StoreBot extends TelegramLongPollingBot {

    private final String botUsername = "GigaHubAssistant_bot";

    // 🔹 Користувацькі стани
    private final Map<Long, String> currentCategory = new HashMap<>();
    private final Map<Long, String> currentSubcategory = new HashMap<>();
    private final Map<Long, Integer> productIndex = new HashMap<>();
    protected Map<Long, Map<String, Object>> lastShownProduct = new HashMap<>();
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> userCart = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> userOrders = new HashMap<>();

    //Права
    private final List<Long> ADMINS = List.of(620298889L, 1030917576L, 533570832L);// тут айді продавців меню
    private final List<Long> DEVELOPERS = List.of(620298889L, 1030917576L, 533570832L, 404670376L, 1181804630L, 6141120338L); // тут айді розробників меню

    // 🔹 Адмінські стани
    private final Map<Long, Long> adminReplyTarget = new HashMap<>();

    private final Map<Long, String> adminEditingProduct = new HashMap<>();
    private final Map<Long, List<String>> adminSelectedProductsRange = new HashMap<>();
    private final Map<Long, String> adminEditingField = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> adminMatchList = new HashMap<>();
    private final Map<Long, String> adminNewCategory = new HashMap<>();
    private final Map<Long, List<String>> supportAnswers = new HashMap<>();
    private final Map<Long, Integer> adminOrderIndex = new HashMap<>();
    private final Map<Long, String> adminSearchSource = new HashMap<>();  // джерело пошуку для кожного користувача
    private final Map<Long, String> adminSearchKeyword = new HashMap<>(); // ключове слово для пошуку
    private final Map<String, Object> tempStorage = new HashMap<>();

    private final CatalogSearcher catalogSearcher = new CatalogSearcher();
    protected Map<Long, List<Map<String, Object>>> searchResults = new HashMap<>();

    private final Map<Long, List<String>> feedbacks = new HashMap<>();

    @SuppressWarnings("unused")
    private final Map<Long, String> previousState = new HashMap<>();
    @SuppressWarnings("unused")
    private final List<String> hitItems = new ArrayList<>();
    @SuppressWarnings("unused")
    private final Map<Long, Long> replyTargets = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(StoreBot.class.getName());

    //Розробників стани
    private final Map<Long, Boolean> developerMenuState = new HashMap<>();
    private final Map<String, String> developerState = new HashMap<>(); // Зберігаємо стан кожного користувача по chatId

    private final PhotoHandler photoHandler = new PhotoHandler(this, userStates, adminEditingProduct);
    private final Map<String, String> tempProductName = new HashMap<>(); // Тимчасово зберігаємо назву товару для ручного оновлення ціни

    private static final String BACK_BUTTON = "⬅️ Назад";
    private static final String ADD_TO_CART_BUTTON = "🛠 Додати в кошик";
    private static final String VIEW_CART_BUTTON = "🛍️ Перейти в кошик";

    public StoreBot(String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public java.io.File downloadTelegramFile(String fileId) throws TelegramApiException {
        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(new GetFile(fileId));
        return downloadFile(tgFile);
    }

    public Map<Long, String> getUserStates() {
        return userStates;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || update.getMessage() == null) return;

        Long userId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText() != null ? update.getMessage().getText().trim() : "";
        String state = userStates.get(userId);

        System.out.println("[DEBUG] Received message from userId=" + userId + ": '" + text + "' (state=" + state + ")");

        String normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cf}\\p{Zs}]+", " ")
                .trim();

        if ("choose_yaml_product".equals(state)) {
            handleChooseYamlProduct(userId, chatId, normalizedText);
            return;
        }

        // 🖼️ Button "Add Photo"
        if (normalizedText.contains("Додати фотографію") || normalizedText.contains("Add Photo")) {
            System.out.println("[DEBUG] Button 'Add Photo' detected for userId=" + userId);

            String productName = adminEditingProduct.get(userId);
            if (productName != null) {
                photoHandler.requestPhotoUpload(userId, chatId, productName); // set state & ask for URL
            } else {
                sendText(chatId, "⚠️ Please select a product first.");
            }
            return;
        }

        // 🧩 User in awaiting_photo state
        if ("awaiting_photo".equals(state)) {
            System.out.println("[DEBUG] User is in awaiting_photo state, delegating to PhotoHandler...");
            photoHandler.handleAwaitingPhoto(userId, chatId, update);
            return;
        }

        // 🔹 DEFAULT DELEGATION TO PHOTO HANDLER
        System.out.println("[DEBUG] Passing message to PhotoHandler for userId=" + userId);
        photoHandler.handleUpdate(userId, chatId, update); // ← вставлено сюди

        // ===== Optional: handle feedback / other states =====
        if (update.getMessage().hasText()) {
            text = update.getMessage().getText().trim();
        }

        if (state != null) {
            try {
                handleFeedbackState(userId, chatId, text, state);
                handleState(userId, chatId, text, state, update);
            } catch (TelegramApiException e) {
                LOGGER.severe("[Bot Error] Failed to handle state for user " + userId + ": " + e.getMessage());
                sendText(chatId, "❌ Сталася помилка при обробці вашого запиту.");
            }
            return;
        }

        if (text.isBlank()) return;

        try {
            // 🔹 Обробка станів користувача
            if (state != null) {
                switch (state) {
                    case "awaiting_pickup_data" -> {
                        List<Map<String, Object>> cart = userCart.get(userId);
                        if (cart == null || cart.isEmpty()) {
                            sendText(chatId, "🛒 Ваш кошик порожній.");
                            userStates.remove(userId);
                            return;
                        }

                        String orderCode = String.format("%04d", new Random().nextInt(10000));
                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("orderCode", orderCode);
                        orderData.put("pickupData", text);
                        orderData.put("items", new ArrayList<>(cart));
                        double total = cart.stream()
                                .mapToDouble(i -> Double.parseDouble(i.getOrDefault("price","0").toString()))
                                .sum();
                        orderData.put("total", total);
                        orderData.put("status", "Нове");
                        orderData.put("date", LocalDateTime.now().toString());
                        orderData.put("type", "pickup");

                        userOrders.computeIfAbsent(userId, k -> new ArrayList<>()).add(orderData);
                        OrderFileManager.addOrder(orderData);

                        // 🔹 Повідомлення адміну
                        for (Long adminId : ADMINS) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("🏬 *Самовивіз*\n");
                            sb.append("🆔 User ID: ").append(userId).append("\n");
                            sb.append("🔢 Код замовлення: ").append(orderCode).append("\n");
                            sb.append("📋 Дані:\n").append(text).append("\n\n");
                            for (Map<String,Object> item : cart) {
                                sb.append("• ").append(item.get("title")).append(" — ").append(item.get("price")).append(" грн\n");
                            }
                            sb.append("\n💰 Всього: ").append(total).append(" грн");
                            sendText(adminId.toString(), sb.toString());
                        }

                        userCart.remove(userId);
                        userStates.remove(userId);
                        sendText(chatId, "✅ Ваше замовлення на самовивіз успішно оформлено!\nКод замовлення: " + orderCode);
                    }

                    case "waiting_for_search" -> {
                        userStates.put(userId, "selecting_product");
                        ProductSearchManager searchHandler = new ProductSearchManager(this);
                        searchHandler.performSearch(userId, chatId, text);
                    }
                    case "selecting_product" -> {
                        ProductSearchManager searchHandler = new ProductSearchManager(this);
                        searchHandler.handleSearchNumber(userId, chatId, text);
                    }

                    case "WAITING_FOR_PRODUCT_NAME" -> {
                        tempProductName.put(chatId, text);
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("💰 Введіть нову ціну для товару \"" + text + "\":")
                                .build());
                        developerState.put(chatId, "WAITING_FOR_NEW_PRICE");
                    }

                    case "WAITING_FOR_NEW_PRICE" -> {
                        try {
                            String productName = tempProductName.get(chatId);
                            double newPrice = Double.parseDouble(text);
                            updateProductPriceInDB(productName, newPrice);
                            execute(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("✅ Ціна для товару \"" + productName + "\" оновлена до " + newPrice + " грн.")
                                    .build());
                        } catch (NumberFormatException e) {
                            execute(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("⚠️ Введіть правильне числове значення ціни.")
                                    .build());
                            return;
                        }
                        developerState.remove(chatId);
                        tempProductName.remove(chatId);
                    }

                    default -> {
                        sendText(chatId, "🔎 Введіть назву товару для пошуку:");
                        userStates.put(userId, "waiting_for_search");
                    }
                }
            }

            // 🔹 Основні команди (кнопки)
            if (text == null) return;

            switch (text) {
                case "/start" -> {
                    clearUserState(userId);
                    Long chatIdLong = update.getMessage().getChatId();
                    String chatIdStr = chatIdLong.toString();

                    // Перевірка коду інвайту
                    String messageText = update.getMessage().getText();
                    if (messageText != null && messageText.length() > 6) {
                        String inviteCode = messageText.substring(7).trim();
                        if (!inviteCode.isBlank()) {
                            boolean incremented = new InviteManager().incrementInviteNumber(inviteCode);
                            if (incremented) {
                                System.out.println("✅ Лічильник number для invite " + inviteCode + " збільшено.");
                            } else {
                                System.out.println("❌ Invite не знайдено: " + inviteCode);
                            }
                        }
                    }

                    // --- Реєстрація користувача
                    UserManager userManager = new UserManager();
                    userManager.registerUser(chatIdLong, update.getMessage().getFrom().getFirstName(), chatIdStr);

                    // --- Відправка меню користувача одразу після /start
                    try {
                        SendMessage menuMsg = createUserMenu(chatIdStr, userId);
                        execute(menuMsg);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        sendText(chatIdStr, "❌ Помилка надсилання меню користувача.");
                    }

                    System.out.println("Користувач натиснув /start: " + chatIdLong);
                }

                case "🧱 Каталог товарів" -> sendCategories(userId);
                case "📋 Кошик" -> {
                    try {
                        showCart(userId);
                    } catch (TelegramApiException e) {
                        LOGGER.severe("[Cart Error] Failed to show cart for userId=" + userId + ": " + e.getMessage());
                    }
                }

                case "🧹 Очистити кошик" -> clearCart(userId);
                case BACK_BUTTON -> {
                    try {
                        handleBack(chatId);
                    } catch (TelegramApiException e) {
                        LOGGER.severe("[Back Button Error] Failed to handle BACK_BUTTON for chatId=" + chatId + ": " + e.getMessage());
                        sendText(chatId, "❌ Сталася помилка при обробці кнопки Назад.");
                    }
                }
                case ADD_TO_CART_BUTTON -> {
                    addToCartTool(userId);
                    return;
                }
                case VIEW_CART_BUTTON -> {
                    showCart(userId);
                    return;
                }

                case "➡ Далі" -> showNextProduct(userId);
                case "🛒 Додати в кошик" -> addToCart(userId);
                case "📍 Адреси та Контакти" -> {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setParseMode("HTML");
                    message.setDisableWebPagePreview(true); // ⬅ вимикає прев’ю
                    message.setText("""
                                        🏘️ Казанка: <a href="https://maps.app.goo.gl/d7GQnKaXedkHDuq97">на мапі</a>
                                        📞 Телефон: <code>(050) 457 84 58</code>

                                        🏘️ Новий Буг: <a href="https://maps.app.goo.gl/YJ5qzxAqXVpZJXYPA">на мапі</a>
                                        📞 Телефон: <code>(050) 493 15 15</code>
                                    """);
                    execute(message);
                }

                case "🌐 Соц-мережі" -> {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setParseMode("HTML");
                    message.setDisableWebPagePreview(true); // ⬅ вимикає прев’ю
                    message.setText("""
                                        🌐 Ми у соціальних мережах:

                                        📘 Facebook: <a href="https://www.facebook.com/p/%D0%93%D0%B8%D0%B3%D0%B0%D1%85%D0%B0%D0%B1-61578183892871/">відкрити</a>
                                        📸 Instagram: <a href="https://www.instagram.com/_gigahub_?igsh=Y211bWRqazhhcmtu&utm_source=qr">відкрити</a>
                                        🎵 TikTok: <a href="tiktok.com/@gigahub2">відкрити</a>

                                        ☕ Також Instagram доступний у CoffeeMax: <a href="https://www.instagram.com/coffee_max_1?igsh=bmhsNDRyN2M5eG5l&utm_source=qr">відкрити</a>
                                    """);
                    execute(message);
                }
                case "💬 Допомога" -> sendMessage(createHelpMenu(chatId));
                case "✉️ Написати консультанту" -> {
                    userStates.put(userId, "ask_consultant");
                    sendText(chatId, "✏️ Напишіть своє питання консультанту:");
                }
                case "💌 Відповіді" -> {
                    List<String> answers = supportAnswers.get(userId);
                    String reply = (answers == null || answers.isEmpty())
                            ? "Поки що немає відповідей від консультантів."
                            : "💌 Відповіді консультантів:\n\n" + String.join("\n\n", answers);
                    sendText(chatId, reply);
                }

                case "🔍 Пошук товару" -> {
                    userStates.put(userId, "waiting_for_search");
                    sendText(chatId, "🔎 Введіть назву товару, який хочете знайти:");
                }

                case "🛒 Замовити товар" -> {
                    List<Map<String, Object>> cart = userCart.get(userId);
                    if (cart == null || cart.isEmpty()) {
                        sendText(chatId, "🛒 Ваш кошик порожній.");
                        return;
                    }

                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    markup.setResizeKeyboard(true);

                    KeyboardRow deliveryRow = new KeyboardRow();
                    deliveryRow.add(new KeyboardButton("🏬 Самовивіз"));
                    deliveryRow.add(new KeyboardButton("📦 Доставка по місту"));
                    deliveryRow.add(new KeyboardButton("📮 Доставка Новою поштою"));

                    KeyboardRow backRow = new KeyboardRow();
                    backRow.add(new KeyboardButton(BACK_BUTTON));

                    markup.setKeyboard(List.of(deliveryRow, backRow));

                    sendMessage(chatId, "Оберіть спосіб доставки:", markup);
                    userStates.put(userId, "awaiting_delivery_choice");
                }

                case "🏬 Самовивіз" -> {
                    tempStorage.put(userId + "_deliveryType", "Самовивіз");
                    userStates.put(userId, "order_pickup");

                    sendText(chatId, """
                                                ✏️ Введіть, будь-ласка, свої дані для самовивозу у форматі:
                                                🏙 Місто
                                                👤 П.І.
                                                📞 Телефон
                                                💳 Номер картки (Магазину)
                                    
                                                📌 Приклад:
                                                Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444
                                            """);
                }

                case "📦 Доставка по місту" -> {
                    tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                    userStates.put(userId, "awaiting_city_delivery");

                    sendText(chatId, """
                                                📝 Введіть, будь-ласка, дані для доставки по місту у форматі:
                                                📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)
                                    
                                                📌 Приклад:
                                                вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444
                                            """);
                }

                case "📮 Доставка Новою поштою" -> {
                    tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                    userStates.put(userId, "awaiting_post_delivery");

                    sendText(chatId, """
                                            📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:
                                            📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)
                                
                                            📌 Приклад:
                                            №12, Іваненко Іван Іванович, +380501234567, 4444
                                          """);
                }

                case "🎯 Хіт продажу" -> {
                    List<HitsManager.Hit> hits = HitsManager.loadHits();
                    if (hits.isEmpty()) {
                        sendText(chatId, "❌ Поки що немає хітів продажу.");
                        return;
                    }

                    for (HitsManager.Hit hit : hits) {
                        String title = hit.title != null ? hit.title : "";
                        String description = hit.description != null ? hit.description : "";

                        // Формуємо текст для повідомлення
                        String textMsg = "";
                        if (!title.isEmpty()) textMsg += "⭐ *" + title + "*";
                        if (!description.isEmpty() && !"немає".equals(description)) {
                            if (!textMsg.isEmpty()) textMsg += "\n\n";
                            textMsg += description;
                        }

                        String caption;
                        if (!textMsg.isEmpty()) {
                            caption = textMsg;
                        } else if (hit.media_url != null && !hit.media_url.equals("немає")) {
                            caption = null; // Для відео/GIF підпис не ставимо
                        } else {
                            caption = "немає";
                        }

                        try {
                            if (hit.media_url != null && !hit.media_url.equals("немає")) {
                                if (hit.media_url.endsWith(".mp4") || hit.media_url.contains("video")) {
                                    // Відео або GIF
                                    SendVideo video = SendVideo.builder()
                                            .chatId(chatId)
                                            .video(new InputFile(hit.media_url))
                                            .caption(caption)
                                            .parseMode("Markdown")
                                            .build();
                                    execute(video);
                                } else {
                                    // Фото
                                    SendPhoto photo = SendPhoto.builder()
                                            .chatId(chatId)
                                            .photo(new InputFile(hit.media_url))
                                            .caption(caption)
                                            .parseMode("Markdown")
                                            .build();
                                    execute(photo);
                                }
                            } else {
                                // Якщо медіа немає
                                sendText(chatId, caption);
                            }
                        } catch (TelegramApiException e) {
                            LOGGER.severe("[Hit Error] Failed to send media for hit: " + hit.id + " - " + e.getMessage());
                            sendText(chatId, "❌ Не вдалося надіслати медіа.");
                        }
                    }
                }

                // Меню розробника
                case "👨‍💻 Меню розробника" -> {
                    if (DEVELOPERS.contains(userId)) {
                        sendMessage(createDeveloperMenu(chatId));
                    } else {
                        sendText(chatId, "⛔ У вас немає доступу.");
                    }
                }

                case "🔄 Оновити каталог" -> {
                    if (DEVELOPERS.contains(userId)) execute(createDeveloperCatalogMenu(chatId));
                    else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "🔗 Запрошувальні посилання" -> {
                    if (DEVELOPERS.contains(userId)) {
                        userStates.put(userId, "invites_menu");
                        sendMessage(createInvitesMenu(chatId));
                    } else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "📜 Логирування" -> {
                    if (DEVELOPERS.contains(userId)) sendMessage(createLogsMenu(chatId));
                    else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "📝 Список онови" -> {
                    if (DEVELOPERS.contains(userId)) {
                        List<String> updates = DeveloperFileManager.getChangelog();
                        if (updates.isEmpty()) sendText(chatId, "📝 Список оновлень поки порожній.");
                        else sendText(chatId, "📝 Список оновлень:\n\n" + String.join("\n\n", updates));
                    } else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "📊 Статистика запрошувань" -> {
                    userStates.put(userId, "logs_invites");
                    handleState(userId, chatId, text, "logs_invites", update);
                }

                case "📊 Статистика без запрошень" -> {
                    userStates.put(userId, "logs_no_invite");
                    handleState(userId, chatId, text, "logs_no_invite", update);
                }

                case "📦 Замовлення" -> {
                    userStates.put(userId, "logs_orders");
                    handleState(userId, chatId, text, "logs_orders", update);
                }

                case "🔄 Оновити каталог (.yml)" -> {
                    sendText(chatId, "📤 Надішліть .yml файл для оновлення каталогу.");
                    developerState.put(chatId, "WAITING_FOR_YML_FULL");
                }

                case "💰 Оновити ціни (.yml)" -> {
                    sendText(chatId, "📤 Надішліть .yml файл для оновлення лише цін (по назві товару).");
                    developerState.put(chatId, "WAITING_FOR_YML_PRICES");
                }

                case "✏️ Оновити ціну по назві товару" -> {
                    sendText(chatId, "✏️ Введіть назву товару, ціну якого хочете змінити:");
                    developerState.put(chatId, "WAITING_FOR_PRODUCT_NAME");
                }

                // Адмін меню
                case "⚙️ Продавца меню" -> {
                    if (ADMINS.contains(userId)) sendMessage(createAdminMenu(chatId));
                    else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "✏️ Редагувати товар" -> {
                    if (DEVELOPERS.contains(userId)) {
                        userStates.put(userId, "edit_product"); // ставимо стан редагування

                        // Відразу показуємо меню вибору джерела
                        try {
                            SendMessage menu = showAdminSearchSourceMenu(userId, Long.parseLong(chatId));
                            execute(menu);
                        } catch (TelegramApiException e) {
                            LOGGER.severe("[Admin Error] Failed to show search source menu for user " + userId + ": " + e.getMessage());
                            sendText(chatId, "❌ Сталася помилка при показі меню вибору джерела пошуку.");
                        }

                    } else {
                        sendText(chatId, "⛔ У вас немає прав для цієї дії.");
                    }
                    return;
                }

                case "🖼️ Додати фотографію" -> {
                    System.out.println("[DEBUG] Button 'Add Photo' clicked by userId=" + userId);

                    String productName = adminEditingProduct.get(userId);
                    if (productName != null) {
                        photoHandler.requestPhotoUpload(userId, chatId, productName);
                    } else {
                        sendText(chatId, "⚠️ Please select a product first.");
                    }
                }

                case "🔹 MySQL" -> {
                    String keyword = adminSearchKeyword.get(userId);
                    CatalogSearcher catalogSearcher = new CatalogSearcher();
                    List<Map<String, Object>> results = catalogSearcher.searchByKeywordsAdmin(keyword);

                    if (results.isEmpty()) {
                        sendText(chatId, "❌ Товар не знайдено: " + keyword);
                    } else {
                        StringBuilder sb = new StringBuilder("🔎 Знайдено товари у MySQL:\n\n");
                        for (int i = 0; i < results.size(); i++) {
                            sb.append(i + 1).append(". ").append(results.get(i).get("name")).append("\n");
                        }
                        sendText(chatId, sb.toString());
                    }
                }

                case "🔹 YAML" -> {
                    adminSearchSource.put(userId, "yaml");
                    userStates.put(userId, "awaiting_yaml_keyword");
                    sendText(chatId, "Введіть ключові слова для пошуку у YAML:");
                }

                case "Редагувати категорії" -> {
                    if (ADMINS.contains(userId)) {
                        userStates.put(userId, "category_management");
                        sendMessage(createCategoryAdminMenu(chatId));
                    } else sendText(chatId, "⛔ У вас немає доступу до цієї функції.");
                }

                case "🛒 Замовлення користувачів" -> {
                    try (Connection conn = DatabaseManager.getConnection()) {

                        // Перевіряємо, чи є замовлення в базі
                        String countSql = "SELECT COUNT(*) FROM orders";
                        try (PreparedStatement countStmt = conn.prepareStatement(countSql);
                             ResultSet countRs = countStmt.executeQuery()) {

                            if (countRs.next() && countRs.getInt(1) == 0) {
                                sendText(chatId, "Поки що немає замовлень.");
                                return;
                            }
                        }

                        adminOrderIndex.put(userId, 0);
                        showAdminOrder(userId, chatId);

                    } catch (SQLException e) {
                        LOGGER.severe("[Admin Error] Failed to load orders for user " + userId + ": " + e.getMessage());
                        sendText(chatId, "❌ Помилка при завантаженні замовлень з бази.");
                    }
                }

                case "✅ Підтвердити" -> {
                    try (Connection conn = DatabaseManager.getConnection()) {

                        String selectSql = "SELECT * FROM orders WHERE status = 'Нове' ORDER BY id ASC LIMIT 1";
                        try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                             ResultSet rs = stmt.executeQuery()) {

                            if (!rs.next()) {
                                sendText(chatId, "Замовлень немає.");
                                break;
                            }

                            long orderId = rs.getLong("id");
                            long orderUserId = rs.getLong("userId");

                            sendText("" + orderUserId, "✅ Ваше замовлення підтверджено! Очікуйте доставку.");

                            String updateSql = "UPDATE orders SET status = 'Підтверджено' WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setLong(1, orderId);
                                updateStmt.executeUpdate();
                            }

                            sendText(chatId, "Замовлення підтверджено ✅");
                            showAdminOrder(userId, chatId);
                        }

                    } catch (SQLException e) {
                        LOGGER.severe("[Admin Error] Failed to confirm order for user " + userId + ": " + e.getMessage());
                        sendText(chatId, "❌ Помилка при підтвердженні замовлення.");
                    }
                }

                case "❌ Відхилити" -> {
                    userStates.put(userId, "reject_order_reason");
                    sendText(chatId, "✏️ Введіть причину відхилення замовлення:");
                }

                case "🗑️ Видалити замовлення" -> {
                    try (Connection conn = DatabaseManager.getConnection()) {

                        String selectSql = "SELECT * FROM orders WHERE status NOT IN ('Видалено', 'Підтверджено', 'Відхилено') ORDER BY id ASC LIMIT 1";
                        try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                             ResultSet rs = stmt.executeQuery()) {

                            if (!rs.isBeforeFirst()) {
                                sendText(chatId, "Замовлень немає.");
                                break;
                            }

                            if (rs.next()) {
                                String orderCode = rs.getString("orderCode");
                                long orderUserId = rs.getLong("userId"); // примітив

                                String updateSql = "UPDATE orders SET status = ?, comment = ? WHERE orderCode = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, "Видалено");
                                    updateStmt.setString(2, "Видалено адміністратором");
                                    updateStmt.setString(3, orderCode);
                                    updateStmt.executeUpdate();
                                }

                                // Використовуємо String.valueOf для перетворення long у String
                                sendText(String.valueOf(orderUserId), "🗑️ Ваше замовлення було видалено адміністратором.");
                                sendText(chatId, "🗑️ Замовлення видалено.");
                                showAdminOrder(userId, chatId);
                            }
                        }

                    } catch (SQLException e) {
                        LOGGER.severe("[Bot Error] Failed to delete order: " + e.getMessage());
                        sendText(chatId, "❌ Помилка при видаленні замовлення.");
                    }
                }

                case "⏭️ Дальше" -> {
                    int idx = adminOrderIndex.getOrDefault(userId, 0);
                    adminOrderIndex.put(userId, idx + 1);
                    showAdminOrder(userId, chatId);
                }
                case "⏮️ Назад" -> {
                    int idx = adminOrderIndex.getOrDefault(userId, 0);
                    if (idx > 0) adminOrderIndex.put(userId, idx - 1);
                    showAdminOrder(userId, chatId);
                }

                case "➡️ Далі" -> {
                    int idx = adminOrderIndex.getOrDefault(userId, 0);
                    idx++;
                    adminOrderIndex.put(userId, idx);
                    showAdminOrder(userId, chatId);
                }

                case "⭐ Додати товар у Хіт продажу" -> {
                    if (!ADMINS.contains(userId)) {
                        sendText(chatId, "⛔ У вас немає доступу.");
                        break;
                    }
                    userStates.put(userId, "awaiting_hit_type"); // <-- тут треба так
                    sendText(chatId, "Ви хочете додати креатив з описом чи тільки медіа?\nНапишіть 'З описом' або 'Тільки медіа':");
                }

                case "💬 Залишити відгук" -> {
                    userStates.put(userId, "waiting_for_feedback");
                    sendText(chatId, "📝 Напишіть свій відгук, ми обов’язково його переглянемо:");
                }

                case "💬 Відгуки користувачів" -> {
                    if (DEVELOPERS.contains(userId)) {
                        Map<Long, List<String>> allReviews = FeedbackManager.getAllFeedbacks();
                        if (allReviews.isEmpty()) {
                            sendText(chatId, "❌ Відгуків поки що немає.");
                        } else {
                            Long targetId = allReviews.keySet().iterator().next();
                            sendMessage(createFeedbackSubMenu(chatId, targetId));
                        }
                    } else sendText(chatId, "⛔ У вас немає доступу.");
                }

                case "✉️ Відповісти на відгук" -> {
                    userStates.put(userId, "writing_reply");
                    sendText(chatId, "✏️ Введіть відповідь для користувача:");
                }

                case "💾 Зберегти відгук" -> {
                    FeedbackManager.saveFeedbacks();
                    sendText(chatId, "💾 Відгук збережено.");
                }

                case "🧹 Видалити відгук" -> {
                    Long target = adminReplyTarget.get(userId);
                    if (target != null) {
                        FeedbackManager.removeLastFeedback(target);
                        sendText(chatId, "🧹 Останній відгук користувача видалено.");
                    } else {
                        sendText(chatId, "❌ Не знайдено користувача для видалення відгуку.");
                    }
                }

                default -> handleText(userId, text);
            }

            // Якщо користувач пише відгук
            if ("waiting_for_feedback".equals(state)) {
                FeedbackManager.addFeedback(userId, text);
                sendText(chatId, "✅ Ваш відгук надіслано адміністратору!");
                userStates.remove(userId);
                return;
            }

            if (text.contains("Самовивіз")) {
                System.out.println("DEBUG: Натиснули Самовивіз");
                userStates.put(userId, "order_pickup");
                tempStorage.put(userId + "_deliveryType", "Самовивіз");
                sendText(chatId,
                        "✏️ Введіть, будь-ласка, свої дані для самовивозу у форматі:\n" +
                                "🏙 Місто\n👤 П.І.\n📞 Телефон\n💳 Номер картки (Магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444"
                );
            } else if (text.contains("Доставка по місту")) {
                System.out.println("DEBUG: Натиснули Доставка по місту");
                userStates.put(userId, "awaiting_city_delivery");
                tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки по місту у форматі:\n" +
                                "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444"
                );
            } else if (text.contains("Нова пошта")) {
                System.out.println("DEBUG: Натиснули Доставка Новою поштою");
                userStates.put(userId, "awaiting_post_delivery");
                tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:\n" +
                                "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "№12, Іваненко Іван Іванович, +380501234567, 4444"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Очистити стан користувача
    private void clearUserState(Long chatId) {
        currentCategory.remove(chatId);
        currentSubcategory.remove(chatId);
        productIndex.remove(chatId);
    }

    // --- Категорії з MySQL ---
    private void sendCategories(Long chatId) throws TelegramApiException {
        CatalogSearcher searcher = new CatalogSearcher();

        List<String> categories = searcher.getCategories();
        if (categories.isEmpty()) {
            sendText(chatId, "❌ Категорії не знайдено.");
            return;
        }

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(buildKeyboard(categories, true))
                .build();

        sendMessage(chatId, "📂 Виберіть категорію:", markup);

        System.out.println("DEBUG: Categories sent in ID order: " + categories);
    }

    // 🔹    Показ кошика
    public void showCart(Long userId) throws TelegramApiException {
        List<Map<String, Object>> cart = userCart.get(userId);

        if (cart == null || cart.isEmpty()) {
            sendMessage(createUserMenu(String.valueOf(userId), userId));
            return;
        }

        StringBuilder sb = new StringBuilder("📋 Ваш кошик:\n\n");
        double total = 0;
        int i = 1;

        for (Map<String, Object> item : cart) {
            String name = item.getOrDefault("name", "Без назви").toString();
            double price = Double.parseDouble(item.getOrDefault("price", "0").toString());
            total += price;
            sb.append(i++).append(". ").append(name).append(" — ").append(price).append(" грн\n");
        }
        sb.append("\n💰 Всього: ").append(total).append(" грн");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🛒 Замовити товар");
        row1.add("🧹 Очистити кошик");

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);

        markup.setKeyboard(List.of(row1, row2));

        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text(sb.toString())
                .replyMarkup(markup)
                .build();

        execute(msg);
    }

    // 🔹 Побудова клавіатури з кнопками + Назад + Кошик
    private List<KeyboardRow> buildKeyboard(List<String> items, boolean withBottom) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow currentRow = new KeyboardRow();
        int count = 0;

        for (String item : items) {
            currentRow.add(item);
            count++;

            if (count == 3) {
                keyboard.add(currentRow);
                currentRow = new KeyboardRow();
                count = 0;
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        if (withBottom) {
            KeyboardRow bottom = new KeyboardRow();
            bottom.add(BACK_BUTTON);
            keyboard.add(bottom);
        }

        return keyboard;
    }

    // 🔹 Очистити кошик
    private void clearCart(Long userId) throws TelegramApiException {
        userCart.remove(userId);
        userStates.remove(userId);
        sendText(String.valueOf(userId), "🧹 Кошик очищено!");
        sendMessage(createUserMenu(String.valueOf(userId), userId));
    }

    // Викликаємо кошик з іншого класу
    public void openCartForUser(Long userId) throws TelegramApiException {
        userStates.remove(userId); // вихід з пошуку
        showCart(userId);          // приватний метод всередині класу
    }

    // Очистити кошик користувача
    public void clearUserCart(Long userId) throws TelegramApiException {
        userStates.remove(userId);
        clearCart(userId);         // приватний метод всередині класу
    }

    private boolean isInDeveloperMenu(Long userId) {
        // якщо ще немає запису, додаємо false
        developerMenuState.putIfAbsent(userId, false);
        return developerMenuState.get(userId);
    }

    // 🔹 Назад
    private void handleBack(String chatId) throws TelegramApiException {
        Long userId = Long.parseLong(chatId);

        System.out.println("[handleBack] User " + userId + " pressed Back.");

        // 🔸 1. Повне очищення тимчасових станів
        getUserStates().remove(userId);
        getLastShownProduct().remove(userId);
        adminMatchList.remove(userId);
        productIndex.remove(userId);

        // 🔸 2. Якщо користувач був у підкатегорії
        if (currentSubcategory.containsKey(userId)) {
            currentSubcategory.remove(userId);
            System.out.println("[handleBack] Returning user " + userId + " to categories from subcategory.");
            if (currentCategory.containsKey(userId)) {
                sendSubcategories(userId, currentCategory.get(userId));
            } else {
                sendCategories(userId);
            }
            return;
        }

        // 🔸 3. Якщо користувач був у категорії
        if (currentCategory.containsKey(userId)) {
            currentCategory.remove(userId);
            System.out.println("[handleBack] Returning user " + userId + " to main menu from category.");
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 🔸 4. Якщо користувач у кошику
        if (userCart.containsKey(userId)) {
            System.out.println("[handleBack] Returning user " + userId + " from cart to main menu.");
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 🔸 5. Якщо користувач в адмін-меню
        if (adminOrderIndex.containsKey(userId)) {
            adminOrderIndex.remove(userId);
            System.out.println("[handleBack] Returning admin " + userId + " to admin menu.");
            sendMessage(createAdminMenu(chatId));
            return;
        }

        // 🔸 6. Якщо користувач у меню розробника
        if (DEVELOPERS.contains(userId) && isInDeveloperMenu(userId)) {
            System.out.println("[handleBack] Returning developer " + userId + " to developer menu.");
            sendMessage(createDeveloperMenu(chatId));
            return;
        }

        // 🔸 7. Якщо користувач був у редагуванні товару (adminEditingProduct або adminSelectedProductsRange)
        if (adminEditingProduct.containsKey(userId) || adminSelectedProductsRange.containsKey(userId)) {
            System.out.println("[handleBack] Returning admin " + userId + " to search source menu from editing.");

            // Очищаємо тимчасові стани редагування
            adminEditingProduct.remove(userId);
            adminSelectedProductsRange.remove(userId);
            adminEditingField.remove(userId);
            userStates.put(userId, "choose_search_source");

            sendMessage(showAdminSearchSourceMenu(userId, Long.parseLong(chatId)));
            return;
        }

        // 🔸 8. За замовчуванням — головне меню
        System.out.println("[handleBack] Default: Returning user " + userId + " to main menu.");
        sendMessage(createUserMenu(chatId, userId));
    }

    // 🔹 Показ наступного товару по id
    private void showNextProduct(Long chatId) throws TelegramApiException {
        String category = currentCategory.get(chatId);
        String subcategory = currentSubcategory.get(chatId);

        int index = productIndex.getOrDefault(chatId, 0);

        CatalogSearcher searcher = new CatalogSearcher();
        List<Map<String, Object>> products = searcher.getProducts(category, subcategory);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ У цій підкатегорії немає товарів.");
            return;
        }

        // Сортуємо по id, щоб показ був завжди у порядку
        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        if (index >= products.size() || index < 0) index = 0;

        Map<String, Object> product = products.get(index);
        lastShownProduct.put(chatId, product);

        // 🔒 Безпечне отримання всіх значень
        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "N/A"));
        String unit = String.valueOf(product.getOrDefault("unit", "шт"));
        String description = String.valueOf(product.getOrDefault("description", ""));
        String photo = String.valueOf(product.getOrDefault("photo", ""));
        String manufacturer = String.valueOf(product.getOrDefault("manufacturer", ""));

        // 🔧 Якщо manufacturer був збережений як BLOB → конвертуємо
        if (product.get("manufacturer") instanceof byte[] bytes) {
            manufacturer = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }

        StringBuilder sb = new StringBuilder("📦 ").append(name)
                .append("\n💰 Ціна: ").append(price).append(" грн за ").append(unit);
        if (!manufacturer.isEmpty() && !"null".equalsIgnoreCase(manufacturer))
            sb.append("\n🏭 Виробник: ").append(manufacturer);
        if (!description.isEmpty() && !"null".equalsIgnoreCase(description))
            sb.append("\n📖 ").append(description);

        // Кнопки
        KeyboardRow row = new KeyboardRow();
        row.add("➡ Далі");
        row.add("🛒 Додати в кошик");
        row.add("🛍️ Перейти в кошик");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboard.add(new KeyboardRow(List.of(new KeyboardButton(BACK_BUTTON))));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        // Відправка фото або тексту
        if (photo != null && !photo.isEmpty() && !"null".equalsIgnoreCase(photo)) {
            sendPhotoFromResources(chatId.toString(), photo, sb.toString(), markup);
        } else {
            sendTextWithMarkup(chatId, sb.toString(), markup);
        }

        // Збільшуємо індекс для наступного показу
        index = (index + 1) % products.size();
        productIndex.put(chatId, index);
    }

    // 🔹 Додати товар у кошик
    private void addToCart(Long chatId) throws TelegramApiException {
        Map<String, Object> product = lastShownProduct.get(chatId);

        if (product == null) {
            sendText(chatId, "❌ Неможливо додати товар. Спробуйте ще раз.");
            return;
        }

        userCart.computeIfAbsent(chatId, k -> new ArrayList<>()).add(product);
        sendText(chatId, "✅ Товар \"" + product.get("name") + "\" додано до кошика!");
    }

    public void addToCartTool(Long userId) {
        Map<String, Object> product = lastShownProduct.get(userId);
        String chatId = String.valueOf(userId);

        if (product == null) {
            sendText(chatId, "❌ Товар не знайдено для додавання в кошик.");
            return;
        }

        userCart.computeIfAbsent(userId, k -> new ArrayList<>()).add(product);

        // повідомлення про успіх
        sendText(chatId, "✅ Товар додано до кошика: " + product.get("name"));

        // тепер показуємо кнопки
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText("🔎 Введіть назву нового товару або оберіть інший товар з попереднього списку:");
        msg.setReplyMarkup(getSearchKeyboard());

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        userStates.put(userId, "waiting_for_search");
    }

    private final UserManager userManager = new UserManager();

    private void handleState(Long userId, String chatId, String text, String state, Update update) {

        switch (state) {
            case "search_catalog" -> handleSearch(userId, chatId, text);
            case "edit_product" -> {
                try {
                    handleEditProductStart(userId, chatId, text);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(userId, "❌ Помилка при редагуванні товару.");
                }
            }
            case "choose_product" -> handleChooseProduct(userId, chatId, text);
            case "editing" -> handleEditing(userId, chatId, text);
            case "awaiting_field_value" -> handleAwaitingField(userId, chatId, text);
            case "awaiting_subcategory" -> handleAddToSubcategory(userId, chatId, text);
            case "add_hit" -> handleAddHit(userId, chatId, text);
            case "add_category" -> handleAddCategory(userId, chatId, text);
            case "add_subcategory" -> handleAddSubcategory(userId, chatId, text);
            case "add_new_subcategory" -> handleAddNewSubcategory(userId, chatId, text);
            case "choose_category_for_sub" -> handleChooseCategoryForSub(userId, chatId, text);
            case "delete_category_select" -> handleDeleteCategorySelect(userId, chatId, text);
            case "category_management" -> handleCategoryManagementState(userId, chatId, text);
            case "waiting_for_search" -> handleWaitingForSearch(userId, chatId, text);
            case "waiting_for_product_number" -> handleWaitingForProductNumber(userId, chatId, text);
            case "choose_search_source" -> {
                try {
                    handleAdminSearchSource(userId, chatId, text);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при виборі джерела пошуку.");
                }
            }

            case "awaiting_search" -> {
                try {
                    handleAdminSearchInput(userId, chatId, text);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при пошуку товару.");
                }
            }

            case "awaiting_yaml_keyword" -> {
                String keyword = text.trim();

                List<Map<String, Object>> results;
                try {
                    results = CatalogUpdater.searchProductsByKeywords(keyword);
                } catch (IOException e) {
                    sendText(chatId, "❌ Помилка при пошуку у YAML: " + e.getMessage());
                    break;
                }

                if (results.isEmpty()) {
                    sendText(chatId, "❌ Товар не знайдено: " + keyword);
                } else {
                    StringBuilder sb = new StringBuilder("🔎 Знайдено товари у YAML:\n\n");
                    for (int i = 0; i < results.size(); i++) {
                        sb.append(i + 1)
                                .append(". ")
                                .append(results.get(i).get("name"))
                                .append("\n");
                    }
                    sendText(chatId, sb.toString());

                    // Зберігаємо результати, щоб можна було вибрати номер
                    adminMatchList.put(userId, results);

                    // Перемикаємо користувача у стан вибору товару
                    userStates.put(userId, "choose_yaml_product");
                }
            }

            case "choose_yaml_product" -> {
                List<Map<String, Object>> matches = adminMatchList.get(userId);
                int index;

                try {
                    index = Integer.parseInt(text.trim()) - 1;
                } catch (NumberFormatException e) {
                    sendText(chatId, "⚠️ Введіть номер товару.");
                    return;
                }

                if (matches == null || index < 0 || index >= matches.size()) {
                    sendText(chatId, "⚠️ Невірний номер. Спробуйте ще раз.");
                    return;
                }

                Map<String, Object> selectedProduct = matches.get(index);
                String productName = (String) selectedProduct.get("name");

                adminEditingProduct.put(userId, productName);
                userStates.put(userId, "yaml_edit_menu"); // ← окремий стан для YAML

                // Відправляємо обмежене меню для YAML
                try {
                    execute(createYamlEditMenu(chatId, productName));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при відправці меню.");
                }
            }

            case "reject_order_reason" -> {
                String reason = text;

                try {
                    Connection conn = DatabaseManager.getConnection();

                    String sql = "SELECT * FROM orders WHERE status != 'Підтверджено' AND status != 'Відхилено' ORDER BY id ASC LIMIT 1";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery();

                    if (!rs.isBeforeFirst()) {
                        sendText(chatId, "Замовлень немає.");
                        userStates.remove(userId);
                        rs.close();
                        stmt.close();
                        break;
                    }

                    if (rs.next()) {
                        Long orderUserId = rs.getLong("userId");
                        String orderCode = rs.getString("orderCode");
                        rs.close();
                        stmt.close();

                        sendText(orderUserId.toString(), "❌ Ваше замовлення відхилено.\nПричина: " + reason);

                        String updateSql = "UPDATE orders SET status = ?, comment = ? WHERE orderCode = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setString(1, "Відхилено");
                        updateStmt.setString(2, reason);
                        updateStmt.setString(3, orderCode);
                        int rows = updateStmt.executeUpdate();
                        updateStmt.close();

                        if (rows == 0) {
                            sendText(chatId, "❌ Не вдалося оновити замовлення у базі.");
                        } else {
                            sendText(chatId, "Замовлення відхилено ✅");
                        }

                        showAdminOrder(userId, chatId);
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при обробці замовлення.");
                }

                // Очищаємо стан
                userStates.remove(userId);
            }

            case "reply_to_customer" -> {
                if (!ADMINS.contains(userId)) {
                    sendText(chatId, "⛔ У вас немає доступу.");
                    break;
                }

                Optional<Long> targetUserIdOpt = supportAnswers.keySet().stream().findFirst();
                if (targetUserIdOpt.isEmpty()) {
                    sendText(chatId, "❌ Немає користувачів для відповіді.");
                    break;
                }

                Long targetUserId = targetUserIdOpt.get();
                List<String> messages = supportAnswers.get(targetUserId);
                if (messages == null || messages.isEmpty()) {
                    sendText(chatId, "❌ Повідомлень від користувача немає.");
                    break;
                }

                String userMessage = messages.get(0); // перше повідомлення користувача

                // Встановлюємо стан для очікування відповіді адміністратора
                userStates.put(userId, "awaiting_admin_reply");
                tempStorage.put(userId + "_reply_to", targetUserId);
                tempStorage.put(userId + "_user_message", userMessage);

                sendText(chatId,
                        "✉ Повідомлення від користувача: " + targetUserId + "\n\n" +
                                userMessage + "\n\n✏️ Введіть вашу відповідь:"
                );
            }

            case "ask_consultant" -> {
                if (text != null) {
                    supportAnswers.computeIfAbsent(userId, k -> new ArrayList<>()).add(text);
                    userStates.remove(userId);
                    sendText(chatId, "✅ Ваше повідомлення надіслано консультанту!");
                }
            }

            // 📌 Введення ID користувача для відповіді
            case "waiting_for_feedback" -> {
                FeedbackManager.addFeedback(userId, text);
                sendText(chatId, "✅ Ваш відгук надіслано адміністратору!");
                userStates.remove(userId);
            }

            case "writing_reply" -> {
                Long replyTargetId = adminReplyTarget.get(userId); // Перейменовано, щоб уникнути конфлікту
                if (replyTargetId != null) {
                    sendText(replyTargetId.toString(), "📩 Відповідь від адміністратора:\n" + text);
                    sendText(chatId, "✅ Відповідь надіслана користувачу " + replyTargetId);
                } else {
                    sendText(chatId, "❌ Не знайдено користувача для відповіді.");
                }
                userStates.remove(userId);
                adminReplyTarget.remove(userId);
            }

            case "awaiting_admin_reply" -> {
                Long replyTargetId = (Long) tempStorage.get(userId + "_reply_to"); // Оголошення змінної
                if (replyTargetId != null) {
                    sendText(replyTargetId.toString(), "💬 Відповідь адміністратора:\n\n" + text);
                    sendText(chatId, "✅ Ваша відповідь надіслана користувачу " + replyTargetId);
                } else {
                    sendText(chatId, "❌ Не знайдено користувача для відповіді.");
                }

                // Очищаємо стан
                userStates.remove(userId);
                tempStorage.remove(userId + "_reply_to");
                tempStorage.remove(userId + "_user_message");
            }

            case "awaiting_manufacturer" -> {
                String productName = (String) tempStorage.get(userId + "_editingProduct");
                System.out.println("DEBUG: Retrieved productName from tempStorage = '" + productName + "'");

                if (productName == null) {
                    sendText(chatId, "❌ Не знайдено товар для редагування.");
                    userStates.put(userId, "admin_menu");
                    return;
                }

                String input = text.trim();
                System.out.println("DEBUG: User input = '" + input + "'");

                boolean success = CatalogEditor.updateProductManufacturer(productName, input);
                System.out.println("DEBUG: updateProductManufacturer returned " + success);

                if (!success) {
                    sendText(chatId, "⚠️ Не вдалося оновити виробника для товару: " + productName);
                } else if (input.equalsIgnoreCase("❌") || input.isEmpty()) {
                    sendText(chatId, "✅ Виробник видалений для товару: " + productName);
                } else {
                    sendText(chatId, "✅ Виробник збережений: " + input);
                }

                // Повертаємо користувача назад у меню редагування
                sendText(chatId, createEditMenu(chatId, userId).getText());
                userStates.put(userId, "edit_product");
            }

            case "choose_delivery_type" -> {
                System.out.println("DEBUG: User ID = " + userId + ", State = " + userStates.get(userId) + ", Text = " + text);
                // Вибір способу доставки
                if ("🏬 Самовивіз".equals(text)) {
                    tempStorage.put(userId + "_deliveryType", "Самовивіз");
                    sendText(chatId, "📝 Введіть, будь ласка, дані для самовивозу у форматі:\n" +
                            "🏙 Місто, 👤 П.І., 📞 Телефон, 💳 Картка");
                    userStates.put(userId, "order_pickup");
                } else if ("📍 Доставка по місту".equals(text)) {
                    tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                    sendText(chatId, "📝 Введіть, будь ласка, дані для доставки по місту у форматі:\n" +
                            "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Картка");
                    userStates.put(userId, "awaiting_city_delivery");
                } else if ("📮 Нова Пошта".equals(text)) {
                    tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                    sendText(chatId, "📝 Введіть, будь ласка, дані для доставки Новою Поштою у форматі:\n" +
                            "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Картка");
                    userStates.put(userId, "awaiting_post_delivery");
                }
            }

            case "awaiting_hit_type" -> {
                if (!ADMINS.contains(userId)) break;
                if (text == null) return;

                if (text.equalsIgnoreCase("з описом")) {
                    userStates.put(userId, "awaiting_hit_title");
                    sendText(chatId, "Введіть назву товару для Хіт продажу:");
                } else if (text.equalsIgnoreCase("тільки медіа")) {
                    userStates.put(userId, "awaiting_hit_media_only");
                    sendText(chatId, "Відправте фото або відео (або напишіть 'немає'):");
                } else {
                    sendText(chatId, "Будь ласка, напишіть 'З описом' або 'Тільки медіа'");
                }
            }

            case "awaiting_hit_title" -> {
                if (text == null || text.isBlank()) {
                    sendText(chatId, "❌ Будь ласка, введіть назву товару.");
                    return;
                }
                tempStorage.put(userId + "_hit_title", text);
                userStates.put(userId, "awaiting_hit_description");
                sendText(chatId, "Введіть опис товару (або напишіть 'немає'):");
            }

            case "awaiting_hit_description" -> {
                if (text == null) {
                    sendText(chatId, "❌ Будь ласка, введіть опис товару.");
                    return;
                }
                tempStorage.put(userId + "_hit_description", text.equalsIgnoreCase("немає") ? "немає" : text);
                userStates.put(userId, "awaiting_hit_media");
                sendText(chatId, "Відправте фото або відео (або напишіть 'немає'):");
            }

            case "awaiting_hit_media" -> {
                String title = tempStorage.getOrDefault(userId + "_hit_title", "немає").toString();
                String description = tempStorage.getOrDefault(userId + "_hit_description", "немає").toString();

                // Завантаження медіа з Telegram на Cloudinary
                String mediaUrl = HitsManager.uploadFromTelegram(this, update.getMessage());
                if (mediaUrl == null) mediaUrl = "немає";

                HitsManager.saveHit(title, description, mediaUrl);

                // Очищення
                userStates.remove(userId);
                tempStorage.remove(userId + "_hit_title");
                tempStorage.remove(userId + "_hit_description");

                sendText(chatId, "✅ Товар успішно додано у Хіт продажу!");

                // Розсилка всім користувачам
                for (String uidStr : userManager.getRegisteredUsers()) {
                    if (!ADMINS.contains(Long.parseLong(uidStr))) {
                        try {
                            sendText(uidStr, "🌟 Новий Хіт продажу з’явився в магазині!\nПерегляньте його у розділі «Хіти продажів»!");
                        } catch (Exception ignored) {}
                    }
                }
            }

            case "awaiting_hit_media_only" -> {
                // Завантаження медіа з Telegram на Cloudinary
                String mediaUrl = HitsManager.uploadFromTelegram(this, update.getMessage());
                if (mediaUrl == null) mediaUrl = "немає";

                HitsManager.saveHit(null, "немає", mediaUrl); // title=null, description="немає"

                userStates.remove(userId);
                tempStorage.remove(userId + "_hit_media");

                sendText(chatId, "✅ Товар успішно додано у Хіт продажу!");

                for (String uidStr : userManager.getRegisteredUsers()) {
                    if (!ADMINS.contains(Long.parseLong(uidStr))) {
                        try {
                            sendText(uidStr, "🌟 Новий Хіт продажу з’явився в магазині!\nПерегляньте його у розділі «Хіти продажів»!");
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Обробка вибору доставки
            case "awaiting_delivery_choice" -> {
                switch (text) {
                    case "🏬 Самовивіз" -> {
                        tempStorage.put(userId + "_deliveryType", "Самовивіз");
                        userStates.put(userId, "order_pickup");
                        sendText(chatId,
                                "✏️ Введіть, будь-ласка, свої дані для самовивозу у форматі:\n" +
                                        "🏙 Місто\n👤 П.І.\n📞 Телефон\n💳 Номер картки (Магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444");
                    }

                    case "📦 Доставка по місту" -> {
                        tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                        userStates.put(userId, "awaiting_city_delivery");
                        sendText(chatId,
                                "📝 Введіть, будь-ласка, дані для доставки по місту у форматі:\n" +
                                        "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444");
                    }

                    case "📮 Доставка Новою поштою" -> {
                        tempStorage.put(userId + "_deliveryType", "Нова пошта");
                        userStates.put(userId, "awaiting_post_delivery");
                        sendText(chatId,
                                "📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:\n" +
                                        "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Номер картки (Магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "№12, Іваненко Іван Іванович, +380501234567, 4444");
                    }

                    case BACK_BUTTON -> {
                        try {
                            handleBack(chatId); // chatId як String
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                            sendText(chatId, "❌ Сталася помилка при обробці кнопки Назад.");
                        }
                    }

                    default -> sendText(chatId, "❌ Будь ласка, оберіть один із варіантів кнопок нижче.");
                }
            }

            // 🔹 Order Pickup
            case "order_pickup" -> {
                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                String[] parts = text.split(",", 4); // Місто, П.І., Телефон, Картка
                String city = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                StringBuilder itemsDb = new StringBuilder();
                double total = 0;
                for (Map<String, Object> item : cart) {
                    String name = item.getOrDefault("name", "Без назви").toString();
                    double price = 0;
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number n) price = n.doubleValue();
                    else if (priceObj != null) {
                        try { price = Double.parseDouble(priceObj.toString()); } catch (NumberFormatException ignored) {}
                    }
                    itemsDb.append(name).append(":").append(price).append(";");
                    total += price;
                }

                try (Connection conn = DatabaseManager.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO orders (userId, orderCode, deliveryType, city, fullName, phone, card, status, item, total, date) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"
                    );
                    stmt.setLong(1, userId);
                    stmt.setString(2, orderCode);
                    stmt.setString(3, "Самовивіз");
                    stmt.setString(4, city);
                    stmt.setString(5, fullName);
                    stmt.setString(6, phone);
                    stmt.setString(7, card);
                    stmt.setString(8, "Нове");
                    stmt.setString(9, itemsDb.toString());
                    stmt.setDouble(10, total);
                    stmt.executeUpdate();
                    stmt.close();

                    // Вставка в окрему таблицю user_cards
                    PreparedStatement insertUser = conn.prepareStatement(
                            "INSERT INTO user_cards (name, city, number, number_carts, bonus) VALUES (?, ?, ?, ?, ?)"
                    );
                    insertUser.setString(1, fullName);
                    insertUser.setString(2, city);
                    insertUser.setString(3, phone);
                    insertUser.setString(4, card); // 4-значна картка
                    insertUser.setString(5, "");    // бонус поки порожній
                    insertUser.executeUpdate();
                    insertUser.close();

                    userCart.remove(userId);
                    userStates.remove(userId);

                    sendText(chatId, "✅ Ваше замовлення успішно оформлено!\nКод замовлення: " + orderCode +
                            "\nВаше замовлення:\n" + itemsDb.toString().replace(";", "\n") +
                            "\n💰 Всього: " + total + " грн\nБудь ласка, заберіть товар у магазині.");

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Сталася помилка при збереженні замовлення.");
                }
            }

            // 🔹 City Delivery
            case "awaiting_city_delivery" -> {
                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                String[] parts = text.split(",", 4); // Адреса, П.І., Телефон, Картка
                String address = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                StringBuilder itemsDb = new StringBuilder();
                double total = 0;
                for (Map<String, Object> item : cart) {
                    String name = item.getOrDefault("name", "Без назви").toString();
                    double price = 0;
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number n) price = n.doubleValue();
                    else if (priceObj != null) {
                        try { price = Double.parseDouble(priceObj.toString()); } catch (NumberFormatException ignored) {}
                    }
                    itemsDb.append(name).append(":").append(price).append(";");
                    total += price;
                }

                try (Connection conn = DatabaseManager.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO orders (userId, orderCode, deliveryType, city, fullName, phone, card, status, item, total, date) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"
                    );
                    stmt.setLong(1, userId);              // ось тут передаємо userId
                    stmt.setString(2, orderCode);
                    stmt.setString(3, "Доставка по місту");
                    stmt.setString(4, address);
                    stmt.setString(5, fullName);
                    stmt.setString(6, phone);
                    stmt.setString(7, card);
                    stmt.setString(8, "Нове");
                    stmt.setString(9, itemsDb.toString());
                    stmt.setDouble(10, total);
                    stmt.executeUpdate();
                    stmt.close();

                    PreparedStatement insertUser = conn.prepareStatement(
                            "INSERT INTO user_cards (name, city, number, number_carts, bonus) VALUES (?, ?, ?, ?, ?)"
                    );
                    insertUser.setString(1, fullName);
                    insertUser.setString(2, address);
                    insertUser.setString(3, phone);
                    insertUser.setString(4, card);
                    insertUser.setString(5, "");
                    insertUser.executeUpdate();
                    insertUser.close();

                    userCart.remove(userId);
                    userStates.remove(userId);

                    sendText(chatId, "✅ Ваше замовлення успішно оформлено!\nКод замовлення: " + orderCode +
                            "\nВаше замовлення:\n" + itemsDb.toString().replace(";", "\n") +
                            "\n💰 Всього: " + total + " грн\nВаш товар буде доставлений за вказаною адресою.");

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Сталася помилка при збереженні замовлення.");
                }
            }

            // 🔹 Nova Poshta Delivery
            case "awaiting_post_delivery" -> {
                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                String[] parts = text.split(",", 4); // Відділення НП, П.І., Телефон, Картка
                String postOffice = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                StringBuilder itemsDb = new StringBuilder();
                double total = 0;
                for (Map<String, Object> item : cart) {
                    String name = item.getOrDefault("name", "Без назви").toString();
                    double price = 0;
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number n) price = n.doubleValue();
                    else if (priceObj != null) {
                        try { price = Double.parseDouble(priceObj.toString()); } catch (NumberFormatException ignored) {}
                    }
                    itemsDb.append(name).append(":").append(price).append(";");
                    total += price;
                }

                try (Connection conn = DatabaseManager.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO orders (userId, orderCode, deliveryType, city, fullName, phone, card, status, item, total, date) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"
                    );
                    stmt.setLong(1, userId);              // ось тут передаємо userId
                    stmt.setString(2, orderCode);
                    stmt.setString(3, "Нова пошта");
                    stmt.setString(4, postOffice);
                    stmt.setString(5, fullName);
                    stmt.setString(6, phone);
                    stmt.setString(7, card);
                    stmt.setString(8, "Нове");
                    stmt.setString(9, itemsDb.toString());
                    stmt.setDouble(10, total);
                    stmt.executeUpdate();
                    stmt.close();

                    PreparedStatement insertUser = conn.prepareStatement(
                            "INSERT INTO user_cards (name, city, number, number_carts, bonus) VALUES (?, ?, ?, ?, ?)"
                    );
                    insertUser.setString(1, fullName);
                    insertUser.setString(2, postOffice);
                    insertUser.setString(3, phone);
                    insertUser.setString(4, card);
                    insertUser.setString(5, "");
                    insertUser.executeUpdate();
                    insertUser.close();

                    userCart.remove(userId);
                    userStates.remove(userId);

                    sendText(chatId, "✅ Ваше замовлення успішно оформлено!\nКод замовлення: " + orderCode +
                            "\nВаше замовлення:\n" + itemsDb.toString().replace(";", "\n") +
                            "\n💰 Всього: " + total + " грн\nВаш товар буде доставлений Новою поштою за вказаним відділенням.");

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Сталася помилка при збереженні замовлення.");
                }
            }

            case "invites_menu" -> {
                switch (text) {
                    case "➕ Додати запрошення" -> {
                        userStates.put(userId, "add_invite");
                        sendText(chatId, "✏️ Введіть дані нового запрошення у форматі:\nName;Kasa;City");
                    }
                    case "🗑️ Видалити запрошення" -> {
                        userStates.put(userId, "delete_invite");
                        sendText(chatId, "✏️ Введіть ID запрошення для видалення:");
                    }
                    case "✏️ Редагувати запрошення" -> {
                        userStates.put(userId, "edit_invite");
                        sendText(chatId, "✏️ Введіть дані для редагування у форматі:\nID;Name;Kasa;City");
                    }
                    case "📄 Показати всі запрошення" -> {
                        String sql = "SELECT * FROM invites ORDER BY id ASC";

                        try {
                            Connection conn = DatabaseManager.getConnection();
                            try (PreparedStatement stmt = conn.prepareStatement(sql);
                                 ResultSet rs = stmt.executeQuery()) {

                                StringBuilder sb = new StringBuilder("🔗 Статистика запрошень:\n\n");
                                boolean hasInvites = false;

                                while (rs.next()) {
                                    hasInvites = true;
                                    sb.append("🆔 ID: ").append(rs.getInt("id")).append("\n")
                                            .append("👤 Ім'я: ").append(rs.getString("name")).append("\n")
                                            .append("💰 Каса: ").append(rs.getString("kasa")).append("\n")
                                            .append("🏙️ Місто: ").append(rs.getString("city")).append("\n")
                                            .append("📈 Кількість приєднались: ").append(rs.getInt("number")).append("\n")
                                            .append("-----------------------------\n");
                                }

                                if (!hasInvites) {
                                    sendText(chatId, "Поки що немає запрошень.");
                                } else {
                                    sendText(chatId, sb.toString());
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            sendText(chatId, "❌ Сталася помилка при отриманні запрошень.");
                        }
                    }
                    default -> sendText(chatId, "❌ Некоректна команда.");
                }
            }

            case "add_invite" -> {
                String[] parts = text.split(";");
                if (parts.length < 3) {
                    sendText(chatId, "❌ Некоректний формат! Використовуйте Name;Kasa;City");
                } else {
                    try {
                        InviteManager inviteManager = new InviteManager(); // SQLException
                        boolean success = inviteManager.addInvite(parts[0], parts[1], parts[2], botUsername);
                        if (success) sendText(chatId, "✅ Запрошення додано!");
                        else sendText(chatId, "❌ Сталася помилка при додаванні запрошення.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        sendText(chatId, "❌ Помилка доступу до бази даних.");
                    }
                }
                userStates.remove(userId);
            }

            case "delete_invite" -> {
                try {
                    int id = Integer.parseInt(text.trim());
                    InviteManager inviteManager = new InviteManager();
                    boolean deleted = inviteManager.deleteInvite(id);
                    if (deleted) sendText(chatId, "✅ Запрошення видалено!");
                    else sendText(chatId, "❌ Запрошення не знайдено.");
                } catch (Exception e) {
                    sendText(chatId, "❌ Некоректний ID!");
                }
                userStates.remove(userId);
            }

            case "edit_invite" -> {
                String[] parts = text.split(";");
                if (parts.length < 4) {
                    sendText(chatId, "❌ Некоректний формат! Використовуйте ID;Name;Kasa;City");
                } else {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        InviteManager inviteManager = new InviteManager();
                        boolean edited = inviteManager.editInvite(id, parts[1], parts[2], parts[3]);
                        if (edited) sendText(chatId, "✅ Запрошення відредаговано!");
                        else sendText(chatId, "❌ Запрошення не знайдено!");
                    } catch (Exception e) {
                        sendText(chatId, "❌ Некоректний ID!");
                    }
                }
                userStates.remove(userId);
            }

            case "logs_invites" -> {
                Map<Integer, Map<String, Object>> invites = DeveloperFileManager.getAllInvites();
                if (invites.isEmpty()) {
                    sendText(chatId, "📊 Поки що немає запрошень.");
                } else {
                    StringBuilder sb = new StringBuilder("📊 Статистика запрошувальних посилань:\n\n");
                    for (Map.Entry<Integer, Map<String, Object>> entry : invites.entrySet()) {
                        Map<String, Object> data = entry.getValue();
                        sb.append("🆔 ID: ").append(entry.getKey()).append("\n")
                                .append("👤 Ім'я: ").append(data.get("name")).append("\n")
                                .append("💰 Каса: ").append(data.get("kasa")).append("\n")
                                .append("🏙️ Місто: ").append(data.get("city")).append("\n")
                                .append("📈 Кількість: ").append(data.get("number")).append("\n")
                                .append("-----------------------------\n");
                    }
                    sendText(chatId, sb.toString());
                }
                userStates.remove(userId);
            }

            case "logs_no_invite" -> {
                List<Long> noInviteUsers = DeveloperFileManager.getNoInviteUsers();
                int count = noInviteUsers.size(); // кількість користувачів без запрошень
                sendText(chatId, "📊 Кількість користувачів, які приєдналися без запрошень: " + count);
                userStates.remove(userId);
            }

            case "logs_orders" -> {
                Map<String, Integer> summary = DeveloperFileManager.getOrdersSummary();
                List<Map<String, String>> rejectedOrders = DeveloperFileManager.getRejectedOrders();

                StringBuilder message = new StringBuilder();
                message.append("📦 Статистика замовлень:\n")
                        .append("Всього замовлень: ").append(summary.getOrDefault("total", 0)).append("\n")
                        .append("Відправлено/готові: ").append(summary.getOrDefault("sent", 0)).append("\n")
                        .append("Відхилено: ").append(summary.getOrDefault("rejected", 0));

                if (!rejectedOrders.isEmpty()) {
                    message.append("\n\nПричини відхилення:");
                    for (Map<String, String> order : rejectedOrders) {
                        message.append("\n• [")
                                .append(order.get("orderCode"))
                                .append("] ")
                                .append(order.get("comment"));
                    }
                }

                sendText(chatId, message.toString());
                userStates.remove(userId);
            }

            case "editing_field_value" -> {
                String field = adminEditingField.get(userId);        // яке поле редагується
                String productName = adminEditingProduct.get(userId);

                System.out.println("DEBUG: User " + userId + " editing field = '" + field + "' for product = '" + productName + "'");

                if (productName == null || field == null) {
                    sendText(chatId, "❌ Сталася помилка. Спробуйте ще раз.");
                    userStates.remove(userId);
                    return;
                }

                String newValue = text.trim();
                System.out.println("DEBUG: New value entered = '" + newValue + "'");

                // --- Перевірка для одиниці виміру ---
                if ("unit".equals(field)) {
                    if (!newValue.equalsIgnoreCase("шт") && !newValue.equalsIgnoreCase("метр")) {
                        sendText(chatId, "❌ Допустимі значення: 'шт' або 'метр'. Спробуйте ще раз:");
                        return; // залишаємо стан await
                    }
                }

                try {
                    boolean success = CatalogEditor.updateField(productName, field, newValue);
                    System.out.println("DEBUG: updateField returned " + success);

                    if (success) {
                        sendText(chatId, "✅ Поле '" + field + "' успішно оновлено для товару '" + productName + "'");
                    } else {
                        sendText(chatId, "⚠️ Не вдалося оновити поле '" + field + "' для товару '" + productName + "'");
                    }
                } catch (Exception e) {
                    sendText(chatId, "❌ Сталася помилка при оновленні поля '" + field + "'");
                    e.printStackTrace();
                }

                // --- Очищення станів ---
                userStates.remove(userId);
                adminEditingField.remove(userId);
                adminEditingProduct.remove(userId);
            }

            case "changelog_menu" -> {
                List<String> logs = DeveloperFileManager.getChangelog();
                if (logs.isEmpty()) sendText(chatId, "📝 Список онови поки що пустий.");
                else sendText(chatId, "📝 Changelog:\n" + String.join("\n", logs));
                userStates.remove(userId);
            }
        }
    }

    // 🔍 Пошук товару
    public void handleSearch(Long userId, String chatId, String text) {
        System.out.println("[handleSearch] User " + userId + " input: '" + text + "'");

        text = text.trim();
        if (text.isEmpty()) {
            sendText(chatId, "⚠️ Введіть назву товару для пошуку.");
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[handleSearch] Found products count: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                sendText(chatId, "❌ Товар не знайдено. Спробуйте інший запит.");
                return;
            }

            if (foundProducts.size() > 1) {
                StringBuilder sb = new StringBuilder("🔎 Знайдено кілька товарів:\n\n");
                int idx = 1;
                for (Map<String, Object> p : foundProducts) {
                    sb.append(idx++).append(". ").append(p.get("name")).append("\n");
                }
                sb.append("\nВведіть номер товару, щоб побачити деталі.");

                searchResults.put(userId, foundProducts);
                System.out.println("[handleSearch] searchResults for user " + userId + ": " + foundProducts);
                sendText(chatId, sb.toString());
                return;
            }

            // ✅ Якщо знайдено один товар
            Map<String, Object> product = foundProducts.get(0);
            lastShownProduct.put(userId, product);

            // Логи для дебагу
            System.out.println("[handleSearch] lastShownProduct updated for userId=" + userId + ": " + product);

            sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Помилка під час пошуку товару.");
        }
    }

    private void handleWaitingForProductNumber(Long userId, String chatId, String text) {
        List<Map<String, Object>> products = searchResults.get(Long.parseLong(chatId));

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ Список товарів порожній. Будь ласка, спробуйте пошук знову.");
            userStates.remove(userId);
            return;
        }

        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(text.trim()) - 1;
        } catch (NumberFormatException e) {
            sendText(chatId, "⚠️ Будь ласка, введіть правильний номер товару.");
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= products.size()) {
            sendText(chatId, "⚠️ Номер товару поза діапазоном. Спробуйте ще раз.");
            return;
        }

        Map<String, Object> selectedProduct = products.get(selectedIndex);

        String message = String.format(
                "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s\n\n🔎 Якщо бажаєте, введіть інший товар для пошуку або натисніть 'Назад' для повернення в головне меню.",
                selectedProduct.get("name"),
                selectedProduct.get("price"),
                selectedProduct.get("category"),
                selectedProduct.get("subcategory")
        );

        // 🔹 Створюємо клавіатуру через KeyboardRow
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Додати в кошик");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🛍 Переглянути кошик");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        // Відправка повідомлення з кнопками
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Помилка при відправці повідомлення.");
        }

        // Очищуємо тимчасові дані
        userStates.remove(userId);
        searchResults.remove(Long.parseLong(chatId));
    }

    private void handleWaitingForSearch(Long userId, String chatId, String text) {
        text = text.trim();

        // ⬅️ Назад → вихід у головне меню
        if (text.equalsIgnoreCase("⬅️ Назад") || text.equalsIgnoreCase("Назад")) {
            getUserStates().remove(userId);
            try {
                execute(createUserMenu(chatId, userId));
            } catch (TelegramApiException e) {
                e.printStackTrace();
                System.out.println("[handleWaitingForSearch] Failed to send main menu to user " + userId);
            }
            System.out.println("[handleWaitingForSearch] User " + userId + " exited search mode.");
            return;
        }

        // 🛍️ Перейти в кошик → вимикаємо пошук перед відкриттям
        if (text.equalsIgnoreCase("🛍️ Перейти в кошик") || text.equalsIgnoreCase("Перейти в кошик")) {
            getUserStates().remove(userId);
            try {
                openCartForUser(userId);
                System.out.println("[handleWaitingForSearch] User " + userId + " opened the cart.");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(String.valueOf(userId), "⚠️ Не вдалося відкрити кошик.");
            }
            return;
        }

        // 🛠 Додати в кошик
        if (text.equals("🛠 Додати в кошик")) {
            Map<String, Object> product = getLastShownProduct().get(userId);
            if (product != null) {
                addToCartTool(userId);
            } else {
                sendText(chatId, "❌ Товар не знайдено для додавання в кошик.");
            }
            return;
        }

        ProductSearchManager searchManager = new ProductSearchManager(this);

        try {
            // Якщо користувач ввів номер товару зі списку
            if (text.matches("\\d+")) {
                searchManager.handleSearchNumber(userId, chatId, text);
                // стан залишаємо тільки якщо користувач справді шукає номер
            } else {
                // Якщо користувач ввів текст → пошук
                getUserStates().put(userId, "waiting_for_search"); // ставимо стан пошуку перед пошуком
                searchManager.performSearch(userId, chatId, text);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Сталася помилка при обробці пошуку товару.");
        }
    }

    // 🔹 Надсилаємо деталі останнього показаного товару з кнопками
    public void sendProductDetailsWithButtons(Long userId, Map<String, Object> product) {
        String chatId = String.valueOf(userId);

        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "N/A"));
        String category = String.valueOf(product.getOrDefault("category", "❓"));
        String subcategory = String.valueOf(product.getOrDefault("subcategory", "❓"));

        String message = String.format(
                "📦 %s\n💰 Ціна: %s грн за шт\n📂 %s → %s\n\n🔎 Виберіть дію нижче або введіть інший товар для пошуку.",
                name, price, category, subcategory
        );

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🛠 Додати в кошик");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🛒 Переглянути кошик");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔙 Назад");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // ✏️ Початок редагування товару для адміна
    private void handleEditProductStart(Long userId, String chatId, String text) throws TelegramApiException {
        // Зберігаємо ключові слова для цього користувача
        adminSearchKeyword.put(userId, text);

        // Відправляємо меню для вибору джерела пошуку
        sendMessage(showAdminSearchSourceMenu(userId, Long.valueOf(chatId)));

        // Переводимо стан юзера на "choose_search_source"
        userStates.put(userId, "choose_search_source");
    }

    // Вибір товару по списку
    private void handleChooseProduct(Long userId, String chatId, String text) {
        List<Map<String, Object>> matches = adminMatchList.get(userId);
        if (matches == null || matches.isEmpty()) {
            sendText(chatId, "❌ Помилка: список товарів порожній.");
            userStates.remove(userId);
            return;
        }

        text = text.trim();
        List<String> selectedProducts = new ArrayList<>();

        try {
            if (text.contains("-")) {
                // Діапазон, наприклад "1-10"
                String[] parts = text.split("-");
                int start = Integer.parseInt(parts[0].trim()) - 1;
                int end = Integer.parseInt(parts[1].trim()) - 1;

                if (start < 0) start = 0;
                if (end >= matches.size()) end = matches.size() - 1;

                for (int i = start; i <= end; i++) {
                    selectedProducts.add((String) matches.get(i).get("name"));
                }
            } else {
                // Одиночні номери, через пробіл або кому
                String[] numbers = text.split("[,\\s]+"); // "1 3 5" або "1,3,5"
                for (String numberStr : numbers) {
                    int index = Integer.parseInt(numberStr.trim()) - 1;
                    if (index >= 0 && index < matches.size()) {
                        selectedProducts.add((String) matches.get(index).get("name"));
                    }
                }
            }

            if (selectedProducts.isEmpty()) {
                sendText(chatId, "❌ Немає валідних номерів для редагування.");
                return;
            }

            // Перший товар для сумісності зі старим кодом
            adminEditingProduct.put(userId, selectedProducts.get(0));

            // Зберігаємо весь список для масового редагування
            adminSelectedProductsRange.put(userId, selectedProducts);

            userStates.put(userId, "editing");
            adminMatchList.remove(userId);

            // Викликаємо меню редагування
            sendMessage(createEditMenu(chatId, userId));

        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Будь ласка, введіть номер або діапазон у форматі '1-10', або через пробіл/кому.");
        }
    }

    // 🔧 Редагування товару
    private void handleEditing(Long userId, String chatId, String text) {
        // Отримуємо список обраних товарів для масового редагування
        List<String> productsToEdit = adminSelectedProductsRange.get(userId);
        String singleProduct = adminEditingProduct.get(userId);

        switch (text) {
            case "✏️ Назву":
                adminEditingField.put(userId, "name");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "Введіть нову назву товару:");
                break;

            case "💰 Ціну":
                adminEditingField.put(userId, "price");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "Введіть нову ціну:");
                break;

            case "📖 Опис":
                adminEditingField.put(userId, "description");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "Введіть новий опис:");
                break;

            case "🗂️ Додати в підкатегорію":
                userStates.put(userId, "awaiting_subcategory");
                sendText(chatId, "✏️ Введіть назву підкатегорії, куди хочете додати товар:");
                break;

            case "🖼️ Додати фотографію":
                System.out.println("[DEBUG] Button 'Add Photo' clicked by userId=" + userId);
                if (productsToEdit != null && productsToEdit.size() > 1) {
                    sendText(chatId, "⚠️ Масове додавання фотографій не підтримується. Виберіть один товар.");
                } else if (singleProduct != null) {
                    startPhotoUpload(userId, chatId, singleProduct);
                } else {
                    sendText(chatId, "⚠️ Будь ласка, спочатку виберіть товар.");
                }
                break;

            case "📏 Одиниця виміру":
                adminEditingField.put(userId, "unit");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "Введіть одиницю виміру для товару (шт або метр):");
                break;

            case "🏭 Виробник":
                adminEditingField.put(userId, "manufacturer");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "✏️ Введіть назву виробника для товару (або ❌ щоб видалити):");
                break;

            default:
                sendText(chatId, "Невідома опція редагування.");
                break;
        }

        // Залишаємо користувача в меню редагування для подальших змін
        sendMessage(createEditMenu(chatId, userId));
    }

    // 📝 Очікування значення для редагування
    private void handleAwaitingField(Long userId, String chatId, String newValue) {
        String field = adminEditingField.get(userId);
        if (field == null) return;

        List<String> productsToEdit = adminSelectedProductsRange.get(userId);

        // Фото масово редагувати не можна
        if ("photo".equals(field)) {
            if (productsToEdit != null && !productsToEdit.isEmpty()) {
                sendText(chatId, "⚠️ Масове додавання фото не підтримується. Виберіть один товар для фотографії.");
            } else {
                String productName = adminEditingProduct.get(userId);
                if (productName != null) {
                    startPhotoUpload(userId, chatId, productName);
                }
            }
            sendMessage(createEditMenu(chatId, userId));
            adminEditingField.remove(userId);
            userStates.put(userId, "editing");
            return;
        }

        // Масове або одиночне оновлення
        if (productsToEdit != null && !productsToEdit.isEmpty()) {
            for (String productName : productsToEdit) {
                CatalogEditor.updateField(productName, field, newValue);
            }
            sendText(chatId, "✅ Поле '" + field + "' успішно оновлено для всіх "
                    + productsToEdit.size() + " товарів у вибраному діапазоні.");
        } else {
            String productName = adminEditingProduct.get(userId);
            if (productName != null) {
                CatalogEditor.updateField(productName, field, newValue);
                sendText(chatId, "✅ Поле '" + field + "' успішно оновлено для товару: " + productName);
            }
        }

        // Повертаємо користувача в меню редагування
        sendMessage(createEditMenu(chatId, userId));

        // Очищаємо поле редагування, залишаємо список товарів
        adminEditingField.remove(userId);
        userStates.put(userId, "editing");
    }

    // ⭐ Додавання хіта продажу
    private void handleAddHit(Long userId, String chatId, String text) {
        hitItems.add("⭐ " + text);
        userStates.remove(userId);
        sendText(chatId, "Товар додано до хітів продажу!");
    }

    private void handleAddCategory(Long userId, String chatId, String text) {
        adminNewCategory.put(userId, text); // зберігаємо назву нової категорії
        userStates.put(userId, "add_subcategory");
        sendText(chatId, "✏️ Введіть назву підкатегорії для категорії '" + text + "' (можна пропустити, залишивши пустим):");
    }

    private void handleAddSubcategory(Long userId, String chatId, String subcategoryName) {
        String categoryName = adminNewCategory.get(userId);
        if (categoryName == null) {
            sendText(chatId, "❌ Сталася помилка. Спробуйте ще раз.");
            userStates.remove(userId);
            return;
        }

        // Додаємо категорію у CatalogEditor
        boolean catAdded = CatalogEditor.addCategory(categoryName);
        if (!catAdded) {
            sendText(chatId, "⚠️ Категорія вже існує: " + categoryName);
        }

        // Додаємо підкатегорію, якщо назва підкатегорії не порожня
        if (subcategoryName != null && !subcategoryName.isEmpty()) {
            boolean subAdded = CatalogEditor.addSubcategory(categoryName, subcategoryName);
            if (!subAdded) {
                sendText(chatId, "⚠️ Підкатегорія вже існує: " + subcategoryName);
            }
        }

        sendText(chatId, "✅ Категорія та підкатегорія додані у каталог:\nКатегорія: " + categoryName +
                (subcategoryName.isEmpty() ? "" : "\nПідкатегорія: " + subcategoryName));

        adminNewCategory.remove(userId);
        userStates.remove(userId);
    }

    private void handleAddToSubcategory(Long userId, String chatId, String subcategoryName) {
        String productName = adminEditingProduct.get(userId);
        if (productName == null) {
            sendText(chatId, "❌ Error: No product selected to add to the subcategory.");
            userStates.remove(userId);
            return;
        }

        System.out.println("INFO: Adding product '" + productName + "' to subcategory '" + subcategoryName + "'");

        // --- Get price from YAML
        double price = CatalogEditor.getProductPriceFromYAML(productName);
        if (price <= 0.0) {
            System.out.println("DEBUG: Price <= 0, setting default 0.0");
            price = 0.0;
        }

        // --- Check subcategory
        if (!CatalogEditor.subcategoryExists(subcategoryName)) {
            sendText(chatId, "❌ Subcategory '" + subcategoryName + "' not found in MySQL database.");
            userStates.remove(userId);
            return;
        }

        // --- Add product
        boolean success = CatalogEditor.addProductToSubcategory(productName, price, subcategoryName);

        if (success) {
            sendText(chatId, "✅ Product '" + productName + "' added to subcategory '" + subcategoryName + "'!");
        } else {
            sendText(chatId, "❌ Failed to add product '" + productName +
                    "' to subcategory '" + subcategoryName + "'. It might already exist.");
        }

        userStates.remove(userId);
    }

    private void handleChooseCategoryForSub(Long userId, String chatId, String categoryName) {
        // Перевіряємо, чи існує така категорія
        if (!CatalogEditor.categoryExists(categoryName)) {
            sendText(chatId, "❌ Категорію '" + categoryName + "' не знайдено. Перевірте назву.");
            return;
        }

        // Зберігаємо вибір і просимо ввести нову підкатегорію
        adminNewCategory.put(userId, categoryName);
        userStates.put(userId, "add_new_subcategory");
        sendText(chatId, "✏️ Введіть назву нової підкатегорії для категорії '" + categoryName + "':");
    }

    private void handleAddNewSubcategory(Long userId, String chatId, String subcategoryName) {
        String categoryName = adminNewCategory.get(userId);
        if (categoryName == null || subcategoryName.isEmpty()) {
            sendText(chatId, "❌ Сталася помилка. Спробуйте ще раз.");
            userStates.remove(userId);
            return;
        }

        boolean added = CatalogEditor.addSubcategory(categoryName, subcategoryName);

        if (added) {
            sendText(chatId, "✅ Підкатегорію '" + subcategoryName + "' додано до категорії '" + categoryName + "'.");
        } else {
            sendText(chatId, "❌ Не вдалося додати підкатегорію '" + subcategoryName + "'. Можливо, вона вже існує.");
        }

        adminNewCategory.remove(userId);
        userStates.remove(userId);
    }

    private void handleCategoryManagementState(Long userId, String chatId, String text) {
        switch (text) {
            case "➕ Додати категорію" -> {
                userStates.put(userId, "add_category"); // тут запускається твій handleAddCategory
                sendText(chatId, "✏️ Введіть назву нової категорії:");
            }
            case "➕ Додати підкатегорію" -> {
                userStates.put(userId, "choose_category_for_sub");
                sendText(chatId, "📂 Введіть назву категорії, до якої хочете додати нову підкатегорію:");
            }
            case "✏️ Змінити назву категорії" -> {
                userStates.put(userId, "rename_category_select");
                sendText(chatId, "✏️ Введіть назву категорії, яку хочете змінити:");
            }
            case "🗑️ Видалити категорію" -> {
                userStates.put(userId, "delete_category_select");
                sendText(chatId, "🗑️ Введіть назву категорії, яку хочете видалити:");
            }
            case BACK_BUTTON -> {
                userStates.remove(userId);
                sendMessage(createAdminMenu(chatId));
            }
            default -> sendText(chatId, "🤖 Не зрозумів команду. Спробуйте ще раз.");
        }
    }

    private void handleDeleteCategorySelect(Long userId, String chatId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            sendText(chatId, "❌ Помилка: назва категорії не може бути порожньою.");
            userStates.remove(userId);
            return;
        }

        boolean removed = CatalogEditor.deleteCategory(categoryName);
        if (removed) {
            sendText(chatId, "✅ Категорія '" + categoryName + "' успішно видалена!");
        } else {
            sendText(chatId, "❌ Категорія '" + categoryName + "' не знайдена. Перевірте назву.");
        }

        userStates.remove(userId);
    }

    private void handleChooseYamlProduct(Long userId, String chatId, String text) {
        List<Map<String, Object>> matches = adminMatchList.get(userId); // список знайдених YAML товарів
        if (matches == null || matches.isEmpty()) {
            sendText(chatId, "❌ Помилка: список товарів порожній.");
            userStates.remove(userId);
            return;
        }

        try {
            int index = Integer.parseInt(text.trim()) - 1;
            if (index < 0 || index >= matches.size()) {
                sendText(chatId, "❌ Некоректний номер. Спробуйте ще раз.");
                return;
            }

            Map<String, Object> selectedProduct = matches.get(index);
            String selectedProductName = (String) selectedProduct.get("name");
            adminEditingProduct.put(userId, selectedProductName); // зберігаємо тільки назву

            // 🟢 Встановлюємо стан YAML-редагування
            userStates.put(userId, "yaml_edit_menu");
            adminMatchList.remove(userId);

            // Відправляємо обмежене меню для YAML
            try {
                execute(createYamlEditMenu(chatId, selectedProductName));
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(chatId, "❌ Помилка при відправці YAML меню.");
            }

        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Будь ласка, введіть номер із списку.");
        }
    }

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

        // 🚫 Перевірка на локальні та blob-посилання
        if (imageUrl.startsWith("blob:") || imageUrl.startsWith("file://") || imageUrl.matches("^[a-zA-Z]:\\\\.*")) {
            sendText(chatId, "❌ Локальні або blob-посилання не підтримуються. Надішліть URL зображення з інтернету.");
            return;
        }

        // ✅ Перевірка на HTTP/HTTPS
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

        userStates.remove(userId);
        adminEditingProduct.remove(userId);
    }

    // Завантаження каталогу у плоский список
    private List<Map<String, Object>> loadCatalogFlat() {
        try {
            CatalogSearcher cs = new CatalogSearcher();
            List<Map<String, Object>> allProducts = new ArrayList<>();

            // Беремо всі категорії
            for (String cat : cs.getCategories()) {
                // Беремо всі підкатегорії
                for (String sub : cs.getSubcategories(cat)) {
                    // Додаємо товари в список
                    allProducts.addAll(cs.getProducts(cat, sub));
                }
            }

            return allProducts;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Відправити меню користувача
    public void sendUserMenu(Long userId) throws TelegramApiException {
        SendMessage menu = createUserMenu(String.valueOf(userId), userId);
        execute(menu); // приватний метод execute вже доступний тут
    }

    // Меню користовувача
    public SendMessage createUserMenu(String chatId, Long userId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🧱 Каталог товарів");
        row1.add("🔍 Пошук товару");
        row1.add("📋 Кошик");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🎯 Хіт продажу");
        row2.add("📍 Адреси та Контакти");
        row2.add("\uD83C\uDF10 Соц-мережі");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("💬 Залишити відгук");
        row3.add("💬 Допомога");
        keyboard.add(row3);

        if (ADMINS.contains(userId)) {
            KeyboardRow adminRow = new KeyboardRow();
            adminRow.add("⚙️ Продавца меню");
            keyboard.add(adminRow);
        }

        if (DEVELOPERS.contains(userId)) {
            KeyboardRow devRow = new KeyboardRow();
            devRow.add("👨‍💻 Меню розробника");
            keyboard.add(devRow);
        }

        markup.setKeyboard(keyboard);
        return SendMessage.builder()
                .chatId(chatId)
                .text("👋 Знову привіт, друже!\n" +
                        "Я, Митрофан 🤖, готовий допомогти тобі:\n" +
                        "🧱 Обирай каталог, 🔍 шукай товари, 🧺 переглядай кошик або ⭐ дивись хіти продажів.\n\n" +
                        "🫶 Робимо покупки швидкими, зручними та приємними!")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createAdminMenu(String chatId) {
        SendMessage msg = new SendMessage(chatId, "🔐 Адмін-панель:");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("🛒 Замовлення користувачів"));
        r1.add(new KeyboardButton("💬 Відповісти покупцю")); // <-- нова кнопка

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton(BACK_BUTTON));
        kb.setKeyboard(List.of(r1, r2));

        msg.setReplyMarkup(kb);
        return msg;
    }

    private SendMessage createDeveloperMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 Оновити каталог");
        row1.add("✏️ Редагувати товар");
        row1.add("Редагувати категорії");
        row1.add("⭐ Додати товар у Хіт продажу");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔗 Запрошувальні посилання");
        row2.add("📜 Логирування");
        row2.add("📝 Список онови");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("💬 Відгуки користувачів");
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text("👨‍💻 Меню розробника, оберіть дію:")
                .replyMarkup(markup)
                .build();
    }

    // Меню оновлення каталога
    private SendMessage createDeveloperCatalogMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // 🔄 Оновити каталог (.yml) і 💰 Оновити ціни (.yml)
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 Оновити каталог (.yml)");
        row1.add("💰 Оновити ціни (.yml)");
        keyboard.add(row1);

        // ✏️ Оновити ціну по назві товару + назад
        KeyboardRow row2 = new KeyboardRow();
        row2.add("✏️ Оновити ціну по назві товару");
        row2.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row2);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text("👨‍💻 Меню оновлення каталогу — оберіть дію:")
                .replyMarkup(markup)
                .build();
    }

    // Меню в пошуку товару
    public void sendProductWithAddToCartRow(Long userId, String chatId, String productText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(productText);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        row.add(ADD_TO_CART_BUTTON);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);
        row2.add(VIEW_CART_BUTTON);

        keyboard.setKeyboard(List.of(row, row2));
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage createEditMenu(String chatId, Long userId) {
        List<String> productsToEdit = adminSelectedProductsRange.get(userId);
        String menuTitle;

        if (productsToEdit != null && !productsToEdit.isEmpty()) {
            menuTitle = "Редагуємо " + productsToEdit.size() + " товарів. Поточний: " + productsToEdit.get(0);
        } else {
            String productName = adminEditingProduct.get(userId);
            menuTitle = "Редагування товару: " + (productName != null ? productName : "не вибрано");
        }

        SendMessage msg = new SendMessage(chatId, menuTitle);

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("✏️ Назву"));
        r1.add(new KeyboardButton("💰 Ціну"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("📖 Опис"));
        r2.add(new KeyboardButton("🗂️ Додати в підкатегорію"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("🖼️ Додати фотографію"));
        r3.add(new KeyboardButton("📏 Одиниця виміру"));

        KeyboardRow r4 = new KeyboardRow();
        r4.add(new KeyboardButton("🏭 Виробник"));
        r4.add(new KeyboardButton(BACK_BUTTON));

        kb.setKeyboard(List.of(r1, r2, r3, r4));
        msg.setReplyMarkup(kb);

        return msg;
    }

    private SendMessage createCategoryAdminMenu(String chatId) {
        SendMessage msg = new SendMessage(chatId, "🔧 Редагування категорій:");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("➕ Додати категорію"));// стартує стан add_category
        r1.add(new KeyboardButton("➕ Додати підкатегорію"));
        r1.add(new KeyboardButton("✏️ Змінити назву категорії"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("🗑️ Видалити категорію"));
        r2.add(new KeyboardButton(BACK_BUTTON));

        kb.setKeyboard(List.of(r1, r2));
        msg.setReplyMarkup(kb);
        return msg;
    }

    private SendMessage createHelpMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("✉️ Написати консультанту");
        row1.add("💌 Відповіді");
        keyboard.add(row1);
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        return SendMessage.builder()
                .chatId(chatId)
                .text("📖 Виберіть один із пунктів для отримання допомоги:\n\n" +
                        "✉️ *Написати консультанту* – Задайте своє питання і отримайте професійну консультацію.\n" +
                        "💌 *Відповіді* – Перегляньте всі відповіді консультантів.")
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createInvitesMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Додати запрошення");
        row1.add("✏️ Редагувати запрошення");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🗑️ Видалити запрошення");
        row2.add("📄 Показати всі запрошення");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return SendMessage.builder()
                .chatId(chatId)
                .text("🔗 Меню запрошувальних посилань:")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createLogsMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📊 Статистика запрошувань");
        row1.add("📊 Статистика без запрошень");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📦 Замовлення");
        row2.add("🔎 Перегляд повідомленней від покупців");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return SendMessage.builder()
                .chatId(chatId)
                .text("📜 Меню логування:")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createFeedbackMenu(String chatId, String userId, String feedbackText) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📩 Відповісти на відгук");
        row1.add("💾 Зберегти відгук");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🗑️ Видалити відгук");
        row2.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row2);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Відгук користувача " + userId + ":\n\n" + feedbackText + "\n\nОберіть дію:")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createYamlEditMenu(String chatId, String productName) {
        SendMessage msg = new SendMessage(chatId, "Редагування товару: " + productName);
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("🗂️ Додати в підкатегорію"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton(BACK_BUTTON));

        kb.setKeyboard(List.of(r1, r2));
        msg.setReplyMarkup(kb);
        return msg;
    }

    public void sendText(String chatId, String text) {
        int maxLength = 4000;
        try {
            for (int start = 0; start < text.length(); start += maxLength) {
                int end = Math.min(start + maxLength, text.length());
                SendMessage msg = new SendMessage(chatId, text.substring(start, end));
                msg.setParseMode("HTML");
                execute(msg);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextWithMarkup(Long chatId, String text, ReplyKeyboardMarkup markup) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);
        execute(message); // метод execute від TelegramLongPollingBot
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\u00A0\\s]+", " ").trim().toLowerCase();
    }

    // 🔹 Обробка текстових кнопок
    private void handleText(Long chatId, String text) throws TelegramApiException {
        text = text.trim();
        System.out.println("[handleText] User " + chatId + " sent: " + text);

        // 🔹 1. Кнопка Назад
        if (text.equals("⬅️ Назад")) {
            System.out.println("[handleText] Back button pressed");
            handleBack(String.valueOf(chatId));
            return;
        }

        // 🔹 2. Кнопка Перейти в кошик
        if (text.equals("🛍️ Перейти в кошик")) {
            System.out.println("[handleText] Open cart button pressed");
            userStates.remove(chatId); // 🔹 Очищуємо стан пошуку
            showCart(chatId);
            return;
        }

        // 🔹 3. Кнопка Очистити кошик
        if (text.equals("🧹 Очистити кошик")) {
            System.out.println("[handleText] Clear cart button pressed");
            userStates.remove(chatId); // 🔹 Вихід із пошуку
            clearCart(chatId);
            return;
        }

        // 🔹 4. Кнопка Додати в кошик
        if (text.equals("🛠 Додати в кошик")) {
            System.out.println("[handleText] Add to cart button pressed");
            addToCartTool(chatId);
            return;
        }

        // 🔹 5. Категорії
        List<String> categories = catalogSearcher.getCategories();
        if (categories.contains(text)) {
            System.out.println("[handleText] Category selected: " + text);
            currentCategory.put(chatId, text);
            currentSubcategory.remove(chatId);
            sendSubcategories(chatId, text);
            return;
        }

        // 🔹 6. Підкатегорії
        if (currentCategory.containsKey(chatId)) {
            String cat = currentCategory.get(chatId);
            List<String> subcats = catalogSearcher.getSubcategories(cat);

            if (subcats.contains(text)) {
                System.out.println("[handleText] Subcategory selected: " + text);
                currentSubcategory.put(chatId, text);
                productIndex.put(chatId, 0);
                sendProduct(chatId);
                return;
            }
        }

        // 🔹 7. Якщо нічого не підійшло — повідомлення
        sendText(chatId, "Невідома команда 😅 Натисніть /start або виберіть із меню.");
    }

    // --- Допоміжні методи для надсилання повідомлень ---
    private void sendText(Long chatId, String text) {
        sendText(chatId.toString(), text);
    }

    private void sendMessage(Long chatId, String text, ReplyKeyboardMarkup markup) {
        sendMessage(chatId.toString(), text, markup);
    }

    private void sendMessage(String chatId, String text, ReplyKeyboardMarkup markup) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(markup)
                    .build();
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // --- Показ підкатегорій ---
    private void sendSubcategories(Long chatId, String categoryName) {
        try {
            Connection conn = DatabaseManager.getConnection(); // одне постійне підключення

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT s.name FROM subcategories s " +
                            "JOIN categories c ON s.category_id = c.id " +
                            "WHERE c.name = ? ORDER BY s.id")) {

                stmt.setString(1, categoryName);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<String> subcategories = new ArrayList<>();
                    while (rs.next()) {
                        subcategories.add(rs.getString("name"));
                    }

                    if (subcategories.isEmpty()) {
                        sendText(chatId, "❌ У цій категорії немає підкатегорій.");
                        return;
                    }

                    ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                            .resizeKeyboard(true)
                            .keyboard(buildKeyboard(subcategories, true))
                            .build();

                    sendMessage(chatId, "📁 Виберіть підкатегорію:", markup);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Помилка при отриманні підкатегорій із бази.");
        }
    }

    // 🔹 Показ товару
    private void sendProduct(Long chatId) throws TelegramApiException {
        String category = currentCategory.get(chatId);
        String subcategory = currentSubcategory.get(chatId);

        System.out.println("\n==============================");
        System.out.println("DEBUG: sendProduct() called for chatId = " + chatId);
        System.out.println("DEBUG: Category = " + category + ", Subcategory = " + subcategory);

        int index = productIndex.getOrDefault(chatId, 0);

        CatalogSearcher searcher = new CatalogSearcher();
        List<Map<String, Object>> products = searcher.getProducts(category, subcategory);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ У цій підкатегорії немає товарів.");
            System.out.println("DEBUG: No products found for category = " + category + ", subcategory = " + subcategory);
            return;
        }

        // 🔢 Сортуємо товари по id
        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        if (index >= products.size() || index < 0) index = 0;

        Map<String, Object> product = products.get(index);
        lastShownProduct.put(chatId, product);

        // 🧩 Безпечне читання даних із мапи
        String name = safeToString(product.get("name"), "Без назви");
        String price = safeToString(product.get("price"), "N/A");
        String unit = safeToString(product.get("unit"), "шт");
        String description = safeToString(product.get("description"), "");
        String photo = safeToString(product.get("photo"), "");

        // 🏭 Виробник — з урахуванням можливого типу BLOB або Blob
        String manufacturer = "";
        Object manufacturerObj = product.get("manufacturer");
        try {
            if (manufacturerObj instanceof byte[] bytes) {
                manufacturer = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } else if (manufacturerObj instanceof java.sql.Blob blob) {
                manufacturer = new String(blob.getBytes(1, (int) blob.length()), java.nio.charset.StandardCharsets.UTF_8);
            } else if (manufacturerObj != null) {
                manufacturer = String.valueOf(manufacturerObj);
            }
        } catch (Exception e) {
            System.err.println("❌ Error reading manufacturer: " + e.getMessage());
        }

        System.out.println("DEBUG: Showing product -> " + name);
        System.out.println("DEBUG: Manufacturer = " + manufacturer);
        System.out.println("DEBUG: Photo = " + photo);
        System.out.println("DEBUG: Description = " + description);
        System.out.println("DEBUG: Price = " + price + ", Unit = " + unit);

        // 🧾 Формування повідомлення
        StringBuilder sb = new StringBuilder("📦 ").append(name)
                .append("\n💰 Ціна: ").append(price).append(" грн за ").append(unit);

        if (!manufacturer.isEmpty() && !"null".equalsIgnoreCase(manufacturer.trim())) {
            sb.append("\n🏭 Виробник: ").append(manufacturer);
        }

        if (!description.isEmpty() && !"null".equalsIgnoreCase(description.trim())) {
            sb.append("\n📖 ").append(description);
        }

        // 🧭 Кнопки
        KeyboardRow row1 = new KeyboardRow();
        row1.add("➡ Далі");
        row1.add("🛒 Додати в кошик");
        row1.add("🛍️ Перейти в кошик");

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        // 🖼️ Відправка контенту
        if (photo != null && !photo.isEmpty() && !"null".equalsIgnoreCase(photo.trim())) {
            sendPhotoFromResources(chatId.toString(), photo, sb.toString(), markup);
            System.out.println("DEBUG: Sent product with photo");
        } else {
            sendTextWithMarkup(chatId, sb.toString(), markup);
            System.out.println("DEBUG: Sent product without photo");
        }

        // 🔁 Оновлюємо індекс для наступного товару
        index = (index + 1) % products.size();
        productIndex.put(chatId, index);

        System.out.println("DEBUG: Product index updated to " + index);
        System.out.println("==============================\n");
    }

    // 🔸 Допоміжний метод — безпечне перетворення в String
    private String safeToString(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String str = String.valueOf(value);
        return ("null".equalsIgnoreCase(str)) ? defaultValue : str;
    }

    private void sendPhoto(String chatId, String fileName, String caption) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("images/" + fileName);

            if (is == null) {
                System.out.println("[PHOTO] Файл не знайдено: " + fileName);
                sendText(chatId, "❌ Фото не знайдено.");
                return;
            }

            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);

            // Створюємо InputFile з InputStream
            InputFile inputFile = new InputFile(is, fileName);
            photo.setPhoto(inputFile);

            photo.setCaption(caption);

            execute(photo);
            System.out.println("[PHOTO] Фото успішно надіслано: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "❌ Сталася помилка при відправці фото.");
        }
    }

    private void createOrderAdminMenu(String chatId, Map<String, Object> order, Long userId) {
        StringBuilder sb = new StringBuilder();

        sb.append("🆔 User ID: ").append(userId).append("\n")
                .append("🔢 Код замовлення: ").append(order.getOrDefault("orderCode", "-")).append("\n")
                .append("📦 Тип замовлення: ").append(order.getOrDefault("deliveryType", "Невідомо")).append("\n\n");

        String deliveryType = (String) order.get("deliveryType");
        if ("Самовивіз".equals(deliveryType)) {
            sb.append("🏙 Місто: ").append(order.getOrDefault("city", "-")).append("\n");
        } else if ("Доставка по місту".equals(deliveryType)) {
            sb.append("🏠 Адреса: ").append(order.getOrDefault("address", "-")).append("\n");
        } else if ("Нова пошта".equals(deliveryType)) {
            sb.append("📮 Відділення НП: ").append(order.getOrDefault("postOffice", "-")).append("\n");
        }

        sb.append("👤 П.І.: ").append(order.getOrDefault("fullName", "-")).append("\n")
                .append("📞 Телефон: ").append(order.getOrDefault("phone", "-")).append("\n")
                .append("💳 Картка: ").append(order.getOrDefault("card", "-")).append("\n\n");

        // Вивід товарів
        String itemsStr = (String) order.get("item");
        if (itemsStr != null && !itemsStr.isEmpty()) {
            String[] itemArr = itemsStr.split(";");
            int i = 1;
            for (String s : itemArr) {
                if (s.isBlank()) continue;
                String[] pair = s.split(":");
                String name = pair[0];
                double price = 0;
                try {
                    if (pair.length > 1) price = Double.parseDouble(pair[1]);
                } catch (Exception ignored) {}
                sb.append(i++).append(". 🛒 ").append(name).append(" — ").append(price).append(" грн\n");
            }
        }

        double total = 0.0;
        Object totalObj = order.get("total");
        if (totalObj instanceof Number) total = ((Number) totalObj).doubleValue();
        else if (totalObj != null) {
            try { total = Double.parseDouble(totalObj.toString()); } catch (Exception ignored) {}
        }
        sb.append("\n💰 Всього: ").append(total).append(" грн");

        // 🔹 Кнопки
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("✅ Підтвердити");
        row1.add("❌ Відхилити");
        row1.add("🗑️ Видалити замовлення");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("⏮️ Назад");
        row2.add("⏭️ Дальше");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(sb.toString());
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "❌ Помилка при відправці повідомлення адміну.");
        }
    }

    // Допоміжний метод для створення клавіатури відгуку
    private ReplyKeyboardMarkup buildFeedbackKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("✉️ Відповісти на відгук");
        row1.add("💾 Зберегти відгук");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🧹 Видалити відгук");
        row2.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

    // Метод створення меню для вибору джерела пошуку
    private SendMessage showAdminSearchSourceMenu(Long userId, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🔹 Виберіть джерело пошуку:");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔍 Пошук у MySQL");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Пошук у YAML");

        markup.setKeyboard(List.of(row1, row2));
        message.setReplyMarkup(markup);

        return message;
    }

    private ReplyKeyboardMarkup getSearchKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true); // робить клавіатуру зручною під мобільний
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        // перший ряд — кнопки для товарів
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🛠 Додати в кошик");
        row1.add("🛍️ Перейти в кошик");
        rows.add(row1);

        // другий ряд — кнопка назад
        KeyboardRow row2 = new KeyboardRow();
        row2.add("⬅️ Назад");
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void handleAdminSearchSource(Long userId, String chatId, String text) throws TelegramApiException {
        if ("🔍 Пошук у MySQL".equals(text)) {
            adminSearchSource.put(userId, "mysql");
            userStates.put(userId, "awaiting_search");
            sendText(chatId, "Введіть ключові слова для пошуку у MySQL:");
        } else if ("🔍 Пошук у YAML".equals(text)) {
            adminSearchSource.put(userId, "yaml");
            userStates.put(userId, "awaiting_search");
            sendText(chatId, "Введіть ключові слова для пошуку у YAML:");
        } else {
            sendText(chatId, "❌ Невідома опція. Спробуйте ще раз.");
            sendMessage(showAdminSearchSourceMenu(userId, Long.valueOf(chatId))); // ще раз показуємо меню
        }
    }

    private void handleAdminSearchInput(Long userId, String chatId, String text) throws TelegramApiException {
        List<Map<String, Object>> results = new ArrayList<>();
        CatalogSearcher searcher = new CatalogSearcher();
        String source = adminSearchSource.getOrDefault(userId, "mysql"); // обране джерело

        if ("mysql".equals(source)) {
            // пошук у MySQL
            results = searcher.searchByKeywordsAdmin(text);
        } else if ("yaml".equals(source)) {
            try {
                results = CatalogUpdater.searchProductsSimple(text); // пошук у YAML
            } catch (Exception e) {
                sendText(chatId, "❌ Помилка при пошуку у YAML: " + e.getMessage());
                return;
            }
        }

        if (results.isEmpty()) {
            sendText(chatId, "❌ Товар не знайдено: " + text);
            return;
        }

        // --- Зберігаємо результати для вибору ---
        adminMatchList.put(userId, results);

        // --- Формуємо список для відправки адміну ---
        StringBuilder sb = new StringBuilder("🔎 Знайдено товари. Введіть номер для редагування:\n\n");
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> prod = results.get(i);
            sb.append(i + 1).append(". ").append(prod.get("name"));
            if (prod.get("price") != null) sb.append(" | Ціна: ").append(prod.get("price"));
            sb.append("\n");
        }

        sendText(chatId, sb.toString());
        userStates.put(userId, "choose_product"); // стан очікування введення номера
    }

    // Головний метод створення меню відгуку
    private SendMessage createFeedbackSubMenu(String chatId, Long targetUserId) {
        ReplyKeyboardMarkup markup = buildFeedbackKeyboard();

        // Отримуємо останній відгук цього користувача
        List<String> feedbacks = FeedbackManager.getAllFeedbacks().get(targetUserId);
        String feedbackText = (feedbacks != null && !feedbacks.isEmpty())
                ? feedbacks.get(feedbacks.size() - 1)
                : "❌ Відгуків немає.";

        // Зберігаємо, щоб знати, кому відповідає адмін
        adminReplyTarget.put(Long.valueOf(chatId), targetUserId);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Відгук користувача " + targetUserId + ":\n\n" + feedbackText + "\n\nОберіть дію:")
                .replyMarkup(markup)
                .build();
    }

    private void showAdminOrder(Long adminId, String chatId) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // Беремо всі активні замовлення
            String sql = "SELECT * FROM orders WHERE status != 'Видалено' ORDER BY id ASC";
            List<Map<String, Object>> orders = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();
                    order.put("id", rs.getInt("id"));
                    order.put("orderCode", rs.getString("orderCode"));
                    order.put("userId", rs.getLong("userId"));
                    order.put("deliveryType", rs.getString("deliveryType"));
                    order.put("city", rs.getString("city"));
                    order.put("address", rs.getString("address"));
                    order.put("postOffice", rs.getString("postOffice"));
                    order.put("fullName", rs.getString("fullName"));
                    order.put("phone", rs.getString("phone"));
                    order.put("card", rs.getString("card"));
                    order.put("status", rs.getString("status"));
                    order.put("date", rs.getDate("date"));
                    order.put("item", rs.getString("item"));

                    Object totalObj = rs.getObject("total");
                    double total = 0;
                    if (totalObj instanceof Number) total = ((Number) totalObj).doubleValue();
                    else if (totalObj != null) {
                        try { total = Double.parseDouble(totalObj.toString()); } catch (Exception ignored) {}
                    }
                    order.put("total", total);

                    orders.add(order);
                }
            }

            if (orders.isEmpty()) {
                sendText(chatId, "Замовлень немає.");
                return;
            }

            // Визначаємо який індекс показувати
            int idx = adminOrderIndex.getOrDefault(adminId, 0);
            if (idx >= orders.size()) idx = orders.size() - 1; // щоб не виходило за межі
            Map<String, Object> orderToShow = orders.get(idx);

            // Показуємо адміну
            createOrderAdminMenu(chatId, orderToShow, orderToShow.get("userId") instanceof Long ? (Long) orderToShow.get("userId") : 0L);

        } catch (SQLException e) {
            e.printStackTrace();
            sendText(chatId, "❌ Помилка при завантаженні замовлень з бази.");
        }
    }

    private void sendSearchedProduct(Long chatId) throws TelegramApiException {
        List<Map<String, Object>> results = searchResults.get(chatId);
        int index = productIndex.getOrDefault(chatId, 0);

        if (results == null || results.isEmpty()) {
            sendText(chatId, "❌ Немає результатів пошуку.");
            return;
        }

        if (index >= results.size()) index = 0;
        Map<String, Object> product = results.get(index);
        lastShownProduct.put(chatId, product);

        String name = product.getOrDefault("name", "Без назви").toString();
        String price = product.getOrDefault("price", "N/A").toString();
        String unit = product.getOrDefault("unit", "шт").toString();
        String description = product.getOrDefault("description", "").toString();
        String photoPath = product.getOrDefault("photo", "").toString();
        String category = product.getOrDefault("category", "").toString();
        String subcategory = product.getOrDefault("subcategory", "").toString();

        StringBuilder sb = new StringBuilder("📦 ").append(name)
                .append("\n💰 Ціна: ").append(price).append(" грн за ").append(unit);
        if (!category.isEmpty() || !subcategory.isEmpty()) {
            sb.append("\n📂 ").append(category);
            if (!subcategory.isEmpty()) sb.append(" → ").append(subcategory);
        }
        if (!description.isEmpty()) sb.append("\n📖 ").append(description);

        KeyboardRow row = new KeyboardRow();
        row.add("➡ Далі");
        row.add("🛒 Додати в кошик");

        List<KeyboardRow> kb = new ArrayList<>();
        kb.add(row);
        kb.add(new KeyboardRow(List.of(new KeyboardButton(BACK_BUTTON))));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(kb);

        if (photoPath != null && !photoPath.isEmpty()) {
            String fileName = new java.io.File(photoPath).getName();
            sendPhotoFromResources(chatId.toString(), fileName, sb.toString(), markup);
        } else {
            sendText(chatId.toString(), sb.toString());
        }

        // Показуємо наступний товар
        index = (index + 1) % results.size();
        productIndex.put(chatId, index);
    }

    private void handleUserFeedback(Long userId, String chatId, String text) {
        userStates.remove(userId);

        feedbacks.computeIfAbsent(userId, k -> new ArrayList<>()).add(text);
        sendText(chatId, "✅ Дякуємо за ваш відгук!");

        // Надсилаємо розробникам
        for (Long devId : DEVELOPERS) {
            sendText(devId.toString(), "🆕 Новий відгук від користувача " + userId + ":\n\n" + text);
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        Long devId = callbackQuery.getFrom().getId();

        try {
            if (data.startsWith("reply:")) {
                Long userId = Long.parseLong(data.split(":")[1]);
                replyTargets.put(devId, userId);
                userStates.put(devId, "waiting_for_reply");
                sendText(chatId, "✍️ Напишіть відповідь для користувача " + userId + ":");
            }

            else if (data.startsWith("save:")) {
                sendText(chatId, "✅ Відгук збережено (поки в пам’яті).");
            }

            else if (data.startsWith("delete:")) {
                String[] parts = data.split(":");
                Long userId = Long.parseLong(parts[1]);
                int hash = Integer.parseInt(parts[2]);

                List<String> list = feedbacks.get(userId);
                if (list != null) {
                    list.removeIf(f -> f.hashCode() == hash);
                    if (list.isEmpty()) feedbacks.remove(userId);
                }

                sendText(chatId, "🗑️ Відгук користувача " + userId + " видалено.");
            }
        } catch (Exception e) {
            sendText(chatId, "⚠️ Помилка при обробці дії.");
            e.printStackTrace();
        }
    }

    public void handleFeedbackState(Long userId, String chatId, String text, String state) throws TelegramApiException {
        switch (state) {
            case "waiting_for_feedback": // користувач пише відгук
                FeedbackManager.addFeedback(userId, text);
                sendText(chatId, "✅ Ваш відгук надіслано адміністратору!");
                userStates.remove(userId);
                break;

            case "writing_reply": // адмін пише відповідь
                Long targetUserId = adminReplyTarget.get(userId);
                if (targetUserId != null) {
                    sendText(targetUserId.toString(), "📩 Відповідь від адміністратора:\n" + text);
                    sendText(chatId, "✅ Відповідь надіслана користувачу " + targetUserId);
                } else {
                    sendText(chatId, "❌ Не знайдено користувача для відповіді.");
                }
                userStates.remove(userId);
                adminReplyTarget.remove(userId);
                break;
        }
    }

    private void sendPhotoFromResources(String chatId, String resourceFileName, String caption, ReplyKeyboardMarkup markup) {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setCaption(caption);
            photo.setReplyMarkup(markup);

            if (resourceFileName.startsWith("http://") || resourceFileName.startsWith("https://")) {
                photo.setPhoto(new InputFile(resourceFileName));
                execute(photo);
                System.out.println("[PHOTO] Фото успішно надіслано з URL: " + resourceFileName);
                return;
            }

            String resourcePath = "images/" + resourceFileName;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (is == null) {
                System.out.println("[PHOTO] Фото не знайдено у ресурсах: " + resourcePath);
                sendText(chatId, "❌ Фото не знайдено: " + resourceFileName);
                return;
            }

            photo.setPhoto(new InputFile(is, resourceFileName));
            execute(photo);
            is.close();
            System.out.println("[PHOTO] Фото успішно надіслано з ресурсів: " + resourceFileName);

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "❌ Сталася помилка при відправленні фото.");
        }
    }

    public void handleFeedbackCallback(Update update) throws TelegramApiException {
        String data = update.getCallbackQuery().getData();
        Long adminId = update.getCallbackQuery().getFrom().getId();
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

        if (data.startsWith("reply_")) {
            Long targetUserId = Long.parseLong(data.split("_")[1]);
            adminReplyTarget.put(adminId, targetUserId); // Map<Long, Long>
            userStates.put(adminId, "writing_reply");
            sendText(chatId, "✏️ Напишіть відповідь для користувача " + targetUserId + ":");

        } else if (data.startsWith("save_")) {
            FeedbackManager.saveFeedbacks();
            sendText(chatId, "💾 Відгук збережено у файлі.");

        } else if (data.startsWith("delete_")) {
            Long targetUserId = Long.parseLong(data.split("_")[1]);
            FeedbackManager.removeLastFeedback(targetUserId);
            sendText(chatId, "🧹 Відгук видалено.");
        }
    }

    private void notifyAllActiveUsersAboutHit() {
        for (Long userId : userStates.keySet()) {
            try {
                execute(SendMessage.builder()
                        .chatId(userId.toString())
                        .text("🌟 Новий Хіт продажу!")
                        .build());
            } catch (Exception e) {
                System.out.println("❌ Не вдалося надіслати користувачу " + userId);
            }
        }
    }

    public void startPhotoUpload(Long userId, String chatId, String productName) {
        photoHandler.requestPhotoUpload(userId, chatId, productName);
    }

    private void setState(Long userId, String newState) {
        String current = userState.get(userId);
        if (current != null) {
            previousState.put(userId, current); // зберігаємо попередній
        }

        // мінімальне звернення до previousState, щоб IDE не лаявся
        previousState.size();

        userState.put(userId, newState);
    }

    // --- Доступ до пошукових результатів ---
    public Map<Long, List<Map<String, Object>>> getSearchResults() {
        return searchResults;
    }

    public Map<Long, Map<String, Object>> getLastShownProduct() {
        return lastShownProduct;
    }

    public void showProductDetails(Long userId) {
        Map<String, Object> product = lastShownProduct.get(userId);
        if (product != null) {
            sendProductDetailsWithButtons(userId, product);
        } else {
            System.out.println("[showProductDetails] No last shown product for user " + userId);
        }
    }

    public void handleAddToCart(Long userId) {
        Map<String, Object> product = lastShownProduct.get(userId);
        if (product == null) {
            sendText(userId.toString(), "❌ Товар не знайдено для додавання в кошик.");
            return;
        }

        userCart.computeIfAbsent(userId, k -> new ArrayList<>());
        userCart.get(userId).add(product);

        sendText(userId.toString(), "✅ Товар додано до кошика!");
        System.out.println("[handleAddToCart] User " + userId + " added product: " + product.get("name"));
    }

    private void updateOrInsertProduct(Map<String, Object> productData) {
        String name = (String) productData.get("name");
        double price = Double.parseDouble(productData.get("price").toString());
        String category = (String) productData.getOrDefault("category", "Uncategorized");
        String description = (String) productData.getOrDefault("description", "");

        String updateQuery = "UPDATE products SET price=?, category=?, description=? WHERE name=?";
        String insertQuery = "INSERT INTO products (name, price, category, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {

            var stmt = conn.prepareStatement(updateQuery);
            stmt.setDouble(1, price);
            stmt.setString(2, category);
            stmt.setString(3, description);
            stmt.setString(4, name);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                var insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setString(1, name);
                insertStmt.setDouble(2, price);
                insertStmt.setString(3, category);
                insertStmt.setString(4, description);
                insertStmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating/inserting product: " + name);
            e.printStackTrace();
        }
    }

    private void updateProductPriceInDB(String name, double price) {
        String query = "UPDATE products SET price=? WHERE name=?";

        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, price);
            stmt.setString(2, name);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                System.out.println("⚠️ Товар '" + name + "' не знайдено, ціна не оновлена.");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating price for product: " + name);
            e.printStackTrace();
        }
    }
}