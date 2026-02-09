package com.example.vericert.service;

import java.security.SecureRandom;
import java.util.Base64;

public class Passwords {
    private static final SecureRandom rnd = new SecureRandom();

    public static String secureRandomBase64(int bytes) {
        byte[] b = new byte[bytes];
        rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
