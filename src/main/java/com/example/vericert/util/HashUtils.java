package com.example.vericert.util;

import java.security.MessageDigest;

// HashUtils.java
public final class HashUtils {
    private HashUtils(){}

    public static String base64UrlSha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return com.nimbusds.jose.util.Base64URL.encode(md.digest(data)).toString();
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r=0; for (int i=0;i<a.length();i++) r |= a.charAt(i) ^ b.charAt(i);
        return r==0;
    }
}