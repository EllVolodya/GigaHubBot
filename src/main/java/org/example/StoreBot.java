package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    private final Map<Long, Boolean> waitingForQuantity = new HashMap<>();
    private final Map<Long, Boolean> waitingForCatalogSearch = new HashMap<>();
    private final Map<Long, Integer> searchIndex = new HashMap<>();
    private final Map<Long, Double> paintArea = new HashMap<>();
    private final Map<Long, Double> paintTemp = new HashMap<>();
    private final Map<Long, Double> paintLayers = new HashMap<>();

    // Для замовлення.
    private final Map<Long, String> tempDeliveryType = new HashMap<>();
    private final Map<Long, String> tempAddress = new HashMap<>();
    private final Map<Long, String> tempFullname = new HashMap<>();
    private final Map<Long, String> tempPhone = new HashMap<>();
    private final Map<Long, String> tempCard = new HashMap<>();
    private final Map<Long, String> tempOrderCode = new HashMap<>();
    private final Map<Long, String> tempOrderItems = new HashMap<>();
    private final Map<Long, Double> tempTotal = new HashMap<>();
    private final Map<Long, Double> tempLength = new HashMap<>();
    private final Map<Long, Double> tempWidth = new HashMap<>();

    // 🔹 Константи WayForPay (заміниш своїми)
    private static final String MERCHANT_ACCOUNT = "YOUR_MERCHANT_ACCOUNT";
    private static final String MERCHANT_SECRET = "YOUR_MERCHANT_SECRET";
    private static final String MERCHANT_DOMAIN = "YOUR_MERCHANT_DOMAIN";

    //Права
    private final List<Long> ADMINS = List.of(620298889L, 1030917576L, 533570832L);// тут айді продавців меню
    private final List<Long> DEVELOPERS = List.of(620298889L, 1030917576L, 533570832L); // тут айді розробників меню
    private final Long CONSULTANT_CHAT_ID = 620298889L;

    // 🔹 Адмінські стани
    private final Map<Long, Boolean> adminConnected = new HashMap<>();

    private final Map<Long, Long> adminReplyTarget = new HashMap<>();

    private final Map<Long, String> adminEditingProduct = new HashMap<>();
    private final Map<Long, List<String>> adminSelectedProductsRange = new HashMap<>();
    private final Map<Long, String> adminEditingField = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> adminMatchList = new HashMap<>();
    private final Map<Long, String> adminNewCategory = new HashMap<>();
    private final Map<Long, List<String>> supportAnswers = new HashMap<>();
    private final Map<Long, Integer> adminOrderIndex = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> adminOrders = new HashMap<>();
    private final Map<Long, String> adminSearchSource = new HashMap<>();  // джерело пошуку для кожного користувача
    private final Map<Long, String> adminSearchKeyword = new HashMap<>(); // ключове слово для пошуку
    private final Map<String, Object> tempStorage = new HashMap<>();

    private final CatalogSearcher catalogSearcher = new CatalogSearcher();
    protected Map<Long, List<Map<String, Object>>> searchResults = new HashMap<>();

    private final Map<Long, Long> consultantReplyTarget = new HashMap<>();

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

    private static StoreBot instance;

    public StoreBot(String botToken) {
        super(botToken);
        instance = this;
    }

    public static StoreBot getInstance() {
        return instance;
    }

    private String getStartParameter(Update update) {
        if (update.getMessage() == null) return null;
        if (update.getMessage().getText() == null) return null;

        String text = update.getMessage().getText().trim();

        // Якщо немає параметра
        if (!text.startsWith("/start")) return null;

        // Якщо просто /start → параметр відсутній
        if (text.equals("/start")) return null;

        // Вирізаємо після /start
        String param = text.replace("/start", "").trim();

        if (param.isEmpty()) return null;

        return param;
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

        Long realChatId = update.getMessage().getChatId();

        String text = update.getMessage().getText() != null
                ? update.getMessage().getText().trim()
                : "";

        String state = userStates.get(userId);

        System.out.println("[DEBUG] Received message from userId=" + userId + ": '" + text + "' (state=" + state + ")");

        String normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cf}\\p{Zs}]+", " ")
                .trim();

        if ("choose_yaml_product".equals(state)) {
            handleChooseYamlProduct(userId, chatId, normalizedText);
            return;
        }

        if (text.startsWith("/start")) {

            try {
                clearUserState(userId);

                Long chatIdLong = update.getMessage().getChatId();
                String chatIdStr = chatIdLong.toString();

                // параметр після /start
                String inviteCode = text.replace("/start", "").trim();

                if (!inviteCode.isEmpty()) {
                    System.out.println("🔥 inviteCode = " + inviteCode);

                    InviteManager inviteManager = new InviteManager();
                    boolean ok = inviteManager.incrementInviteNumber(inviteCode);

                    if (ok) System.out.println("✅ Лічильник збільшено");
                    else    System.out.println("❌ Invite не знайдено");
                }

                // реєстрація
                new UserManager().registerUser(
                        chatIdLong,
                        update.getMessage().getFrom().getFirstName(),
                        chatIdStr
                );

                // меню
                execute(createUserMenu(chatIdStr, userId));

            } catch (SQLException e) {
                e.printStackTrace();
                sendText(update.getMessage().getChatId().toString(),
                        "❌ Сталася помилка бази даних при обробці інвайту.");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(update.getMessage().getChatId().toString(),
                        "❌ Помилка надсилання меню.");
            }
            return; // важливо!
        }

        if (waitingForQuantity.getOrDefault(realChatId, false)) {

            try {
                int quantity = Integer.parseInt(text);

                if (quantity <= 0) {
                    sendText(realChatId, "❌ Кількість має бути більше 0. Введіть ще раз:");
                    return;
                }

                Map<String, Object> product = lastShownProduct.get(realChatId);

                if (product == null) {
                    sendText(realChatId, "❌ Помилка: товар не знайдено.");
                    waitingForQuantity.put(realChatId, false);
                    return;
                }

                Map<String, Object> cartItem = new HashMap<>(product);
                cartItem.put("quantity", quantity);

                userCart.computeIfAbsent(realChatId, k -> new ArrayList<>()).add(cartItem);

                sendText(realChatId,
                        "🧺 Товар додано у кошик!\n\n" +
                                "📦 " + product.get("name") + "\n" +
                                "🔢 Кількість: " + quantity + "\n\n" +
                                "Митрофан радісно поклав товар у твій кошик 😊\n" +
                                "Можеш продовжувати покупки — я поруч!");

                waitingForQuantity.put(realChatId, false);
                return;

            } catch (NumberFormatException e) {
                sendText(realChatId, "❌ Це не число. Введіть кількість ще раз:");
                return;
            }
        }

        if (waitingForCatalogSearch.getOrDefault(realChatId, false)) {

            waitingForCatalogSearch.put(realChatId, false);

            String query = text.toLowerCase();

            // ❗ БЕРЕМО КАТЕГОРІЮ ТІЛЬКИ ПО realChatId (Long)
            String category = currentCategory.get(realChatId);
            String subcategory = currentSubcategory.get(realChatId);

            if (category == null || subcategory == null) {
                sendText(chatId, "❌ Ви не вибрали категорію.");
                return;
            }

            CatalogSearcher searcher = new CatalogSearcher();
            List<Map<String, Object>> all = searcher.getProducts(category, subcategory);

            if (all == null || all.isEmpty()) {
                sendText(chatId, "❌ У цій підкатегорії немає товарів.");
                return;
            }

            List<Map<String, Object>> found = new ArrayList<>();
            for (Map<String, Object> p : all) {
                String name = String.valueOf(p.get("name")).toLowerCase();
                if (name.contains(query)) {
                    found.add(p);
                }
            }

            if (found.isEmpty()) {
                sendText(chatId, "😔 Нічого не знайдено.");
                return;
            }

            // Зберігаємо знайдені товари (ПРИВ’ЯЗАНО ДО ЧАТУ)
            searchResults.put(realChatId, found);
            searchIndex.put(realChatId, 0);

            // Показуємо перший знайдений товар
            try {
                showSearchProduct(chatId);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(chatId, "❌ Помилка при показі знайденого товару.");
            }
            return;
        }

        if ("admin_search_order_code".equals(state)) {

            String code = text.trim();

            if (!code.matches("\\d{1,10}")) {
                sendText(chatId, "❌ Код має складатися тільки з цифр. Спробуйте ще раз:");
                return;
            }

            Map<String, Object> foundOrder = findAdminOrderByCode(code);

            if (foundOrder == null) {
                sendText(chatId, "😔 Замовлення з кодом " + code + " не знайдено.");
            } else {
                // Показуємо знайдене замовлення
                createOrderAdminMenu(chatId, foundOrder, (Long) foundOrder.get("userId"));
            }

            userStates.remove(userId);
            return;
        }

        if ("consultant_reply".equals(userStates.get(CONSULTANT_CHAT_ID))
                && text != null && !text.isBlank()) {

            Long target = consultantReplyTarget.get(CONSULTANT_CHAT_ID);

            // надсилаємо покупцю
            sendText(target, "📩 Відповідь консультанта:\n\n" + text);

            // зберігаємо відповідь у історію
            supportAnswers.computeIfAbsent(target, k -> new ArrayList<>())
                    .add("📨 Відповідь консультанта:\n" + text);

            // повідомляємо консультанта
            sendText(CONSULTANT_CHAT_ID, "✔ Повідомлення надіслано!");

            // очищаємо стан
            userStates.remove(CONSULTANT_CHAT_ID);

            return;
        }

        if ("consultant_reply".equals(userStates.get(CONSULTANT_CHAT_ID))
                && update.getMessage().hasPhoto()) {

            Long target = consultantReplyTarget.get(CONSULTANT_CHAT_ID);

            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize largest = photos.get(photos.size() - 1);
            String fileId = largest.getFileId();

            try {
                SendPhoto ph = new SendPhoto();
                ph.setChatId(target.toString());
                ph.setPhoto(new InputFile(fileId));
                ph.setCaption("📸 Відповідь від консультанта:");

                execute(ph);

                // зберегти в історію як фото
                supportAnswers.computeIfAbsent(target, k -> new ArrayList<>())
                        .add("📸 Відповідь консультанта: [фото]");

                sendText(CONSULTANT_CHAT_ID, "✔ Фото надіслано покупцю!");

            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(CONSULTANT_CHAT_ID, "❌ Помилка при надсиланні фото.");
            }

            // вихід зі стану
            userStates.remove(CONSULTANT_CHAT_ID);
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
                        handleBack(userId, chatId);
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
                case "⬅ Назад" -> showPreviousProduct(userId);
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
                    sendText(chatId, "✏\uFE0F Напишіть своє питання — я передам консультанту.  \n" +
                            "Фото теж можна кидати \uD83D\uDCF8\uD83D\uDE09");
                }
                case "💌 Відповіді" -> {
                    List<String> answers = supportAnswers.get(userId);
                    String reply = (answers == null || answers.isEmpty())
                            ? "Поки що немає відповідей від консультантів."
                            : "💌 Відповіді консультантів:\n\n" + String.join("\n\n", answers);
                    sendText(chatId, reply);
                }
                case "🧮 Будівельний калькулятор" -> {
                    userStates.put(userId, "material_calc_menu");
                    sendMaterialCalcMenu(chatId);
                }


                case "🔍 Пошук товару" -> {
                    userStates.put(userId, "waiting_for_search");

                    sendText(chatId,
                            "🔍 Гаразд, друже! Митрофан вже взяв ліхтарик і пішов шукати між полицями 👷‍♂️✨\n\n" +
                                    "Введи назву товару, модель або навіть те, як ти його називаєш у побуті.\n" +
                                    "Я знайду найближчий варіант — навіть якщо ти напишеш «бойлер», а він у каталозі як «водонагрівач» 😉");
                }

                case "🔍 Пошук" -> {
                    waitingForCatalogSearch.put(userId, true);
                    sendText(chatId, "🔍 Введіть назву товару для пошуку:");
                }

                case "🔍 Пошук коду" -> {
                    userStates.put(userId, "admin_search_order_code");
                    sendText(chatId, "🔍 Введіть код замовлення (наприклад: 9141):");
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

                    sendMessage(chatId, "\uD83D\uDE9A Оберіть спосіб доставки:\n" +
                            "\n" +
                            "\uD83C\uDFEC Самовивіз\n" +
                            "\uD83D\uDCE6 Доставка по місту\n" +
                            "\uD83D\uDCEE Нова Пошта\n" +
                            "\n" +
                            "Митрофан усе підготує та передасть замовлення на відправку максимально швидко \uD83D\uDE09", markup);
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
                                                💳 Бонусна картка (Від магазину)
                                    
                                                📌 Приклад:
                                                Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444
                                            """);
                }

                case "📦 Доставка по місту" -> {
                    tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                    userStates.put(userId, "awaiting_city_delivery");

                    sendText(chatId, """
                                                📝 Введіть, будь-ласка, дані для доставки по місту у форматі:
                                                📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)
                                    
                                                📌 Приклад:
                                                вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444
                                            """);
                }

                case "📮 Доставка Новою поштою" -> {
                    tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                    userStates.put(userId, "awaiting_post_delivery");

                    sendText(chatId, """
                                            📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:
                                            📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)
                                
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

                case "💬 Відповісти покупцю" -> {
                    Long target = consultantReplyTarget.get(CONSULTANT_CHAT_ID);

                    if (target == null) {
                        sendText(CONSULTANT_CHAT_ID, "❌ Немає покупця для відповіді.");
                        return;
                    }

                    userStates.put(CONSULTANT_CHAT_ID, "consultant_reply");

                    sendText(CONSULTANT_CHAT_ID,
                            "✏️ Напишіть відповідь або надішліть фото — я передам покупцю.");
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

                            sendText("" + orderUserId, "\uD83E\uDDF1 Ваше замовлення прийнято!\n" +
                                    "\n" +
                                    "Митрофан вже складає його на піддон \uD83D\uDE09  \n" +
                                    "Дякуємо, що обираєте GigaHub ❤\uFE0F");

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
                                "🏙 Місто\n👤 П.І.\n📞 Телефон\n💳 Бонусна картка (Від магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444"
                );
            } else if (text.contains("Доставка по місту")) {
                System.out.println("DEBUG: Натиснули Доставка по місту");
                userStates.put(userId, "awaiting_city_delivery");
                tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки по місту у форматі:\n" +
                                "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444"
                );
            } else if (text.contains("Нова пошта")) {
                System.out.println("DEBUG: Натиснули Доставка Новою поштою");
                userStates.put(userId, "awaiting_post_delivery");
                tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:\n" +
                                "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
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

        sendMessage(chatId, "\uD83D\uDCC2 Обирай категорію, друже!  \n" +
                "Я вже підтягнув рукавички та готовий порпатись у наших складських полицях \uD83D\uDE04\uD83E\uDDF1\n" +
                "\n" +
                "У нас усе акуратно розкладено: цемент тут, фарба там, інструменти під рукою, а хіти продажів — просто ближче до виходу.  \n" +
                "Ти лише скажи, що шукаєш — і я миттю знайду найкращі варіанти!\n" +
                "\n" +
                "Занурюйся в каталог — почнемо будувати разом \uD83D\uDCAA✨", markup);

        System.out.println("DEBUG: Categories sent in ID order: " + categories);
    }

    // 🔹    Показ кошика
    public void showCart(Long userId) throws TelegramApiException {
        List<Map<String, Object>> cart = userCart.get(userId);

        if (cart == null || cart.isEmpty()) {
            sendMessage(createUserMenu(String.valueOf(userId), userId));
            return;
        }

        StringBuilder sb = new StringBuilder("🧺 Ваш кошик, друже! \n")
                .append("Митрофан перевірив полички і зібрав усе, що ви додали 👇\n\n");

        double total = 0;
        int i = 1;

        for (Map<String, Object> item : cart) {

            String name = item.getOrDefault("name", "Без назви").toString();
            double price = Double.parseDouble(item.getOrDefault("price", "0").toString());

            // 🔥 Кількість товару
            int quantity = Integer.parseInt(item.getOrDefault("quantity", 1).toString());

            // 🔥 Сума за позицію
            double lineTotal = price * quantity;
            total += lineTotal;

            sb.append("🔸 ").append(i++).append(". ").append(name).append("")
                    .append("\n   🔢 Кількість: ").append(quantity)
                    .append("\n   💵 Ціна за 1: ").append(price).append(" грн")
                    .append("\n   💰 Сума: ").append(lineTotal).append(" грн\n\n");
        }

        sb.append("━━━━━━━━━━━━━━\n")
                .append("💳 Всього до оплати: ").append(total).append(" грн\n")
                .append("🧱 Митрофан каже: «Готовий оформити замовлення, як тільки будеш готовий!» 🤖");

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

    private void handleMaterialCalcMenu(Long userId, String chatId, String text) {
        switch (text) {

            case "⬅️ Назад", "Назад" -> {
                // повернення в головне меню
                userStates.remove(userId);
                sendMessage(createUserMenu(chatId, userId));
            }

            case "🎨 Фарба" -> {
                userStates.put(userId, "paint_length");
                sendText(chatId, "🧱 Вкажи *довжину кімнати* у метрах:");
            }

            case "🧱 Плитка" -> {
                userStates.put(userId, "tile_length");
                sendText(chatId, "🧱 Вкажи *довжину кімнати* у метрах:");
            }

            case "📏 Ламінат" -> {
                userStates.put(userId, "laminate_length");
                sendText(chatId, "📏 Вкажи *довжину кімнати* у метрах:");
            }

            case "🔨 Вагонка" -> {
                userStates.put(userId, "wall_length");
                sendText(chatId, "🔨 Вкажи *довжину стін* у метрах:");
            }

            case "📐 Плінтус" -> {
                userStates.put(userId, "baseboard_length");
                sendText(chatId, "📐 Вкажи *довжину кімнати* у метрах:");
            }

            default -> sendText(chatId, "❓ Оберіть дію з меню.");
        }
    }

    private void handlePaintArea(Long userId, String chatId, String text) {
        try {
            double area = Double.parseDouble(text.replace(",", "."));
            paintTemp.put(userId, area);
            userStates.put(userId, "calc_paint_layers");
            sendText(chatId, "🎨 Вкажіть кількість шарів фарби (1 або 2):");
        } catch (Exception e) {
            sendText(chatId, "⚠️ Введіть число, наприклад 12.5");
        }
    }

    private void handlePaintLayers(Long userId, String chatId, String text) {
        try {
            int layers = Integer.parseInt(text.trim());
            if (layers < 1 || layers > 5) {
                sendText(chatId, "⚠️ Вкажіть коректну кількість шарів (1–5)");
                return;
            }

            double area = paintTemp.getOrDefault(userId, 0.0);
            double totalArea = area * layers;

            userStates.put(userId, "calc_paint_consumption");

            sendText(chatId,
                    "🔢 Вкажіть витрату фарби на 1 м² (наприклад 0.15):\n" +
                            "(Зазвичай 0.12–0.18 л/м²)");
            paintLayers.put(userId, totalArea);

        } catch (Exception e) {
            sendText(chatId, "⚠️ Вкажіть число, наприклад 2");
        }
    }

    private void handlePaintConsumption(Long userId, String chatId, String text) {
        try {
            double consumption = Double.parseDouble(text.replace(",", "."));
            double totalArea = paintLayers.getOrDefault(userId, 0.0);

            double liters = totalArea * consumption;

            sendText(chatId,
                    "🎨 *Результат розрахунку фарби:*\n" +
                            "📐 Площа з урахуванням шарів: " + totalArea + " м²\n" +
                            "🪣 Потрібно приблизно: *" + String.format("%.2f", liters) + " л*\n\n" +
                            "Хочеш порахувати ще щось?");
            sendMaterialCalcMenu(chatId);
            userStates.put(userId, "material_calc_menu");

        } catch (Exception e) {
            sendText(chatId, "⚠️ Вкажіть число, наприклад 0.15");
        }
    }

    private void handleTileArea(Long userId, String chatId, String text) {
        try {
            double area = Double.parseDouble(text.replace(",", "."));
            double reserve = area * 1.10; // 10% запас

            sendText(chatId,
                    "🧱 *Результат розрахунку плитки:*\n" +
                            "📐 Площа: " + area + " м²\n" +
                            "➕ Запас 10%\n" +
                            "📦 Потрібно: *" + String.format("%.2f", reserve) + " м²*\n\n" +
                            "Бажаєш розрахувати ще щось?");
            sendMaterialCalcMenu(chatId);
            userStates.put(userId, "material_calc_menu");

        } catch (Exception e) {
            sendText(chatId, "⚠️ Введіть число, наприклад 12.3");
        }
    }

    private void handleBaseboardLength(Long userId, String chatId, String text) {
        try {
            double perimeter = Double.parseDouble(text.replace(",", "."));
            double pieces = perimeter / 2.5; // 1 планка = 2.5м

            sendText(chatId,
                    "📐 *Розрахунок плінтуса:*\n" +
                            "📏 Периметр кімнати: " + perimeter + " м\n" +
                            "🪵 Довжина 1 планки: 2.5 м\n" +
                            "📦 Потрібно: *" + Math.ceil(pieces) + " шт*\n\n" +
                            "Готово! Обери наступний матеріал:");
            sendMaterialCalcMenu(chatId);
            userStates.put(userId, "material_calc_menu");

        } catch (Exception e) {
            sendText(chatId, "⚠️ Введіть число, наприклад 18.2");
        }
    }

    private void handleLaminateArea(Long userId, String chatId, String text) {
        try {
            double area = Double.parseDouble(text.replace(",", "."));
            double reserve = area * 1.07; // 7% запас

            sendText(chatId,
                    "📏 *Розрахунок ламінату:*\n" +
                            "📐 Площа кімнати: " + area + " м²\n" +
                            "➕ Запас 7%\n" +
                            "📦 Потрібно: *" + String.format("%.2f", reserve) + " м²*\n\n" +
                            "Готово! Обери інший розрахунок:");

            sendMaterialCalcMenu(chatId);
            userStates.put(userId, "material_calc_menu");

        } catch (Exception e) {
            sendText(chatId, "⚠️ Введіть число, наприклад 14.5");
        }
    }

    private void handleWallArea(Long userId, String chatId, String text) {
        try {
            double area = Double.parseDouble(text.replace(",", "."));
            double reserve = area * 1.10;

            sendText(chatId,
                    "🔨 *Розрахунок вагонки:*\n" +
                            "📐 Площа стін: " + area + " м²\n" +
                            "➕ Запас 10%\n" +
                            "🪵 Потрібно: *" + String.format("%.2f", reserve) + " м²*\n\n" +
                            "Готово!");

            sendMaterialCalcMenu(chatId);
            userStates.put(userId, "material_calc_menu");

        } catch (Exception e) {
            sendText(chatId, "⚠️ Введіть число, наприклад 20.5");
        }
    }

    private void handleRoomLength(Long userId, String chatId, String text, String nextState) {
        try {
            double length = Double.parseDouble(text.replace(",", "."));
            tempLength.put(userId, length);

            userStates.put(userId, nextState);
            sendText(chatId, "➡️ Тепер вкажи *ширину кімнати* у метрах:");
        } catch (Exception e) {
            sendText(chatId, "⚠️ Введи число, наприклад 4.2");
        }
    }

    private double handleRoomWidth(Long userId, String chatId, String text) {
        try {
            double width = Double.parseDouble(text.replace(",", "."));
            tempWidth.put(userId, width);

            double length = tempLength.getOrDefault(userId, 0.0);
            return length * width;

        } catch (Exception e) {
            sendText(chatId, "⚠️ Введи число, наприклад 3.5");
            return -1;
        }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void handleBack(Long userId, String chatId) throws TelegramApiException {
        System.out.println("[handleBack] User " + userId + " pressed Back.");

        String state = userStates.get(userId);

        // ❗ 0. Блокування кнопки Назад під час оформлення замовлення
        if ("awaiting_delivery_choice".equals(state) ||
                "order_pickup".equals(state) ||
                "awaiting_city_delivery".equals(state) ||
                "awaiting_post_delivery".equals(state)) {

            sendText(chatId,
                    "❗ Повернення назад недоступне під час оформлення замовлення.\n" +
                            "📝 Будь ласка, завершіть заповнення даних або оформіть замовлення.");
            return;
        }

        // ❗ 0.2 Повернення назад з КАЛЬКУЛЯТОРА МАТЕРІАЛІВ
        Set<String> MATERIAL_STATES = Set.of(
                "material_calc_menu",
                "paint_length", "paint_width",
                "calc_paint_layers", "calc_paint_consumption",
                "tile_length", "tile_width", "tile_size",
                "laminate_length", "laminate_width",
                "wall_length", "wall_width",
                "baseboard_length", "baseboard_width"
        );

        if (state != null && MATERIAL_STATES.contains(state)) {

            // очищаємо тимчасові дані калькулятора
            tempLength.remove(userId);
            tempWidth.remove(userId);
            paintTemp.remove(userId);
            paintLayers.remove(userId);

            // ставимо стан null
            userStates.remove(userId);

            // повернення в ГОЛОВНЕ МЕНЮ
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 1. Очищаємо стани
        userStates.remove(userId);
        lastShownProduct.remove(userId);
        adminMatchList.remove(userId);
        productIndex.remove(userId);

        // 2. Повернення з підкатегорії
        if (currentSubcategory.containsKey(userId)) {
            currentSubcategory.remove(userId);

            if (currentCategory.containsKey(userId)) {
                sendSubcategories(userId, currentCategory.get(userId));
            } else {
                sendCategories(userId);
            }
            return;
        }

        // 3. Повернення з категорії
        if (currentCategory.containsKey(userId)) {
            currentCategory.remove(userId);
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 4. Повернення з кошика
        if (userCart.containsKey(userId)) {
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 5. Повернення з адмін-меню
        if (adminOrderIndex.containsKey(userId)) {
            adminOrderIndex.remove(userId);
            sendMessage(createAdminMenu(chatId));
            return;
        }

        // 6. Повернення з меню розробника
        if (DEVELOPERS.contains(userId) && isInDeveloperMenu(userId)) {
            sendMessage(createDeveloperMenu(chatId));
            return;
        }

        // 7. Повернення після редагування товару
        if (adminEditingProduct.containsKey(userId) || adminSelectedProductsRange.containsKey(userId)) {

            adminEditingProduct.remove(userId);
            adminSelectedProductsRange.remove(userId);
            adminEditingField.remove(userId);
            userStates.remove(userId);

            sendText(chatId, "↩️ Ви повернулися до меню розробника.");
            sendMessage(createDeveloperMenu(chatId));
            return;
        }

        // 8. Головне меню
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

        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        // ⬆️ Перехід вперед ПЕРЕД відображенням
        index = (index + 1) % products.size();
        productIndex.put(chatId, index);

        Map<String, Object> product = products.get(index);
        lastShownProduct.put(chatId, product);

        sendProductMessage(chatId, product);
    }

    // 🔹 Показ предедущего товару по id
    private void showPreviousProduct(Long chatId) throws TelegramApiException {

        String category = currentCategory.get(chatId);
        String subcategory = currentSubcategory.get(chatId);

        int index = productIndex.getOrDefault(chatId, 0);

        CatalogSearcher searcher = new CatalogSearcher();
        List<Map<String, Object>> products = searcher.getProducts(category, subcategory);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ У цій підкатегорії немає товарів.");
            return;
        }

        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        // ⬇️ Перехід назад ПЕРЕД відображенням
        index = (index - 1 + products.size()) % products.size();
        productIndex.put(chatId, index);

        Map<String, Object> product = products.get(index);
        lastShownProduct.put(chatId, product);

        sendProductMessage(chatId, product);
    }

    private void sendProductMessage(Long chatId, Map<String, Object> product) throws TelegramApiException {

        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "N/A"));
        String unit = String.valueOf(product.getOrDefault("unit", "шт")).toLowerCase();
        String description = String.valueOf(product.getOrDefault("description", ""));
        String photo = String.valueOf(product.getOrDefault("photo", ""));
        String manufacturer = String.valueOf(product.getOrDefault("manufacturer", ""));

        StringBuilder sb = new StringBuilder("📦 ").append(name);

        // 🔥 НОВА ЛОГІКА ПІД ЗАМОВЛЕННЯ
        if (unit.equals("під замовлення")) {
            sb.append("\n💬 *Товар доступний під замовлення*")
                    .append("\n📞 Уточніть ціну у консультанта");
        } else {
            sb.append("\n💰 Ціна: ").append(price).append(" грн за ").append(unit);
        }

        if (!manufacturer.isEmpty() && !"null".equalsIgnoreCase(manufacturer))
            sb.append("\n🏭 Виробник: ").append(manufacturer);

        if (!description.isEmpty() && !"null".equalsIgnoreCase(description))
            sb.append("\n📖 ").append(description);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("⬅ Назад");
        row1.add("🔍 Пошук");
        row1.add("➡ Далі");

        KeyboardRow row2 = new KeyboardRow();

        if (unit.equals("під замовлення")) {
            row2.add("✉ Написати консультанту");
        } else {
            row2.add("🛒 Додати в кошик");
        }

        row2.add("🛍️ Перейти в кошик");

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));

        List<KeyboardRow> keyboard = List.of(row1, row2, row3);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        if (photo != null && !photo.isEmpty() && !"null".equalsIgnoreCase(photo)) {
            sendPhotoFromResources(chatId.toString(), photo, sb.toString(), markup);
        } else {
            sendTextWithMarkup(chatId, sb.toString(), markup);
        }
    }

    private void addToCart(Long chatId) throws TelegramApiException {
        Map<String, Object> product = lastShownProduct.get(chatId);

        if (product == null) {
            sendText(chatId, "❌ Не вдалося визначити товар. Спробуй ще раз 🙏");
            return;
        }

        String productName = product.getOrDefault("name", "Без назви").toString();
        String unit = product.getOrDefault("unit", "").toString().toLowerCase();

        if (unit.equals("під замовлення")) {

            sendText(chatId,
                    "📦 *Товар під замовлення!*\n\n" +
                            "🔎 \"" + productName + "\" наразі недоступний для швидкого замовлення.\n" +
                            "Я вже передав інформацію нашому консультанту — він відповість тобі найближчим часом! 🧱🤝");

            String adminMsg =
                    "📢 *Новий запит під замовлення!*\n\n" +
                            "👤 Покупець хоче дізнатися про товар:\n" +
                            "📦 " + productName + "\n\n" +
                            "🆔 User ID: " + chatId + "\n" +
                            "💬 Чат ID: " + chatId + "\n\n" +
                            "Натисніть кнопку нижче, щоб відповісти 👇";

            sendReplyButtonToConsultant(chatId);

            sendText(String.valueOf(CONSULTANT_CHAT_ID), adminMsg);

            return;
        }

        waitingForQuantity.put(chatId, true);

        sendText(chatId,
                "📥 Добре, друже! Скільки тобі потрібно?\n" +
                        "👉 *" + productName + "*\n\n" +
                        "Введи кількість числом (наприклад: 3)");
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

        Long realChatId = Long.parseLong(chatId);
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
            case "material_calc_menu" -> handleMaterialCalcMenu(userId, chatId, text);
            case "calc_paint_area" -> handlePaintArea(userId, chatId, text);
            case "calc_paint_layers" -> handlePaintLayers(userId, chatId, text);
            case "calc_paint_consumption" -> handlePaintConsumption(userId, chatId, text);

            case "calc_tile_area" -> handleTileArea(userId, chatId, text);
            case "calc_laminate_area" -> handleLaminateArea(userId, chatId, text);
            case "calc_wall_area" -> handleWallArea(userId, chatId, text);
            case "calc_baseboard_length" -> handleBaseboardLength(userId, chatId, text);

            case "paint_length" -> handleRoomLength(userId, chatId, text, "paint_width");
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

            case "paint_width" -> {
                double area = handleRoomWidth(userId, chatId, text);
                if (area <= 0) return;

                paintTemp.put(userId, area);
                userStates.put(userId, "calc_paint_layers");

                sendText(chatId,
                        "🎨 Площа кімнати: *" + String.format("%.2f", area) + " м²*\n" +
                                "Тепер вкажи кількість шарів фарби (1–5):");
            }

            case "tile_length" -> handleRoomLength(userId, chatId, text, "tile_width");

            case "tile_width" -> {
                double area = handleRoomWidth(userId, chatId, text);
                if (area <= 0) return;

                double need = area * 1.10;

                sendText(chatId,
                        "🧱 *Розрахунок плитки*\n" +
                                "📐 Площа: " + area + " м²\n" +
                                "➕ Запас 10%\n" +
                                "📦 Потрібно: *" + String.format("%.2f", need) + " м²*");

                sendMaterialCalcMenu(chatId);
                userStates.put(userId, "material_calc_menu");
            }

            case "laminate_length" -> handleRoomLength(userId, chatId, text, "laminate_width");

            case "laminate_width" -> {
                double area = handleRoomWidth(userId, chatId, text);
                if (area <= 0) return;

                double need = area * 1.07;

                sendText(chatId,
                        "📏 *Розрахунок ламінату*\n" +
                                "📐 Площа кімнати: " + area + " м²\n" +
                                "➕ Запас 7%\n" +
                                "📦 Потрібно: *" + String.format("%.2f", need) + " м²*");

                sendMaterialCalcMenu(chatId);
                userStates.put(userId, "material_calc_menu");
            }

            case "wall_length" -> handleRoomLength(userId, chatId, text, "wall_width");

            case "wall_width" -> {
                double area = handleRoomWidth(userId, chatId, text);
                if (area <= 0) return;

                double need = area * 1.10;

                sendText(chatId,
                        "🔨 *Розрахунок вагонки*\n" +
                                "📐 Площа стін: " + area + " м²\n" +
                                "➕ Запас 10%\n" +
                                "🪵 Потрібно: *" + String.format("%.2f", need) + " м²*");

                sendMaterialCalcMenu(chatId);
                userStates.put(userId, "material_calc_menu");
            }

            case "baseboard_length" -> handleRoomLength(userId, chatId, text, "baseboard_width");

            case "baseboard_width" -> {
                double area = handleRoomWidth(userId, chatId, text);
                if (area <= 0) return;

                double perimeter = (tempLength.get(userId) + tempWidth.get(userId)) * 2;
                double pieces = Math.ceil(perimeter / 2.5);

                sendText(chatId,
                        "📐 *Розрахунок плінтуса*\n" +
                                "📏 Периметр кімнати: " + String.format("%.2f", perimeter) + " м\n" +
                                "🪵 Потрібно планок: *" + pieces + " шт*");

                sendMaterialCalcMenu(chatId);
                userStates.put(userId, "material_calc_menu");
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

                // 📸 Якщо користувач відправляє фото
                if (update.getMessage().hasPhoto()) {

                    List<PhotoSize> photos = update.getMessage().getPhoto();
                    PhotoSize largest = photos.get(photos.size() - 1);
                    String fileId = largest.getFileId();

                    // ✏️ Текст під фотографією
                    String caption = update.getMessage().getCaption();
                    if (caption == null) caption = "";

                    try {
                        SendPhoto msg = new SendPhoto();
                        msg.setChatId(CONSULTANT_CHAT_ID.toString());
                        msg.setPhoto(new InputFile(fileId));
                        msg.setCaption(
                                "📸 Фото від покупця:\n" +
                                        "🆔 User ID: " + userId + "\n" +
                                        "💬 Chat ID: " + realChatId + "\n\n" +
                                        (caption.isEmpty() ? "" : ("📝 Підпис до фото:\n" + caption))
                        );

                        execute(msg);

                        sendReplyButtonToConsultant(userId);

                        sendText(realChatId, "📸 Ваше фото передано консультанту!");

                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(realChatId, "❌ Помилка при відправці фото консультанту.");
                    }

                    userStates.remove(userId);
                    return;
                }

                // ✏️ Якщо користувач відправив текст
                if (text != null && !text.isBlank()) {

                    // автоматична відповідь бота (AI-підказка)
                    String botReply = botAutoAnswer(text);

                    if (botReply != null) {
                        sendText(realChatId, botReply + "\n\n✉️ Якщо хочеш поговорити з консультантом — натисни кнопку.");
                        userStates.remove(userId);
                        return;
                    }

                    // якщо бот не знає → передаємо консультанту
                    try {
                        SendMessage adminMsg = new SendMessage(
                                CONSULTANT_CHAT_ID.toString(),
                                "📩 Нове текстове повідомлення від покупця:\n" +
                                        "🆔 User ID: " + userId + "\n" +
                                        "💬 Chat ID: " + realChatId + "\n\n" +
                                        "📝 Текст:\n" + text
                        );

                        execute(adminMsg);

                        sendReplyButtonToConsultant(userId);

                        sendText(realChatId, "Ваше повідомлення передано консультанту!");

                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(realChatId, "❌ Помилка при пересилці повідомлення консультанту.");
                    }

                    userStates.remove(userId);
                    return;
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
                                        "🏙 Місто\n👤 П.І.\n📞 Телефон\n💳 Бонусна картка (Від магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444");
                    }

                    case "📦 Доставка по місту" -> {
                        tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                        userStates.put(userId, "awaiting_city_delivery");
                        sendText(chatId,
                                "📝 Введіть, будь-ласка, дані для доставки по місту у форматі:\n" +
                                        "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444");
                    }

                    case "📮 Доставка Новою поштою" -> {
                        tempStorage.put(userId + "_deliveryType", "Нова пошта");
                        userStates.put(userId, "awaiting_post_delivery");
                        sendText(chatId,
                                "📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:\n" +
                                        "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
                                        "📌 Приклад:\n" +
                                        "№12, Іваненко Іван Іванович, +380501234567, 4444");
                    }

                    case BACK_BUTTON -> {
                        try {
                            handleBack(userId, chatId); // chatId як String
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
                if (text.equals("⬅️ Назад")) {
                    sendText(chatId,
                            "❗ Ви зараз оформлюєте замовлення.\n" +
                                    "⬅️ Повернення назад буде доступне після завершення оформлення.");
                    return;
                }

                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String[] parts = text.split(",", 4); // Місто, П.І., Телефон, Картка
                String city = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                tempDeliveryType.put(userId, "Самовивіз");
                tempAddress.put(userId, city);        // у city тримаємо місто
                tempFullname.put(userId, fullName);
                tempPhone.put(userId, phone);
                tempCard.put(userId, card);

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                tempOrderCode.put(userId, orderCode);

                StringBuilder items = new StringBuilder();
                double total = 0;

                for (Map<String, Object> item : cart) {
                    String name = item.get("name").toString();
                    double price = Double.parseDouble(item.get("price").toString());
                    int qty = item.get("quantity") == null ? 1 : Integer.parseInt(item.get("quantity").toString());

                    total += price * qty;
                    items.append(name).append(":").append(price).append(":").append(qty).append(";");
                }

                tempOrderItems.put(userId, items.toString());
                tempTotal.put(userId, total);

                askPaymentMethod(userId, Long.valueOf(chatId));
                userStates.put(userId, "awaiting_payment_method");
            }

            // 🔹 City Delivery
            case "awaiting_city_delivery" -> {
                if (text.equals("⬅️ Назад")) {
                    sendText(chatId,
                            "❗ Ви зараз оформлюєте замовлення.\n" +
                                    "⬅️ Повернення назад буде доступне після завершення оформлення.");
                    return;
                }

                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String[] parts = text.split(",", 4); // Адреса, П.І., Телефон, Картка
                String address = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                tempDeliveryType.put(userId, "Доставка по місту");
                tempAddress.put(userId, address);
                tempFullname.put(userId, fullName);
                tempPhone.put(userId, phone);
                tempCard.put(userId, card);

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                tempOrderCode.put(userId, orderCode);

                StringBuilder items = new StringBuilder();
                double total = 0;

                for (Map<String, Object> item : cart) {
                    String name = item.get("name").toString();
                    double price = Double.parseDouble(item.get("price").toString());
                    int qty = item.get("quantity") == null ? 1 : Integer.parseInt(item.get("quantity").toString());

                    total += price * qty;
                    items.append(name).append(":").append(price).append(":").append(qty).append(";");
                }

                tempOrderItems.put(userId, items.toString());
                tempTotal.put(userId, total);

                askPaymentMethod(userId, Long.valueOf(chatId));
                userStates.put(userId, "awaiting_payment_method");
            }

            // 🔹 Nova Poshta Delivery
            case "awaiting_post_delivery" -> {
                if (text.equals("⬅️ Назад")) {
                    sendText(chatId,
                            "❗ Ви зараз оформлюєте замовлення.\n" +
                                    "⬅️ Повернення назад буде доступне після завершення оформлення.");
                    return;
                }

                List<Map<String, Object>> cart = userCart.get(userId);
                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                String[] parts = text.split(",", 4); // Відділення НП, П.І., Телефон, Картка
                String postOffice = parts.length > 0 ? parts[0].trim() : "Невідомо";
                String fullName = parts.length > 1 ? parts[1].trim() : "Невідомо";
                String phone = parts.length > 2 ? parts[2].trim() : "Невідомо";
                String card = parts.length > 3 ? parts[3].trim() : "0000";

                tempDeliveryType.put(userId, "Нова пошта");
                tempAddress.put(userId, postOffice);
                tempFullname.put(userId, fullName);
                tempPhone.put(userId, phone);
                tempCard.put(userId, card);

                String orderCode = String.format("%04d", new Random().nextInt(10000));
                tempOrderCode.put(userId, orderCode);

                StringBuilder items = new StringBuilder();
                double total = 0;

                for (Map<String, Object> item : cart) {
                    String name = item.get("name").toString();
                    double price = Double.parseDouble(item.get("price").toString());
                    int qty = item.get("quantity") == null ? 1 : Integer.parseInt(item.get("quantity").toString());

                    total += price * qty;
                    items.append(name).append(":").append(price).append(":").append(qty).append(";");
                }

                tempOrderItems.put(userId, items.toString());
                tempTotal.put(userId, total);

                askPaymentMethod(userId, Long.valueOf(chatId));
                userStates.put(userId, "awaiting_payment_method");
            }

            case "awaiting_payment_method" -> {

                if (text.equals("💵 Готівка")) {
                    createOrderWithPayment(userId, Long.valueOf(chatId), "Готівка", false);
                    return;
                }

                if (text.equals("💳 Оплата карткою")) {
                    createOrderWithPayment(userId, Long.valueOf(chatId), "Картка", true);
                    return;
                }

                sendText(chatId, "\uD83D\uDCB3 Яким способом хочете оплатити?\n" +
                        "\n" +
                        "\uD83D\uDCB5 Готівка — розрахуєтесь при отриманні  \n" +
                        "\uD83D\uDCB3 Оплата карткою — миттєва та зручно\n" +
                        "\n" +
                        "Митрофан тримає термінал… а ну, що вибираєш? \uD83D\uDE04");
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
                String field = adminEditingField.get(userId);
                String singleProduct = adminEditingProduct.get(userId);
                List<String> productsToEdit = adminSelectedProductsRange.get(userId);

                if (field == null || (singleProduct == null && (productsToEdit == null || productsToEdit.isEmpty()))) {
                    sendText(chatId, "❌ Сталася помилка. Спробуйте ще раз.");
                    userStates.remove(userId);
                    return;
                }

                String newValue = text.trim();
                System.out.println("DEBUG: User " + userId + " entered new value '" + newValue + "' for field '" + field + "'");

                // Перевірка для одиниці виміру
                if ("unit".equals(field) && !newValue.equalsIgnoreCase("шт") && !newValue.equalsIgnoreCase("метр")) {
                    sendText(chatId, "❌ Допустимі значення: 'шт' або 'метр'. Спробуйте ще раз:");
                    return;
                }

                if (productsToEdit != null && !productsToEdit.isEmpty()) {
                    // Масове оновлення
                    int successCount = 0;
                    for (String productName : productsToEdit) {
                        boolean updated = CatalogEditor.updateField(productName, field, newValue);
                        if (updated) successCount++;
                    }

                    // Формуємо рядок з вибраними товарами (діапазон або номери)
                    String selection = productsToEdit.size() > 1 ? "1-" + productsToEdit.size() : "1";
                    sendText(chatId, "✅ Поле '" + field + "' успішно оновлено для всіх " + successCount +
                            " товарів у вибраному діапазоні (" + selection + ").");

                } else if (singleProduct != null) {
                    // Оновлення одного товару
                    boolean success = CatalogEditor.updateField(singleProduct, field, newValue);
                    if (success) {
                        sendText(chatId, "✅ Поле '" + field + "' успішно оновлено для товару '" + singleProduct + "'");
                    } else {
                        sendText(chatId, "⚠️ Не вдалося оновити поле '" + field + "' для товару '" + singleProduct + "'");
                    }
                }

                // 🔹 Залишаємо користувача в меню редагування для подальших змін
                sendMessageSafely(createEditMenu(chatId, userId));

                // --- Очищення станів для поля (але залишаємо список товарів для подальшого редагування)
                adminEditingField.remove(userId);
                userStates.put(userId, "editing"); // повертаємо користувача в режим редагування
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
            sendText(chatId,
                    "🤖 Митрофан стоїть готовий шукати, але ти нічого не написав 😊\n" +
                            "Спробуй ввести назву товару ще раз.");
            return;
        }

        try {
            CatalogSearcher searcher = new CatalogSearcher();

            List<Map<String, Object>> foundProducts = searcher.searchMixedFromYAML(text);
            System.out.println("[handleSearch] Found products count: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                sendText(chatId,
                        "😕 Митрофан переглянув усі полиці, але нічого не знайшов за запитом:\n" +
                                "👉 *" + text + "*\n\n" +
                                "Спробуй інший варіант назви або уточни модель.");
                return;
            }

            if (foundProducts.size() > 1) {

                StringBuilder sb = new StringBuilder(
                        "🔎 Митрофан знайшов кілька варіантів:\n\n");

                int idx = 1;
                for (Map<String, Object> p : foundProducts) {
                    sb.append("`")
                            .append(idx++)
                            .append("` — ")
                            .append(p.get("name"))
                            .append("\n");
                }

                sb.append("\n📝 Введи *номер товару*, щоб показати деталі.");

                searchResults.put(userId, foundProducts);
                System.out.println("[handleSearch] searchResults stored for user " + userId);

                sendText(chatId, sb.toString());
                return;
            }

            Map<String, Object> product = foundProducts.get(0);
            lastShownProduct.put(userId, product);

            System.out.println("[handleSearch] Showing product for userId=" + userId + ": " + product);

            sendProductDetailsWithButtons(userId, product);

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "⚠️ Відбулася технічна помилка.\n" +
                            "Митрофан вже крутив дроти й скоро полагодить 😄");
        }
    }

    private void handleWaitingForProductNumber(Long userId, String chatId, String text) {
        List<Map<String, Object>> products = searchResults.get(Long.parseLong(chatId));

        if (products == null || products.isEmpty()) {
            sendText(chatId,
                    "🔍 Схоже, список товарів порожній.\n" +
                            "Митрофан уже намагається знайти щось схоже 😉");
            userStates.remove(userId);
            return;
        }

        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(text.trim()) - 1;
        } catch (NumberFormatException e) {
            sendText(chatId, "⚠️ Будь ласка, введіть *правильний номер* товару з списку.");
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= products.size()) {
            sendText(chatId, "⚠️ Номер поза діапазоном! Митрофан просить спробувати ще 😉");
            return;
        }

        Map<String, Object> product = products.get(selectedIndex);

        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "0"));
        String unit = String.valueOf(product.getOrDefault("unit", "шт")).toLowerCase();
        String category = String.valueOf(product.getOrDefault("category", "❓"));
        String subcategory = String.valueOf(product.getOrDefault("subcategory", "❓"));

        // 🔥 Формуємо текст ціни
        String priceText;
        boolean isCustom = unit.equals("під замовлення");

        if (isCustom) {
            priceText =
                    "💬 *Товар доступний під замовлення*\n" +
                            "📞 Ціну потрібно уточнити у консультанта.";
        } else {
            priceText = "💰 Ціна: *" + price + " грн* за " + unit;
        }

        // 🔥 Формуємо фінальне повідомлення
        String message = String.format(
                "📦 *%s*\n%s\n\n📂 Категорія: %s → %s\n\n" +
                        "🔎 Можете обрати дію нижче або ввести інший товар.",
                name, priceText, category, subcategory
        );

        // 🔹 Формуємо клавіатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        if (isCustom) {
            row1.add("✉ Написати консультанту");
        } else {
            row1.add("➕ Додати в кошик");
        }
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🛍 Переглянути кошик");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        // 🔹 Відправка повідомлення
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Митрофан не зміг відправити товар 😥 Спробуйте ще раз.");
        }

        // 🔹 Очищуємо тимчасові дані
        userStates.remove(userId);
        searchResults.remove(Long.parseLong(chatId));
    }

    private void handleWaitingForSearch(Long userId, String chatId, String text) {
        text = text.trim();

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
                return;
            }

            // Якщо користувач ввів текст → пошук
            getUserStates().put(userId, "waiting_for_search");
            searchManager.performSearch(userId, chatId, text);

        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Сталася помилка при обробці пошуку товару.");
        }
    }

    // 🔹 Надсилаємо деталі останнього показаного товару з кнопками
    public void sendProductDetailsWithButtons(Long userId, Map<String, Object> product) {
        String chatId = String.valueOf(userId);

        String name = String.valueOf(product.getOrDefault("name", "Без назви"));
        String price = String.valueOf(product.getOrDefault("price", "0"));
        String unit = String.valueOf(product.getOrDefault("unit", "шт")).toLowerCase();
        String category = String.valueOf(product.getOrDefault("category", "❓"));
        String subcategory = String.valueOf(product.getOrDefault("subcategory", "❓"));

        boolean isCustomOrder = unit.equals("під замовлення");

        // 🔥 Ціна
        String priceText;
        if (isCustomOrder) {
            priceText =
                    "💬 *Товар доступний під замовлення*\n" +
                            "📞 Для уточнення ціни — зверніться до консультанта.";
        } else {
            priceText = "💰 Ціна: *" + price + " грн* за " + unit;
        }

        // 🔥 Повідомлення
        String message = String.format(
                "📦 *%s*\n%s\n\n" +
                        "📂 Категорія: %s → %s\n\n" +
                        "🔎 Оберіть дію нижче або введіть інший товар для пошуку.",
                name, priceText, category, subcategory
        );

        // 🔥 Клавіатура
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();

        if (isCustomOrder) {
            row1.add("✉ Написати консультанту");
        } else {
            row1.add("🛠 Додати в кошик");
        }

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
        sendMessage.setParseMode("Markdown");
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Сталася помилка при відправці товару.");
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
            sendText(chatId, "❌ Список товарів порожній.");
            userStates.remove(userId);
            return;
        }

        text = text.trim();
        List<String> selectedProducts = new ArrayList<>();

        try {
            // Діапазон 1-10
            if (text.contains("-")) {
                String[] parts = text.split("-");
                int start = Integer.parseInt(parts[0].trim()) - 1;
                int end = Integer.parseInt(parts[1].trim()) - 1;

                if (start < 0) start = 0;
                if (end >= matches.size()) end = matches.size() - 1;

                for (int i = start; i <= end; i++) {
                    selectedProducts.add((String) matches.get(i).get("name"));
                }
            } else if (text.contains(",")) { // список 1,3,5
                String[] parts = text.split(",");
                for (String part : parts) {
                    int index = Integer.parseInt(part.trim()) - 1;
                    if (index >= 0 && index < matches.size()) {
                        selectedProducts.add((String) matches.get(index).get("name"));
                    }
                }
            } else { // один товар
                int index = Integer.parseInt(text) - 1;
                if (index >= 0 && index < matches.size()) {
                    selectedProducts.add((String) matches.get(index).get("name"));
                }
            }

            if (selectedProducts.isEmpty()) {
                sendText(chatId, "❌ Немає валідних номерів для редагування.");
                return;
            }

            // Зберігаємо вибрані товари
            adminSelectedProductsRange.put(userId, selectedProducts);
            adminEditingProduct.put(userId, selectedProducts.get(0));
            userStates.put(userId, "editing");

            // Чистимо список пошуку
            adminMatchList.remove(userId);

            // Показуємо меню редагування **тільки один раз**
            showEditMenuOnce(userId, chatId);

        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Введіть правильні номери або діапазон у форматі '1-10' або '1,3,5'.");
        }
    }

    // 🔧 Редагування товару
    private void handleEditing(Long userId, String chatId, String text) {
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

                sendText(chatId,
                        "Введіть одиницю виміру для товару:\n\n" +
                                "Доступні варіанти:\n" +
                                "• шт\n" +
                                "• метр\n" +
                                "• м2\n" +
                                "• під замовлення\n\n" +
                                "✏️ Введіть одне зі значень:");
                break;

            case "🏭 Виробник":
                adminEditingField.put(userId, "manufacturer");
                userStates.put(userId, "awaiting_field_value");
                sendText(chatId, "✏️ Введіть назву виробника для товару (або ❌ щоб видалити):");
                break;

            case BACK_BUTTON:
                // 🔹 Очищаємо тимчасові стани редагування
                adminEditingField.remove(userId);
                adminEditingProduct.remove(userId);
                adminSelectedProductsRange.remove(userId);
                userStates.remove(userId);

                // 🔹 Повертаємо користувача в меню розробника
                sendText(chatId, "↩️ Ви повернулися до меню розробника.");
                sendMessageSafely(createDeveloperMenu(chatId));
                return;

            default:
                sendText(chatId, "Невідома опція редагування.");
                break;
        }

        // 🔹 Показуємо меню редагування тільки якщо це не кнопка "Назад"
        sendMessageSafely(createEditMenu(chatId, userId));
    }

    // 📝 Очікування значення для редагування
    private void handleAwaitingField(Long userId, String chatId, String newValue) {
        String field = adminEditingField.get(userId);
        List<String> productsToEdit = adminSelectedProductsRange.get(userId);
        String singleProduct = adminEditingProduct.get(userId);

        if (field == null) return;

        if ("unit".equals(field)) {

            newValue = newValue.trim().toLowerCase();

            if (!newValue.equals("шт")
                    && !newValue.equals("метр")
                    && !newValue.equals("м2")
                    && !newValue.equals("під замовлення")) {

                sendText(chatId,
                        "❌ Недопустиме значення.\n" +
                                "Можна вводити лише: шт, метр, м2, під замовлення.\nСпробуйте ще раз:");
                return;
            }
        }

        if (productsToEdit != null && !productsToEdit.isEmpty()) {

            int successCount = 0;

            for (String productName : productsToEdit) {
                boolean updated = CatalogEditor.updateField(productName, field, newValue);

                if (newValue.equals("під замовлення")) {
                    CatalogEditor.updateField(productName, "price", "");
                }

                if (updated) successCount++;
            }

            sendText(chatId,
                    "✅ Одиницю виміру оновлено для " + successCount + " товарів.\n" +
                            (newValue.equals("під замовлення")
                                    ? "💡 Ціна очищена, бо товар під замовлення."
                                    : ""));
        }

        else if (singleProduct != null) {

            boolean success = CatalogEditor.updateField(singleProduct, field, newValue);

            if (newValue.equals("під замовлення")) {
                CatalogEditor.updateField(singleProduct, "price", "");
            }

            if (success) {
                sendText(chatId,
                        "✅ Одиницю виміру оновлено для товару '" + singleProduct + "'.\n" +
                                (newValue.equals("під замовлення")
                                        ? "💡 Ціна очищена, бо товар під замовлення."
                                        : ""));
            } else {
                sendText(chatId, "⚠️ Не вдалося оновити одиницю виміру.");
            }
        }

        adminEditingField.remove(userId);

        sendText(chatId, "Виберіть наступну дію:");
        sendMessageSafely(createEditMenu(chatId, userId));

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
                .text("\uD83D\uDC4B Радий бачити тебе знову, друже!\n" +
                        "\n" +
                        "Я — Митрофан \uD83E\uDD16, твій особистий провідник у світі будматеріалів GigaHub.  \n" +
                        "Готовий допомогти у всьому, що стосується ремонту та будівництва:\n" +
                        "\n" +
                        "\uD83D\uDCC2 Переглядай каталог  \n" +
                        "\uD83D\uDD0D Шукай потрібні товари  \n" +
                        "\uD83D\uDED2 Заглядай у свій кошик  \n" +
                        "\uD83D\uDD25 Або дивись популярні хіти, які беруть найчастіше!\n" +
                        "\n" +
                        "Я поруч — зробимо твій вибір простим, а покупки приємними \uD83D\uDCAA✨")
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
    // Метод для першого показу меню редагування
    private void showEditMenuOnce(Long userId, String chatId) {
        List<String> productsToEdit = adminSelectedProductsRange.get(userId);
        String menuTitle;

        if (productsToEdit != null && !productsToEdit.isEmpty()) {
            menuTitle = "Редагуємо " + productsToEdit.size() + " товарів. Вибрані: ";
            if (productsToEdit.size() > 1) {
                menuTitle += "1-" + productsToEdit.size();
            } else {
                menuTitle += "1";
            }
            menuTitle += "\nПоточний: " + productsToEdit.get(0);
        } else {
            String productName = adminEditingProduct.get(userId);
            menuTitle = "Редагування товару: " + (productName != null ? productName : "не вибрано");
        }

        sendText(chatId, menuTitle); // текст один раз
        sendMessageSafely(createEditMenu(chatId, userId)); // тільки кнопки
    }

    // 🔹 Створюємо клавіатуру меню редагування
    private SendMessage createEditMenu(String chatId, Long userId) {
        SendMessage msg = new SendMessage(chatId, "Виберіть дію:");

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
        r4.add(new KeyboardButton("⬅️ Назад"));

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
        row2.add("🧮 Будівельний калькулятор");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row3);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text(
                        "📘 *Митрофан на зв’язку!*\n\n" +
                                "Тут зібрав усе, що допоможе тобі швидко отримати потрібну інформацію 👇\n\n" +

                                "✉️ *Написати консультанту*\n" +
                                "Передам твоє питання нашим майстрам — підкажуть і допоможуть обрати найкращий варіант.\n\n" +

                                "💌 *Переглянути відповіді*\n" +
                                "Тут знаходяться всі відповіді консультанта. Нічого не загубиться!\n\n" +

                                "🧮 *Будівельний калькулятор*\n" +
                                "Допоможу порахувати: скільки потрібно фарби, плитки, ламінату, вагонки або плінтуса."
                )
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build();
    }

    private void sendMaterialCalcMenu(String chatId) {

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🎨 Фарба");
        row1.add("🧱 Плитка");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📏 Ламінат");
        row2.add("🔨 Вагонка");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📐 Плінтус");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row4);

        markup.setKeyboard(keyboard);

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("🔧 Оберіть, що потрібно порахувати:")
                .replyMarkup(markup)
                .build();

        try {
            execute(msg);
        } catch (Exception ignored) {}
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
            handleBack(chatId, chatId.toString());
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

                    sendMessage(chatId, "\uD83E\uDDF1 Обери підкатегорію, друже!\n" +
                            "\n" +
                            "Митрофан уже стоїть біля стелажа — каска на місці, ліхтар увімкнений \uD83D\uDD26  \n" +
                            "Залишилось тільки сказати, що саме шукаємо — і я миттю знайду потрібний товар \uD83D\uDE09", markup);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠️ Помилка при отриманні підкатегорій із бази.");
        }
    }

    //🔸Показ товару
    private void sendProduct(Long chatId) throws TelegramApiException {

        String category = currentCategory.get(chatId);
        String subcategory = currentSubcategory.get(chatId);

        int index = productIndex.getOrDefault(chatId, 0);

        CatalogSearcher searcher = new CatalogSearcher();
        List<Map<String, Object>> products = searcher.getProducts(category, subcategory);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ У цій підкатегорії зараз порожньо.\n" +
                    "Митрофан уже перевіряє полиці 😄");
            return;
        }

        // Сортуємо по id
        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        if (index >= products.size() || index < 0) index = 0;

        Map<String, Object> product = products.get(index);
        lastShownProduct.put(chatId, product);

        // ----- Дані -----
        String name = safeToString(product.get("name"), "Без назви");
        String price = safeToString(product.get("price"), "0");
        String unit = safeToString(product.get("unit"), "шт").toLowerCase();
        String description = safeToString(product.get("description"), "");
        String photo = safeToString(product.get("photo"), "");

        // Виробник
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
        } catch (Exception ignored) {}

        boolean isCustomOrder = unit.equals("під замовлення");

        // ----- Формування тексту -----
        StringBuilder sb = new StringBuilder();
        sb.append("📦 ").append(name).append("\n");

        if (isCustomOrder) {
            sb.append("💬 Цей товар доступний лише під замовлення.\n")
                    .append("📞 Якщо хочеш дізнатись ціну — натисни кнопку *“Написати консультанту”*, і я миттю передам запит! 🔧😉");
        } else {
            sb.append("💰 Ціна: ").append(price).append(" грн за ").append(unit);
        }

        if (!manufacturer.isEmpty() && !"null".equalsIgnoreCase(manufacturer.trim()))
            sb.append("\n🏭 Виробник: ").append(manufacturer);

        if (!description.isEmpty() && !"null".equalsIgnoreCase(description.trim()))
            sb.append("\n📖 ").append(description);

        // ----- КНОПКИ -----
        KeyboardRow row1 = new KeyboardRow();
        row1.add("⬅ Назад");
        row1.add("🔍 Пошук");
        row1.add("➡ Далі");

        KeyboardRow row2 = new KeyboardRow();

        if (isCustomOrder) {
            row2.add("✉ Написати консультанту");
        } else {
            row2.add("🛒 Додати в кошик");
        }

        row2.add("🛍️ Перейти в кошик");

        KeyboardRow row3 = new KeyboardRow();
        row3.add(BACK_BUTTON);

        List<KeyboardRow> keyboard = List.of(row1, row2, row3);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(keyboard);
        markup.setResizeKeyboard(true);

        // ----- Фото або текст -----
        if (!photo.isEmpty() && !"null".equalsIgnoreCase(photo.trim())) {
            sendPhotoFromResources(chatId.toString(), photo, sb.toString(), markup);
        } else {
            sendTextWithMarkup(chatId, sb.toString(), markup);
        }

        // Перехід на наступний товар
        productIndex.put(chatId, (index + 1) % products.size());
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

    // 🔸 Показ замовлення користовувача Адміну
    private void createOrderAdminMenu(String chatId, Map<String, Object> order, Long userId) {
        StringBuilder sb = new StringBuilder();

        sb.append("🆔 User ID: ").append(userId).append("\n")
                .append("🔢 Код замовлення: ").append(order.getOrDefault("orderCode", "-")).append("\n")
                .append("📦 Тип замовлення: ").append(order.getOrDefault("deliveryType", "Невідомо")).append("\n\n");

        // 🔹 Спосіб оплати
        String paymentMethod = String.valueOf(order.getOrDefault("paymentmethod", "Готівка"));
        String paymentStatus = String.valueOf(order.getOrDefault("paymentstatus", "Очікується"));

        sb.append("💳 Спосіб оплати: ").append(paymentMethod).append("\n")
                .append("📌 Статус оплати: ").append(paymentStatus).append("\n\n");

        // 🔹 Тип доставки
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
                .append("💳 Картка (магазину): ").append(order.getOrDefault("card", "-")).append("\n\n");

        // 🛒 Товари
        String itemsStr = (String) order.get("item");

        if (itemsStr != null && !itemsStr.isEmpty()) {

            String[] itemArr = itemsStr.split(";");
            int i = 1;

            for (String s : itemArr) {

                if (s.isBlank()) continue;

                String[] parts = s.split(":");
                String name = parts[0];

                double price = 0;
                int quantity = 1;

                try {
                    if (parts.length > 1)
                        price = Double.parseDouble(parts[1]);

                    if (parts.length > 2)
                        quantity = Integer.parseInt(parts[2]);

                } catch (Exception ignored) {}

                double lineTotal = price * quantity;

                sb.append(i++).append(". 🛒 ").append(name).append("\n")
                        .append("   🔢 Кількість: ").append(quantity).append("\n")
                        .append("   💵 Ціна за 1: ").append(price).append(" грн\n")
                        .append("   💰 Разом: ").append(lineTotal).append(" грн\n\n");
            }
        }

        // 🔹 Загальна сума
        double total = 0.0;
        Object totalObj = order.get("total");

        if (totalObj instanceof Number)
            total = ((Number) totalObj).doubleValue();
        else if (totalObj != null) {
            try {
                total = Double.parseDouble(totalObj.toString());
            } catch (Exception ignored) {}
        }

        sb.append("💳 **Всього до оплати:** ").append(total).append(" грн\n");

        // 🔘 Кнопки
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("✅ Підтвердити");
        row1.add("❌ Відхилити");
        row1.add("🗑️ Видалити замовлення");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("⏮️ Назад");
        row2.add("🔍 Пошук коду");
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

        // Рядки з кнопками пошуку
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

    private void handleAdminSearchSource(Long userId, String chatId, String text)
            throws TelegramApiException {

        if ("🔍 Пошук у MySQL".equals(text)) {

            adminSearchSource.put(userId, "mysql");
            userStates.put(userId, "awaiting_search");

            sendText(chatId, "Введіть ключові слова для пошуку у MySQL:");
            return;
        }

        if ("🔍 Пошук у YAML".equals(text)) {

            adminSearchSource.put(userId, "yaml");
            userStates.put(userId, "awaiting_search");

            sendText(chatId, "Введіть ключові слова для пошуку у YAML:");
            return;
        }

        sendText(chatId, "❌ Невідома опція. Оберіть із меню.");
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

    private void showAdminOrder(Long userId, String chatId) {

        // 1️⃣ Завантажуємо список замовлень ТІЛЬКИ ОДИН РАЗ
        List<Map<String, Object>> list = adminOrders.get(userId);

        if (list == null) {

            list = new ArrayList<>();

            String sql = "SELECT * FROM orders ORDER BY id DESC";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();

                    order.put("id", rs.getLong("id"));
                    order.put("userId", rs.getLong("userId"));
                    order.put("orderCode", rs.getString("orderCode"));
                    order.put("deliveryType", rs.getString("deliveryType"));

                    String city = rs.getString("city");
                    order.put("city", city);
                    order.put("address", city);
                    order.put("postOffice", city);

                    order.put("fullName", rs.getString("fullName"));
                    order.put("phone", rs.getString("phone"));
                    order.put("card", rs.getString("card"));
                    order.put("status", rs.getString("status"));
                    order.put("item", rs.getString("item"));
                    order.put("total", rs.getDouble("total"));

                    list.add(order);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                sendText(chatId, "❌ Помилка читання замовлень.");
                return;
            }

            adminOrders.put(userId, list);
            adminOrderIndex.put(userId, 0);
        }

        // 2️⃣ Якщо замовлень немає
        if (list.isEmpty()) {
            sendText(chatId, "❌ Замовлень немає.");
            return;
        }

        // 3️⃣ Беремо поточний індекс
        int idx = adminOrderIndex.getOrDefault(userId, 0);

        // 4️⃣ Корекція індекса (щоб не вилітав)
        if (idx < 0) idx = 0;
        if (idx >= list.size()) idx = list.size() - 1;

        adminOrderIndex.put(userId, idx);

        // 5️⃣ Беремо реальне замовлення
        Map<String, Object> order = list.get(idx);
        Long clientId = (Long) order.get("userId");

        // 6️⃣ Відображення
        createOrderAdminMenu(chatId, order, clientId);
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

    private void sendMessageSafely(SendMessage msg) {
        try {
            sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSearchProduct(String chatId) throws TelegramApiException {
        Long userId = Long.valueOf(chatId);

        List<Map<String, Object>> list = searchResults.get(userId);
        int index = searchIndex.getOrDefault(userId, 0);

        if (list == null || list.isEmpty()) {
            sendText(chatId, "❌ Результатів пошуку немає.");
            return;
        }

        if (index < 0) index = 0;
        if (index >= list.size()) index = list.size() - 1;

        searchIndex.put(userId, index);

        Map<String, Object> product = list.get(index);
        lastShownProduct.put(userId, product);

        sendProductMessage(Long.valueOf(chatId), product);
    }

    private Map<String, Object> findAdminOrderByCode(String code) {
        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM orders WHERE orderCode = ? LIMIT 1"
            );
            stmt.setString(1, code);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> order = new HashMap<>();

                order.put("id", rs.getLong("id"));
                order.put("userId", rs.getLong("userId"));
                order.put("orderCode", rs.getString("orderCode"));
                order.put("deliveryType", rs.getString("deliveryType"));
                order.put("city", rs.getString("city"));
                order.put("address", rs.getString("address"));
                order.put("postOffice", rs.getString("postOffice"));
                order.put("fullName", rs.getString("fullName"));
                order.put("phone", rs.getString("phone"));
                order.put("card", rs.getString("card"));
                order.put("status", rs.getString("status"));
                order.put("item", rs.getString("item"));
                order.put("total", rs.getDouble("total"));
                order.put("date", rs.getString("date"));

                return order;
            }

            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void forwardTextToConsultant(Long userId, Long chatIdLong, String text) {
        try {
            SendMessage msg = new SendMessage(
                    CONSULTANT_CHAT_ID.toString(),
                    "📩 Нове повідомлення від покупця:\n" +
                            "🆔 User ID: " + userId + "\n" +
                            "💬 Chat ID: " + chatIdLong + "\n\n" +
                            text
            );

            execute(msg);

            // показуємо кнопку відповіді
            sendReplyButtonToConsultant(userId);

            sendText(chatIdLong, "Ваше повідомлення передано консультанту!");
            userStates.remove(userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forwardPhotoToConsultant(Long userId, Long chatIdLong, Update update) {
        try {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize largest = photos.get(photos.size() - 1);
            String fileId = largest.getFileId();

            SendPhoto msg = new SendPhoto();
            msg.setChatId(CONSULTANT_CHAT_ID.toString());
            msg.setPhoto(new InputFile(fileId));
            msg.setCaption(
                    "📩 Фото від покупця:\n" +
                            "🆔 User ID: " + userId + "\n" +
                            "💬 Chat ID: " + chatIdLong
            );

            execute(msg);

            sendReplyButtonToConsultant(userId);

            sendText(chatIdLong, "Ваше фото передано консультанту!");
            userStates.remove(userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendReplyButtonToConsultant(Long targetUserId) throws TelegramApiException {

        consultantReplyTarget.put(CONSULTANT_CHAT_ID, targetUserId);

        SendMessage replyBtn = new SendMessage(
                CONSULTANT_CHAT_ID.toString(),
                "🤝 Покупець чекає на відповідь. Хочете відповісти?"
        );

        ReplyKeyboardMarkup km = new ReplyKeyboardMarkup();
        km.setResizeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        row.add("💬 Відповісти покупцю");

        km.setKeyboard(List.of(row));
        replyBtn.setReplyMarkup(km);

        execute(replyBtn);
    }

    private String botAutoAnswer(String text) {
        text = text.toLowerCase();

        if (text.contains("цемент") || text.contains("цем") || text.contains("cement"))
            return "Так, цемент є в наявності ✔️";

        if (text.contains("ціна") || text.contains("скільки"))
            return "Вкажіть, будь ласка, точну назву товару — скажу точну ціну.";

        if (text.contains("доставка"))
            return "Так, є доставка по місту, самовивіз і Нова Пошта 🚚";

        return null;
    }

    private void askPaymentMethod(Long userId, Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("💵 Готівка");
        row1.add("💳 Оплата карткою");

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);

        markup.setKeyboard(List.of(row1, row2));

        sendMessage(chatId.toString(),
                "Оберіть спосіб оплати:",
                markup);
    }

    private void createOrderWithPayment(Long userId,
                                        Long chatId,
                                        String method,
                                        boolean isOnline) {

        String orderCode = tempOrderCode.get(userId);
        String deliveryType = tempDeliveryType.get(userId);
        String addressOrCityOrPost = tempAddress.get(userId);
        String fullname = tempFullname.get(userId);
        String phone = tempPhone.get(userId);
        String card = tempCard.get(userId);
        String items = tempOrderItems.get(userId);
        double total = tempTotal.getOrDefault(userId, 0.0);

        if (orderCode == null || deliveryType == null || items == null) {
            sendText(chatId.toString(), "❌ Помилка: не вдалося знайти дані замовлення.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO orders " +
                            "(userId, orderCode, deliveryType, city, address, postOffice, " +
                            " fullName, phone, card, item, total, paymentmethod, paymentstatus, status, date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"
            );

            stmt.setLong(1, userId);
            stmt.setString(2, orderCode);
            stmt.setString(3, deliveryType);

            // 🔹 Розподіл по полях в залежності від типу доставки
            String city = null;
            String address = null;
            String postOffice = null;

            switch (deliveryType) {
                case "Самовивіз" -> city = addressOrCityOrPost;
                case "Доставка по місту" -> address = addressOrCityOrPost;
                case "Нова пошта" -> postOffice = addressOrCityOrPost;
            }

            stmt.setString(4, city);
            stmt.setString(5, address);
            stmt.setString(6, postOffice);
            stmt.setString(7, fullname);
            stmt.setString(8, phone);
            stmt.setString(9, card);
            stmt.setString(10, items);
            stmt.setDouble(11, total);
            stmt.setString(12, method);                        // paymentmethod

            // paymentstatus
            String paymentStatus = isOnline ? "Очікується" : "Готівка";
            stmt.setString(13, paymentStatus);

            stmt.setString(14, "Нове");                        // status (замовлення)

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId.toString(), "❌ Сталася помилка при створенні замовлення.");
            return;
        }

        // 🔹 Очищаємо тимчасові дані
        tempDeliveryType.remove(userId);
        tempAddress.remove(userId);
        tempFullname.remove(userId);
        tempPhone.remove(userId);
        tempCard.remove(userId);
        tempOrderCode.remove(userId);
        tempOrderItems.remove(userId);
        tempTotal.remove(userId);
        userCart.remove(userId);
        userStates.remove(userId);

        // 🔹 Якщо онлайн оплата — генеруємо лінк
        if (isOnline) {
            String payUrl = generatePaymentUrl((int) total, orderCode);
            sendPaymentButton(chatId, payUrl);

            sendText(chatId.toString(),
                    "✅ Ваше замовлення сформовано!\n" +
                            "🔢 Код замовлення: " + orderCode + "\n" +
                            "💰 Сума: " + total + " грн\n\n" +
                            "Для оплати натисніть кнопку нижче 👇");
        } else {
            sendText(chatId.toString(),
                    "✅ Ваше замовлення оформлено!\n" +
                            "🔢 Код: " + orderCode + "\n" +
                            "Спосіб оплати: Готівка при отриманні.\n" +
                            "Дякуємо за замовлення! 🧱");
        }
    }

    private void sendPaymentButton(Long chatId, String url) {
        InlineKeyboardButton payButton = InlineKeyboardButton.builder()
                .text("💳 Оплатити онлайн")
                .url(url)
                .build();

        List<List<InlineKeyboardButton>> rows = List.of(List.of(payButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("💳 Натисніть кнопку, щоб перейти до оплати:");
        msg.setReplyMarkup(markup);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String generatePaymentUrl(int amount, String orderCode) {
        long orderDate = System.currentTimeMillis() / 1000;

        // Назва товару для WayForPay
        String productName = "Замовлення " + orderCode;

        // TODO: Додай генерацію підпису по документації WayForPay
        // Зараз — спрощений варіант без підпису (для тестів)
        String encodedName = URLEncoder.encode(productName, StandardCharsets.UTF_8);

        return "https://secure.wayforpay.com/pay?" +
                "merchantAccount=" + MERCHANT_ACCOUNT +
                "&merchantDomainName=" + MERCHANT_DOMAIN +
                "&orderReference=" + orderCode +
                "&orderDate=" + orderDate +
                "&amount=" + amount +
                "&currency=UAH" +
                "&productName=" + encodedName +
                "&productPrice=" + amount +
                "&productCount=1";
    }
}