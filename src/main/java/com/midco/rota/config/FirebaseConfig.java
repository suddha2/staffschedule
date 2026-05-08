package com.midco.rota.config;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

	private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

	@Value("${firebase.service-account-path:classpath:firebase-service-account.json}")
	private String serviceAccountPath;

	private final ApplicationContext applicationContext;

	public FirebaseConfig(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	public void init() {
		if (!FirebaseApp.getApps().isEmpty()) {
			log.info("FirebaseApp already initialized — skipping");
			return;
		}
		Resource resource = applicationContext.getResource(serviceAccountPath);
		if (!resource.exists()) {
			log.warn("Firebase service account not found at {}. FCM and Google ID-token verification will fail at runtime "
					+ "until the file is provided. The application will still start.", serviceAccountPath);
			return;
		}
		try (InputStream in = resource.getInputStream()) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(in))
					.build();
			FirebaseApp.initializeApp(options);
			log.info("FirebaseApp initialized from {}", serviceAccountPath);
		} catch (IOException e) {
			log.error("Failed to initialize FirebaseApp from {}", serviceAccountPath, e);
		}
	}

	@Bean
	public boolean firebaseInitialized() {
		return !FirebaseApp.getApps().isEmpty();
	}
}
