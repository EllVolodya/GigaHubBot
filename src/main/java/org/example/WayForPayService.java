package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WayForPayService {

    private static final String API_URL = "https://api.wayforpay.com/api";

    private static final String MERCHANT_ACCOUNT = "gigahub_com_ua";
    private static final String MERCHANT_SECRET = "169f041d72364f273bea894dbe93bd7ecae6b4b6";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private final Gson gson = new Gson();

    private String generateSignatureMD5(String... params) {
        try {
            String data = String.join(";", params);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            String result = hex.toString();

            System.out.println(">>> SIGNATURE STRING:");
            System.out.println(data);
            System.out.println(">>> MD5 SIGNATURE = " + result + " (len=" + result.length() + ")");

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createInvoice(String orderCode, int amount) {

        long orderDate = System.currentTimeMillis() / 1000;
        String productName = "Order_" + orderCode;

        // 🔥 ОБОВ’ЯЗКОВО додаємо SECRET в кінець
        String signature = generateSignatureMD5(
                MERCHANT_ACCOUNT,
                MERCHANT_DOMAIN,
                orderCode,
                String.valueOf(orderDate),
                String.valueOf(amount),
                "UAH",
                productName,
                "1",
                String.valueOf(amount),
                MERCHANT_SECRET    // ← дуже важливо!
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

            System.out.println("FINAL JSON:");
            System.out.println(jsonBody);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            InputStream is = conn.getResponseCode() == 200
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String json = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            System.out.println("WayForPay response: " + json);

            JsonObject resp = gson.fromJson(json, JsonObject.class);

            if (resp.has("invoiceUrl")) {
                return resp.get("invoiceUrl").getAsString();
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
