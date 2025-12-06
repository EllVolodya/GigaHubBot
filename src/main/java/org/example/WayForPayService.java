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
    private static final String MERCHANT_SECRET = "52d8e5b871968d3c50a0023ed9b632b9b802986a";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    // ✔ правильний метод формування підпису
    private String sign(String merchantAccount,
                        String merchantDomainName,
                        String orderReference,
                        String orderDate,
                        String amount,
                        String currency,
                        String productName,
                        String productCount,
                        String productPrice) {
        try {
            String signatureSource =
                    merchantAccount + ";" +
                            merchantDomainName + ";" +
                            orderReference + ";" +
                            orderDate + ";" +
                            amount + ";" +
                            currency + ";" +
                            productName + ";" +
                            productCount + ";" +
                            productPrice + ";" +
                            MERCHANT_SECRET;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(signatureSource.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ✔ правильне створення інвойса
    public String createInvoice(String orderCode, int amount) {
        long orderDate = System.currentTimeMillis() / 1000;

        String productName = "Замовлення " + orderCode;

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

        // 🔥 Формуємо JSON ВРУЧНУ — БЕЗ GSON, БЕЗ ПРОБЛЕМ
        String jsonBody = "{"
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

        System.out.println("👉 FINAL BODY:");
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

            InputStream response = conn.getResponseCode() == 200 ?
                    conn.getInputStream() : conn.getErrorStream();

            String result = new BufferedReader(new InputStreamReader(response))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            System.out.println("🔍 WayForPay response: " + result);

            if (result.contains("invoiceUrl")) {
                int start = result.indexOf("invoiceUrl") + 13;
                int end = result.indexOf("\"", start);
                return result.substring(start, end);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
