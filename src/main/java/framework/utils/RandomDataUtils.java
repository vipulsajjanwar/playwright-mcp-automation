package framework.utils;

import java.util.Random;

public class RandomDataUtils {

    private static final Random random = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";

    private RandomDataUtils() {}

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String generateRandomEmail() {
        return "user_" + generateRandomString(6).toLowerCase() + "@example.com";
    }

    public static String generateRandomPhone() {
        StringBuilder sb = new StringBuilder("+1");
        for (int i = 0; i < 10; i++) {
            sb.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
        }
        return sb.toString();
    }
}
