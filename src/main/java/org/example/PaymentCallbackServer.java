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

    private static final long ADMIN_ID = 620298889L;
    private final StoreBot bot;

    public PaymentCallbackServer(StoreBot bot) {
        this.bot = bot;
    }

    public void start() {
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // 🔥 Правильний endpoint для WayForPay
            server.createContext("/pay/callback", this::handleCallback);

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("🚀 WayForPay callback server started on port: " + port);
            System.out.println("📡 Listening on /pay/callback");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📩 Callback data: " + body);

        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        String orderCode = json.get("orderReference").getAsString();
        double receivedAmount = json.get("amount").getAsDouble();

        String status = json.get("transactionStatus").getAsString();
        boolean success = status.equalsIgnoreCase("Approved")
                || status.equalsIgnoreCase("PAYMENT_APPROVED");

        updateOrderAndNotify(orderCode, receivedAmount, success);

        byte[] ok = "OK".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, ok.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(ok);
        }
    }

    private void updateOrderAndNotify(String orderCode, double receivedAmount, boolean success) {
        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement sel = conn.prepareStatement(
                    "SELECT userId, total FROM orders WHERE orderCode=?"
            );
            sel.setString(1, orderCode);
            ResultSet rs = sel.executeQuery();

            if (!rs.next()) {
                System.out.println("⚠ Order not found: " + orderCode);
                return;
            }

            long userId = rs.getLong("userId");
            double total = rs.getDouble("total");

            boolean amountCorrect = Math.abs(total - receivedAmount) < 0.01;

            String finalStatus = success
                    ? (amountCorrect ? "Оплачено" : "Підозріла сума")
                    : "Відхилено";

            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE orders SET paymentstatus=? WHERE orderCode=?"
            );
            upd.setString(1, finalStatus);
            upd.setString(2, orderCode);
            upd.executeUpdate();

            // 🔔 User notify
            String userMsg = switch (finalStatus) {
                case "Оплачено" -> "✅ Ваше замовлення *" + orderCode + "* успішно оплачено!";
                case "Підозріла сума" -> "⚠ Оплата отримана, але сума не співпадає!";
                default -> "❌ Оплату не вдалося провести!";
            };
            bot.sendText(String.valueOf(userId), userMsg);

            // 🔔 Admin notify
            String adminMsg =
                    "💳 *Оплата замовлення*\n" +
                            "🔢 Код: *" + orderCode + "*\n" +
                            "👤 User: " + userId + "\n" +
                            "💰 Очікувалося: " + total + " грн\n" +
                            "💰 Отримано: " + receivedAmount + " грн\n" +
                            "📌 Статус: *" + finalStatus + "*";

            bot.sendText(String.valueOf(ADMIN_ID), adminMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
