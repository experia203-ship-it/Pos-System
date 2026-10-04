package com.connectors.pos.license;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseCryptoTest {

    private static String signedKeyFor(KeyPair pair, String deviceCode) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        signer.update(LicenseCrypto.message(deviceCode));
        return LicenseCrypto.group(LicenseCrypto.toBase32(signer.sign()), 5);
    }

    @Test
    void base32RoundTripsAnyBytes() {
        byte[] data = new byte[64];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 7 + 3);
        }
        assertArrayEquals(data, LicenseCrypto.fromBase32(LicenseCrypto.toBase32(data)));
    }

    @Test
    void deviceCodeIsStableAndSixteenCharacters() {
        String code = LicenseCrypto.deviceCodeFor("machine-guid-1");
        assertEquals(code, LicenseCrypto.deviceCodeFor("machine-guid-1"));
        assertNotEquals(code, LicenseCrypto.deviceCodeFor("machine-guid-2"));
        assertEquals(16, LicenseCrypto.normalize(code).length());
    }

    @Test
    void validKeyVerifiesEvenWhenPastedWithSpacesAndLowerCase() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String deviceCode = LicenseCrypto.deviceCodeFor("machine-guid-1");
        String key = signedKeyFor(pair, deviceCode);

        assertTrue(LicenseCrypto.verify(pair.getPublic(), deviceCode, key));
        assertTrue(LicenseCrypto.verify(pair.getPublic(), deviceCode, "  " + key.toLowerCase().replace("-", " \n") + " "));
    }

    @Test
    void keyDoesNotWorkOnAnotherPcOrWhenChanged() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        KeyPair attacker = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String deviceCode = LicenseCrypto.deviceCodeFor("machine-guid-1");
        String key = signedKeyFor(pair, deviceCode);

        assertFalse(LicenseCrypto.verify(pair.getPublic(), LicenseCrypto.deviceCodeFor("machine-guid-2"), key));
        assertFalse(LicenseCrypto.verify(pair.getPublic(), deviceCode, key.substring(0, key.length() - 6)));
        assertFalse(LicenseCrypto.verify(pair.getPublic(), deviceCode, signedKeyFor(attacker, deviceCode)));
        assertFalse(LicenseCrypto.verify(pair.getPublic(), deviceCode, ""));
    }
}
