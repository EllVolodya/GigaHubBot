package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WayForPayService {

    private static final String API_URL = "https://api.wayforpay.com/api";

    private static final String MERCHANT_ACCOUNT = "freelance_user_688b1c35af55c";
    private static final String MERCHANT_SECRET  = "52d8e5b871968d3c50a0023ed9b632b9b802986a";
    private static final String MERCHANT_DOMAIN  = "gigahub.com.ua";

    private String sign(String... params) {
        try {
            String data = String.join(";", params) + MERCHANT_SECRET;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));

            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createInvoice(String orderCode, int amount) {
        long orderDate = System.currentTimeMillis() / 1000;

        String productName = "Замовлення " + orderCode;

        // 🟦 Правильні поля для підпису
        String signature = sign(
                MERCHANT_ACCOUNT,
                MERCHANT_DOMAIN,
                orderCode,
                String.valueOf(orderDate),
                String.valueOf(amount),
                "UAH",
                productName,
                "1",
                String.valueOf(amount)
        );

        // 🟩 Чистий JSON руками — BEZ GSON
        String jsonBody =
                "{"
                        + "\"transactionType\":\"CREATE_INVOICE\","
                        + "\"apiVersion\":1,"
                        + "\"merchantAccount\":\"" + MERCHANT_ACCOUNT + "\","
                        + "\"merchantDomainName\":\"" + MERCHANT_DOMAIN + "\","
                        + "\"merchantSignature\":\"" + signature + "\","
                        + "\"orderReference\":\"" + orderCode + "\","
                        + "\"orderDate\":" + orderDate + ","
                        + "\"amount\":" + amount + ","
                        + "\"currency\":\"UAH\","
                        + "\"productName\":[\"" + productName + "\"],"
                        + "\"productPrice\":[" + amount + "],"
                        + "\"productCount\":[1],"
                        + "\"serviceUrl\":\"https://gigahubbot2-production.up.railway.app/pay/callback\","
                        + "\"returnUrl\":\"https://gigahub.com.ua\""
                        + "}";

        System.out.println("👉 FINAL JSON:");
        System.out.println(jsonBody);

        try {
            URL url = new URL(API_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            InputStream is = conn.getResponseCode() == 200
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String resp = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            System.out.println("🔍 WayForPay response: " + resp);

            if (resp.contains("invoiceUrl")) {
                int start = resp.indexOf("invoiceUrl") + 13;
                int end = resp.indexOf("\"", start);
                return resp.substring(start, end);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
