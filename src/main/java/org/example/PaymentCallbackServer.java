package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;

public class PaymentCallbackServer {

    private static final long ADMIN_ID = 620298889L;
    private static final String SECRET_KEY = "8723037eda8285f659a8a9e3d92404fc23c4f2e1";
    private final StoreBot bot;

    public PaymentCallbackServer(StoreBot bot) {
        this.bot = bot;
    }

    public void start() {
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/pay/callback", this::handleCallback);

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("WayForPay callback server started on port: " + port);
            System.out.println("Listening on /pay/callback");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, "Method Not Allowed", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("WayForPay callback: " + body);

        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            sendResponse(exchange, "invalid json", 400);
            return;
        }

        // КРИТИЧНО: перевірка підпису
        if (!isValidSignature(json)) {
            System.out.println("INVALID SIGNATURE FROM WAYFORPAY!");
            sendResponse(exchange, "invalid signature", 400);
            return;
        }

        String orderCode = json.get("orderReference").getAsString();
        double receivedAmount = json.get("amount").getAsDouble();
        String status = json.get("transactionStatus").getAsString();

        boolean success = "Approved".equals(status);

        updateOrderAndNotify(orderCode, receivedAmount, success);

        sendResponse(exchange, "OK", 200);
    }

    private boolean isValidSignature(JsonObject json) {
        if (!json.has("signature")) return false;

        String receivedSignature = json.get("signature").getAsString();
        JsonObject copy = json.deepCopy();
        copy.remove("signature");

        String[] fields = {
                "merchantAccount", "orderReference", "amount", "currency",
                "authCode", "cardPan", "transactionStatus", "reasonCode"
        };

        StringBuilder data = new StringBuilder();
        for (String field : fields) {
            JsonElement el = copy.get(field);
            if (el != null && !el.isJsonNull()) {
                data.append(el.getAsString());
            }
            data.append(";");
        }

        String calculated = hmacMD5(data.toString(), SECRET_KEY);
        return calculated.equals(receivedSignature);
    }

    private String hmacMD5(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacMD5");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void updateOrderAndNotify(String orderCode, double receivedAmount, boolean success) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // Перевіряємо, чи вже оплачено — щоб НЕ було дублів
            PreparedStatement check = conn.prepareStatement(
                    "SELECT paymentstatus, userId, total FROM orders WHERE orderCode = ?"
            );
            check.setString(1, orderCode);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Order not found: " + orderCode);
                return;
            }

            String currentStatus = rs.getString("paymentstatus");
            if ("Оплачено".equals(currentStatus)) {
                System.out.println("Вже оплачено раніше: " + orderCode);
                return; // вже було — не спамимо
            }

            long userId = rs.getLong("userId");
            double total = rs.getDouble("total");
            boolean amountCorrect = Math.abs(total - receivedAmount) < 0.01;

            String finalStatus = success
                    ? (amountCorrect ? "Оплачено" : "Підозріла сума")
                    : "Відхилено";

            // Оновлюємо статус
            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE orders SET paymentstatus = ?, paidAt = NOW() WHERE orderCode = ?"
            );
            upd.setString(1, finalStatus);
            upd.setString(2, orderCode);
            upd.executeUpdate();

            // Повідомлення користувачу
            String userMsg = switch (finalStatus) {
                case "Оплачено" -> "Ваше замовлення *" + orderCode + "* успішно оплачено!\nДякуємо за покупку!";
                case "Підозріла сума" -> "Оплата отримана, але сума не співпадає. Зв’яжіться з нами.";
                default -> "Оплату не вдалося провести для замовлення *" + orderCode + "*";
            };
            bot.sendText(String.valueOf(userId), userMsg);

            // Повідомлення адміну
            String adminMsg = "Оплата замовлення\n" +
                    "Код: *" + orderCode + "*\n" +
                    "Користувач: " + userId + "\n" +
                    "Очікувалося: " + total + " грн\n" +
                    "Отримано: " + receivedAmount + " грн\n" +
                    "Статус: *" + finalStatus + "*";

            bot.sendText(String.valueOf(ADMIN_ID), adminMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}