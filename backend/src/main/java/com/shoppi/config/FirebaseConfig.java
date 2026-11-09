package com.shoppi.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración de Firebase Admin SDK para el backend de Shoppi.
 *
 * <p>Firebase es auxiliar: el backend usa el Admin SDK para emitir custom-tokens
 * (upload a Storage) y enviar push con FCM. No reemplaza la autenticación JWT
 * propia ni la fuente de verdad (PostgreSQL).</p>
 *
 * <p>La credencial se inyecta con {@code FIREBASE_SERVICE_ACCOUNT_JSON}. Puede ser
 * una ruta absoluta a un archivo JSON, una ruta {@code classpath:...} o el JSON en
 * crudo (útil en IaaS/CI).</p>
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public FirebaseApp firebaseApp(
            @Value("${shoppi.firebase.service-account-json:${FIREBASE_SERVICE_ACCOUNT_JSON:}}")
            String raw) throws IOException {

        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException(
                    "FIREBASE_SERVICE_ACCOUNT_JSON es obligatoria. "
                            + "Descarga la clave privada desde Firebase Console "
                            + "(Configuración del proyecto -> Cuentas de servicio) "
                            + "y define la variable o shoppi.firebase.service-account-json.");
        }

        String json = leerJson(raw.trim());
        JsonNode root = objectMapper.readTree(json);
        String projectId = root.path("project_id").asText(null);

        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException(
                    "El JSON de la cuenta de servicio no contiene project_id.");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        if (!FirebaseApp.getApps().isEmpty()) {
            FirebaseApp existing = FirebaseApp.getInstance();
            log.info("FirebaseApp ya inicializado para el proyecto {}",
                    existing.getOptions().getProjectId());
            return existing;
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("FirebaseApp inicializado para el proyecto {}",
                app.getOptions().getProjectId());
        return app;
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    private String leerJson(String raw) throws IOException {
        if (raw.startsWith("{")) {
            return raw;
        }

        if (raw.startsWith("classpath:")) {
            String resource = raw.substring("classpath:".length());
            try (InputStream stream = new ClassPathResource(resource).getInputStream()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        Path path = Path.of(raw);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "No se encontró el archivo de cuenta de servicio: " + path);
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
