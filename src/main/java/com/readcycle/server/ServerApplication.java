package com.readcycle.server;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		// Use environment variables if running on Railway or any production environment
		String googleClientId = System.getenv("GOOGLE_CLIENT_ID");
		String googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

		// If not found in env, fall back to .env file (likely in local dev)
		if (googleClientId == null || googleClientSecret == null) {
			try {
				Dotenv dotenv = Dotenv.configure()
						.ignoreIfMissing()
						.load();

				if (googleClientId == null)
					googleClientId = dotenv.get("GOOGLE_CLIENT_ID");

				if (googleClientSecret == null)
					googleClientSecret = dotenv.get("GOOGLE_CLIENT_SECRET");

			} catch (Exception e) {
				System.err.println("Failed to load .env file: " + e.getMessage());
			}
		}

		// Set values as system properties so Spring can pick them up if needed
		if (googleClientId != null)
			System.setProperty("GOOGLE_CLIENT_ID", googleClientId);
		if (googleClientSecret != null)
			System.setProperty("GOOGLE_CLIENT_SECRET", googleClientSecret);

		SpringApplication.run(ServerApplication.class, args);
	}
}
