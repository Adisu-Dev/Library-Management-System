package db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    /**
     * Hashes a plain text password using the SHA-256 algorithm.
     * @param password The plain text password to hash.
     * @return The hashed password as a hexadecimal string.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                // 🚀 ዘመናዊ እና አጭር የ Hex መለወጫ
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            // ስህተት ካጋጠመ የት ጋር እንደሆነ በግልፅ እንዲያሳውቅ
            throw new RuntimeException("Error encrypting password: Algorithm not found", ex);
        }
    }
}