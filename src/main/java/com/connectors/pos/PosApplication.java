package com.connectors.pos;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.LoggerFactory;
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class PosApplication {

	private static final int PORT = 8080;   // keep in sync with server.port
	private static final String URL = "http://localhost:" + PORT + "/auth/login";
	private static FileLock instanceLock;   // static so it isn't garbage collected
	private static ConfigurableApplicationContext ctx;
	private static final Logger log = LoggerFactory.getLogger(PosApplication.class);
	@Value("${app.desktop.enable:true}")
	private boolean isDesktopEnabled;
	public static void main(String[] args) throws IOException {
		String appData = System.getenv("APPDATA");
		Path base = appData != null ? Paths.get(appData) : Paths.get(System.getProperty("user.home"));
		Path dataDir = base.resolve("MyPOS");
		Files.createDirectories(dataDir);
		System.setProperty("app.data.dir", dataDir.toString().replace("\\", "/"));

		// Single instance: if already running, just open the browser and exit
		FileChannel ch = FileChannel.open(dataDir.resolve("app.lock"),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		boolean lockHeldByThisJvm = false;
		try {
			instanceLock = ch.tryLock();
		} catch (OverlappingFileLockException e) {
			// Spring DevTools (IntelliJ) re-runs main() inside the same JVM after startup;
			// this JVM already owns the lock, so it is not a second instance.
			lockHeldByThisJvm = true;
		}
		if (instanceLock == null && !lockHeldByThisJvm) {
			openBrowser();
			return;
		}

		ctx = new SpringApplicationBuilder(PosApplication.class).headless(false).run(args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		if(!isDesktopEnabled) return;
		try {
			openBrowser();
			addTrayIcon();
		} catch (Throwable e) {
			log.error("Error opening browser or adding tray icon:", e);
		}
	}

	private static void openBrowser() {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(URL));
			} else {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + URL);
			}
		} catch (Exception e) {
			log.error("Error opening browser:", e);
		}
	}

	private void addTrayIcon() {
		if (!SystemTray.isSupported()) return;
		try {
			Image img = Toolkit.getDefaultToolkit()
					.getImage(PosApplication.class.getResource("/static/icon.png"));
			PopupMenu menu = new PopupMenu();
			MenuItem open = new MenuItem("Open POS");
			open.addActionListener(e -> openBrowser());
			MenuItem exit = new MenuItem("Exit");
			exit.addActionListener(e -> System.exit(SpringApplication.exit(ctx)));
			menu.add(open);
			menu.add(exit);
			TrayIcon icon = new TrayIcon(img, "My POS", menu);
			icon.setImageAutoSize(true);
			icon.addActionListener(e -> openBrowser());   // double-click
			SystemTray.getSystemTray().add(icon);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}