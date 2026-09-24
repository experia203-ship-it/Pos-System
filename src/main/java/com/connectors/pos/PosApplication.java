package com.connectors.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
@EnableCaching

public class PosApplication {

	//public static void main(String[] args) {
		//SpringApplication.run(PosApplication.class, args);
	//}



	public static void main(String[] args) {
		// Disable headless mode so Java can interact with the Windows Desktop
		SpringApplicationBuilder builder = new SpringApplicationBuilder(PosApplication.class);
		builder.headless(false).run(args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void openBrowser() {
		if (!GraphicsEnvironment.isHeadless()) {
			try {
				String url = "http://localhost:8080/auth/login";
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(new URI(url));
				} else {
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

