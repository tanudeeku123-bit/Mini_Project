import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility for cryptographically secure salted password hashing using PBKDF2.
 */
public class PasswordUtils {
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte random cryptographic salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes a password with a given salt using PBKDF2WithHmacSHA256.
     */
    public static String hashPassword(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with PBKDF2", e);
        }
    }

    /**
     * Verifies a candidate password against a stored salt and hash.
     */
    public static boolean verifyPassword(char[] password, String saltHex, String expectedHashHex) {
        if (password == null || saltHex == null || expectedHashHex == null) {
            return false;
        }
        saltHex = saltHex.trim();
        expectedHashHex = expectedHashHex.trim();
        if (saltHex.isEmpty() || expectedHashHex.isEmpty() || saltHex.length() % 2 != 0) {
            return false;
        }
        try {
            byte[] salt = hexToBytes(saltHex);
            if (salt.length == 0) return false;
            String computedHashHex = hashPassword(password, salt);
            return slowEquals(computedHashHex, expectedHashHex);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        int diff = a.length() ^ b.length();
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int d1 = Character.digit(hex.charAt(i), 16);
            int d2 = Character.digit(hex.charAt(i + 1), 16);
            if (d1 == -1 || d2 == -1) {
                return new byte[0];
            }
            data[i / 2] = (byte) ((d1 << 4) + d2);
        }
        return data;
    }
}

