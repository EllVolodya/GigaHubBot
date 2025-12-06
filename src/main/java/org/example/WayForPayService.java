package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class WayForPayService {

    private static final String API_URL = "https://api.wayforpay.com/api";

    private static final String MERCHANT_ACCOUNT = "freelance_user_688b1c35af55c";
    private static final String MERCHANT_SECRET = "52d8e5b871968d3c50a0023ed9b632b9b802986a";
    private static final String MERCHANT_DOMAIN = "wayforpay.com"; // може бути іналаштуєш сам

    private final Gson gson = new Gson();

    private String sign(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createInvoice(String orderCode, int amount) {

        long orderDate = System.currentTimeMillis() / 1000;
        String productName = "Замовлення " + orderCode;

        String signatureData = String.join(";",
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

        String signature = sign(signatureData);

        JsonObject body = new JsonObject();
        body.addProperty("transactionType", "CREATE_INVOICE");
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

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            InputStream response = conn.getInputStream();
            String json = new BufferedReader(new InputStreamReader(response))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            JsonObject jsonResponse = gson.fromJson(json, JsonObject.class);

            if (jsonResponse.has("invoiceUrl")) {
                return jsonResponse.get("invoiceUrl").getAsString();
            } else {
                System.out.println("❌ WayForPay Response Error:");
                System.out.println(json);  // ← тут буде справжня помилка
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}