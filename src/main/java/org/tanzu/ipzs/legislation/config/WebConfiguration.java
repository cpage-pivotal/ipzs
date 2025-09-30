package org.tanzu.ipzs.legislation.config;

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class WebConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(WebConfiguration.class);
    private final int sessionTimeout = 1440;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")  // Use allowedOriginPatterns instead of allowedOrigins when allowCredentials is true
                        .allowedMethods(
                                HttpMethod.GET.name(),
                                HttpMethod.POST.name(),
                                HttpMethod.PUT.name(),
                                HttpMethod.DELETE.name(),
                                HttpMethod.OPTIONS.name())
                        .allowedHeaders("*")
                        .allowCredentials(true)  // Allow credentials (cookies)
                        .maxAge(3600);
            }
        };
    }

    @Bean
    public ServletContextInitializer sessionInitializer() {
        return servletContext -> {
            logger.info("Configuring session management with timeout: {} seconds", sessionTimeout);

            // Set session timeout
            servletContext.setSessionTimeout(sessionTimeout);

            // Set tracking modes - use cookies
            servletContext.setSessionTrackingModes(java.util.Set.of(
                    SessionTrackingMode.COOKIE
            ));

            // Configure session cookie
            SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
            sessionCookieConfig.setName("JSESSIONID");
            sessionCookieConfig.setHttpOnly(true);
            sessionCookieConfig.setSecure(false); // Set to true in production with HTTPS
            sessionCookieConfig.setPath("/");
            sessionCookieConfig.setMaxAge(sessionTimeout * 60); // Convert minutes to seconds
            // sessionCookieConfig.setSameSite("Lax"); // Recommended for cross-origin scenarios
        };
    }

    @Bean
    public SSLContext sslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
        return sslContext;
    }
}