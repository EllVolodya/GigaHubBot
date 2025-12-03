package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;

public class PaymentCallbackServer {

    // 🔥 Admin ID (твій)
    private static final long ADMIN_ID = 620298889L;

    private final StoreBot bot;

    public PaymentCallbackServer(StoreBot bot) {
        this.bot = bot;
    }

    public void start() {
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/wfp-callback", this::handleCallback);
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("🚀 WayForPay callback server started on port " + port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String jsonBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📩 Callback from WayForPay: " + jsonBody);

        JsonObject obj = JsonParser.parseString(jsonBody).getAsJsonObject();

        String orderReference = obj.get("orderReference").getAsString();
        String transactionStatus = obj.get("transactionStatus").getAsString();
        double receivedAmount = obj.get("amount").getAsDouble();

        boolean success = "Approved".equalsIgnoreCase(transactionStatus)
                || "PAYMENT_APPROVED".equalsIgnoreCase(transactionStatus);

        // 🔹 Перевірка суми + оновлення статусу
        updateOrderAndNotify(orderReference, receivedAmount, success);

        byte[] resp = "OK".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void updateOrderAndNotify(String orderCode, double receivedAmount, boolean success) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // Спочатку знайдемо замовлення
            PreparedStatement sel = conn.prepareStatement(
                    "SELECT userId, total FROM orders WHERE orderCode=?"
            );
            sel.setString(1, orderCode);
            ResultSet rs = sel.executeQuery();

            if (!rs.next()) {
                System.out.println("⚠ Замовлення з кодом " + orderCode + " не знайдено.");
                return;
            }

            long userId = rs.getLong("userId");
            double dbTotal = rs.getDouble("total");

            // 🔒 Перевірка суми
            boolean amountOk = Math.abs(dbTotal - receivedAmount) < 0.01;

            String finalStatus;
            if (success && amountOk) {
                finalStatus = "Оплачено";
            } else if (success) {
                finalStatus = "Підозріла сума";
            } else {
                finalStatus = "Відхилено";
            }

            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE orders SET paymentstatus=? WHERE orderCode=?"
            );
            upd.setString(1, finalStatus);
            upd.setString(2, orderCode);
            upd.executeUpdate();

            // 🔔 Повідомляємо користувача
            String userMsg;
            if ("Оплачено".equals(finalStatus)) {
                userMsg = "✅ Оплату за замовлення *" + orderCode + "* успішно підтверджено!";
            } else if ("Підозріла сума".equals(finalStatus)) {
                userMsg = "⚠ Оплату отримано, але сума не співпадає. Консультант зв'яжеться з вами.";
            } else {
                userMsg = "❌ Оплата за замовлення *" + orderCode + "* не пройшла.";
            }

            bot.sendText(String.valueOf(userId), userMsg);

            // 🔔 Повідомляємо адміна
            String adminMsg = "💳 *Оплата замовлення*\n" +
                    "🔢 Код: *" + orderCode + "*\n" +
                    "👤 User ID: " + userId + "\n" +
                    "💰 Сума з бази: " + dbTotal + " грн\n" +
                    "💰 Сума з WayForPay: " + receivedAmount + " грн\n" +
                    "📌 Статус: *" + finalStatus + "*";

            bot.sendText(String.valueOf(ADMIN_ID), adminMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}