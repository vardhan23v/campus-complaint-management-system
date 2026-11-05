package com.campuscomplaint.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Turns a plain password into the value stored in users.password_hash, and
 * checks a typed password against it.
 *
 * Algorithm: PBKDF2 with HMAC-SHA256, a random 16-byte salt per user and
 * 210,000 iterations (the OWASP recommendation for PBKDF2-SHA256). It ships
 * with every JDK, so no extra library is needed.
 *
 * Stored format (fits in VARCHAR(255)):
 *
 *   pbkdf2_sha256$210000$<salt, Base64>$<hash, Base64>
 *
 * Keeping the iteration count inside the value means it can be raised later
 * without breaking existing accounts.
 *
 * Run the main method to create hashes for sample-data.sql:
 *   java -cp target/classes com.campuscomplaint.util.PasswordUtil Admin@123
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // utility class: never instantiated
    }

    /** @return a new salted hash of the password in the stored format */
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    /**
     * Compares a typed password with the hash stored in the database.
     * The comparison runs in constant time, so its duration reveals nothing.
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.trim().split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // malformed number or Base64: treat as a wrong password
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Every Java 21 runtime provides PBKDF2WithHmacSHA256.
            throw new IllegalStateException("PBKDF2 is not available in this JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Prints a hash for every argument - used to prepare sample-data.sql. */
    public static void main(String[] args) {
        for (String password : args) {
            System.out.println(password + " -> " + hash(password));
        }
    }
}
