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

    private static final String MERCHANT_ACCOUNT = "gigahub_com_ua1";
    private static final String MERCHANT_SECRET = "8723037eda8285f659a8a9e3d92404fc23c4f2e1";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private static final Gson gson = new Gson();

    public String createInvoice(String orderCode, int amount) {

        try {
            String productName = "Order_" + orderCode;
            long orderDate = System.currentTimeMillis() / 1000;

            // === 1. Signature base string (без секретного ключа) ===
            String base =
                    MERCHANT_ACCOUNT + ";" +
                            MERCHANT_DOMAIN + ";" +
                            orderCode + ";" +
                            orderDate + ";" +
                            amount + ";" +
                            "UAH" + ";" +
                            productName + ";" +
                            1 + ";" +
                            amount;

            // === 2. Додаємо secretKey ===
            String fullSignature = base + ";" + MERCHANT_SECRET;

            // === 3. MD5 ===
            String merchantSignature = md5(fullSignature);

            // === JSON Request ===
            JsonObject request = new JsonObject();
            request.addProperty("transactionType", "CREATE_INVOICE");
            request.addProperty("apiVersion", 1);
            request.addProperty("merchantAccount", MERCHANT_ACCOUNT);
            request.addProperty("merchantDomainName", MERCHANT_DOMAIN);
            request.addProperty("merchantSignature", merchantSignature);
            request.addProperty("orderReference", orderCode);
            request.addProperty("orderDate", orderDate);
            request.addProperty("amount", amount);
            request.addProperty("currency", "UAH");
            request.add("productName", gson.toJsonTree(new String[]{productName}));
            request.add("productPrice", gson.toJsonTree(new Integer[]{amount}));
            request.add("productCount", gson.toJsonTree(new Integer[]{1}));
            request.addProperty("returnUrl", "https://gigahub.com.ua");
            request.addProperty("serviceUrl",
                    "https://gigahubbot2-production.up.railway.app/pay/callback");

            System.out.println("\nFINAL REQUEST JSON:");
            System.out.println(gson.toJson(request));

            String response = sendPost(request.toString());
            System.out.println("WayForPay response: " + response);

            JsonObject resp = gson.fromJson(response, JsonObject.class);
            if (resp.has("invoiceUrl")) {
                return resp.get("invoiceUrl").getAsString();
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String sendPost(String json) throws Exception {

        URL url = new URL(API_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        conn.getResponseCode() == 200 ?
                                conn.getInputStream() :
                                conn.getErrorStream()
                )
        );

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
