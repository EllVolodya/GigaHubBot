package org.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class OldSignatureUtil {

    public static String generateSignature(String... params) {
        try {
            String data = String.join(";", params);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(data.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(md5);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}