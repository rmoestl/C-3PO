package org.c_3po.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumCalculator {
    public static byte[] computeSha1Hash(Path file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (var bis = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[1024];

            int numBytesRead = 0;
            while (numBytesRead != -1) {
                numBytesRead = bis.read(buffer);
                if (numBytesRead > 0) {

                    // It's crucial to not just pass `buffer` because then `.update`
                    // would add all of `buffer` even though `.read` might has read less bytes
                    // than buffer's length.
                    md.update(buffer, 0, numBytesRead);
                }
            }

            return md.digest();
        }
    }

    /**
     * Source: https://www.baeldung.com/java-byte-arrays-hex-strings
     */
    public static String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

    /**
     * Source: https://www.baeldung.com/java-byte-arrays-hex-strings
     */
    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
}
