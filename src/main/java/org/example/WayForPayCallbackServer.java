package org.example;

import com.google.gson.Gson;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;

public class WayForPayCallbackServer {

    private final StoreBot bot;
    private final Gson gson = new Gson();

    public WayForPayCallbackServer(StoreBot bot) {
        this.bot = bot;
    }

    // ------------------ START SERVER ------------------
    public void start(int port) throws Exception {

        Server server = new Server(port);

        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(new ServletHolder(new CallbackServlet()), "/callback");

        server.setHandler(handler);
        server.start();

        System.out.println("🌍 WayForPay callback server started on port " + port);
    }

    // ------------------ SERVLET ------------------
    public class CallbackServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

            StringBuilder sb = new StringBuilder();
            String line;

            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            System.out.println("📩 WayForPay callback received:");
            System.out.println(sb);

            JSONObject json = new JSONObject(sb.toString());

            String orderCode = json.optString("orderReference", "");
            String status = json.optString("transactionStatus", "");
            double amount = json.optDouble("amount", 0);

            // обновляем
            updateOrderStatus(orderCode, status, amount);

            // WayForPay requires a specific "OK" response
            JSONObject ok = new JSONObject();
            ok.put("orderReference", orderCode);
            ok.put("status", "accept");

            resp.setContentType("application/json");
            resp.getWriter().write(ok.toString());
        }
    }

    // ------------------ DB UPDATE ------------------
    private void updateOrderStatus(String orderCode, String status, double amount) {

        String newStatus = switch (status) {
            case "Approved" -> "Оплачено";
            case "Pending" -> "Очікується";
            case "Expired", "Declined" -> "Відхилено";
            default -> "Невідомо";
        };

        try (Connection conn = DatabaseManager.getConnection()) {

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE orders SET status=?, paymentMethod='Картка', total=? WHERE orderCode=?"
            );
            stmt.setString(1, newStatus);
            stmt.setDouble(2, amount);
            stmt.setString(3, orderCode);
            stmt.executeUpdate();

            // find userId
            PreparedStatement getUser = conn.prepareStatement(
                    "SELECT userId FROM orders WHERE orderCode=?"
            );
            getUser.setString(1, orderCode);
            ResultSet rs = getUser.executeQuery();

            Long userId = null;
            if (rs.next()) userId = rs.getLong("userId");

            // notify
            notifyAdmin(orderCode, newStatus, amount);
            if (userId != null) notifyUser(userId, newStatus);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------ NOTIFY ADMIN ------------------
    private void notifyAdmin(String orderCode, String status, double amount) {

        String adminId = "620298889";

        String msg = "💳 *Оплата замовлення*\n" +
                "🔢 Код: *" + orderCode + "*\n" +
                "📌 Статус: *" + status + "*\n" +
                "💰 Сума: *" + amount + " грн*";

        bot.sendText(adminId, msg); // тут НЕ потрібно try/catch
    }

    // ------------------ NOTIFY USER ------------------
    private void notifyUser(Long userId, String status) {

        String msg = switch (status) {
            case "Оплачено" -> "✅ Ваше замовлення *успішно оплачено*! Дякуємо ❤️";
            case "Очікується" -> "⏳ Оплата обробляється...";
            case "Відхилено" -> "❌ Оплату не вдалося провести.";
            default -> "ℹ️ Статус оплати оновлено: " + status;
        };

        bot.sendText(userId.toString(), msg); // і тут так само
    }
}