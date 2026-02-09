package com.example.vericert.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmCrypto {

    private final SecretKey key;
    private final SecureRandom rnd = new SecureRandom();

    public AesGcmCrypto(byte[] keyBytes32) {
        this.key = new SecretKeySpec(keyBytes32, "AES");
        if (keyBytes32.length != 32) throw new IllegalArgumentException("Master key must be 32 bytes");
    }

    public String encryptToBase64(String plaintext) {
        try {
            byte[] iv = new byte[12];
            rnd.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(1 + iv.length + ct.length);
            bb.put((byte)1); // version
            bb.put(iv);
            bb.put(ct);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decryptFromBase64(String encB64) {
        try {
            byte[] all = Base64.getDecoder().decode(encB64);
            ByteBuffer bb = ByteBuffer.wrap(all);
            byte ver = bb.get(); // 1
            if (ver != 1) throw new IllegalArgumentException("Bad enc version");

            byte[] iv = new byte[12];
            bb.get(iv);
            byte[] ct = new byte[bb.remaining()];
            bb.get(ct);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
