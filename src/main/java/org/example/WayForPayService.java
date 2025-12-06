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

        // ✔ Українські символи збережені
        String productName = "Замовлення " + orderCode;

        // ✔ правильний виклик sign()
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

        JsonObject body = new JsonObject();
        body.addProperty("transactionType", "CREATE_INVOICE");
        body.addProperty("apiVersion", 1);
        body.addProperty("merchantAccount", MERCHANT_ACCOUNT);
        body.addProperty("merchantDomainName", MERCHANT_DOMAIN);
        body.addProperty("merchantSignature", signature);
        body.addProperty("orderReference", orderCode);
        body.addProperty("orderDate", orderDate);
        body.addProperty("amount", amount);
        body.addProperty("currency", "UAH");

        body.add("productName", gson.toJsonTree(new String[]{productName}));
        body.add("productPrice", gson.toJsonTree(new int[]{amount}));
        body.add("productCount", gson.toJsonTree(new int[]{1}));

        body.addProperty("serviceUrl", "https://gigahubbot2-production.up.railway.app/pay/callback");
        body.addProperty("returnUrl", "https://gigahub.com.ua");

        try {
            URL url = new URL(API_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setDoOutput(true);

            String jsonBody = gson.toJson(body);

            System.out.println("👉 WayForPay request body (GSON):");
            System.out.println(jsonBody);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            InputStream is;

            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String json = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            System.out.println("🔍 WayForPay RAW response: " + json);

            JsonObject jsonResponse = gson.fromJson(json, JsonObject.class);

            if (jsonResponse.has("invoiceUrl")) {
                return jsonResponse.get("invoiceUrl").getAsString();
            } else {
                System.out.println("❌ WayForPay Error JSON:");
                System.out.println(json);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
