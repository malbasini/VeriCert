package com.example.vericert.util;

import java.io.ByteArrayOutputStream;

public class QrUtil {
    public static byte[] png(String content, int size) {
        try {
            var hints = new java.util.HashMap<com.google.zxing.EncodeHintType,Object>();
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 1);
            var matrix = new com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);
            var img = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(matrix);
            var out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch(Exception e){ throw new RuntimeException(e); }
    }
}