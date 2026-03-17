package io.learnsharegrow.freezertracker.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

final class PasswordHasher {
  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

  private PasswordHasher() {}

  static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  static String bcrypt(String value) {
    return ENCODER.encode(value);
  }

  static boolean matchesBcrypt(String value, String hash) {
    return ENCODER.matches(value, hash);
  }

  static boolean matches(String password, String pepper, String storedHash, String salt) {
    if (storedHash == null || storedHash.isBlank()) {
      return false;
    }
    if (storedHash.startsWith("$2")) {
      return matchesBcrypt(password + pepper, storedHash);
    }
    String legacySalt = salt == null ? "" : salt;
    String expected = sha256(legacySalt + password + pepper);
    return expected.equals(storedHash);
  }
}
