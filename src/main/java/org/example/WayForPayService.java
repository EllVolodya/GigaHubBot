package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class WayForPayService {

    private static final String API_URL = "https://api.wayforpay.com/api";

    // Якщо хочеш — перенеси в Environment Variables на Railway
    private static final String MERCHANT_ACCOUNT = "gigahub_com_ua1";
    private static final String MERCHANT_SECRET = "8723037eda8285f659a8a9e3d92404fc23c4f2e1";
    private static final String MERCHANT_DOMAIN = "gigahub.com.ua";

    private static final Gson gson = new Gson();

    public String createInvoice(String orderCode, int amount) {
        try {
            long orderDate = System.currentTimeMillis() / 1000;
            String productName = "Order_" + orderCode;

            // Правильні масиви
            String[] productNames = {productName};
            int[] productCounts = {1};
            String[] productPrices = {String.valueOf(amount)};

            // Використовуємо HMAC-SHA256 (64 символи) — після активації в кабінеті
            // Якщо ще не активували — закоментуй рядок нижче і розкоментуй MD5-версію внизу
            String merchantSignature = generateMd5Signature(
                    MERCHANT_ACCOUNT,
                    MERCHANT_DOMAIN,
                    orderCode,
                    orderDate,
                    String.valueOf(amount),
                    "UAH",
                    productNames,
                    productCounts,
                    productPrices
            );

            // ← Тимчасовий варіант на MD5 (32 символи), якщо підтримка ще не увімкнула SHA256
            // String merchantSignature = generateMd5Signature(
            //         MERCHANT_ACCOUNT, MERCHANT_DOMAIN, orderCode, orderDate,
            //         String.valueOf(amount), "UAH", productNames, productCounts, productPrices);

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

            // Масиви як JSON-масиви
            JsonArray nameArr = new JsonArray();
            for (String s : productNames) nameArr.add(s);
            JsonArray countArr = new JsonArray();
            for (int i : productCounts) countArr.add(i);
            JsonArray priceArr = new JsonArray();
            for (String s : productPrices) priceArr.add(s);

            request.add("productName", nameArr);
            request.add("productCount", countArr);
            request.add("productPrice", priceArr);

            request.addProperty("returnUrl", "https://gigahub.com.ua");
            request.addProperty("serviceUrl", "https://gigahubbot2-production.up.railway.app/pay/callback");

            System.out.println("FINAL REQUEST JSON: " + gson.toJson(request));

            String response = sendPost(request.toString());
            System.out.println("WayForPay response: " + response);

            JsonObject resp = gson.fromJson(response, JsonObject.class);

            if (resp.has("invoiceUrl")) {
                return resp.get("invoiceUrl").getAsString();
            } else {
                System.err.println("WayForPay error: " + response);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // === НОВИЙ ПІДПИС: HMAC-SHA256 (64 символи) ===
    private String generateHmacSha256Signature(
            String merchantAccount, String merchantDomainName,
            String orderReference, long orderDate, String amount, String currency,
            String[] productName, int[] productCount, String[] productPrice) {

        String joinedNames = String.join(";;", productName);
        String joinedCounts = Arrays.stream(productCount)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(";;"));
        String joinedPrices = String.join(";;", productPrice);

        String rawString = String.join(";",
                merchantAccount,
                merchantDomainName,
                orderReference,
                String.valueOf(orderDate),
                amount,
                currency,
                joinedNames,
                joinedCounts,
                joinedPrices
        );

        // Екранування
        String escaped = rawString.replace("\\", "\\\\").replace(";", "\\;");

        return hmacSha256(escaped, MERCHANT_SECRET);
    }

    // === СТАРИЙ ПІДПИС: MD5 (32 символи) — працює одразу ===
    private String generateMd5Signature(
            String merchantAccount, String merchantDomainName,
            String orderReference, long orderDate, String amount, String currency,
            String[] productName, int[] productCount, String[] productPrice) {

        String joinedNames = String.join(";;", productName);
        String joinedCounts = Arrays.stream(productCount)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(";;"));
        String joinedPrices = String.join(";;", productPrice);

        String rawString = String.join(";",
                merchantAccount, merchantDomainName, orderReference,
                String.valueOf(orderDate), amount, currency,
                joinedNames, joinedCounts, joinedPrices
        );

        String escaped = rawString.replace("\\", "\\\\").replace(";", "\\;");
        return md5(escaped + ";" + MERCHANT_SECRET);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sendPost(String json) throws IOException {
        URL url = new URL(API_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream() : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}