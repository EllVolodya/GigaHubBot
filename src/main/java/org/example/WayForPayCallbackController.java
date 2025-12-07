package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class WayForPayCallbackController extends HttpServlet {

    private static final String SECRET_KEY = "8723037eda8285f659a8a9e3d92404fc23c4f2e1";
    private static final Gson gson = new Gson();

    // Ось тут — твій клас бота (заміни на свій справжній)
    private static final StoreBot bot = StoreBot.getInstance(); // ← якщо у тебе StoreBot

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        String requestBody = body.toString();
        System.out.println("WayForPay callback: " + requestBody);

        JsonObject json;
        try {
            json = gson.fromJson(requestBody, JsonObject.class);
        } catch (Exception e) {
            resp.getWriter().write("invalid json");
            resp.setStatus(400);
            return;
        }

        if (!isValidSignature(json)) {
            System.out.println("Invalid signature!");
            resp.getWriter().write("error");
            return;
        }

        String orderReference = json.get("orderReference").getAsString();
        String status = json.get("transactionStatus").getAsString();

        if ("Approved".equals(status)) {
            notifySuccess(orderReference);
        }

        resp.getWriter().write("ok");
    }

    private boolean isValidSignature(JsonObject json) {
        if (!json.has("signature")) return false;
        String receivedSig = json.get("signature").getAsString();
        json.remove("signature");

        String[] fields = {"merchantAccount", "orderReference", "amount", "currency",
                "authCode", "cardPan", "transactionStatus", "reasonCode"};
        StringBuilder data = new StringBuilder();
        for (String field : fields) {
            JsonElement el = json.get(field);
            if (el != null && !el.isJsonNull()) {
                data.append(el.getAsString());
            }
            data.append(";");
        }
        return hmacMD5(data.toString(), SECRET_KEY).equals(receivedSig);
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

    private void notifySuccess(String orderCode) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Перевірка, чи вже відправляли
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT payment_notified FROM orders WHERE orderCode = ?"
            );
            ps.setString(1, orderCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("payment_notified") == 1) {
                return;
            }

            // Оновлюємо статус
            ps = conn.prepareStatement(
                    "UPDATE orders SET paymentstatus = 'Оплачено', payment_notified = 1, paidAt = NOW() WHERE orderCode = ?"
            );
            ps.setString(1, orderCode);
            ps.executeUpdate();

            // Отримуємо userId
            ps = conn.prepareStatement("SELECT userId FROM orders WHERE orderCode = ?");
            ps.setString(1, orderCode);
            rs = ps.executeQuery();
            if (rs.next()) {
                long userId = rs.getLong("userId");

                // Ось ці два рядки — заміни на свій спосіб відправки повідомлення
                bot.sendText(String.valueOf(userId),
                        "Ваше замовлення *" + orderCode + "* успішно оплачено!\n" +
                                "Дякуємо за покупку! Скоро зв’яжемося з вами");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}