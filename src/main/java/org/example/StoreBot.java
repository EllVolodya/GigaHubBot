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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Document;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.time.LocalDateTime;

public class StoreBot extends TelegramLongPollingBot {

    private final String botUsername = "GigaHubAssistant_bot";

    // 🔹 Користувацькі стани
    private final Map<Long, String> currentCategory = new HashMap<>();
    private final Map<Long, String> currentSubcategory = new HashMap<>();
    private Map<Long, String> currentFilter = new HashMap<>();
    private Map<Long, String> currentSubfilter = new HashMap<>();

    private Map<Long, String> selectedFilter = new HashMap<>();

    private final Map<Long, Integer> productIndex = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> productsToEdit = new HashMap<>();
    protected Map<Long, Map<String, Object>> lastShownProduct = new HashMap<>();
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> userPhones = new HashMap<>();
    private final Map<Long, String> userState = new HashMap<>();
    private Map<Long, String> userSelectedSubcategory = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> userCart = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> userOrders = new HashMap<>();
    private Map<Long, String> userLastCategory = new HashMap<>();
    private final Map<Long, List<String>> userFilters = new HashMap<>();

    private final Map<Long, String> selectedSubcategoryForFilter = new HashMap<>();
    private Map<Long,String> selectedFilterForSubfilter = new HashMap<>();
    private Map<Long, Integer> editItemIndex = new HashMap<>();
    private final Map<Long, Boolean> waitingForQuantity = new HashMap<>();
    private final Map<Long, Boolean> waitingForCatalogSearch = new HashMap<>();
    private final Map<Long, Integer> searchIndex = new HashMap<>();
    private final Map<Long, Double> paintTemp = new HashMap<>();
    private final Map<Long, Double> paintLayers = new HashMap<>();
    private final Map<Long, String> lastUserMessage = new HashMap<>();
    private final Map<Long, List<Map<String, Object>>> userProductLists = new HashMap<>();

    private final StateManager stateManager = new StateManager();
    private final Map<Long, String> state = new HashMap<>();

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

    //Права
    private final List<Long> ADMINS = List.of(620298889L, 1030917576L, 533570832L);// тут айді продавців меню
    private final List<Long> DEVELOPERS = List.of(620298889L, 1030917576L, 533570832L, 1976709178L, 404670376L); // тут айді розробників меню (Убрать потом 1976709178Артур, 404670376Віка)
    private final Long CONSULTANT_CHAT_ID = 620298889L;

    // 🔹 Адмінські стани
    private final Map<Long, Long> adminReplyTarget = new HashMap<>();

    private final Map<Long, List<ExcelToMySqlImporter.ExcelProduct>> adminExcelResults = new HashMap<>();
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
    private Map<Long, Integer> filterPage = new HashMap<>();
    private Map<Long, Map<String, Object>> adminSelectedProduct = new HashMap<>(); // 🔹 один товар
    private Map<Long, List<Map<String, Object>>> adminSelectedProducts = new HashMap<>(); // 🔹 список товарів

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

    private static final String VIDEO_HELP_FILE_ID =
            "BAACAgIAAyEFAATOLPSsAAMCaWdUVJPb_3IkmZP6ydnb7_KbVd4AAqefAAKeUkFLURTvQ-M9SPE4BA";

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

