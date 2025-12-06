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
    private static final String MERCHANT_SECRET = "169f041d72364f273bea894dbe93bd7ecae6b4b6";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private static final Gson gson = new Gson();

    public String createInvoice(String orderCode, int amount) {

        try {
            String productName = "Order_" + orderCode;     // ⚠️ Лише англійська!
            long orderDate = System.currentTimeMillis() / 1000;

            // ==== Формуємо строку для підпису (ВАЖЛИВО!) ====
            String signatureData =
                    MERCHANT_ACCOUNT + ";" +
                            MERCHANT_DOMAIN + ";" +
                            orderCode + ";" +
                            orderDate + ";" +
                            amount + ";" +
                            "UAH" + ";" +
                            productName + ";" +
                            1 + ";" +
                            amount + ";" +
                            MERCHANT_SECRET;

            String merchantSignature = md5(signatureData);

            // ==== Формуємо JSON запит ====
            JsonObject request = new JsonObject();
            request.addProperty("transactionType", "CREATE_INVOICE");
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

            System.out.println("FINAL REQUEST JSON:");
            System.out.println(gson.toJson(request));

            // ===== Надсилаємо запит =====
            String response = sendPost(request.toString());
            System.out.println("WayForPay response: " + response);

            // ===== Парсимо відповідь =====
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

        BufferedReader reader;
        if (conn.getResponseCode() == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
