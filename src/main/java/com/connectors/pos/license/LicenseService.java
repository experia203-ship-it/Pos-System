package com.connectors.pos.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Offline, per-PC license check. Only created when app.license.enabled=true
 * (the installer build turns it on), so development and the online demo are unaffected.
 */
@Service
@ConditionalOnProperty(name = "app.license.enabled", havingValue = "true")
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    private final Path licenseFile;
    private final String deviceCode;
    private final PublicKey publicKey;
    private volatile boolean licensed;

    public LicenseService(@Value("${app.data.dir:}") String dataDir) {
        Path dir = (dataDir == null || dataDir.isBlank())
                ? Paths.get(System.getProperty("user.home"), "MyPOS")
                : Paths.get(dataDir);
        this.licenseFile = dir.resolve("license.key");
        this.deviceCode = LicenseCrypto.deviceCodeFor(readMachineId());
        this.publicKey = loadPublicKey();
        this.licensed = checkStoredLicense();
        log.info("License check: device code {} - {}", deviceCode, licensed ? "ACTIVATED" : "not activated");
    }

    public boolean isLicensed() {
        return licensed;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    /** Returns true when the key is valid for this PC; the key is then remembered on disk. */
    public synchronized boolean activate(String key) {
        if (publicKey == null || !LicenseCrypto.verify(publicKey, deviceCode, key)) {
            return false;
        }
        licensed = true;
        try {
            Files.createDirectories(licenseFile.getParent());
            Files.writeString(licenseFile, LicenseCrypto.group(LicenseCrypto.normalize(key), 5),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not save the license file {}", licenseFile, e);
        }
        return true;
    }

    private boolean checkStoredLicense() {
        if (publicKey == null || !Files.isReadable(licenseFile)) {
            return false;
        }
        try {
            return LicenseCrypto.verify(publicKey, deviceCode, Files.readString(licenseFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return false;
        }
    }

    private PublicKey loadPublicKey() {
        ClassPathResource resource = new ClassPathResource("license-public.key");
        if (!resource.exists()) {
            log.error("license-public.key is missing from the build - nobody can activate. Run license-tool.bat init, then rebuild.");
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return LicenseCrypto.publicKeyFromBase64(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("license-public.key could not be read", e);
            return null;
        }
    }

    /** Stable id of this PC: the Windows MachineGuid (falls back to the host name). */
    private static String readMachineId() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                String systemRoot = System.getenv("SystemRoot");
                String reg = systemRoot != null ? systemRoot + "\\System32\\reg.exe" : "reg";
                Process p = new ProcessBuilder(reg, "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                        "/v", "MachineGuid").redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor(5, TimeUnit.SECONDS);
                for (String line : out.split("\\R")) {
                    if (line.contains("MachineGuid")) {
                        String[] parts = line.trim().split("\\s+");
                        String guid = parts[parts.length - 1];
                        if (guid.length() >= 8) {
                            return guid;
                        }
                    }
                }
            } else {
                Path machineId = Paths.get("/etc/machine-id");
                if (Files.isReadable(machineId)) {
                    return Files.readString(machineId).trim();
                }
            }
        } catch (Exception ignored) {
            // fall through to the fallback below
        }
        String host = "unknown-host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            // keep default
        }
        return host + "|" + System.getProperty("user.name", "");
    }
}
