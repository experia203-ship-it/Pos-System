import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

/**
 * Seller-side tool for MyPOS activation keys. Run it through license-tool.bat (project root):
 *   license-tool.bat init                     -> once: creates your private key + the public key for the app
 *   license-tool.bat sign CODE [customer]     -> for each sale: turns the customer's device code into a key
 *
 * The PRIVATE key lives in  %USERPROFILE%\.mypos-license\private.key  (outside the project, never commit it,
 * and back it up: without it you cannot create keys any more).
 */
public class LicenseTool {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        switch (args[0]) {
            case "init" -> init();
            case "sign" -> sign(args);
            default -> usage();
        }
    }

    private static void usage() {
        System.out.println("MyPOS license tool");
        System.out.println("  license-tool.bat init                     (run once)");
        System.out.println("  license-tool.bat sign CODE [customer]     (CODE = the device code the customer sent)");
    }

    private static Path keyDir() {
        return Paths.get(System.getProperty("user.home"), ".mypos-license");
    }

    private static void init() throws Exception {
        Path privateFile = keyDir().resolve("private.key");
        Path publicFile = Paths.get("src", "main", "resources", "license-public.key");
        if (Files.exists(privateFile)) {
            System.out.println("Already set up. Your private key is here: " + privateFile);
            System.out.println("(Not touching it - creating a new one would invalidate every key you already sold.)");
            return;
        }
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Files.createDirectories(keyDir());
        Files.writeString(privateFile, Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded()) + "\n");
        Files.createDirectories(publicFile.getParent());
        Files.writeString(publicFile, Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPublic().getEncoded()) + "\n");
        System.out.println("Done.");
        System.out.println("  Private key (SECRET, back it up, never commit it): " + privateFile);
        System.out.println("  Public key (goes inside the app):                  " + publicFile.toAbsolutePath());
        System.out.println("Now run build-installer.bat to build an installer that uses this key.");
    }

    private static void sign(String[] args) throws Exception {
        if (args.length < 2) {
            usage();
            return;
        }
        String code = normalize(args[1]);
        if (code.length() != 16) {
            System.out.println("That code does not look right. It should have 16 letters/numbers, like K7QP-3XMD-9WTA-B2HF.");
            return;
        }
        Path privateFile = keyDir().resolve("private.key");
        if (!Files.exists(privateFile)) {
            System.out.println("No private key yet. Run  license-tool.bat init  first.");
            return;
        }
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(Files.readString(privateFile).trim())));
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(("MYPOS1|" + code).getBytes(StandardCharsets.UTF_8));
        String key = group(toBase32(signer.sign()), 5);

        String customer = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        String line = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ",\""
                + customer.replace("\"", "'") + "\"," + group(code, 4) + "\n";
        Files.writeString(keyDir().resolve("issued.csv"), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        System.out.println();
        System.out.println("Activation key" + (customer.isEmpty() ? "" : " for " + customer) + " (" + group(code, 4) + "):");
        System.out.println();
        System.out.println(key);
        System.out.println();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(key), null);
            System.out.println("It is also copied to your clipboard. Send ONLY the key to the customer, nothing else in the same message.");
        } catch (Throwable ignored) {
            System.out.println("Send ONLY the key to the customer, nothing else in the same message.");
        }
        System.out.println("(Logged in " + keyDir().resolve("issued.csv") + ")");
    }

    // ---- same Base32 / formatting rules as the app's LicenseCrypto ----

    private static String normalize(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (ALPHABET.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toBase32(byte[] data) {
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

    private static String group(String text, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += size) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(text, i, Math.min(text.length(), i + size));
        }
        return sb.toString();
    }
}