        if (update.hasMessage() && update.getMessage().hasVideo()) {

            Message message = update.getMessage();
            String chatId = message.getChatId().toString();

            String fileId = message.getVideo().getFileId();
            Integer duration = message.getVideo().getDuration();
            Integer width = message.getVideo().getWidth();
            Integer height = message.getVideo().getHeight();

            System.out.println("========== VIDEO RECEIVED ==========");
            System.out.println("FILE_ID: " + fileId);
            System.out.println("DURATION: " + duration);
            System.out.println("RESOLUTION: " + width + "x" + height);
            System.out.println("====================================");

            SendMessage reply = new SendMessage();
            reply.setChatId(chatId);
            reply.setText("🎥 VIDEO FILE_ID:\n\n" + fileId);

            try {
                execute(reply);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            return;
        }

        Long userId = update.getMessage().getFrom().getId();

        String chatId = update.getMessage().getChatId().toString();

        Long realChatId = update.getMessage().getChatId();

        String text = update.getMessage().getText() != null
                ? update.getMessage().getText().trim()
                : "";

        String state = userStates.get(userId);

        // 🔥 Обробка кількості
        if ("awaiting_quantity".equals(state)) {

            int qty;

            try {
                qty = Integer.parseInt(text.trim());

                if (qty <= 0) {
                    sendText(chatId, "❌ Кількість має бути більше 0.");
                    return;
                }

            } catch (NumberFormatException e) {
                sendText(chatId, "❌ Введіть число (1, 2, 3...)");
                return;
            }

            Map<String, Object> product =
                    (Map<String, Object>) tempStorage.get(userId + "_product");

            if (product == null) {
                sendText(chatId, "❌ Товар не знайдено.");
                return;
            }

            Map<String, Object> cartItem = new HashMap<>(product);
            cartItem.put("quantity", qty);

            List<Map<String, Object>> cart =
                    userCart.getOrDefault(userId, new ArrayList<>());

            boolean found = false;

            for (Map<String, Object> item : cart) {

                if (item.get("id") != null &&
                        item.get("id").toString().equals(cartItem.get("id").toString())) {

                    int oldQty = Integer.parseInt(item.get("quantity").toString());
                    item.put("quantity", oldQty + qty);

                    found = true;
                    break;
                }
            }

            if (!found) {
                cart.add(cartItem);
            }

            userCart.put(userId, cart);

            sendText(chatId,
                    "✅ Додано в кошик: " +
                            product.get("name") +
                            " (" + qty + " шт)");

            lastShownProduct.put(userId, product);

            tempStorage.remove(userId + "_product");

            // 🔥 ВАЖЛИВИЙ ФІКС — НЕ remove, а ставимо стан кошика
            userStates.put(userId, "in_cart");

            // 🔥 одразу показуємо кошик
            showCartNice(userId, chatId);

            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
            markup.setResizeKeyboard(true);

            KeyboardRow row1 = new KeyboardRow();
            row1.add("🚛 Оформити замовлення");

            KeyboardRow row2 = new KeyboardRow();
            row2.add(BACK_BUTTON);

            markup.setKeyboard(List.of(row1, row2));

            sendMessage(chatId, "📦 Ваш кошик:", markup);

            return;
        }

        // 🔥 ОБРОБКА КОШИКА (ГЛОБАЛЬНО)
        if ("in_cart".equals(state)) {

            System.out.println("[STATE] in_cart TEXT=" + text);

            if (text != null && text.contains("Назад")) {
                try {
                    handleBack(userId, chatId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (text.equals("🚛 Оформити замовлення")) {

                userStates.put(userId, "awaiting_phone");

                sendText(chatId, "📞 Введіть номер телефону для оформлення замовлення:");

                return;
            }

            sendText(chatId, "📦 Ви в кошику. Оберіть дію або натисніть Назад.");
            return;
        }

        if (text != null && text.contains("Оформити замовлення")) {

            List<Map<String, Object>> cart = userCart.get(userId);

            if (cart == null || cart.isEmpty()) {
                sendText(chatId, "❌ Кошик порожній.");
                return;
            }

            userStates.put(userId, "awaiting_phone");

            sendText(chatId,
                    "📞 Введіть номер телефону для оформлення замовлення:");

            return;
        }

        // 🔹 Пагінація фільтрів
        if (text.equals("Продовжити")) {

            int page = filterPage.getOrDefault(userId, 0);
            page++;

            filterPage.put(userId, page);

            if (state != null && state.contains("filter")) {
                // 🔥 адмін режим
                sendMessageSafely(
                        createFilterSelectMenuAdmin(userId, chatId, page)
                );
            } else {
                // 🔹 юзер режим
                sendMessageSafely(
                        createFilterSelectMenu(userId, chatId, page)
                );
            }

            return;
        }

        if (text.equals("Попередні")) {

            int page = filterPage.getOrDefault(userId, 0);
            page--;

            if (page < 0) page = 0;

            filterPage.put(userId, page);

            if (state != null && state.contains("filter")) {
                // 🔥 адмін режим
                sendMessageSafely(
                        createFilterSelectMenuAdmin(userId, chatId, page)
                );
            } else {
                // 🔹 юзер режим
                sendMessageSafely(
                        createFilterSelectMenu(userId, chatId, page)
                );
            }

            return;
        }

        // 🗑 Видалення товару з кошика
        if ("awaiting_remove_item".equals(userStates.get(userId))) {

            // 🔙 Якщо натиснули Назад
            if (text.equals("⬅️ Назад")) {

                userStates.remove(userId);

                sendText(chatId, "↩️ Видалення товару скасовано.");

                try {
                    showCart(userId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при відкритті кошика.");
                }

                return;
            }

            try {

                int index = Integer.parseInt(text) - 1;

                List<Map<String, Object>> cart = userCart.get(userId);

                if (cart == null || cart.isEmpty()) {
                    sendText(chatId, "🧺 Кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                if (index < 0 || index >= cart.size()) {
                    sendText(chatId,
                            "❌ Невірний номер товару.\n" +
                                    "Введіть номер від 1 до " + cart.size());
                    return;
                }

                Map<String, Object> removedItem = cart.remove(index);

                sendText(chatId,
                        "🗑 Видалено товар:\n" +
                                "📦 " + removedItem.get("name"));

                userStates.remove(userId);

                try {
                    showCart(userId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при відкритті кошика.");
                }

            } catch (NumberFormatException e) {

                sendText(chatId,
                        "❌ Введіть номер товару цифрою.\n" +
                                "Наприклад: 1");
            }

            return;
        }

        // 🔹 Створення нового фільтра
        if ("awaiting_new_filter".equals(state)) {

            if (text.equals("⬅️ Назад")) {
                userStates.remove(userId);
                sendMessageSafely(createFiltersAdminMenu(chatId));
                return;
            }

            createFilter(text.trim());

            userStates.remove(userId);

            sendText(chatId, "✅ Фільтр створено: " + text);

            sendMessageSafely(createFiltersAdminMenu(chatId));

            return;
        }

        if ("awaiting_filter_for_subfilter".equals(state)) {

            if (BACK_BUTTON.equals(text)) {
                userStates.put(userId, "editing");
                sendMessageSafely(createEditMenu(chatId, userId));
                return;
            }

            if (text == null || text.isBlank()) {
                sendText(chatId, "❌ Оберіть або введіть фільтр.");
                return;
            }

            List<String> filters = catalogSearcher.getFiltersAdmin();

            // 🔥 ПІДТРИМКА ВВОДУ
            String input = text.trim();

            Optional<String> match = filters.stream()
                    .filter(f -> f.equalsIgnoreCase(input))
                    .findFirst();

            if (match.isEmpty()) {
                sendText(chatId, "❌ Фільтр не знайдено.");
                return;
            }

            String filter = match.get();

            selectedFilter.put(userId, filter);

            CatalogSearcher searcher = new CatalogSearcher();
            boolean hasSubfilters = searcher.hasSubfilters(filter);

            // 🔥 ЯКЩО НЕМА ПІДФІЛЬТРІВ → ДОДАЄМО ОДРАЗУ
            if (!hasSubfilters) {

                handleAddToFilter(userId, chatId, filter, null);
                return;
            }

            // 🔥 ЯКЩО Є → ЙДЕМО ДАЛІ
            userStates.put(userId, "awaiting_subfilter_select");

            sendText(chatId, "🔽 Оберіть або введіть підфільтр:");
            sendMessageSafely(createSubfilterMenu(filter, chatId));

            return;
        }

        if ("awaiting_filter_for_product_subfilter".equals(state)) {

            if (BACK_BUTTON.equals(text)) {
                userStates.put(userId, "editing");
                sendMessageSafely(createEditMenu(chatId, userId));
                return;
            }

            if (text == null || text.isBlank()) {
                sendText(chatId, "❌ Оберіть або введіть фільтр.");
                return;
            }

            List<String> filters = catalogSearcher.getFiltersAdmin();

            String input = text.trim().toLowerCase();

            List<String> found = filters.stream()
                    .filter(f -> f.toLowerCase().contains(input))
                    .toList();

            if (found.isEmpty()) {
                sendText(chatId, "❌ Фільтр не знайдено.");
                return;
            }

            String filter;

            if (found.size() == 1) {
                filter = found.get(0);
            } else {
                sendText(chatId, "🔎 Знайдено кілька фільтрів, оберіть:");
                sendMessageSafely(createFilterSelectMenuAdmin(userId, chatId, 0));
                return;
            }

            selectedFilter.put(userId, filter);

            boolean hasSubfilters = catalogSearcher.hasSubfilters(filter);

            if (!hasSubfilters) {
                handleAddToFilter(userId, chatId, filter, null);
                return;
            }

            userStates.put(userId, "awaiting_subfilter_select");

            sendText(chatId, "🔽 Оберіть або введіть підфільтр:");
            sendMessageSafely(createSubfilterMenu(filter, chatId));

            return;
        }

        if ("awaiting_filter_select".equals(state)) {

            if (BACK_BUTTON.equals(text)) {
                userStates.remove(userId);
                sendMessageSafely(createEditMenu(chatId, userId));
                return;
            }

            if (text == null || text.isBlank()) {
                sendText(chatId, "❌ Оберіть фільтр.");
                return;
            }

            Map<String, Object> product = adminSelectedProduct.get(userId);

            if (product == null) {
                sendText(chatId, "❌ Товар не знайдено.");
                userStates.remove(userId);
                return;
            }

            CatalogSearcher searcher = new CatalogSearcher();

            boolean success = searcher.addProductToFilter(product, text, null);

            userStates.remove(userId);

            sendText(chatId,
                    success
                            ? "✅ Додано у фільтр: " + text
                            : "❌ Помилка."
            );

            sendMessageSafely(createEditMenu(chatId, userId));
        }

        // 🔹 Вибір підфільтра
        if ("awaiting_subfilter_select".equals(state)) {

            if (BACK_BUTTON.equals(text)) {
                userStates.put(userId, "awaiting_filter_for_product_subfilter");
                sendMessageSafely(createFilterSelectMenuAdmin(userId, chatId, 0));
                return;
            }

            String filter = selectedFilter.get(userId);

            if (filter == null) {
                sendText(chatId, "❌ Помилка.");
                userStates.remove(userId);
                return;
            }

            String subfilter = text == null ? null : text.trim();

            if (subfilter == null || subfilter.isBlank()) {
                sendText(chatId, "❌ Оберіть або введіть підфільтр.");
                return;
            }

            handleAddToFilter(userId, chatId, filter, subfilter);
            return;
        }

        if ("awaiting_edit_item".equals(userStates.get(userId))) {

            try {

                int index = Integer.parseInt(text) - 1;

                List<Map<String,Object>> cart = userCart.get(userId);

                if (index < 0 || index >= cart.size()) {
                    sendText(chatId,"❌ Невірний номер товару.");
                    return;
                }

                editItemIndex.put(userId,index);

                userStates.put(userId,"awaiting_new_quantity");

                sendText(chatId,
                        "🔢 Введіть нову кількість:");

            } catch (Exception e) {

                sendText(chatId,"❌ Введіть номер товару цифрою.");
            }

            return;
        }

        if ("awaiting_new_quantity".equals(userStates.get(userId))) {

            try {

                int newQty = Integer.parseInt(text);

                if (newQty <= 0) {
                    sendText(chatId,"❌ Кількість має бути більше 0.");
                    return;
                }

                int index = editItemIndex.get(userId);

                List<Map<String,Object>> cart = userCart.get(userId);

                Map<String,Object> item = cart.get(index);

                item.put("quantity",newQty);

                double price = Double.parseDouble(item.get("price").toString());

                double sum = price * newQty;

                sendText(chatId,
                        "✅ Кількість змінено!\n\n" +
                                "📦 " + item.get("name") + "\n" +
                                "🔢 Нова кількість: " + newQty + "\n" +
                                "💰 Нова сума: " + sum + " грн");

                userStates.remove(userId);
                editItemIndex.remove(userId);

                showCart(userId);

            } catch (Exception e) {

                sendText(chatId,"❌ Введіть число.");
            }

            return;
        }

        if (state != null) {

            System.out.println("[STATE] " + state + " TEXT=" + text);

            handleState(userId, chatId, text, state, update);

            return;
        }

        System.out.println("[DEBUG] Received message from userId=" + userId + ": '" + text + "' (state=" + state + ")");

        String normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cf}\\p{Zs}]+", " ")
                .trim();

        if (update.hasMessage()) {

            long telegramId = update.getMessage().getFrom().getId();
            String currentChatId = update.getMessage().getChatId().toString();

            userManager.updateChatId(telegramId, currentChatId);
        }

        if ("choose_yaml_product".equals(state)) {
            handleChooseYamlProduct(userId, chatId, normalizedText);
            return;
        }

        if (text.startsWith("/start")) {

            Long chatIdLong = update.getMessage().getChatId();
            String chatIdStr = chatIdLong.toString(); // 🔥 ВИНЕСЛИ СЮДИ

            try {
                clearUserState(userId);

                String inviteCode = text.replace("/start", "").trim();

                UserManager userManager = new UserManager();

                SendMessage msg = userManager.registerUser(
                        userId,
                        update.getMessage().getFrom().getFirstName(),
                        chatIdStr
                );

                if (msg != null && !inviteCode.isEmpty()) {

                    System.out.println("🔥 inviteCode = " + inviteCode);

                    InviteManager inviteManager = new InviteManager();
                    boolean ok = inviteManager.incrementInviteNumber(inviteCode);

                    if (ok) System.out.println("✅ Лічильник збільшено");
                    else    System.out.println("❌ Invite не знайдено");
                }

                execute(createUserMenu(chatIdStr, userId));

            } catch (SQLException e) {
                e.printStackTrace();
                sendText(chatIdStr, "❌ Помилка бази даних.");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(chatIdStr, "❌ Помилка Telegram.");
            }

            return;
        }

        if (waitingForQuantity.getOrDefault(realChatId, false)) {

            // 🔹 Якщо користувач натиснув Назад
            if (text.equals("⬅️ Назад")) {

                waitingForQuantity.put(realChatId, false);

                sendText(realChatId,
                        "↩️ Ви скасували додавання товару до кошика.");

                try {
                    showRemainingProductList(realChatId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                return;
            }

            try {

                int requestedQty = Integer.parseInt(text);

                if (requestedQty <= 0) {
                    sendText(realChatId,
                            "❌ Кількість має бути більше 0.\n" +
                                    "Введіть ще раз:");
                    return;
                }

                Map<String, Object> product =
                        lastShownProduct.get(realChatId);

                if (product == null) {
                    sendText(realChatId,
                            "❌ Помилка: товар не знайдено.");
                    waitingForQuantity.put(realChatId, false);
                    return;
                }

                // 🔹 Отримуємо кількість зі складу
                Object qtyObj = product.get("quantity");

                int maxAvailable = -1;

                if (qtyObj instanceof Number) {
                    maxAvailable = ((Number) qtyObj).intValue();
                }
                else if (qtyObj instanceof String) {
                    try {
                        String cleaned = ((String) qtyObj)
                                .replaceAll("\\D+", "");

                        if (!cleaned.isEmpty()) {
                            maxAvailable = Integer.parseInt(cleaned);
                        }
                    } catch (Exception ignored) {}
                }

                // 🔴 Якщо склад = 0
                if (maxAvailable == 0) {
                    sendText(realChatId,
                            "⛔ На складі зараз: 0 шт.\n" +
                                    "Будь ласка, уточніть наявність у консультанта 🙏");
                    waitingForQuantity.put(realChatId, false);
                    return;
                }

                // 🔴 Якщо користувач просить більше ніж є
                if (maxAvailable > 0 && requestedQty > maxAvailable) {

                    sendText(realChatId,
                            "⚠️ На складі доступно: " + maxAvailable + " шт.\n" +
                                    "Введіть меншу кількість 👇");

                    return;
                }

                // ✅ Додаємо товар у кошик
                Map<String, Object> cartItem =
                        new HashMap<>(product);

                cartItem.put("quantity", requestedQty);

                userCart
                        .computeIfAbsent(realChatId,
                                k -> new ArrayList<>())
                        .add(cartItem);

                waitingForQuantity.put(realChatId, false);

                sendText(realChatId,
                        "🧺 Товар додано у кошик!\n\n" +
                                "📦 " + product.get("name") + "\n" +
                                "🔢 Замовлено: " + requestedQty +
                                (maxAvailable > 0 ?
                                        "\n📦 Було на складі: " + maxAvailable : "")
                );

                try {
                    showRemainingProductList(realChatId);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                return;

            } catch (NumberFormatException e) {

                sendText(realChatId,
                        "❌ Це не число.\n" +
                                "Введіть кількість ще раз:");

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

            // зберігаємо відповідь в історію
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
        photoHandler.handleUpdate(userId, chatId, update);

        if (text == null || text.isBlank()) {
            return;
        }

        // 🔎 Режим пошуку товару
        if ("waiting_for_search".equals(state)) {

            // 🔴 Ігноруємо пусті повідомлення
            if (text == null || text.isBlank()) {
                return;
            }

            try {

                // 🛒 Додати в кошик
                if (text.equals(ADD_TO_CART_BUTTON) || text.equals("🛒 Додати в кошик")) {
                    addToCart(userId);
                    return;
                }

                // 🛍 Перейти в кошик
                if (text.equals(VIEW_CART_BUTTON) || text.equals("🛍️ Перейти в кошик")) {
                    showCart(userId);
                    return;
                }

                // ⬅️ Назад
                if (text.equals(BACK_BUTTON) || text.equals("⬅️ Назад")) {
                    userStates.remove(userId);
                    sendUserMenu(userId);
                    return;
                }

                // 🚚 ВИБІР ДОСТАВКИ (важливо)
                if (text.equals("🏬 Самовивіз")
                        || text.equals("📦 Доставка по місту")) {

                    userStates.remove(userId); // виходимо з пошуку
                    return;
                }

                // 🔢 Вибір товару по номеру
                if (text.matches("\\d+")) {

                    ProductSearchManager searchHandler =
                            new ProductSearchManager(this);

                    searchHandler.handleSearchNumber(userId, chatId, text);
                    return;
                }

                // 🔎 Пошук по тексту
                ProductSearchManager searchHandler =
                        new ProductSearchManager(this);

                searchHandler.performSearch(userId, chatId, text);
                return;

            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendText(chatId, "❌ Помилка під час пошуку.");
                return;
            }
        }

        // ===== Optional: handle feedback / other states =====
        if (update.getMessage().hasText()) {
            text = update.getMessage().getText().trim();
        }

        if (text.matches("\\d+") && userProductLists.containsKey(realChatId)) {
            try {
                handleProductSelection(realChatId, Integer.parseInt(text));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return; // ❗ ОБОВʼЯЗКОВО
        }

        //if (text.isBlank()) return;

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

                    case "selecting_product" -> {
                        if (text.matches("\\d+")) {
                            ProductSearchManager searchHandler =
                                    new ProductSearchManager(this);
                            searchHandler.handleSearchNumber(userId, chatId, text);
                            return;
                        }
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
                        return;
                    }
                }
            }

            // 🔹 Основні команди (кнопки)
            if (text == null) return;

            switch (text) {
                case "🧱 Каталог товарів" -> sendCategories(userId);
                case "\uD83D\uDED2 Кошик" -> {
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

                case "🛒 Замовити товар" -> {

                    List<Map<String, Object>> cart = userCart.get(userId);
                    Map<String, Object> product = lastShownProduct.get(userId);

                    if (product == null) {
                        sendText(chatId, "❌ Спочатку відкрийте товар.");
                        return;
                    }

                    // 🔥 якщо вже є в кошику → показуємо
                    if (cart != null && !cart.isEmpty()) {

                        boolean alreadyInCart = cart.stream().anyMatch(item ->
                                item.get("id").toString().equals(product.get("id").toString())
                        );

                        if (alreadyInCart) {

                            showCartNice(userId, chatId);

                            // 🔥 ФІКС: обовʼязково ставимо state
                            userStates.put(userId, "in_cart");

                            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                            markup.setResizeKeyboard(true);

                            KeyboardRow row1 = new KeyboardRow();
                            row1.add("🚛 Оформити замовлення");

                            KeyboardRow row2 = new KeyboardRow();
                            row2.add(BACK_BUTTON);

                            markup.setKeyboard(List.of(row1, row2));

                            sendMessage(chatId, "📦 Ваш кошик:", markup);
                            return;
                        }
                    }

                    // 🔥 додаємо новий товар → питаємо кількість
                    tempStorage.put(userId + "_product", product);

                    // 🔥 залишаємо логіку як була
                    userStates.put(userId, "awaiting_quantity");

                    sendText(chatId, "🔢 Введіть кількість (наприклад: 1, 2, 5):");
                }

                case "🚛 Оформити замовлення" -> {

                    List<Map<String, Object>> cart = userCart.get(userId);

                    if (cart == null || cart.isEmpty()) {
                        sendText(chatId, "❌ Кошик порожній.");
                        return;
                    }

                    userStates.put(userId, "awaiting_phone");

                    sendText(chatId,
                            "📞 Введіть номер телефону для оформлення замовлення:");
                }

                case "➡ Далі" -> showNextProduct(userId);
                case "⬅ Назад" -> showPreviousProduct(userId);
                case "Назад" -> {
                    userStates.remove(userId);
                    adminEditingProduct.remove(userId);
                    adminSelectedProductsRange.remove(userId);
                    adminEditingField.remove(userId);

                    sendMessage(createDeveloperMenu(chatId));
                }
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
                case "Написати консультанту" -> {
                    userStates.put(userId, "ask_consultant");

                    sendText(chatId, """
                        ✏️ Напишіть своє питання — я передам консультанту.
                        Фото теж можна кидати 📸😉
                        """);
                    break;
                }
                case "💬 Залишити відгук" -> {
                    userStates.put(userId, "waiting_for_feedback");
                    sendText(chatId,
                            "📝 Напишіть свій відгук.\n" +
                                    "Ми уважно його прочитаємо та врахуємо 🙏");
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

                case "⚙️ Керування фільтрами" -> {
                    sendMessageSafely(createFiltersAdminMenu(chatId));
                }

                case "➕ Додати фільтр" -> {
                    userStates.put(userId, "awaiting_new_filter");
                    sendText(chatId, "✏️ Введіть назву нового фільтра:");
                }

                case "➕ Додати підфільтр" -> {

                    userStates.put(userId, "awaiting_filter_for_subfilter");
                    filterPage.put(userId, 0);

                    sendMessageSafely(createFilterSelectMenuAdmin(userId, chatId, 0)); // 🔥 АДМІН

                    return;
                }

                case "✏️ Редагувати фільтр" -> {
                    userStates.put(userId, "awaiting_filter_edit");
                    sendMessageSafely(createFilterSelectMenu(userId, chatId, 0));
                }

                case "🗑 Видалити фільтр" -> {
                    userStates.put(userId, "awaiting_filter_delete");
                    sendMessageSafely(createFilterSelectMenu(userId, chatId, 0));
                }

                case "📋 Список фільтрів" -> {
                    sendFiltersList(chatId);
                }

                case "📦 Товари у фільтрі" -> {
                    userStates.put(userId, "awaiting_filter_products");
                    sendMessageSafely(createFilterSelectMenu(userId, chatId, 0));
                }

                case "🧹 Очистити фільтр" -> {
                    userStates.put(userId, "awaiting_filter_clear");
                    sendMessageSafely(createFilterSelectMenu(userId, chatId, 0));
                }

                /*case "🛒 Замовити товар" -> {

                    userStates.remove(userId); // 🔥 ОЧИЩАЄМО СТАН ПОШУКУ

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

                    KeyboardRow backRow = new KeyboardRow();
                    backRow.add(new KeyboardButton(BACK_BUTTON));

                    markup.setKeyboard(List.of(deliveryRow, backRow));

                    sendMessage(chatId,
                            "🚚 Оберіть спосіб доставки:\n\n" +
                                    "🏬 Самовивіз\n" +
                                    "📦 Доставка по місту\n" +
                                    "Митрофан усе підготує та передасть замовлення максимально швидко 😉",
                            markup);

                    userStates.put(userId, "awaiting_delivery_choice");
                }*/

                case "❌ Видалити товар" -> {

                    List<Map<String, Object>> cart = userCart.get(userId);

                    if (cart == null || cart.isEmpty()) {
                        sendText(chatId, "🧺 Кошик порожній.");
                        return;
                    }

                    userStates.put(userId, "awaiting_remove_item");

                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    markup.setResizeKeyboard(true);

                    List<KeyboardRow> rows = new ArrayList<>();

                    KeyboardRow numbers = new KeyboardRow();

                    for (int i = 1; i <= cart.size(); i++) {
                        numbers.add(String.valueOf(i));
                    }

                    rows.add(numbers);

                    KeyboardRow backRow = new KeyboardRow();
                    backRow.add("⬅️ Назад");

                    rows.add(backRow);

                    markup.setKeyboard(rows);

                    SendMessage msg = SendMessage.builder()
                            .chatId(chatId)
                            .text("🗑 Введіть номер товару, який хочете видалити.\n\nНаприклад: 1")
                            .replyMarkup(markup)
                            .build();

                    execute(msg);
                }

                case "✏️ Змінити кількість" -> {

                    List<Map<String,Object>> cart = userCart.get(userId);

                    if (cart == null || cart.isEmpty()) {
                        sendText(chatId,"🧺 Кошик порожній.");
                        return;
                    }

                    userStates.put(userId,"awaiting_edit_item");

                    sendText(chatId,
                            "✏️ Введіть номер товару, який хочете змінити.\n\n" +
                                    "Наприклад: 1");
                }

                case "🏬 Самовивіз" -> {

                    // 🔥 ВИХОДИМО З ПОШУКУ
                    userStates.remove(userId);

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

                    // 🔥 ВИХОДИМО З ПОШУКУ
                    userStates.remove(userId);

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

                    // 🔥 ВИХОДИМО З ПОШУКУ
                    userStates.remove(userId);

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

                case "💰 Оновити ціни (.xlsx)" -> {
                    sendText(chatId, "📤 Send the Excel (.xlsx) file to update product prices.");

                    stateManager.setState(userId, "WAITING_FOR_XLSX_PRICES");
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
                    Long targetUserId = consultantReplyTarget.get(CONSULTANT_CHAT_ID);

                    if (targetUserId == null) {
                        sendText(CONSULTANT_CHAT_ID, "❌ Немає покупця, який чекає відповіді.");
                        return;
                    }

                    // Отримуємо останнє повідомлення цього покупця
                    String lastMsg = lastUserMessage.get(targetUserId);
                    if (lastMsg == null) lastMsg = "— (останнє повідомлення не збережено)";

                    // Виводимо продавцю деталі покупця
                    sendText(
                            CONSULTANT_CHAT_ID,
                            "🧍‍♂️ Покупець:\n" +
                                    "🆔 User ID: " + targetUserId + "\n" +
                                    "💬 Повідомлення: " + lastMsg + "\n\n" +
                                    "✏️ Напишіть відповідь або надішліть фото — я передам покупцю."
                    );

                    // Переводимо продавця в режим відповіді
                    userStates.put(CONSULTANT_CHAT_ID, "consultant_reply");
                    break;
                }

                case "✅ Підтвердити" -> {

                    try (Connection conn = DatabaseManager.getConnection()) {

                        conn.setAutoCommit(false);

                        String selectSql =
                                "SELECT id, userId, item FROM orders " +
                                        "WHERE status='Нове' ORDER BY id ASC LIMIT 1";

                        long orderId;
                        long orderUserId;
                        String itemsText;

                        try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                             ResultSet rs = stmt.executeQuery()) {

                            if (!rs.next()) {
                                sendText(chatId, "Замовлень немає.");
                                conn.rollback();
                                break;
                            }

                            orderId = rs.getLong("id");
                            orderUserId = rs.getLong("userId");
                            itemsText = rs.getString("item");
                        }

                        userManager.decreaseProductsFromText(conn, itemsText);

                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE orders SET status='Підтверджено' WHERE id=?")) {
                            upd.setLong(1, orderId);
                            upd.executeUpdate();
                        }

                        conn.commit();

                        sendText(String.valueOf(orderUserId),
                                "🧱 Ваше замовлення прийнято!\n\n" +
                                        "Митрофан вже складає його на піддон 😄\n" +
                                        "Дякуємо, що обираєте GigaHub ❤️");

                        sendText(chatId, "Замовлення підтверджено ✅");
                        showAdminOrder(Long.valueOf(userId), String.valueOf(chatId));

                    } catch (Exception e) {
                        LOGGER.severe("[Admin Error] Failed to confirm order: " + e.getMessage());
                        try { sendText(chatId, "❌ Помилка при підтвердженні замовлення."); } catch (Exception ignored) {}
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
                    userStates.put(userId, "awaiting_hit_type");
                    sendText(chatId, "Ви хочете додати креатив з описом чи тільки медіа?\nНапишіть 'З описом' або 'Тільки медіа':");
                }

                case "🎥 Відеоінструкція" -> {
                    SendVideo video = new SendVideo();
                    video.setChatId(chatId);
                    video.setVideo(new InputFile(VIDEO_HELP_FILE_ID));
                    video.setCaption("🎥 Відеоінструкція користування ботом\n\n" +
                            "Тут показано, як оформити замовлення, оплатити та звернутися в підтримку.");

                    try {
                        execute(video);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        sendText(chatId, "❌ Не вдалося відкрити відеоінструкцію.");
                    }
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

            // 🔹 Створення нового фільтра
            if ("awaiting_new_filter".equals(state)) {

                if (text.equals("⬅️ Назад")) {
                    userStates.remove(userId);
                    sendMessageSafely(createFiltersAdminMenu(chatId));
                    return;
                }

                createFilter(text.trim());

                userStates.remove(userId);

                sendText(chatId, "✅ Фільтр створено: " + text);

                sendMessageSafely(createFiltersAdminMenu(chatId));

                return;
            }

            // 🚚 Вибір способу доставки
            if (text.equals("🏬 Самовивіз")) {

                System.out.println("DEBUG: Натиснули Самовивіз");

                userStates.remove(userId); // 🔥 виходимо з пошуку
                userStates.put(userId, "order_pickup");

                tempStorage.put(userId + "_deliveryType", "Самовивіз");

                sendText(chatId,
                        "✏️ Введіть, будь-ласка, свої дані для самовивозу у форматі:\n" +
                                "🏙 Місто\n" +
                                "👤 П.І.\n" +
                                "📞 Телефон\n" +
                                "💳 Бонусна картка (Від магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444"
                );

            } else if (text.equals("📦 Доставка по місту")) {

                System.out.println("DEBUG: Натиснули Доставка по місту");

                userStates.remove(userId); // 🔥 вихід з пошуку
                userStates.put(userId, "awaiting_city_delivery");

                tempStorage.put(userId + "_deliveryType", "Доставка по місту");

                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки по місту у форматі:\n" +
                                "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444"
                );

            } else if (text.equals("📮 Доставка Новою поштою")) {

                System.out.println("DEBUG: Натиснули Доставка Новою поштою");

                userStates.remove(userId); // 🔥 вихід з пошуку
                userStates.put(userId, "awaiting_post_delivery");

                tempStorage.put(userId + "_deliveryType", "Нова Пошта");

                sendText(chatId,
                        "📝 Введіть, будь-ласка, дані для доставки Новою Поштою у форматі:\n" +
                                "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Бонусна картка (Від магазину)\n\n" +
                                "📌 Приклад:\n" +
                                "№12, Іваненко Іван Іванович, +380501234567, 4444"
                );
            }

            if (developerState.getOrDefault(chatId, "").equals("WAITING_FOR_XLSX_PRICES")) {

                if (update.getMessage().hasDocument()) {

                    Document doc = update.getMessage().getDocument();

                    if (!doc.getFileName().endsWith(".xlsx")) {
                        sendText(chatId, "❌ Це не .xlsx файл. Надішліть коректний Excel.");
                        return;
                    }

                    String fileId = doc.getFileId();
                    handleExcelPriceUpdate(chatId, fileId);

                    developerState.remove(chatId);
                }
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

        KeyboardRow row2 = new KeyboardRow();
        row2.add("❌ Видалити товар");
        row2.add("✏️ Змінити кількість");
        row2.add("🧹 Очистити кошик");

        KeyboardRow row3 = new KeyboardRow();
        row3.add(BACK_BUTTON);

        markup.setKeyboard(List.of(row1, row2, row3));

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
    public void openCartForUser(Long userId) {

        String chatId = String.valueOf(userId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🛒 Ваш кошик:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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

    private void handleExcelPriceUpdate(String chatId, String fileId) {
        try {
            System.out.println("=== 📥 STARTING PRICE UPDATE FROM EXCEL ===");

            File file = downloadFile(fileId);
            System.out.println("File downloaded: " + file.getAbsolutePath());

            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            int updated = 0;
            int notFound = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) {
                    System.out.println("⚠️ Row " + i + " is empty — skipping");
                    continue;
                }

                // PRODUCT NAME (column C)
                Cell nameCell = row.getCell(2);
                if (nameCell == null) {
                    System.out.println("⚠️ Missing product name in row " + i);
                    continue;
                }

                String productName = nameCell.getStringCellValue().trim();
                if (productName.isEmpty()) {
                    System.out.println("⚠️ Empty product name in row " + i);
                    continue;
                }

                // PRICE (column H)
                Cell priceCell = row.getCell(7);
                if (priceCell == null) {
                    System.out.println("⚠️ Missing price in row " + i + " (" + productName + ")");
                    continue;
                }

                double newPrice = 0;

                switch (priceCell.getCellType()) {
                    case NUMERIC:
                        newPrice = priceCell.getNumericCellValue();
                        break;

                    case STRING:
                        try {
                            newPrice = Double.parseDouble(priceCell.getStringCellValue().trim());
                        } catch (Exception e) {
                            System.out.println("❌ Invalid price format in row " + i + " (" + productName + ")");
                            continue;
                        }
                        break;

                    default:
                        System.out.println("⚠️ Unsupported cell type for price in row " + i + " (" + productName + ")");
                        continue;
                }

                System.out.println("➡️ Read: " + productName + " | new price = " + newPrice);

                // Update in DB
                if (updatePriceByName(productName, newPrice)) {
                    updated++;
                    System.out.println("   ✅ UPDATED: " + productName + " → " + newPrice);
                } else {
                    notFound++;
                    System.out.println("   ❌ NOT FOUND IN DATABASE: " + productName);
                }
            }

            workbook.close();

            System.out.println("=== ✅ PRICE UPDATE COMPLETED ===");
            System.out.println("Updated: " + updated);
            System.out.println("Not found: " + notFound);

            sendText(chatId,
                    "✅ *Price update completed!*\n\n" +
                            "🔄 Updated products: *" + updated + "*\n" +
                            "❌ Not found: *" + notFound + "*"
            );

        } catch (Exception e) {
            System.out.println("=== ❌ ERROR PROCESSING EXCEL FILE ===");
            e.printStackTrace();
            sendText(chatId, "❌ Error processing Excel file.");
        }
    }

    private boolean updatePriceByName(String name, double price) {
        String sql = "UPDATE products SET price = ? WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, price);
            stmt.setString(2, name);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    // Метод повернення назад
    private void handleBack(Long userId, String chatId) throws TelegramApiException {

        System.out.println("[handleBack] User " + userId + " pressed Back.");

        String state = userStates.get(userId);

        // 🔴 1. ВИХІД З ОФОРМЛЕННЯ
        if ("awaiting_delivery_choice".equals(state) ||
                "order_pickup".equals(state) ||
                "awaiting_city_delivery".equals(state) ||
                "awaiting_payment_method".equals(state) ||
                "awaiting_phone".equals(state)) {

            tempDeliveryType.remove(userId);
            tempAddress.remove(userId);
            tempFullname.remove(userId);
            tempPhone.remove(userId);
            tempCard.remove(userId);
            tempOrderCode.remove(userId);
            tempOrderItems.remove(userId);
            tempTotal.remove(userId);

            userStates.remove(userId);

            sendText(chatId, "↩️ Повертаємо вас до каталогу товарів.");

            if (currentSubcategory.containsKey(userId)) {
                sendProductList(Long.valueOf(chatId));
                return;
            }

            if (currentCategory.containsKey(userId)) {
                sendSubcategories(userId, currentCategory.get(userId));
                return;
            }

            sendCategories(userId);
            return;
        }

        // 🟠 2. ВИХІД З КОШИКА
        if ("in_cart".equals(state)) {

            userStates.remove(userId);

            sendText(chatId, "↩️ Повертаємо вас до каталогу.");

            if (currentSubcategory.containsKey(userId)) {
                sendProductList(Long.valueOf(chatId));
                return;
            }

            if (currentCategory.containsKey(userId)) {
                sendSubcategories(userId, currentCategory.get(userId));
                return;
            }

            sendCategories(userId);
            return;
        }

        // 🟡 3. КАЛЬКУЛЯТОР
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

            tempLength.remove(userId);
            tempWidth.remove(userId);
            paintTemp.remove(userId);
            paintLayers.remove(userId);

            userStates.remove(userId);

            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 🟡 4. З ПІДКАТЕГОРІЇ
        if (currentSubcategory.containsKey(userId)) {

            currentSubcategory.remove(userId);

            if (currentCategory.containsKey(userId)) {
                sendSubcategories(userId, currentCategory.get(userId));
            } else {
                sendCategories(userId);
            }

            return;
        }

        // 🟢 5. З КАТЕГОРІЇ
        if (currentCategory.containsKey(userId)) {

            currentCategory.remove(userId);
            sendMessage(createUserMenu(chatId, userId));
            return;
        }

        // 🔵 6. АДМІН
        if (adminOrderIndex.containsKey(userId)) {

            adminOrderIndex.remove(userId);
            sendMessage(createAdminMenu(chatId));
            return;
        }

        // 🔵 7. РОЗРОБНИК
        if (DEVELOPERS.contains(userId) && isInDeveloperMenu(userId)) {

            sendMessage(createDeveloperMenu(chatId));
            return;
        }

        // 🔵 8. РЕДАГУВАННЯ
        if (adminEditingProduct.containsKey(userId) ||
                adminSelectedProductsRange.containsKey(userId)) {

            adminEditingProduct.remove(userId);
            adminSelectedProductsRange.remove(userId);
            adminEditingField.remove(userId);
            userStates.remove(userId);

            sendText(chatId, "↩️ Ви повернулися до меню розробника.");
            sendMessage(createDeveloperMenu(chatId));
            return;
        }

        // ⚪ 9. FALLBACK
        userStates.remove(userId);
        lastShownProduct.remove(userId);
        adminMatchList.remove(userId);
        productIndex.remove(userId);

        sendMessage(createUserMenu(chatId, userId));
    }

    //Показ товара новий
    private void handleProductSelection(Long chatId, int choice) throws TelegramApiException {

        List<Map<String, Object>> products = userProductLists.get(chatId);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ Список порожній.");
            return;
        }

        if (choice < 1 || choice > products.size()) {
            sendText(chatId, "❌ Невірний номер.");
            return;
        }

        Map<String, Object> product = products.get(choice - 1);

        // 🔥 зберігаємо товар
        lastShownProduct.put(chatId, product);

        // видаляємо його зі списку
        products.remove(choice - 1);

        // 🔹 СПОЧАТКУ список
        if (!products.isEmpty()) {

            StringBuilder sb = new StringBuilder("\n📦 Інші товари:\n\n");

            for (int i = 0; i < products.size(); i++) {
                sb.append(i + 1)
                        .append(". ")
                        .append(products.get(i).get("name"))
                        .append("\n");
            }

            sendText(chatId, sb.toString());
        }

        // 🔹 ПОТІМ товар
        sendProductFromMap(chatId, product);
    }

    private void showNextProduct(Long chatId) throws TelegramApiException {

        String category = currentCategory.get(chatId);
        String subcategory = currentSubcategory.get(chatId);

        if (category == null || subcategory == null) return;

        CatalogSearcher searcher = new CatalogSearcher();
        List<Map<String, Object>> products = searcher.getProducts(category, subcategory);

        if (products == null || products.isEmpty()) {
            sendText(chatId, "У цій підкатегорії немає товарів.");
            productIndex.remove(chatId);
            return;
        }

        products.sort(Comparator.comparingInt(p -> ((Number) p.get("id")).intValue()));

        long uniqueKey = (chatId.toString() + category + subcategory).hashCode();
        Long key = uniqueKey;

        Integer savedIndex = productIndex.get(key);
        int currentIndex;

        if (savedIndex == null) {
            // Перший раз у цій підкатегорії — показуємо перший товар
            currentIndex = 0;
        } else {
            // Вже були — переходимо до наступного
            currentIndex = (savedIndex + 1) % products.size();
        }

        productIndex.put(key, currentIndex);
        Map<String, Object> product = products.get(currentIndex);
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
            row2.add("Написати консультанту");
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

    // 🛒 Добавление в кошик
    private void addToCart(Long chatId) throws TelegramApiException {

        Map<String, Object> product = lastShownProduct.get(chatId);

        System.out.println("===== ADD TO CART DEBUG =====");
        System.out.println("ChatId: " + chatId);
        System.out.println("Product map: " + product);

        if (product == null) {
            sendText(chatId, "❌ Не вдалося визначити товар. Спробуй ще раз 🙏");
            return;
        }

        String productName = String.valueOf(product.getOrDefault("name", "Без назви"));
        String unit = String.valueOf(product.getOrDefault("unit", "")).toLowerCase();

        System.out.println("Product name: " + productName);
        System.out.println("Unit: " + unit);

        int maxAvailable = 0;

        // 🔹 1. пробуємо взяти склад з Map
        if (product.containsKey("stock")) {
            maxAvailable = parseIntSafe(product.get("stock"));
            System.out.println("Stock taken from: stock");
        }
        else if (product.containsKey("quantity")) {
            maxAvailable = parseIntSafe(product.get("quantity"));
            System.out.println("Stock taken from: quantity");
        }
        else if (product.containsKey("available")) {
            maxAvailable = parseIntSafe(product.get("available"));
            System.out.println("Stock taken from: available");
        }

        // 🔹 2. якщо не знайшли — беремо з БД
        if (maxAvailable == 0 && product.containsKey("id")) {

            try {

                int productId = parseIntSafe(product.get("id"));

                Connection conn = DatabaseManager.getConnection();

                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT quantity FROM products WHERE id = ?"
                );

                stmt.setInt(1, productId);

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    maxAvailable = rs.getInt("quantity");
                    System.out.println("Stock loaded from DB: " + maxAvailable);
                }

                rs.close();
                stmt.close();

            } catch (Exception e) {
                System.out.println("ERROR loading stock from DB");
                e.printStackTrace();
            }
        }

        System.out.println("Calculated maxAvailable: " + maxAvailable);

        // 🔴 товар під замовлення
        if (unit.equals("під замовлення")) {

            sendText(chatId,
                    "📦 *Товар під замовлення!*\n\n" +
                            "🔎 \"" + productName + "\" наразі недоступний для швидкого замовлення.\n" +
                            "Наш консультант зв’яжеться з тобою найближчим часом 🧱🤝");

            String adminMsg =
                    "📢 *Новий запит під замовлення!*\n\n" +
                            "👤 Покупець цікавиться товаром:\n" +
                            "📦 " + productName + "\n\n" +
                            "🆔 Chat ID: " + chatId;

            sendReplyButtonToConsultant(chatId);
            sendText(String.valueOf(CONSULTANT_CHAT_ID), adminMsg);

            return;
        }

        waitingForQuantity.put(chatId, true);

        System.out.println("Waiting for quantity set for user: " + chatId);

        StringBuilder msg = new StringBuilder();

        msg.append("📥 Добре, друже! Скільки тобі потрібно?\n")
                .append("👉 *").append(productName).append("*\n\n");

        msg.append("📦 Наявність на складі: *")
                .append(maxAvailable)
                .append(" шт.*\n");

        if (maxAvailable <= 0) {
            msg.append("⛔ Наразі товар відсутній.\n");
        }

        msg.append("\nВведи кількість числом (наприклад: 3)");

        sendText(chatId, msg.toString());

        System.out.println("===== END ADD TO CART =====");
    }

    // 🔧 Безпечний парсер числа
    private int parseIntSafe(Object value) {

        if (value == null) return 0;

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public void addToCartTool(Long userId) {

        Map<String, Object> product = lastShownProduct.get(userId);

        if (product == null) {
            sendText(String.valueOf(userId),
                    "❌ Товар не знайдено для додавання в кошик.");
            return;
        }

        // 🔥 Запускаємо ту саму логіку що і в каталозі
        try {
            addToCart(userId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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

            case "awaiting_excel_keyword" -> {

                String keyword = text.trim();

                List<ExcelToMySqlImporter.ExcelProduct> results;
                try {
                    results = ExcelToMySqlImporter.searchInExcel(keyword);
                } catch (Exception e) {
                    sendText(chatId, "❌ Помилка при пошуку у Excel: " + e.getMessage());
                    return;
                }

                if (results.isEmpty()) {
                    sendText(chatId, "❌ Товар не знайдено у Excel: " + keyword);
                    return;
                }

                // зберігаємо результати
                adminExcelResults.put(userId, results);

                StringBuilder sb = new StringBuilder(
                        "🔎 Знайдено товари. Введіть номер для редагування:\n\n"
                );

                for (int i = 0; i < results.size(); i++) {
                    var p = results.get(i);
                    sb.append(i + 1)
                            .append(". ")
                            .append(p.name);

                    if (p.price != null) {
                        sb.append(" | Ціна: ").append(p.price);
                    }

                    sb.append("\n");
                }

                sendText(chatId, sb.toString());

                // переходимо до вибору номера
                userStates.put(userId, "choose_excel_product");
            }

            case "choose_excel_product" -> {

                int index;
                try {
                    index = Integer.parseInt(text.trim()) - 1;
                } catch (NumberFormatException e) {
                    sendText(chatId, "❌ Введіть номер зі списку");
                    return;
                }

                List<ExcelToMySqlImporter.ExcelProduct> list =
                        adminExcelResults.get(userId);

                if (list == null || index < 0 || index >= list.size()) {
                    sendText(chatId, "❌ Невірний номер");
                    return;
                }

                ExcelToMySqlImporter.ExcelProduct product = list.get(index);

                try {
                    long productId =
                            ExcelToMySqlImporter.importSingleProduct(product);

                    adminEditingProduct.put(userId, String.valueOf(productId));

                    // переходимо в режим редагування товару
                    userStates.put(userId, "editing_product");

                    execute(createEditMenu(chatId, userId));

                } catch (Exception e) {
                    sendText(chatId, "❌ Помилка додавання товару: " + e.getMessage());
                    e.printStackTrace();
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

                    String caption = update.getMessage().getCaption();
                    if (caption == null) caption = "";

                    // 🔥 ЗБЕРІГАЄМО ОСТАННЄ ПОВІДОМЛЕННЯ ПОКУПЦЯ
                    lastUserMessage.put(userId, caption.isBlank() ? "📸 Фото без підпису" : caption);

                    try {
                        SendPhoto msg = new SendPhoto();
                        msg.setChatId(CONSULTANT_CHAT_ID.toString());
                        msg.setPhoto(new InputFile(fileId));
                        msg.setCaption(
                                "📸 Фото від покупця:\n" +
                                        "🆔 User ID: " + userId + "\n" +
                                        "💬 Chat ID: " + realChatId + "\n\n" +
                                        (caption.isEmpty() ? "" : "📝 Підпис:\n" + caption)
                        );

                        execute(msg);

                        sendReplyButtonToConsultant(userId);
                        sendText(realChatId, "📸 Ваше фото передано консультанту!");

                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(realChatId, "❌ Помилка при відправці фото консультанту.");
                    }

                    userStates.remove(userId);
                    break;
                }


                // ✏️ Якщо користувач відправив текст
                if (text != null && !text.isBlank()) {

                    // 🔥 ЗБЕРІГАЄМО ОСТАННІЙ ТЕКСТ ПОКУПЦЯ
                    lastUserMessage.put(userId, text);

                    // AI-підказка
                    String botReply = botAutoAnswer(text);

                    if (botReply != null) {
                        sendText(realChatId, botReply + "\n\n✉️ Якщо хочеш поговорити з консультантом — натисни кнопку.");
                        userStates.remove(userId);
                        break;
                    }

                    // Передаємо консультанту
                    try {
                        SendMessage adminMsg = new SendMessage(
                                CONSULTANT_CHAT_ID.toString(),
                                "📩 Нове повідомлення від покупця:\n" +
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
                    break;
                }

                break;
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

                System.out.println("DEBUG: User ID = " + userId +
                        ", State = " + userStates.get(userId) +
                        ", Text = " + text);

                // 🔥 Виходимо зі стану пошуку
                userStates.remove(userId);

                // 🏬 Самовивіз
                if ("🏬 Самовивіз".equals(text)) {

                    tempStorage.put(userId + "_deliveryType", "Самовивіз");
                    userStates.put(userId, "order_pickup");

                    sendText(chatId,
                            "✏️ Введіть, будь ласка, дані для самовивозу у форматі:\n" +
                                    "🏙 Місто, 👤 П.І., 📞 Телефон, 💳 Бонусна картка\n\n" +
                                    "📌 Приклад:\n" +
                                    "Казанка, Сидоренко Олена Олексіївна, +380631234567, 4444");

                    return;
                }

                // 📦 Доставка по місту
                if ("📦 Доставка по місту".equals(text)) {

                    tempStorage.put(userId + "_deliveryType", "Доставка по місту");
                    userStates.put(userId, "awaiting_city_delivery");

                    sendText(chatId,
                            "📝 Введіть, будь ласка, дані для доставки по місту у форматі:\n" +
                                    "📍 Адреса, 👤 П.І., 📞 Телефон, 💳 Бонусна картка\n\n" +
                                    "📌 Приклад:\n" +
                                    "вул. Шевченка 10, Казанка, Петров Петро Петрович, +380671234567, 4444");

                    return;
                }

                // 📮 Нова Пошта
                if ("📮 Доставка Новою поштою".equals(text)) {

                    tempStorage.put(userId + "_deliveryType", "Нова Пошта");
                    userStates.put(userId, "awaiting_post_delivery");

                    sendText(chatId,
                            "📝 Введіть, будь ласка, дані для доставки Новою Поштою у форматі:\n" +
                                    "📮 Відділення НП, 👤 П.І., 📞 Телефон, 💳 Бонусна картка\n\n" +
                                    "📌 Приклад:\n" +
                                    "№12, Іваненко Іван Іванович, +380501234567, 4444");

                    return;
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

                // 1️⃣ Дані
                String title = String.valueOf(
                        tempStorage.getOrDefault(userId + "_hit_title", "немає")
                );

                String description = String.valueOf(
                        tempStorage.getOrDefault(userId + "_hit_description", "немає")
                );

                // 2️⃣ Медіа
                String mediaUrl = "немає";
                if (text == null || !text.equalsIgnoreCase("немає")) {
                    mediaUrl = HitsManager.uploadFromTelegram(this, update.getMessage());
                    if (mediaUrl == null) mediaUrl = "немає";
                }

                // 3️⃣ Зберігаємо
                HitsManager.saveHit(title, description, mediaUrl);

                // 4️⃣ Очищення
                userStates.remove(userId);
                tempStorage.remove(userId + "_hit_title");
                tempStorage.remove(userId + "_hit_description");

                // 5️⃣ Адміну
                sendText(chatId, "✅ Товар успішно додано у Хіт продажу!");

                // 🔔 6️⃣ РОЗСИЛКА ВСІМ (ПО CHAT_ID З БД)
                for (String targetChatId : userManager.getAllChatIds()) {
                    try {
                        sendText(
                                targetChatId,
                                "🌟 Новий Хіт продажу з’явився в магазині!\n" +
                                        "👉 Перегляньте його у розділі «Хіти продажів»!"
                        );
                    } catch (Exception ignored) {}
                }
            }

            case "awaiting_hit_media_only" -> {

                // 1️⃣ Медіа
                String mediaUrl = "немає";
                if (text == null || !text.equalsIgnoreCase("немає")) {
                    mediaUrl = HitsManager.uploadFromTelegram(this, update.getMessage());
                    if (mediaUrl == null) mediaUrl = "немає";
                }

                // 2️⃣ Зберігаємо
                HitsManager.saveHit(null, "немає", mediaUrl);

                // 3️⃣ Очищення
                userStates.remove(userId);

                // 4️⃣ Адміну
                sendText(chatId, "✅ Товар успішно додано у Хіт продажу!");

                // 🔔 5️⃣ РОЗСИЛКА ВСІМ (ПО CHAT_ID З БД)
                for (String targetChatId : userManager.getAllChatIds()) {
                    try {
                        sendText(
                                targetChatId,
                                "🌟 Новий Хіт продажу з’явився в магазині!\n" +
                                        "👉 Перегляньте його у розділі «Хіти продажів»!"
                        );
                    } catch (Exception ignored) {}
                }
            }

            case "choose_subcategory_for_filter" -> {

                try {

                    chooseSubcategoryForFilter(userId, Long.valueOf(chatId), text);

                } catch (TelegramApiException e) {

                    e.printStackTrace();

                    sendText(chatId, "❌ Помилка при виборі підкатегорії.");
                }

                return;
            }

            case "choose_filter_for_subcategory" -> {

                String subcategory = currentSubcategory.get(userId);
                if (subcategory == null) {
                    subcategory = selectedSubcategoryForFilter.get(userId);
                }

                List<String> filters = catalogSearcher.getFiltersAdmin();

                if (!filters.contains(text)) {
                    return;
                }

                if (subcategory == null) {

                    sendText(chatId, "❌ Підкатегорія не вибрана.");
                    return;
                }

                addFilterToSubcategory(subcategory, text);

                sendText(chatId,
                        "✅ Фільтр '" + text +
                                "' додано до підкатегорії '" +
                                subcategory + "'");

                userStates.remove(userId);
                selectedSubcategoryForFilter.remove(userId);

                return;
            }

            case "awaiting_filter_for_subfilter" -> {

                String category = currentCategory.get(userId);
                String subcategory = currentSubcategory.get(userId);

                List<String> filters = catalogSearcher.getFilters(category, subcategory);

                if (!filters.contains(text)) {
                    return;
                }

                selectedFilterForSubfilter.put(userId, text);

                userStates.put(userId, "awaiting_subfilter_name");

                sendText(chatId, "✏️ Введіть назву підфільтра:");

                return;
            }

            case "awaiting_subfilter_name" -> {

                createSubfilter(userId, chatId, text);
                userStates.remove(userId);
            }

            case "awaiting_filter_for_subcategory" -> {

                String subcategory = selectedSubcategoryForFilter.get(userId);

                if (subcategory == null) {

                    sendText(chatId, "❌ Підкатегорія не вибрана.");
                    return;
                }

                String category = currentCategory.get(userId);
                List<String> filters = catalogSearcher.getFilters(category, subcategory);

                if (!filters.contains(text)) {
                    return;
                }

                addFilterToSubcategory(subcategory, text);

                sendText(chatId,
                        "✅ Фільтр '" + text +
                                "' додано до підкатегорії '" +
                                subcategory + "'");

                userStates.remove(userId);
                selectedSubcategoryForFilter.remove(userId);

                return;
            }

            /// 🚚 Обробка вибору доставки
            case "awaiting_delivery_choice" -> {

                if (text.contains("Самовивіз")) {

                    userStates.put(userId, "order_pickup");
                    tempStorage.put(userId + "_deliveryType", "Самовивіз");

                    sendText(chatId,
                            "✏️ Введіть дані для самовивозу:\n\n" +
                                    "🏙 Місто\n" +
                                    "👤 Ім'я Прізвище\n" +
                                    "📞 Телефон\n\n" +
                                    "Можна вводити у будь-якому форматі\n" +
                                    "Наприклад:\n" +
                                    "Казанка Іван Петренко +380501234567");

                    return;
                }

                if (text.contains("Доставка по місту")) {

                    userStates.put(userId, "awaiting_city_delivery");
                    tempStorage.put(userId + "_deliveryType", "Доставка по місту");

                    sendText(chatId,
                            "📝 Введіть дані для доставки:\n\n" +
                                    "🏙 Місто\n" +
                                    "👤 Ім'я Прізвище\n" +
                                    "📞 Телефон\n\n" +
                                    "Можна вводити у будь-якому форматі\n" +
                                    "Наприклад:\n" +
                                    "Казанка Іван Петренко +380501234567");

                    return;
                }

                if (text.equals(BACK_BUTTON)) {
                    try {
                        handleBack(userId, chatId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                sendText(chatId, "❌ Оберіть спосіб доставки кнопкою.");
            }

            //Самовивіз
            case "order_pickup" -> {

                if (text.equals(BACK_BUTTON)) {

                    userStates.remove(userId);

                    try {
                        showCart(userId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        sendText(chatId, "❌ Помилка при відкритті кошика.");
                    }

                    return;
                }

                List<Map<String,Object>> cart = userCart.get(userId);

                if(cart == null || cart.isEmpty()){
                    sendText(chatId,"🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                Map<String,String> data = parseDeliveryData(text);

                if(data == null){
                    sendText(chatId,
                            "❌ Не вдалося розпізнати дані.\n\n"+
                                    "Спробуйте:\n"+
                                    "Казанка Іван Петренко +380501234567");
                    return;
                }

                String city = data.get("city");
                String fullName = data.get("name");
                String phone = data.get("phone");

                tempDeliveryType.put(userId,"Самовивіз");
                tempAddress.put(userId,city);
                tempFullname.put(userId,fullName);
                tempPhone.put(userId,phone);
                tempCard.put(userId,"0000");

                createTempOrder(userId,cart);

                askPaymentMethod(userId,Long.parseLong(chatId));
                userStates.put(userId,"awaiting_payment_method");
            }

            //Доставка по місту
            case "awaiting_city_delivery" -> {

                if (text.equals(BACK_BUTTON)) {

                    userStates.remove(userId);

                    try {
                        showCart(userId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        sendText(chatId, "❌ Помилка при відкритті кошика.");
                    }

                    return;
                }

                List<Map<String,Object>> cart = userCart.get(userId);

                if(cart == null || cart.isEmpty()){
                    sendText(chatId,"🛒 Ваш кошик порожній.");
                    userStates.remove(userId);
                    return;
                }

                Map<String,String> data = parseDeliveryData(text);

                if(data == null){
                    sendText(chatId,
                            "❌ Не вдалося розпізнати дані.\n\n"+
                                    "Спробуйте:\n"+
                                    "Казанка Іван Петренко +380501234567");
                    return;
                }

                String address = data.get("city");
                String fullName = data.get("name");
                String phone = data.get("phone");

                tempDeliveryType.put(userId,"Доставка по місту");
                tempAddress.put(userId,address);
                tempFullname.put(userId,fullName);
                tempPhone.put(userId,phone);
                tempCard.put(userId,"0000");

                createTempOrder(userId,cart);

                askPaymentMethod(userId,Long.parseLong(chatId));
                userStates.put(userId,"awaiting_payment_method");
            }

            case "awaiting_payment_method" -> {

                if (text.equals("⬅️ Назад")) {
                    try {
                        handleBack(userId, chatId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }

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

            case "awaiting_phone" -> {

                if (BACK_BUTTON.equals(text)) {
                    try {
                        handleBack(userId, chatId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                if (text == null || text.length() < 8) {
                    sendText(chatId, "❌ Введіть коректний номер телефону.");
                    return;
                }

                // 🔥 зберігаємо телефон
                userPhones.put(userId, text.trim());

                userStates.put(userId, "awaiting_payment_method");

                ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                markup.setResizeKeyboard(true);

                KeyboardRow row1 = new KeyboardRow();
                row1.add("💵 Готівка");
                row1.add("💳 Оплата карткою");

                KeyboardRow row2 = new KeyboardRow();
                row2.add(BACK_BUTTON);

                markup.setKeyboard(List.of(row1, row2));

                sendMessage(chatId, "💳 Оберіть спосіб оплати:", markup);
            }

            case "invites_menu" -> {
                // Клавіатура з кнопкою «Назад», яка працює ТІЛЬКИ в цьому меню
                ReplyKeyboardMarkup backKeyboard = new ReplyKeyboardMarkup();
                backKeyboard.setResizeKeyboard(true);
                backKeyboard.setOneTimeKeyboard(false);

                KeyboardRow row = new KeyboardRow();
                row.add("Назад");                    // саме ця кнопка
                backKeyboard.setKeyboard(List.of(row));

                // ВАЖЛИВО: обробляємо натискання «Назад» ПЕРЕД усім іншим
                if (text != null && text.equals("Назад")) {
                    // очищаємо всі стани й повертаємося в головне меню розробника
                    userStates.remove(userId);
                    adminEditingProduct.remove(userId);
                    adminSelectedProductsRange.remove(userId);
                    adminEditingField.remove(userId);

                    sendMessage(createDeveloperMenu(chatId));
                    return;                              // виходимо, щоб не попасти в default нижче
                }

                // Основні кнопки меню запрошень
                switch (text) {

                    case "➕ Додати запрошення" -> {
                        userStates.put(userId, "add_invite");
                        sendText(chatId, "Введіть дані нового запрошення у форматі:\nName;Kasa;City");
                        sendMessage(chatId, "Ок, чекаю на дані…", backKeyboard);
                    }

                    case "🗑️ Видалити запрошення" -> {
                        userStates.put(userId, "delete_invite");
                        sendText(chatId, "Введіть ID запрошення для видалення:");
                        sendMessage(chatId, "Ок, чекаю ID…", backKeyboard);
                    }

                    case "✏️ Редагувати запрошення" -> {
                        userStates.put(userId, "edit_invite");
                        sendText(chatId, "Введіть дані для редагування у форматі:\nID;Name;Kasa;City");
                        sendMessage(chatId, "Ок, чекаю нові дані…", backKeyboard);
                    }

                    case "📄 Показати всі запрошення" -> {
                        String sql = "SELECT * FROM invites ORDER BY id ASC";

                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {

                            StringBuilder sb = new StringBuilder("Статистика запрошень:\n\n");
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

                            sendText(chatId, hasInvites ? sb.toString() : "Поки що немає запрошень.");

                        } catch (SQLException e) {
                            e.printStackTrace();
                            sendText(chatId, "Помилка бази даних.");
                        }

                        sendMessage(chatId, "Оберіть дію або натисніть Назад", backKeyboard);
                    }

                    // якщо натиснули щось інше — просто нагадуємо
                    default -> {
                        sendText(chatId, "Некоректна команда. Спробуйте ще раз.");
                        sendMessage(chatId, "Оберіть дію або натисніть Назад", backKeyboard);
                    }
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

    private String getState(Long userId) {
        return state.get(userId);
    }

    private void clearState(Long userId) {
        state.remove(userId);
        System.out.println("[STATE] Cleared state for userId=" + userId);
    }

    private void handleUpdate(Update update) {

        Message msg = update.getMessage();
        Long userId = msg.getFrom().getId();
        String chatId = msg.getChatId().toString();
        String text = msg.getText();

        String currentState = getState(userId);

        System.out.println("[DEBUG] Update from userId=" + userId + ", state=" + currentState);

        Document doc = msg.getDocument();
        if (doc != null) {

            String fileName = doc.getFileName().toLowerCase();

            System.out.println("[DEBUG] File received: " + fileName);
            System.out.println("[DEBUG] Current state: " + currentState);

            if ("WAITING_FOR_XLSX_PRICES".equals(currentState)) {

                if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {

                    System.out.println("[DEBUG] Excel accepted → processing...");
                    clearState(userId);

                    handleExcelPriceUpdate(chatId, doc.getFileId());
                    return;
                }

                sendText(chatId, "⚠ Please upload a valid Excel (.xlsx) file.");
                return;
            }

            sendText(chatId, "⚠ Unexpected file. No command is waiting right now.");
            return;
        }

        if (text != null) {

            System.out.println("[DEBUG] Text received: '" + text + "'");

            String lower = text.toLowerCase().trim();

            if (lower.contains("оновити") && lower.contains("xlsx")) {

                sendText(chatId, "📤 Send Excel (.xlsx) file to update product prices.");

                setState(userId, "WAITING_FOR_XLSX_PRICES");

                System.out.println("[DEBUG] State changed → WAITING_FOR_XLSX_PRICES");
                return;
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
            List<Map<String, Object>> foundProducts =
                    searcher.searchMixedFromYAML(text);

            System.out.println("[handleSearch] Found products count: " + foundProducts.size());

            if (foundProducts.isEmpty()) {
                sendText(chatId,
                        "😕 Митрофан переглянув усі полиці, але нічого не знайшов за запитом:\n" +
                                "👉 *" + text + "*\n\n" +
                                "Спробуй інший варіант назви або уточни модель.");
                return;
            }

            // 🔥 Якщо знайдено кілька товарів — показуємо список
            if (foundProducts.size() > 1) {

                StringBuilder sb = new StringBuilder(
                        "🔎 Митрофан знайшов кілька варіантів:\n\n");

                for (int i = 0; i < foundProducts.size(); i++) {
                    sb.append(i + 1)
                            .append(". ")
                            .append(foundProducts.get(i).get("name"))
                            .append("\n");
                }

                sb.append("\n📝 Введи номер товару, щоб показати деталі.");

                searchResults.put(userId, foundProducts);
                productIndex.put(userId, 0);

                sendText(chatId, sb.toString());
                return;
            }

            // 🔥 Якщо товар один — показуємо як у каталозі
            Map<String, Object> product = foundProducts.get(0);

            lastShownProduct.put(userId, product);

            // 👉 використовуємо ЄДИНИЙ метод показу
            sendProductFromMap(userId, product);

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
            row1.add("Написати консультанту");
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

    // 🔎 Пошук товару
    private void handleWaitingForSearch(Long userId, String chatId, String text) {

        text = text.trim();

        try {

            // 🔢 Якщо бот очікує кількість товару
            if (waitingForQuantity.getOrDefault(userId, false)) {

                if (!text.matches("\\d+")) {
                    sendText(chatId, "❌ Введіть кількість числом (наприклад: 3)");
                    return;
                }

                int qty = Integer.parseInt(text);

                handleProductQuantity(userId, qty);

                waitingForQuantity.remove(userId);

                return;
            }

            // ⬅️ Назад
            if (text.equalsIgnoreCase("⬅️ Назад") || text.equalsIgnoreCase("Назад")) {

                userStates.remove(userId);

                execute(createUserMenu(chatId, userId));

                System.out.println("[handleWaitingForSearch] User "
                        + userId + " exited search mode.");

                return;
            }

            // 🛍️ Перейти в кошик
            if (text.equals(VIEW_CART_BUTTON)) {

                userStates.remove(userId);

                showCart(userId);

                System.out.println("[handleWaitingForSearch] User "
                        + userId + " opened the cart.");

                return;
            }

            // 🛒 Додати в кошик
            if (text.equalsIgnoreCase("🛠 Додати в кошик")
                    || text.equalsIgnoreCase("🔧 Додати в кошик")
                    || text.contains("Додати в кошик")) {

                addToCart(userId);
                return;
            }

            ProductSearchManager searchManager = new ProductSearchManager(this);

            // 🔢 Якщо користувач ввів номер товару
            if (text.matches("\\d+")) {

                searchManager.handleSearchNumber(userId, chatId, text);

                return;
            }

            // 🔎 Якщо введений текст — запускаємо пошук
            userStates.put(userId, "waiting_for_search");

            searchManager.performSearch(userId, chatId, text);

        } catch (TelegramApiException e) {

            e.printStackTrace();

            sendText(chatId, "❌ Сталася помилка під час пошуку.");
        }
    }

    private void handleProductQuantity(Long userId, int qty) {

        Map<String, Object> product = lastShownProduct.get(userId);

        if (product == null) {
            sendText(userId, "❌ Товар не знайдено.");
            return;
        }

        product.put("quantity", qty);

        userCart.computeIfAbsent(userId, k -> new ArrayList<>()).add(product);

        sendText(userId,
                "✅ Товар додано в кошик!\n" +
                        "📦 " + product.get("name") +
                        "\n🔢 Кількість: " + qty);
    }

    public void safeAddToCart(Long userId) {
        try {
            addToCart(userId);
        } catch (Exception e) {
            e.printStackTrace();
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
            row1.add("Написати консультанту");
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

        List<Map<String, Object>> selectedProducts = new ArrayList<>();

        try {

            // 🔹 ДІАПАЗОН
            if (text.contains("-")) {

                String[] parts = text.split("-");
                int start = Integer.parseInt(parts[0].trim()) - 1;
                int end = Integer.parseInt(parts[1].trim()) - 1;

                start = Math.max(start, 0);
                end = Math.min(end, matches.size() - 1);

                for (int i = start; i <= end; i++) {
                    selectedProducts.add(matches.get(i));
                }

            }
            // 🔹 СПИСОК
            else if (text.contains(",")) {

                String[] parts = text.split(",");

                for (String part : parts) {
                    int index = Integer.parseInt(part.trim()) - 1;

                    if (index >= 0 && index < matches.size()) {
                        selectedProducts.add(matches.get(index));
                    }
                }
            }
            // 🔹 ОДИН
            else {

                int index = Integer.parseInt(text) - 1;

                if (index >= 0 && index < matches.size()) {
                    selectedProducts.add(matches.get(index));
                }
            }

            if (selectedProducts.isEmpty()) {
                sendText(chatId, "❌ Немає валідних номерів.");
                return;
            }

            // 🔥 ВАЖЛИВО — ОСЬ ФІКС
            adminSelectedProducts.put(userId, selectedProducts);

            Map<String, Object> currentProduct = selectedProducts.get(0);
            adminSelectedProduct.put(userId, currentProduct);

            adminEditingProduct.put(userId,
                    currentProduct.get("name").toString());

            userStates.put(userId, "editing");

            adminMatchList.remove(userId);

            StringBuilder sb = new StringBuilder();

            sb.append("✏️ Редагуємо ")
                    .append(selectedProducts.size())
                    .append(" товар(ів)\n\n");

            sb.append("📦 Поточний:\n")
                    .append(currentProduct.get("name"));

            sendText(chatId, sb.toString());

            showEditMenuOnce(userId, chatId);

        } catch (NumberFormatException e) {
            sendText(chatId,
                    "❌ Введіть номер, діапазон (1-10) або список (1,3,5)");
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

                List<Map<String, Object>> products = adminSelectedProducts.get(userId);

                // 🔥 fallback для одного товару
                if (products == null || products.isEmpty()) {

                    Map<String, Object> single = adminSelectedProduct.get(userId);

                    if (single != null) {
                        products = new ArrayList<>();
                        products.add(single);
                    }
                }

                if (products == null || products.isEmpty()) {
                    sendText(chatId, "❌ Немає вибраних товарів.");
                    break;
                }

                userStates.put(userId, "awaiting_subcategory");

                sendText(chatId, "📁 Введіть назву підкатегорії або оберіть:");

                break;

            case "🧩 Додати в підфільтр":

                List<Map<String, Object>> selectedProducts = adminSelectedProducts.get(userId);
                //Map<String, Object> singleProduct = adminSelectedProduct.get(userId);

                if ((selectedProducts == null || selectedProducts.isEmpty()) && singleProduct == null) {
                    sendText(chatId, "⚠️ Спочатку виберіть товар.");
                    return;
                }

                userStates.put(userId, "awaiting_filter_for_product_subfilter");

                sendText(chatId, "🔽 Оберіть або введіть фільтр:");
                sendMessageSafely(createFilterSelectMenuAdmin(userId, chatId, 0));

                return;

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

            case "⚙️ Додати в фільтр":

                if (adminSelectedProduct.get(userId) == null) {
                    sendText(chatId, "⚠️ Спочатку виберіть товар.");
                    break;
                }

                userStates.put(userId, "awaiting_filter_select");

                sendText(chatId, "🔽 Оберіть фільтр:");

                sendMessageSafely(createFilterSelectMenu(userId, chatId, 0));

                return;

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

    private void handleAddToSubcategory(Long userId, String chatId, String text) {

        // 🔥 1. BACK
        if (text != null && text.equals(BACK_BUTTON)) {

            userStates.put(userId, "editing");

            sendText(chatId, "↩️ Повертаємось до редагування.");

            sendMessageSafely(createEditMenu(chatId, userId));
            return;
        }

        String subcategoryName = text.trim();

        // 🔥 2. БЕРЕМО СПИСОК
        List<Map<String, Object>> products = adminSelectedProducts.get(userId);

        System.out.println("🔥 PRODUCTS BEFORE = " +
                (products == null ? "NULL" : products.size()));

        // 🔥 fallback (1 товар)
        if (products == null || products.isEmpty()) {

            Map<String, Object> single = adminSelectedProduct.get(userId);

            if (single != null) {
                products = new ArrayList<>();
                products.add(single);
                System.out.println("⚠️ FALLBACK TO SINGLE PRODUCT");
            }
        }

        if (products == null || products.isEmpty()) {
            sendText(chatId, "❌ Немає вибраних товарів.");
            return;
        }

        // 🔥 3. ПЕРЕВІРКА ПІДКАТЕГОРІЇ
        if (!CatalogEditor.subcategoryExists(subcategoryName)) {
            sendText(chatId, "❌ Підкатегорія '" + subcategoryName + "' не знайдена.");
            return;
        }

        int added = 0;
        int exists = 0;
        int errors = 0;

        // 🔥 4. ОБРОБКА ВСІХ ТОВАРІВ
        for (Map<String, Object> product : products) {

            try {

                String name = safeToString(product.get("name"), null);

                if (name == null || name.isBlank()) continue;

                double price = CatalogEditor.getProductPriceFromYAML(name);
                if (price <= 0) price = 0;

                boolean success = CatalogEditor.addProductToSubcategory(
                        name,
                        price,
                        subcategoryName
                );

                if (success) {
                    added++;
                } else {
                    exists++;
                }

            } catch (Exception e) {
                errors++;
                e.printStackTrace();
            }
        }

        // 🔥 5. РЕЗУЛЬТАТ
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Результат:\n\n");
        sb.append("➕ Додано: ").append(added).append("\n");
        sb.append("⚠️ Вже існує: ").append(exists).append("\n");

        if (errors > 0) {
            sb.append("❌ Помилки: ").append(errors).append("\n");
        }

        sb.append("📦 Всього: ").append(products.size());

        sendText(chatId, sb.toString());

        // 🔥 6. ПОВЕРНЕННЯ
        userStates.put(userId, "editing");

        sendMessageSafely(createEditMenu(chatId, userId));
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
                userStates.put(userId, "add_category");
                sendText(chatId, "✏️ Введіть назву нової категорії:");
            }

            case "➕ Додати підкатегорію" -> {
                userStates.put(userId, "choose_category_for_sub");
                sendText(chatId, "📂 Введіть назву категорії, до якої хочете додати нову підкатегорію:");
            }

            case "➕ Додати фільтр у підкатегорію" -> {

                userStates.put(userId, "choose_subcategory_for_filter");

                try {
                    sendSubcategoriesForFilter(Long.valueOf(chatId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Помилка при відкритті списку підкатегорій.");
                }

                return;
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
                sendMessage(createDeveloperMenu(chatId));
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
    public void sendUserMenu(Long userId) {

        String chatId = String.valueOf(userId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 Головне меню");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Меню користовувача
    public SendMessage createUserMenu(String chatId, Long userId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🧱 Каталог товарів");
        row1.add("🎥 Відеоінструкція");
        row1.add("💬 Допомога");
        //row1.add("🔍 Пошук товару");
        //row1.add("\uD83D\uDED2 Кошик");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        //row2.add("🎯 Хіт продажу");
        row2.add("📍 Адреси та Контакти");
        row2.add("\uD83C\uDF10 Соц-мережі");
        keyboard.add(row2);

        /*KeyboardRow row3 = new KeyboardRow();
        row3.add("🎥 Відеоінструкція");
        row3.add("💬 Допомога");
        keyboard.add(row3);*/

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
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("⭐ Додати товар у Хіт продажу");
        row2.add("⚙️ Керування фільтрами");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔗 Запрошувальні посилання");
        row3.add("📜 Логирування");
        row3.add("💬 Відгуки користувачів");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton(BACK_BUTTON));
        keyboard.add(row4);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text("👨‍💻 Меню розробника, оберіть дію:")
                .replyMarkup(markup)
                .build();
    }

    // Меню оновлення
    private SendMessage createDeveloperCatalogMenu(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // 🔄 Оновити каталог (.yml) і 💰 Оновити ціни (.yml)
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 Оновити каталог (.yml)");
        row1.add("💰 Оновити ціни (.xlsx)");
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
        r4.add(new KeyboardButton("⚙️ Додати в фільтр"));

        KeyboardRow r5 = new KeyboardRow();
        r5.add(new KeyboardButton("🧩 Додати в підфільтр"));

        KeyboardRow r6 = new KeyboardRow();
        r6.add(new KeyboardButton("⬅️ Назад"));

        kb.setKeyboard(List.of(r1, r2, r3, r4, r5, r6));
        msg.setReplyMarkup(kb);

        return msg;
    }

    private SendMessage createCategoryAdminMenu(String chatId) {

        SendMessage msg = new SendMessage(chatId, "🔧 Редагування категорій:");

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("➕ Додати категорію"));
        r1.add(new KeyboardButton("➕ Додати підкатегорію"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("➕ Додати фільтр у підкатегорію"));
        r2.add(new KeyboardButton("✏️ Змінити назву категорії"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("🗑️ Видалити категорію"));
        r3.add(new KeyboardButton(BACK_BUTTON));

        kb.setKeyboard(List.of(r1, r2, r3));

        msg.setReplyMarkup(kb);

        return msg;
    }

    private SendMessage createHelpMenu(String chatId) {

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Написати консультанту");
        row1.add("💌 Відповіді");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🧮 Будівельний калькулятор");
        row2.add("💬 Залишити відгук");
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
                                "Допоможу порахувати: скільки потрібно фарби, плитки, ламінату, вагонки або плінтуса." +

                                "\n\n💬 *Залишити відгук*\n" +
                                "Поділись своєю думкою про наш сервіс — нам важливо знати, що можна покращити 💙"
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
        row3.add("Назад");
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

    private void handleText(Long chatId, String text) throws TelegramApiException {

        text = text.trim();

        System.out.println("[handleText] User " + chatId + " sent: " + text);

        String state = userStates.get(chatId);

        /* НАЗАД */
        if (text.equals("⬅️ Назад") || text.equals("Назад")) {

            userStates.remove(chatId);

            sendMessageSafely(createFiltersAdminMenu(chatId.toString()));
            return;
        }

        /* ПАГІНАЦІЯ ФІЛЬТРІВ */
        if ("awaiting_filter_for_subfilter".equals(state)) {

            int page = filterPage.getOrDefault(chatId, 0);

            if (text.equals("Продовжити")) {
                sendFilters(chatId, page + 1);
                return;
            }

            if (text.equals("Попередні")) {
                sendFilters(chatId, page - 1);
                return;
            }

            List<String> filters = userFilters.get(chatId);

            if (filters != null && filters.contains(text)) {

                selectedFilterForSubfilter.put(chatId, text);
                userStates.put(chatId, "awaiting_subfilter_name");

                sendText(chatId, "✏️ Введіть назву підфільтра:");
                return;
            }
        }

        /* КАТЕГОРІЇ */
        List<String> categories = catalogSearcher.getCategories();

        if (categories.contains(text)) {

            currentCategory.put(chatId, text);
            currentSubcategory.remove(chatId);

            sendSubcategories(chatId, text);
            return;
        }

        /* ПІДКАТЕГОРІЇ */
        if (currentCategory.containsKey(chatId)) {

            String category = currentCategory.get(chatId);

            List<String> subcats = catalogSearcher.getSubcategories(category);

            if (subcats.contains(text)) {

                currentSubcategory.put(chatId, text);
                productIndex.put(chatId, 0);

                List<String> filters = catalogSearcher.getFilters(category, text);

                userFilters.put(chatId, filters);
                filterPage.put(chatId, 0);

                sendFilters(chatId, 0);
                return;
            }
        }

        /* ПІДФІЛЬТРИ */
        String filter = currentFilter.get(chatId);

        if (filter != null) {

            List<String> subfilters = catalogSearcher.getSubfilters(filter);

            if (subfilters.contains(text)) {

                currentSubfilter.put(chatId, text);

                sendProductList(chatId);
                return;
            }
        }

        /* ФІЛЬТРИ */
        List<String> filters = userFilters.get(chatId);

        if (filters != null && filters.contains(text)) {

            currentFilter.put(chatId, text);
            currentSubfilter.remove(chatId);

            if (catalogSearcher.hasProductsInFilter(text)) {

                sendProductList(chatId);

            } else if (catalogSearcher.hasSubfilters(text)) {

                sendSubfilters(chatId, text, 0);

            } else {

                sendText(chatId, "❌ У цьому фільтрі немає товарів.");
            }

            return;
        }

        /* ВИБІР ТОВАРУ */
        if (userProductLists.containsKey(chatId)) {

            List<Map<String, Object>> list = userProductLists.get(chatId);

            try {

                int index = Integer.parseInt(text) - 1;

                if (index >= 0 && index < list.size()) {

                    Map<String, Object> product = list.get(index);

                    sendProductFromMap(chatId, product);
                    return;
                }

            } catch (NumberFormatException ignored) {}
        }

        /* НЕВІДОМА КОМАНДА */
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

            if (markup != null && markup.getKeyboard() != null) {

                int rows = markup.getKeyboard().size();
                int buttons = 0;

                for (KeyboardRow r : markup.getKeyboard()) {
                    buttons += r.size();
                }

                System.out.println("[DEBUG] Rows: " + rows + " Buttons: " + buttons);
            }

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

        String sql = """
        SELECT s.name
        FROM subcategories s
        JOIN categories c ON s.category_id = c.id
        WHERE c.name = ?
        ORDER BY s.id
    """;

        List<String> subcategories = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    subcategories.add(rs.getString("name"));
                }
            }

            if (subcategories.isEmpty()) {
                sendText(chatId, "❌ У цій категорії немає підкатегорій.");
                return;
            }

            ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboard(buildKeyboard(subcategories, true))
                    .build();

            sendMessage(
                    chatId,
                    "🧱 Обери підкатегорію, друже!\n\n" +
                            "Митрофан уже стоїть біля стелажа — каска на місці, ліхтар увімкнений 🔦\n" +
                            "Залишилось тільки сказати, що саме шукаємо — і я миттю знайду потрібний товар 😉",
                    markup
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendText(chatId, "⚠️ Помилка при отриманні підкатегорій із бази.");
        }
    }

    // 🔸 Показ товарів списком
    private void sendProductList(Long chatId) throws TelegramApiException {

        Long userId = chatId;

        String category = currentCategory.get(userId);
        String subcategory = currentSubcategory.get(userId);
        String filter = currentFilter.get(userId);
        String subfilter = currentSubfilter.get(userId);

        CatalogSearcher searcher = new CatalogSearcher();

        List<Map<String, Object>> products = searcher.findProducts(
                category,
                subcategory,
                filter,
                subfilter
        );

        if (products == null || products.isEmpty()) {
            sendText(chatId.toString(), "❌ Товарів у цьому розділі немає.");
            return;
        }

        // 🔥 зберігаємо список (ключ — chatId ок для UI)
        userProductLists.put(chatId, new ArrayList<>(products));

        StringBuilder sb = new StringBuilder("📦 Обери товар:\n\n");

        for (int i = 0; i < products.size(); i++) {

            Map<String, Object> product = products.get(i);

            String name = safeToString(product.get("name"), "Без назви");
            String price = safeToString(product.get("price"), "0");

            sb.append(i + 1)
                    .append(". ")
                    .append(name)
                    .append(" — ")
                    .append(price)
                    .append(" грн\n");
        }

        sendText(chatId.toString(), sb.toString());

        // 🔥 кнопка назад
        KeyboardRow row = new KeyboardRow();
        row.add(BACK_BUTTON);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        sendTextWithMarkup(chatId, "👇 Введи номер товару:", markup);
    }

    private void showRemainingProductList(Long chatId)
            throws TelegramApiException {

        List<Map<String, Object>> products =
                userProductLists.get(chatId);

        if (products == null || products.isEmpty()) {
            sendText(chatId,
                    "✅ Це був останній товар у підкатегорії.");
            return;
        }

        StringBuilder sb =
                new StringBuilder("\n📦 Інші товари:\n\n");

        for (int i = 0; i < products.size(); i++) {
            sb.append(i + 1)
                    .append(". ")
                    .append(products.get(i).get("name"))
                    .append("\n");
        }

        sendText(chatId, sb.toString());
    }

    /// Показ товару після списку
    public void sendProductFromMap(Long chatId, Map<String, Object> product) throws TelegramApiException {

        String name = safeToString(product.get("name"), "Без назви");
        String price = safeToString(product.get("price"), "0");
        String unit = safeToString(product.get("unit"), "шт").toLowerCase();
        String description = safeToString(product.get("description"), "");
        String photo = safeToString(product.get("photo"), "");

        boolean isCustomOrder = unit.equals("під замовлення");

        StringBuilder sb = new StringBuilder();
        sb.append("📦 ").append(name).append("\n");

        if (isCustomOrder) {
            sb.append("💬 Товар доступний під замовлення.\n")
                    .append("📞 Для уточнення ціни — натисни кнопку консультанта.");
        } else {
            sb.append("💰 Ціна: ").append(price).append(" грн за ").append(unit);
        }

        if (!description.isEmpty() && !"null".equalsIgnoreCase(description.trim())) {
            sb.append("\n📖 ").append(description);
        }

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        if (!isCustomOrder) {
            row1.add("🛒 Замовити товар");
        } else {
            row1.add("Написати консультанту");
        }
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);
        keyboard.add(row2);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        // 🔥 Відправка
        if (!photo.isEmpty() && !"null".equalsIgnoreCase(photo.trim())) {
            sendPhotoFromResources(chatId.toString(), photo, sb.toString(), markup);
        } else {
            sendTextWithMarkup(chatId, sb.toString(), markup);
        }
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

    public void sendSearchProduct(Long userId,
                                  String chatId,
                                  String text) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(getSearchKeyboard());

        try {
            execute(message);
        } catch (Exception e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }

    // 🔸 Показ замовлення користовувача Адміну
    private void createOrderAdminMenu(String chatId, Map<String, Object> order, Long userId) {
        String orderCode = String.valueOf(order.get("orderCode"));

        // Оновлюємо дані з бази — щоб статус оплати був актуальний!
        Map<String, Object> freshOrder = getOrderByCode(orderCode);
        if (freshOrder == null || freshOrder.isEmpty()) {
            sendText(chatId, "Помилка: замовлення не знайдено в базі.");
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("User ID: ").append(userId).append("\n")
                .append("Код замовлення: ").append(freshOrder.getOrDefault("orderCode", "-")).append("\n")
                .append("Тип замовлення: ").append(freshOrder.getOrDefault("deliveryType", "Невідомо")).append("\n\n");

        // Спосіб оплати та статус — красиво і зрозуміло
        String paymentMethodRaw = String.valueOf(freshOrder.getOrDefault("paymentmethod", ""));
        String paymentStatusRaw = String.valueOf(freshOrder.getOrDefault("paymentstatus", ""));

        String paymentMethod = switch (paymentMethodRaw) {
            case "Готівка" -> "Готівкою при отриманні";
            case "Очікується", "Оплачено", "Відхилено", "Підозріла сума" -> "Онлайн-оплата (карткою)";
            default -> "Не вказано";
        };

        String paymentStatus = switch (paymentStatusRaw) {
            case "Оплачено"       -> "Оплачено";
            case "Очікується"     -> "Очікується оплата";
            case "Відхилено"      -> "Оплата відхилена";
            case "Підозріла сума" -> "Підозріла сума!";
            case "Готівка"        -> "Готівкою при отриманні";
            default               -> "Не вказано";
        };

        sb.append("Спосіб оплати: ").append(paymentMethod).append("\n")
                .append("Статус оплати: ").append(paymentStatus).append("\n\n");

        // Доставка
        String deliveryType = (String) freshOrder.get("deliveryType");
        if ("Самовивіз".equals(deliveryType)) {
            sb.append("Місто: ").append(freshOrder.getOrDefault("city", "-")).append("\n");
        } else if ("Доставка по місту".equals(deliveryType)) {
            sb.append("Адреса: ").append(freshOrder.getOrDefault("address", "-")).append("\n");
        }
        /*} else if ("Нова пошта".equals(deliveryType)) {
            sb.append("Відділення НП: ").append(freshOrder.getOrDefault("postOffice", "-")).append("\n");
        }*/

        sb.append("П.І.: ").append(freshOrder.getOrDefault("fullName", "-")).append("\n")
                .append("Телефон: ").append(freshOrder.getOrDefault("phone", "-")).append("\n")
                .append("Картка (магазину): ").append(freshOrder.getOrDefault("card", "-")).append("\n\n");

        // Товари
        String itemsStr = (String) freshOrder.get("item");
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
                    if (parts.length > 1) price = Double.parseDouble(parts[1]);
                    if (parts.length > 2) quantity = Integer.parseInt(parts[2]);
                } catch (Exception ignored) {}
                double lineTotal = price * quantity;

                sb.append(i++).append(". ").append(name).append("\n")
                        .append("   Кількість: ").append(quantity).append("\n")
                        .append("   Ціна: ").append(price).append(" грн\n")
                        .append("   Разом: ").append(lineTotal).append(" грн\n\n");
            }
        }

        // Загальна сума
        double total = 0.0;
        Object totalObj = freshOrder.get("total");
        if (totalObj instanceof Number) {
            total = ((Number) totalObj).doubleValue();
        } else if (totalObj != null) {
            try { total = Double.parseDouble(totalObj.toString()); } catch (Exception ignored) {}
        }

        sb.append("**Всього до оплати:** ").append(String.format("%.2f", total)).append(" грн\n");

        // Кнопки
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
        row3.add(BACK_BUTTON);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(sb.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendText(chatId, "Помилка при відправці повідомлення адміну.");
        }
    }

    private Map<String, Object> getOrderByCode(String orderCode) {
        String sql = "SELECT userId, orderCode, deliveryType, city, address, postOffice, " +
                "fullName, phone, card, item, total, paymentmethod, paymentstatus, status, date " +
                "FROM orders WHERE orderCode = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, orderCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, Object> order = new HashMap<>();
                order.put("userId", rs.getObject("userId"));
                order.put("orderCode", rs.getObject("orderCode"));
                order.put("deliveryType", rs.getObject("deliveryType"));
                order.put("city", rs.getObject("city"));
                order.put("address", rs.getObject("address"));
                order.put("postOffice", rs.getObject("postOffice"));
                order.put("fullName", rs.getObject("fullName"));
                order.put("phone", rs.getObject("phone"));
                order.put("card", rs.getObject("card"));
                order.put("item", rs.getObject("item"));
                order.put("total", rs.getObject("total"));
                order.put("paymentmethod", rs.getObject("paymentmethod"));
                order.put("paymentstatus", rs.getObject("paymentstatus"));
                order.put("status", rs.getObject("status"));
                order.put("date", rs.getObject("date"));
                return order;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        row2.add("🔍 Пошук з EXCEL");

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
        row1.add(VIEW_CART_BUTTON);
        rows.add(row1);

        // другий ряд — кнопка назад
        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void handleAdminSearchSource(Long userId, String chatId, String text)
            throws TelegramApiException {

        switch (text) {

            case "🔍 Пошук у MySQL" -> {
                adminSearchSource.put(userId, "mysql");
                userStates.put(userId, "awaiting_search");
                sendText(chatId, "🔍 Введіть ключові слова для пошуку у MySQL:");
            }

            case "🔍 Пошук з EXCEL" -> {
                adminSearchSource.put(userId, "excel");
                userStates.put(userId, "awaiting_search");
                sendText(chatId, "🔍 Введіть назву товару для пошуку в Excel:");
            }

            default -> sendText(chatId, "❌ Невідома опція. Оберіть із меню.");
        }
    }

    public void handleSearchNumber(Long userId, String chatId, String text) {

        List<Map<String, Object>> results = searchResults.get(userId);

        if (results == null || results.isEmpty()) {
            sendText(chatId, "❌ Список товарів порожній.");
            return;
        }

        int index;

        try {
            index = Integer.parseInt(text) - 1;
        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Введіть номер товару цифрою.");
            return;
        }

        if (index < 0 || index >= results.size()) {
            sendText(chatId, "❌ Невірний номер товару.");
            return;
        }

        Map<String, Object> product = results.get(index);

        // 🔥 ОСЬ ГОЛОВНЕ — зберігаємо товар
        lastShownProduct.put(userId, product);

        try {
            sendProductFromMap(userId, product);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleAdminSearchInput(Long userId, String chatId, String text)
            throws TelegramApiException {

        String source = adminSearchSource.getOrDefault(userId, "mysql");
        List<Map<String, Object>> results = new ArrayList<>();

        switch (source) {

            case "mysql" -> {
                CatalogSearcher searcher = new CatalogSearcher();
                results = searcher.searchByKeywordsAdmin(text);
            }

            case "yaml" -> {
                try {
                    results = CatalogUpdater.searchProductsSimple(text);
                } catch (Exception e) {
                    sendText(chatId, "❌ Помилка при пошуку у YAML: " + e.getMessage());
                    return;
                }
            }

            case "excel" -> {
                try {
                    List<ExcelToMySqlImporter.ExcelProduct> excelResults =
                            ExcelToMySqlImporter.searchInExcel(text);

                    for (ExcelToMySqlImporter.ExcelProduct p : excelResults) {

                        Map<String, Object> map = new HashMap<>();

                        map.put("name", p.name);
                        map.put("price", p.price);
                        map.put("group", p.group); // 🔥 ДОДАЛИ ГРУПУ
                        map.put("_excel", p);

                        results.add(map);
                    }

                } catch (Exception e) {
                    sendText(chatId, "❌ Помилка при пошуку у Excel: " + e.getMessage());
                    return;
                }
            }
        }

        if (results == null || results.isEmpty()) {
            sendText(chatId, "❌ Товар не знайдено: " + text);
            return;
        }

        // 🔥 ЗБЕРІГАЄМО
        adminMatchList.put(userId, results);

        StringBuilder sb = new StringBuilder();
        sb.append("🔎 Знайдено товари. Введіть номер для редагування:\n\n");

        for (int i = 0; i < results.size(); i++) {

            Map<String, Object> prod = results.get(i);

            String name = safeToString(prod.get("name"), "Без назви");
            String price = safeToString(prod.get("price"), "");
            String group = safeToString(prod.get("group"), "");

            sb.append(i + 1).append(". ").append(name);

            // 🔥 показ групи
            if (!group.isBlank() && !"null".equalsIgnoreCase(group)) {
                sb.append(" [").append(group).append("]");
            }

            // 🔥 показ ціни
            if (!price.isBlank()) {
                sb.append(" | Ціна: ").append(price);
            }

            sb.append("\n");
        }

        sendText(chatId, sb.toString());

        userStates.put(userId, "choose_product");
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

    //Пошук товара в каталозі
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

        // Рядок з кнопкою відповісти
        KeyboardRow row1 = new KeyboardRow();
        row1.add("💬 Відповісти покупцю");

        // Рядок з кнопкою назад (використовує BACK_BUTTON)
        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);

        km.setKeyboard(List.of(row1, row2));
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

        // 🔹 беремо кошик
        List<Map<String, Object>> cart = userCart.get(userId);

        if (cart == null || cart.isEmpty()) {
            sendText(chatId, "❌ Кошик порожній.");
            return;
        }

        // 🔹 генеруємо код замовлення
        int code = (int)(System.currentTimeMillis() % 10000);
        String orderCode = "ORD-" + code;

        // 🔹 телефон
        String phone = userPhones.get(userId);

        if (phone == null || phone.isBlank()) {
            sendText(chatId, "❌ Не знайдено номер телефону.");
            return;
        }

        // 🔹 інші дані (можуть бути null — це ок)
        String deliveryType = tempDeliveryType.getOrDefault(userId, "Не вказано");
        String addressOrCityOrPost = tempAddress.get(userId);
        String fullname = tempFullname.getOrDefault(userId, "Користувач");
        String card = tempCard.get(userId);

        // 🔹 формуємо items + total
        StringBuilder itemsBuilder = new StringBuilder();
        double total = 0.0;

        for (Map<String, Object> item : cart) {

            String name = String.valueOf(item.get("name"));
            int qty = Integer.parseInt(item.get("quantity").toString());
            double price = Double.parseDouble(item.get("price").toString());

            itemsBuilder.append(name)
                    .append(" x")
                    .append(qty)
                    .append("; ");

            total += price * qty;
        }

        String items = itemsBuilder.toString();

        // 🔹 розподіл доставки
        String city = null;
        String address = null;
        String postOffice = null;

        switch (deliveryType) {
            case "Самовивіз" -> city = addressOrCityOrPost;
            case "Доставка по місту" -> address = addressOrCityOrPost;
        }

        // ================= DB =================
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
            stmt.setString(4, city);
            stmt.setString(5, address);
            stmt.setString(6, postOffice);
            stmt.setString(7, fullname);
            stmt.setString(8, phone);
            stmt.setString(9, card);
            stmt.setString(10, items);
            stmt.setDouble(11, Math.round(total * 100.0) / 100.0);
            stmt.setString(12, method);

            String paymentStatus = isOnline ? "Очікується" : "Готівка";
            stmt.setString(13, paymentStatus);

            stmt.setString(14, "Нове");

            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("ORDER ERROR:");
            e.printStackTrace();

            sendText(chatId,
                    "❌ Помилка при створенні замовлення.\n" +
                            "Спробуйте ще раз пізніше.");
            return;
        }

        // ================= ОЧИСТКА =================
        userCart.remove(userId);
        userStates.remove(userId);
        userPhones.remove(userId);

        tempDeliveryType.remove(userId);
        tempAddress.remove(userId);
        tempFullname.remove(userId);
        tempCard.remove(userId);
        tempOrderCode.remove(userId);
        tempOrderItems.remove(userId);
        tempTotal.remove(userId);

        // ================= ОПЛАТА =================
        if (isOnline) {

            WayForPayService wfp = new WayForPayService();
            String payUrl = wfp.createInvoice(orderCode, (int) total);

            if (payUrl == null || payUrl.isBlank()) {
                sendText(chatId,
                        "❌ Помилка створення оплати. Спробуйте пізніше.");
                return;
            }

            sendPaymentButton(chatId, payUrl);

            sendText(chatId,
                    "✅ Замовлення створено!\n\n" +
                            "🔢 Код: " + orderCode + "\n" +
                            "💰 Сума: " + total + " грн\n\n" +
                            "💳 Натисніть кнопку нижче для оплати 👇");

        } else {

            sendText(chatId,
                    "✅ Замовлення оформлено!\n\n" +
                            "🔢 Код: " + orderCode + "\n" +
                            "💵 Оплата: готівка при отриманні\n\n" +
                            "📞 Наш менеджер зателефонує вам найближчим часом\n" +
                            "для підтвердження замовлення.\n\n" +
                            "Дякуємо за покупку 🙌");
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

    private int extractMaxQuantity(String quantityText) {
        if (quantityText == null) return -1;

        int max = -1;
        Matcher matcher = Pattern.compile("(\\d+)").matcher(quantityText);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value > max) max = value;
        }
        return max;
    }

    private void confirmFirstNewOrder(Long chatId, long adminUserId) {
        try (Connection conn = DatabaseManager.getConnection()) {

            conn.setAutoCommit(false);

            String selectSql =
                    "SELECT id, userId, item FROM orders " +
                            "WHERE status='Нове' ORDER BY id ASC LIMIT 1";

            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    sendText(chatId, "Замовлень немає.");
                    conn.rollback();
                    return;
                }

                long orderId = rs.getLong("id");
                long orderUserId = rs.getLong("userId");
                String itemsText = rs.getString("item");

                System.out.println("DEBUG confirm orderId=" + orderId + " userId=" + orderUserId);
                System.out.println("DEBUG itemsText=[" + itemsText + "]");

                // 🔻 мінус складу
                userManager.decreaseProductsFromAnyOrderItemFormat(conn, itemsText);

                // 🔄 статус
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE orders SET status='Підтверджено' WHERE id=?")) {
                    upd.setLong(1, orderId);
                    int updRows = upd.executeUpdate();
                    System.out.println("DEBUG orders status updatedRows=" + updRows);
                }

                conn.commit();

                // 👤 користувачу
                sendText(String.valueOf(orderUserId),
                        "🧱 Ваше замовлення прийнято!\n\n" +
                                "Митрофан вже складає його на піддон 😄\n" +
                                "Дякуємо, що обираєте GigaHub ❤️");

                sendText(chatId, "Замовлення підтверджено ✅");
                showAdminOrder(Long.valueOf(adminUserId), String.valueOf(chatId));
            }

        } catch (Exception e) {
            e.printStackTrace();
            try { sendText(chatId, "❌ Помилка при підтвердженні замовлення."); } catch (Exception ignored) {}
        }
    }

    public void decreaseProductsFromAnyOrderItemFormat(Connection conn, String item) throws SQLException {
        if (item == null || item.isBlank()) return;

        int totalDecreased = 0;

        // ✅ Формат A: name:price:qty; name2:price:qty;
        // Приклад: "Цемент ...:185.0:9;"
        String[] chunks = item.split(";");
        for (String chunk : chunks) {
            chunk = chunk.trim();
            if (chunk.isEmpty()) continue;

            String[] parts = chunk.split(":");
            if (parts.length >= 3) {
                String name = parts[0].trim();
                String qtyStr = parts[parts.length - 1].trim();

                Integer qty = safeParseInt(qtyStr);
                if (qty == null) continue;

                Long productId = findProductIdByName(conn, name);
                System.out.println("DEBUG parseA name=[" + name + "] qty=" + qty + " productId=" + productId);

                if (productId != null) {
                    int changed = decreaseProductQuantity(conn, productId, qty);
                    totalDecreased += changed;
                }
            }
        }

        if (totalDecreased == 0 && item.contains("Кількість")) {
            String[] lines = item.split("\n");
            String currentName = null;
            Integer currentQty = null;

            for (String line : lines) {
                String t = line.trim();

                if (t.matches("^\\d+\\.\\s+.+")) {
                    currentName = t.replaceFirst("^\\d+\\.\\s+", "").trim();
                } else if (t.startsWith("Кількість:")) {
                    currentQty = safeParseInt(t.replace("Кількість:", "").trim());
                }

                if (currentName != null && currentQty != null) {
                    Long productId = findProductIdByName(conn, currentName);
                    System.out.println("DEBUG parseB name=[" + currentName + "] qty=" + currentQty + " productId=" + productId);

                    if (productId != null) {
                        int changed = decreaseProductQuantity(conn, productId, currentQty);
                        totalDecreased += changed;
                    }
                    currentName = null;
                    currentQty = null;
                }
            }
        }

        System.out.println("DEBUG totalDecreasedRows=" + totalDecreased);
    }

    private Long findProductIdByName(Connection conn, String name) throws SQLException {
        if (name == null || name.isBlank()) return null;

        // нормалізація (щоб менше ламалось на пробілах)
        String norm = name.toLowerCase().replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();

        String sql = "SELECT id, name FROM products WHERE LOWER(name) LIKE ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + norm + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String dbName = rs.getString("name");
                    System.out.println("DEBUG DB MATCH [" + dbName + "] id=" + id);
                    return id;
                }
            }
        }

        System.out.println("DEBUG NO DB MATCH for [" + norm + "]");
        return null;
    }
    public int decreaseProductQuantity(Connection conn, long productId, int minus) throws SQLException {
        String selectSql = "SELECT quantity FROM products WHERE id = ?";
        String updateSql = "UPDATE products SET quantity = ? WHERE id = ?";

        String quantityText;
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setLong(1, productId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("DEBUG product not found id=" + productId);
                    return 0;
                }
                quantityText = rs.getString("quantity");
            }
        }

        int current = extractMaxQuantity(quantityText);
        if (current < 0) {
            System.out.println("DEBUG quantity has no number id=" + productId + " text=[" + quantityText + "]");
            return 0;
        }

        int newQty = Math.max(0, current - minus);

        int updatedRows;
        try (PreparedStatement update = conn.prepareStatement(updateSql)) {
            update.setString(1, String.valueOf(newQty)); // пишемо просто число
            update.setLong(2, productId);
            updatedRows = update.executeUpdate();
        }

        System.out.println("DEBUG decrease id=" + productId + " was=" + current + " minus=" + minus + " now=" + newQty + " updatedRows=" + updatedRows);
        return updatedRows;
    }

    private Integer safeParseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return null; }
    }

    private String escapeTelegram(String text) {

        if (text == null) return "";

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private List<String> getFiltersFromDatabase(){

        List<String> filters = new ArrayList<>();

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT name FROM filters ORDER BY name");

            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                filters.add(rs.getString("name"));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return filters;
    }

    private SendMessage createFilterSelectMenu(Long userId, String chatId, int page) {

        // 🔥 БЕРЕМО КОНТЕКСТ
        String category = currentCategory.get(userId);
        String subcategory = currentSubcategory.get(userId);

        System.out.println("🔥 CATEGORY = " + category);
        System.out.println("🔥 SUBCATEGORY = " + subcategory);

        if (category == null || subcategory == null) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Помилка: не вибрана категорія або підкатегорія.")
                    .build();
        }

        // 🔥 ГОЛОВНИЙ ФІКС
        List<String> filters = catalogSearcher.getFilters(category, subcategory);

        if (filters == null || filters.isEmpty()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Фільтрів немає.")
                    .build();
        }

        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) filters.size() / pageSize);

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        filterPage.put(userId, page);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, filters.size());

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        int count = 0;

        for (int i = start; i < end; i++) {

            row.add(filters.get(i));
            count++;

            if (count == 2) {
                rows.add(row);
                row = new KeyboardRow();
                count = 0;
            }
        }

        if (!row.isEmpty()) rows.add(row);

        // 🔹 ПАГІНАЦІЯ
        KeyboardRow nav = new KeyboardRow();

        if (page > 0) nav.add("Попередні");
        if (page < totalPages - 1) nav.add("Продовжити");

        if (!nav.isEmpty()) rows.add(nav);

        // 🔹 НАЗАД
        KeyboardRow back = new KeyboardRow();
        back.add(BACK_BUTTON);
        rows.add(back);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);

        return SendMessage.builder()
                .chatId(chatId)
                .text("🔎 Оберіть фільтр (" + (page + 1) + "/" + totalPages + ")")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createFilterSelectMenuAdmin(Long userId, String chatId, int page) {

        List<String> filters = catalogSearcher.getFiltersAdmin();

        if (filters == null || filters.isEmpty()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Фільтрів немає.")
                    .build();
        }

        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) filters.size() / pageSize);

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        filterPage.put(userId, page);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, filters.size());

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        int count = 0;

        for (int i = start; i < end; i++) {

            row.add(filters.get(i));
            count++;

            if (count == 2) {
                rows.add(row);
                row = new KeyboardRow();
                count = 0;
            }
        }

        if (!row.isEmpty()) rows.add(row);

        // 🔹 ПАГІНАЦІЯ
        KeyboardRow nav = new KeyboardRow();

        if (page > 0) nav.add("Попередні");
        if (page < totalPages - 1) nav.add("Продовжити");

        if (!nav.isEmpty()) rows.add(nav);

        // 🔹 НАЗАД
        KeyboardRow back = new KeyboardRow();
        back.add(BACK_BUTTON);
        rows.add(back);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);

        return SendMessage.builder()
                .chatId(chatId)
                .text("🛠 Оберіть фільтр (адмін) (" + (page + 1) + "/" + totalPages + ")")
                .replyMarkup(markup)
                .build();
    }

    private SendMessage createFiltersAdminMenu(String chatId) {

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Додати фільтр");
        row1.add("➕ Додати підфільтр");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("✏️ Редагувати фільтр");
        row2.add("🗑 Видалити фільтр");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📋 Список фільтрів");
        row3.add("📦 Товари у фільтрі");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("🧹 Очистити фільтр");

        KeyboardRow row5 = new KeyboardRow();
        row5.add(BACK_BUTTON);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        markup.setKeyboard(keyboard);

        return SendMessage.builder()
                .chatId(chatId)
                .text("⚙️ Керування фільтрами:")
                .replyMarkup(markup)
                .build();
    }

    private void sendFiltersList(String chatId) {

        StringBuilder sb = new StringBuilder("📋 Список фільтрів:\n\n");

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT name FROM filters ORDER BY name");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                sb.append("• ").append(rs.getString("name")).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        sendText(chatId, sb.toString());
    }

    //Створення фільтра
    private void createFilter(String name) {

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO filters(name) VALUES (?)");

            ps.setString(1, name);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Додати товар у фільтр
    private boolean addProductToFilter(Long userId, String filterName, String subfilterName) {

        Map<String, Object> product = adminSelectedProduct.get(userId);

        System.out.println("==== ADD PRODUCT TO FILTER ====");
        System.out.println("USER: " + userId);
        System.out.println("FILTER: " + filterName);
        System.out.println("SUBFILTER: " + subfilterName);
        System.out.println("PRODUCT: " + product);

        if (product == null) {
            System.out.println("❌ PRODUCT IS NULL");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection()) {

            // 🔹 1. ID товару
            Integer productId = null;

            if (product.containsKey("id") && product.get("id") != null) {
                productId = Integer.parseInt(product.get("id").toString());
                System.out.println("✔ Product ID from map: " + productId);
            } else {
                System.out.println("⚠ No ID, searching by name...");

                String name = product.get("name").toString();

                String findSql = "SELECT id FROM products WHERE LOWER(name) LIKE ? LIMIT 1";

                try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                    ps.setString(1, "%" + name.toLowerCase() + "%");

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        productId = rs.getInt("id");
                        System.out.println("✔ Found product in DB: " + productId);
                    } else {
                        System.out.println("❌ Product not found in DB");
                        return false;
                    }
                }
            }

            // 🔹 2. filter_id
            Integer filterId = null;

            String filterSql = "SELECT id FROM filters WHERE LOWER(name) LIKE ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(filterSql)) {
                ps.setString(1, "%" + filterName.toLowerCase() + "%");

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    filterId = rs.getInt("id");
                    System.out.println("✔ Filter ID: " + filterId);
                } else {
                    System.out.println("❌ Filter NOT found: " + filterName);
                    return false;
                }
            }

            // 🔹 3. subfilter_id (опціонально)
            Integer subfilterId = null;

            if (subfilterName != null) {

                String subSql = """
                SELECT sf.id
                FROM subfilters sf
                JOIN filters f ON sf.filter_id = f.id
                WHERE LOWER(sf.name) LIKE ? AND LOWER(f.name) LIKE ?
            """;

                try (PreparedStatement ps = conn.prepareStatement(subSql)) {
                    ps.setString(1, "%" + subfilterName.toLowerCase() + "%");
                    ps.setString(2, "%" + filterName.toLowerCase() + "%");

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        subfilterId = rs.getInt("id");
                        System.out.println("✔ Subfilter ID: " + subfilterId);
                    } else {
                        System.out.println("⚠ No subfilter found");
                    }
                }
            }

            // 🔥 4. UPDATE
            String updateSql = """
            UPDATE products
            SET filter_id=?, subfilter_id=?
            WHERE id=?
        """;

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {

                ps.setInt(1, filterId);

                if (subfilterId != null) {
                    ps.setInt(2, subfilterId);
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }

                ps.setInt(3, productId);

                int updated = ps.executeUpdate();

                System.out.println("UPDATE RESULT: " + updated);

                if (updated == 0) {
                    System.out.println("❌ UPDATE = 0 (нічого не оновилось)");
                    return false;
                }

                System.out.println("✅ SUCCESS");
                return true;
            }

        } catch (Exception e) {
            System.out.println("❌ EXCEPTION:");
            e.printStackTrace();
            return false;
        }
    }

    private void sendFilters(Long chatId, int page) throws TelegramApiException {

        List<String> filters = userFilters.get(chatId);

        if (filters == null || filters.isEmpty()) {
            sendText(chatId, "❌ Фільтри не знайдено.");
            return;
        }

        int pageSize = 20;
        int totalPages = (int) Math.ceil((double) filters.size() / pageSize);

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        filterPage.put(chatId, page);

        int start = page * pageSize;
        int end = Math.min(start + pageSize, filters.size());

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (int i = start; i < end; i++) {

            String filter = filters.get(i);

            if (filter.length() > 25) {
                filter = filter.substring(0, 25);
            }

            row.add(filter);

            if (row.size() == 2) {
                rows.add(row);
                row = new KeyboardRow();
            }
        }

        if (!row.isEmpty()) {
            rows.add(row);
        }

        KeyboardRow nav = new KeyboardRow();

        if (page > 0) nav.add("Попередні");
        if (page < totalPages - 1) nav.add("Продовжити");

        if (!nav.isEmpty()) rows.add(nav);

        KeyboardRow back = new KeyboardRow();
        back.add(BACK_BUTTON);
        rows.add(back);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);

        sendMessage(chatId,
                "🔎 Обери фільтр (" + (page + 1) + "/" + totalPages + "):",
                markup);
    }

    private void sendSubfilters(Long chatId, String filterName, int page) throws TelegramApiException {

        if (filterName == null || filterName.isBlank()) {
            sendText(chatId, "❌ Помилка: фільтр не визначений.");
            return;
        }

        List<String> subfilters = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement ps = conn.prepareStatement("""
            SELECT sf.name
            FROM subfilters sf
            JOIN filters f ON sf.filter_id = f.id
            WHERE LOWER(f.name) = LOWER(?)
        """);

            ps.setString(1, filterName.trim());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");

                if (name != null && !name.isBlank()) {
                    subfilters.add(name.trim());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "❌ Помилка при завантаженні підфільтрів.");
            return;
        }

        // 🔴 якщо нічого не знайдено
        if (subfilters.isEmpty()) {
            sendText(chatId, "❌ Підфільтрів не знайдено для: " + filterName);
            return;
        }

        // 🔥 Убираємо дублікати (як у тебе було на скріні)
        subfilters = subfilters.stream().distinct().toList();

        // 🔹 Пагінація
        int pageSize = 20;
        int totalPages = (int) Math.ceil((double) subfilters.size() / pageSize);

        if (totalPages == 0) totalPages = 1;

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * pageSize;
        int end = Math.min(start + pageSize, subfilters.size());

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (int i = start; i < end; i++) {

            String sub = subfilters.get(i);

            // ❗ НЕ обрізаємо текст — це було джерело багів
            row.add(sub);

            if (row.size() == 2) {
                rows.add(row);
                row = new KeyboardRow();
            }
        }

        if (!row.isEmpty()) {
            rows.add(row);
        }

        // 🔹 Навігація
        KeyboardRow nav = new KeyboardRow();

        if (page > 0) {
            nav.add("⬅️ Попередні");
        }

        if (page < totalPages - 1) {
            nav.add("➡️ Далі");
        }

        if (!nav.isEmpty()) {
            rows.add(nav);
        }

        // 🔹 Назад
        KeyboardRow back = new KeyboardRow();
        back.add(BACK_BUTTON);
        rows.add(back);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);

        sendMessage(
                chatId,
                "🔎 Обери підфільтр (" + (page + 1) + "/" + totalPages + "):",
                markup
        );
    }

    private boolean hasSubfilters(String filterName){

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

    private void sendProductsByFilter(Long chatId, String filterName) {

        String sql = """
        SELECT p.name, p.price
        FROM products p
        JOIN filters f ON p.filter_id = f.id
        WHERE f.name = ?
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, filterName);

            ResultSet rs = ps.executeQuery();

            StringBuilder message = new StringBuilder("📦 Товари:\n\n");

            boolean found = false;

            while (rs.next()) {

                found = true;

                message.append("🔹 ")
                        .append(rs.getString("name"))
                        .append(" — ")
                        .append(rs.getDouble("price"))
                        .append(" грн\n");
            }

            if (!found) {
                sendText(chatId, "❌ У цьому фільтрі немає товарів.");
                return;
            }

            sendText(chatId, message.toString());

        } catch (Exception e) {

            e.printStackTrace();
            sendText(chatId, "⚠️ Помилка при отриманні товарів.");
        }
    }

    private void sendSubcategoriesForFilter(Long chatId) throws TelegramApiException {

        CatalogSearcher searcher = new CatalogSearcher();

        List<String> subcategories = searcher.getAllSubcategories();

        if(subcategories.isEmpty()){
            sendText(chatId,"❌ Підкатегорій немає.");
            return;
        }

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for(int i=0;i<subcategories.size();i++){

            row.add(subcategories.get(i));

            if(row.size() == 2){
                rows.add(row);
                row = new KeyboardRow();
            }
        }

        if(!row.isEmpty()){
            rows.add(row);
        }

        KeyboardRow back = new KeyboardRow();
        back.add("⬅️ Назад");

        rows.add(back);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);

        sendMessage(chatId,"📂 Обери підкатегорію:",markup);
    }

    private void chooseSubcategoryForFilter(Long userId, Long chatId, String text) throws TelegramApiException {

        List<String> subcategories = catalogSearcher.getAllSubcategories();

        if (!subcategories.contains(text)) {
            sendText(chatId.toString(), "❌ Такої підкатегорії не існує.");
            return;
        }

        // зберігаємо підкатегорію
        selectedSubcategoryForFilter.put(userId, text);

        // змінюємо state
        userStates.put(userId, "choose_filter_for_subcategory");

        filterPage.put(userId, 0);

        sendMessageSafely(createFilterSelectMenuAdmin(userId, chatId.toString(), 0));
    }

    public void addFilterToSubcategory(String subcategoryName,String filterName){

        String sql = """
        UPDATE filters
        SET subcategory_id = (
            SELECT id FROM subcategories WHERE name = ?
        )
        WHERE name = ?
    """;

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, subcategoryName);
            ps.setString(2, filterName);

            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void addFilter(Long userId, String chatId, String filterName) {

        String subcategory = userSelectedSubcategory.get(userId);

        CatalogSearcher searcher = new CatalogSearcher();

        searcher.insertFilter(filterName, subcategory);

        userStates.remove(userId);
        userSelectedSubcategory.remove(userId);

        sendText(chatId, "✅ Фільтр успішно додано.");
    }

    private void chooseFilterForSubfilter(Long userId, String chatId, String filterName){

        // 🔹 ІГНОРУЄМО кнопки
        if (filterName.equals("Продовжити") ||
                filterName.equals("Попередні") ||
                filterName.equals("Назад")) {
            return;
        }

        // 🔥 БЕРЕМО ЗБЕРЕЖЕНІ ФІЛЬТРИ (НЕ з БД!)
        List<String> filters = userFilters.get(userId);

        if (filters == null || !filters.contains(filterName)) {
            sendText(chatId, "❌ Фільтр не знайдено.");
            return;
        }

        selectedFilterForSubfilter.put(userId, filterName);

        userStates.put(userId, "awaiting_subfilter_name");

        sendText(chatId, "✏️ Введіть назву підфільтра:");
    }

    private void createSubfilter(Long userId, String chatId, String subfilterName){

        String filterName = selectedFilterForSubfilter.get(userId);

        try(Connection conn = DatabaseManager.getConnection()){

            PreparedStatement getFilter = conn.prepareStatement(
                    "SELECT id FROM filters WHERE name=?");

            getFilter.setString(1,filterName);

            ResultSet rs = getFilter.executeQuery();

            if(rs.next()){

                int filterId = rs.getInt("id");

                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO subfilters(name,filter_id) VALUES(?,?)");

                insert.setString(1,subfilterName);
                insert.setInt(2,filterId);

                insert.executeUpdate();

                sendText(chatId,
                        "✅ Підфільтр '"+subfilterName+
                                "' додано до фільтра '"+filterName+"'");

            }

        }catch(Exception e){
            e.printStackTrace();
        }

        userStates.remove(userId);
        selectedFilterForSubfilter.remove(userId);
    }

    private Map<String,String> parseDeliveryData(String text){

        Map<String,String> data = new HashMap<>();

        // вставляємо пробіли між буквами і цифрами
        text = text.replaceAll("([\\p{L}])([0-9])", "$1 $2");
        text = text.replaceAll("([0-9])([\\p{L}])", "$1 $2");

        // шукаємо телефон
        Pattern phonePattern = Pattern.compile("(\\+?\\d{10,13})");
        Matcher phoneMatcher = phonePattern.matcher(text);

        if(!phoneMatcher.find()) return null;

        String phone = phoneMatcher.group(1);

        text = text.replace(phone,"");

        // прибираємо коми
        text = text.replaceAll("[,;]", " ");

        // нормалізуємо пробіли
        text = text.trim().replaceAll("\\s+"," ");

        String[] words = text.split(" ");

        if(words.length == 0) return null;

        String city = words[0];

        StringBuilder nameBuilder = new StringBuilder();

        for(int i=1;i<words.length;i++){
            nameBuilder.append(words[i]).append(" ");
        }

        String name = nameBuilder.toString().trim();

        if(name.isEmpty()) name = "Клієнт";

        data.put("city",city);
        data.put("name",name);
        data.put("phone",phone);

        return data;
    }

    private void createTempOrder(Long userId, List<Map<String, Object>> cart) {

        String orderCode = String.format("%04d", new Random().nextInt(10000));
        tempOrderCode.put(userId, orderCode);

        StringBuilder items = new StringBuilder();
        double total = 0;

        for (Map<String, Object> item : cart) {

            String name = item.get("name").toString();

            double price = Double.parseDouble(
                    item.get("price").toString()
            );

            int qty = item.get("quantity") == null
                    ? 1
                    : Integer.parseInt(item.get("quantity").toString());

            total += price * qty;

            items.append(name)
                    .append(":")
                    .append(price)
                    .append(":")
                    .append(qty)
                    .append(";");
        }

        tempOrderItems.put(userId, items.toString());
        tempTotal.put(userId, total);
    }

    private SendMessage createSubfilterMenu(String filter, String chatId) {

        SendMessage msg = new SendMessage(chatId, "Оберіть підфільтр:");

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        CatalogSearcher searcher = new CatalogSearcher();
        List<String> subfilters = searcher.getSubfilters(filter);

        List<KeyboardRow> keyboard = new ArrayList<>();

        for (String sub : subfilters) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(sub));
            keyboard.add(row);
        }

        KeyboardRow back = new KeyboardRow();
        back.add(new KeyboardButton("⬅️ Назад"));
        keyboard.add(back);

        kb.setKeyboard(keyboard);
        msg.setReplyMarkup(kb);

        return msg;
    }

    private int getOrCreateProductId(Map<String, Object> product) {

        try (Connection conn = DatabaseManager.getConnection()) {

            String name = product.get("name").toString().trim();

            // 🔥 1. шукаємо НЕ по точному співпадінню
            String findSql = "SELECT id FROM products WHERE LOWER(name) LIKE ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setString(1, "%" + name.toLowerCase() + "%");

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

            // 🔥 2. якщо нема — створюємо
            String insertSql = """
            INSERT INTO products(name, price, unit)
            VALUES (?, ?, ?)
        """;

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name);
                ps.setObject(2, product.getOrDefault("price", 0));
                ps.setString(3, product.getOrDefault("unit", "шт").toString());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private void showCartNice(Long userId, String chatId) {

        List<Map<String, Object>> cart = userCart.get(userId);

        if (cart == null || cart.isEmpty()) {
            sendText(chatId, "🛒 Кошик порожній.");
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("🧺 Ваш кошик, друже!\n");
        sb.append("Митрофан перевірив полички і зібрав усе, що ви додали 👇\n\n");

        double total = 0;
        int i = 1;

        for (Map<String, Object> item : cart) {

            String name = item.get("name").toString();

            double price = Double.parseDouble(
                    item.get("price").toString().replace(",", ".")
            );

            int qty = Integer.parseInt(item.get("quantity").toString());

            double sum = price * qty;
            total += sum;

            sb.append("🔸 ").append(i++).append(". ").append(name).append("\n");
            sb.append("   🔢 Кількість: ").append(qty).append("\n");
            sb.append("   💵 Ціна за 1: ").append(String.format("%.2f", price)).append(" грн\n");
            sb.append("   💰 Сума: ").append(String.format("%.2f", sum)).append(" грн\n\n");
        }

        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("💳 Всього до оплати: ")
                .append(String.format("%.2f", total))
                .append(" грн\n");

        sb.append("🧱 Митрофан каже: «Готовий оформити замовлення!» 🤖");

        sendText(chatId, sb.toString());
    }

    private void askDelivery(String chatId) {

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🏬 Самовивіз");
        row1.add("📦 Доставка по місту");

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BACK_BUTTON);

        markup.setKeyboard(List.of(row1, row2));

        sendMessage(chatId, "🚚 Оберіть спосіб доставки:", markup);
    }

    private void handleAddToFilter(Long userId, String chatId, String filter, String subfilter) {

        CatalogSearcher searcher = new CatalogSearcher();

        int added = 0;
        int errors = 0;

        List<Map<String, Object>> products = adminSelectedProducts.get(userId);

        if (products != null && !products.isEmpty()) {

            for (Map<String, Object> p : products) {
                try {
                    if (searcher.addProductToFilter(p, filter, subfilter)) {
                        added++;
                    } else {
                        errors++;
                    }
                } catch (Exception e) {
                    errors++;
                }
            }

            sendText(chatId,
                    "📊 Результат:\n\n" +
                            "➕ Додано: " + added + "\n" +
                            (errors > 0 ? "❌ Помилки: " + errors + "\n" : "") +
                            "📂 " + filter +
                            (subfilter != null ? " → " + subfilter : "")
            );

        } else {

            Map<String, Object> product = adminSelectedProduct.get(userId);

            if (product == null) {
                sendText(chatId, "❌ Товар не знайдено.");
                userStates.remove(userId);
                return;
            }

            boolean success = searcher.addProductToFilter(product, filter, subfilter);

            String name = safeToString(product.get("name"), "Без назви");

            sendText(chatId,
                    success
                            ? "✅ Додано:\n📦 " + name +
                            "\n📂 " + filter +
                            (subfilter != null ? " → " + subfilter : "")
                            : "❌ Помилка"
            );
        }

        userStates.put(userId, "editing");
        selectedFilter.remove(userId);

        sendMessageSafely(createEditMenu(chatId, userId));
    }
}
