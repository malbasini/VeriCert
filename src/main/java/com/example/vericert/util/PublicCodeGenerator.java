package com.example.vericert.util;

import java.security.SecureRandom;

public final class PublicCodeGenerator {
    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ALPH = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray(); // no 0 O I 1

    private PublicCodeGenerator(){}

    public static String newInvoiceCode() {
        return "INV-" + randomBase32(6);
    }

    private static String randomBase32(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            out[i] = ALPH[RND.nextInt(ALPH.length)];
        }
        return new String(out);
    }
}
