package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WayForPayService {

    private static final String API_URL = "https://api.wayforpay.com/api";

    // ВАЖНО: Якщо розгортаєш на Railway — краще винести в Environment Variables!
    private static final String MERCHANT_ACCOUNT = "gigahub_com_ua1";
    private static final String MERCHANT_SECRET = "8723037eda8285f659a8a9e3d92404fc23c4f2e1";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private static final Gson gson = new Gson();

    public String createInvoice(String orderCode, int amount) {
        try {
            String productName = "Order_" + orderCode;
            long orderDate = System.currentTimeMillis() / 1000;

            // Правильні масиви (навіть якщо 1 елемент)
            String[] productNameArr = {productName};
            int[] productCountArr = {1};
            String[] productPriceArr = {String.valueOf(amount)};

            // Генеруємо правильний підпис
            String merchantSignature = generateSignature(
                    MERCHANT_ACCOUNT,
                    MERCHANT_DOMAIN,
                    orderCode,
                    orderDate,
                    String.valueOf(amount),
                    "UAH",
                    productNameArr,
                    productCountArr,
                    productPriceArr
            );

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
            request.add("productName", gson.toJsonTree(productNameArr));
            request.add("productCount", gson.toJsonTree(productCountArr));
            request.add("productPrice", gson.toJsonTree(productPriceArr));
            request.addProperty("returnUrl", "https://gigahub.com.ua");
            request.addProperty("serviceUrl", "https://gigahubbot2-production.up.railway.app/pay/callback");

            System.out.println("\nFINAL REQUEST JSON:");
            System.out.println(gson.toJson(request));

            String response = sendPost(request.toString());
            System.out.println("WayForPay response: " + response);

            JsonObject resp = gson.fromJson(response, JsonObject.class);
            if (resp.has("invoiceUrl")) {
                return resp.get("invoiceUrl").getAsString();
            } else {
                System.out.println("WayForPay error: " + response);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Правильна генерація підпису (HMAC-SHA256)
    private String generateSignature(
            String merchantAccount,
            String merchantDomainName,
            String orderReference,
            long orderDate,
            String amount,
            String currency,
            String[] productName,
            int[] productCount,
            String[] productPrice
    ) {
        String[] fields = {
                merchantAccount,
                merchantDomainName,
                orderReference,
                String.valueOf(orderDate),
                amount,
                currency,
                String.join(";;", productName),
                String.join(";;", Arrays.stream(productCount).mapToObj(String::valueOf).toArray(String[]::new)),
                String.join(";;", productPrice)
        };

        String concatenated = String.join(";", fields);

        // Екранування символів ; і \
        concatenated = concatenated.replace("\\", "\\\\").replace(";", "\\;");

        return hmacSha256(concatenated, MERCHANT_SECRET);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256", e);
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

        int responseCode = conn.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300 ?
                conn.getInputStream() : conn.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
}