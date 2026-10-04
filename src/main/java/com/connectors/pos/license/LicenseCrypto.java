package com.connectors.pos.license;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

/**
 * Pure-Java helpers for the offline license check (no Spring, so it is easy to test).
 * A license key is an Ed25519 signature of "MYPOS1|" + deviceCode, written in Base32.
 * The program only contains the PUBLIC key, so a customer cannot create keys for other PCs.
 * tools/license/LicenseTool.java (the seller's tool) contains the matching signing code.
 */
public final class LicenseCrypto {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String MESSAGE_PREFIX = "MYPOS1|";

    private LicenseCrypto() {}

    /** Upper-cases and drops everything that is not a Base32 character (spaces, dashes, new lines). */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (ALPHABET.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    public static byte[] fromBase32(String text) {
        String s = normalize(text);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : s.toCharArray()) {
            buffer = (buffer << 5) | ALPHABET.indexOf(c);
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    /** "ABCDEFGH" with size 4 becomes "ABCD-EFGH". */
    public static String group(String text, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += size) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(text, i, Math.min(text.length(), i + size));
        }
        return sb.toString();
    }

    /** The short code the customer sends to the seller, e.g. K7QP-3XMD-9WTA-B2HF. */
    public static String deviceCodeFor(String machineId) {
        byte[] hash = sha256(("MYPOS|" + machineId).getBytes(StandardCharsets.UTF_8));
        return group(toBase32(Arrays.copyOf(hash, 10)), 4);   // 80 bits = exactly 16 characters
    }

    public static byte[] message(String deviceCode) {
        return (MESSAGE_PREFIX + normalize(deviceCode)).getBytes(StandardCharsets.UTF_8);
    }

    public static boolean verify(PublicKey publicKey, String deviceCode, String licenseKey) {
        try {
            byte[] signature = fromBase32(licenseKey);
            if (signature.length != 64) {
                return false;
            }
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(message(deviceCode));
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public static PublicKey publicKeyFromBase64(String base64) throws GeneralSecurityException {
        byte[] der = Base64.getMimeDecoder().decode(base64.trim());
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
