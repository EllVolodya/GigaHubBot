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

    /**
     * Правильний підпис для CREATE_INVOICE
     * Порядок параметрів строго з документації.
     */
    private String createSignature(String merchantAccount,
                                   String merchantDomain,
                                   String orderReference,
                                   long orderDate,
                                   int amount,
                                   String currency,
                                   String productName,
                                   int productCount,
                                   int productPrice) {

        try {
            String data = merchantAccount + ";" +
                    merchantDomain + ";" +
                    orderReference + ";" +
                    orderDate + ";" +
                    amount + ";" +
                    currency + ";" +
                    productName + ";" +
                    productCount + ";" +
                    productPrice + ";" +
                    MERCHANT_SECRET;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createInvoice(String orderCode, int amount) {

        long orderDate = System.currentTimeMillis() / 1000;
        String productName = "Замовлення " + orderCode;

        String signature = createSignature(
                MERCHANT_ACCOUNT,
                MERCHANT_DOMAIN,
                orderCode,
                orderDate,
                amount,
                "UAH",
                productName,
                1,
                amount
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

            JsonObject response = gson.fromJson(json, JsonObject.class);

            if (response.has("invoiceUrl")) {
                return response.get("invoiceUrl").getAsString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
