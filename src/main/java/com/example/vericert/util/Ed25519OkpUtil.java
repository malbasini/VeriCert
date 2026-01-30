package com.example.vericert.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class Ed25519OkpUtil {
    private Ed25519OkpUtil() {}

    /** Legge un PEM e restituisce i bytes DER decodificati (senza header/footer). */
    public static byte[] readPemDer(Path pemPath, String pemType) throws Exception {
        String pem = Files.readString(pemPath);
        String base64 = pem
                .replace("-----BEGIN " + pemType + "-----", "")
                .replace("-----END " + pemType + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Estrae i 32 byte della CHIAVE PUBBLICA Ed25519 dal DER X.509 SubjectPublicKeyInfo.
     * Struttura: BIT STRING (0x03) di lunghezza 0x22 (34), con 1 byte "unused bits" = 0x00,
     * seguiti da 32 byte = chiave pubblica raw.
     */
    public static byte[] extractEd25519PublicXFromSpki(byte[] spkiDer) {
        // cerca il tag BIT STRING (0x03)
        for (int i = 0; i < spkiDer.length - 2; i++) {
            if (spkiDer[i] == 0x03) {
                int len = spkiDer[i + 1] & 0xFF;
                int unusedBits = spkiDer[i + 2] & 0xFF; // deve essere 0
                int start = i + 3;
                if (unusedBits == 0 && len >= 33 && start + (len - 1) <= spkiDer.length) {
                    // I successivi 32 byte sono la chiave pubblica raw
                    byte[] x = new byte[32];
                    System.arraycopy(spkiDer, start, x, 0, 32);
                    return x;
                }
            }
        }
        throw new IllegalArgumentException("SPKI non riconosciuto come Ed25519");
    }

    /** Estrae i 32 byte privati (seed) da una private key PKCS#8 Ed25519 usando JCA. */
    public static byte[] ed25519PrivateDFromPKCS8(byte[] pkcs8Der) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        PrivateKey pk = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Der));
        if (!(pk instanceof EdECPrivateKey ed)) {
            throw new IllegalArgumentException("Non è una Ed25519 private key");
        }
        byte[] d = ed.getBytes().get(); // 32 byte
        if (d.length != 32) {
            throw new IllegalStateException("Ed25519 private bytes invalidi (attesi 32)");
        }
        return d;
    }

    public static byte[] extractEd25519XFromSpki(byte[] spkiDer) {
        // Cerca il tag BIT STRING (0x03), che contiene: [1 byte unused bits (=0x00)] + 32 byte di chiave
        for (int i = 0; i < spkiDer.length - 2; i++) {
            if (spkiDer[i] == 0x03) {
                int len = spkiDer[i + 1] & 0xFF;
                int unused = spkiDer[i + 2] & 0xFF;
                int start = i + 3;
                if (unused == 0 && len >= 33 && start + 32 <= spkiDer.length) {
                    byte[] x = new byte[32];
                    System.arraycopy(spkiDer, start, x, 0, 32);
                    return x;
                }
            }
        }
        throw new IllegalArgumentException("SPKI non riconosciuto come Ed25519");
    }

}
