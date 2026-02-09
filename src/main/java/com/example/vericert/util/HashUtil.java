package com.example.vericert.util;

public class HashUtil {
    public static String sha256Hex(byte[] data){
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(data));
        } catch(Exception e){ throw new RuntimeException(e); }
    }
}