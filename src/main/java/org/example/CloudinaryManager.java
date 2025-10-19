package org.example;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryManager {
    private static final Cloudinary cloudinary;

    static {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dohs4bvma",
                "api_key", "673113637171148",
                "api_secret", "kcKYT7Ju_1g_x-00oxzuhBGs6Ps",
                "secure", true
        ));
    }

    public static String uploadImage(File file, String folder) {
        try {
            System.out.println("[Cloudinary] Uploading file: " + file.getName() + " (" + file.length() + " bytes)");

            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "folder", folder,
                    "overwrite", true
            ));

            System.out.println("[Cloudinary] Upload result: " + uploadResult);

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[Cloudinary] Upload failed: " + e.getMessage());
            return null;
        }
    }

    public static void deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) return;

            int idx = imageUrl.indexOf("/upload/");
            if (idx == -1) return;

            String publicPart = imageUrl.substring(idx + 8);
            String[] parts = publicPart.split("/");

            if (parts[0].matches("v\\d+")) {
                publicPart = publicPart.substring(parts[0].length() + 1);
            }

            publicPart = publicPart.replaceAll("\\.[^.]+$", "");

            System.out.println("[Cloudinary] Deleting image public_id=" + publicPart);

            cloudinary.uploader().destroy(publicPart, ObjectUtils.emptyMap());

        } catch (Exception e) {
            System.err.println("[Cloudinary] Failed to delete image: " + e.getMessage());
        }
    }
}
