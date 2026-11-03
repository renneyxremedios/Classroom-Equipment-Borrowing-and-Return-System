package edu.cs26.equipment;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Passwords {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private Passwords() { }

    static String newSalt() {
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }

    static String hash(char[] password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (Exception ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    static boolean matches(char[] password, String salt, String expectedHash) {
        byte[] computed = Base64.getDecoder().decode(hash(password, salt));
        byte[] expected = Base64.getDecoder().decode(expectedHash);
        boolean matches = MessageDigest.isEqual(computed, expected);
        Arrays.fill(computed, (byte) 0);
        return matches;
    }
}
