/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.http_api.push_notification;

import bisq.common.data.Pair;
import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.IESParameterSpec;

import javax.crypto.Cipher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with the Bisq Relay service.
 * Sends encrypted push notifications to iOS devices via APNs.
 * Uses a dedicated I/O executor to avoid blocking the common ForkJoinPool.
 */
@Slf4j
public class BisqRelayClient {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String relayBaseUrl;
    private final ObjectMapper objectMapper;
    private final Optional<NetworkService> networkService;
    private final ExecutorService ioExecutor;

    public BisqRelayClient(String relayBaseUrl, Optional<NetworkService> networkService) {
        this.relayBaseUrl = relayBaseUrl;
        this.objectMapper = new ObjectMapper();
        this.networkService = networkService;
        // Create a dedicated cached thread pool for I/O operations
        // Cached pool is appropriate for I/O-bound tasks with variable concurrency
        this.ioExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "bisq-relay-io");
            thread.setDaemon(true); // Allow JVM to exit even if threads are running
            return thread;
        });
    }

    /**
     * Send an encrypted push notification to a device via the Bisq Relay.
     *
     * @param deviceToken    The APNs device token
     * @param publicKeyBase64 The device's public key (Base64 encoded)
     * @param payload        The notification payload to encrypt
     * @param isUrgent       Whether the notification is urgent
     * @return CompletableFuture with NotificationResult indicating success and whether to unregister
     */
    public CompletableFuture<NotificationResult> sendNotification(String deviceToken, String publicKeyBase64,
                                                                   Map<String, Object> payload, boolean isUrgent) {
        // Use dedicated I/O executor instead of common ForkJoinPool for blocking HTTP operations
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Encrypt the payload with the device's public key
                String encryptedPayload = encryptPayload(payload, publicKeyBase64);

                // Prepare the request to Bisq Relay
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("encrypted", encryptedPayload);
                requestBody.put("isUrgent", isUrgent);

                String requestJson = objectMapper.writeValueAsString(requestBody);

                // Validate device token to prevent path injection attacks
                // APNs device tokens are hex strings (64 chars for production, 128 for some formats)
                // Only allow alphanumeric characters to prevent path traversal
                if (!deviceToken.matches("^[a-zA-Z0-9]+$")) {
                    throw new IllegalArgumentException("Invalid device token format: contains non-alphanumeric characters");
                }

                // Send POST request to Bisq Relay via Tor
                // Device token is already URL-safe (hex string), no encoding needed
                String endpoint = String.format("/v1/apns/device/%s", deviceToken);

                // Use Tor HTTP client if onion address, otherwise use direct connection
                if (relayBaseUrl.contains(".onion") && networkService.isPresent()) {
                    return sendViaTor(endpoint, requestJson, deviceToken);
                } else {
                    log.warn("Bisq Relay URL is not an onion address or NetworkService not available. " +
                            "Push notifications should use Tor for privacy. URL: {}", relayBaseUrl);
                    return sendDirect(endpoint, requestJson, deviceToken);
                }
            } catch (Exception e) {
                log.error("Error sending push notification to Bisq Relay", e);
                return NotificationResult.failed(deviceToken);
            }
        }, ioExecutor); // Use dedicated I/O executor for blocking HTTP operations
    }

    private NotificationResult sendViaTor(String endpoint, String requestJson, String deviceToken) {
        BaseHttpClient httpClient = null;
        try {
            NetworkService ns = networkService.orElseThrow(() ->
                new IllegalStateException("NetworkService is required for Tor connections"));

            // Get the Tor HTTP client from NetworkService
            // For POST requests, baseUrl should include the full path
            // The TorHttpClient will extract the path from the URI
            String fullUrl = relayBaseUrl + endpoint;

            log.info("Sending push notification via Tor to: {} (device: {}...)",
                    fullUrl, deviceToken.substring(0, Math.min(10, deviceToken.length())));

            httpClient = ns.getHttpClient(
                fullUrl,
                "Bisq-Push-Notification/1.0",
                TransportType.TOR
            );

            // Use the POST method with the JSON body
            // The TorHttpClient will handle the SOCKS proxy connection automatically
            String response = httpClient.post(
                requestJson,  // For POST, param is the request body
                Optional.of(new Pair<>("Content-Type", "application/json"))
            );

            log.info("Successfully sent push notification via Tor to device token: {}... Response: {}",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())),
                    response.substring(0, Math.min(100, response.length())));

            return NotificationResult.success(deviceToken);
        } catch (Exception e) {
            log.error("Error sending push notification via Tor to device: {}...",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())), e);

            // Check if this is a BadDeviceToken or isUnregistered error
            return parseErrorResponse(e, deviceToken);
        } finally {
            // Always shutdown the HTTP client to release resources
            if (httpClient != null) {
                try {
                    httpClient.shutdown();
                } catch (Exception e) {
                    log.warn("Error shutting down HTTP client for device: {}...",
                            deviceToken.substring(0, Math.min(10, deviceToken.length())), e);
                }
            }
        }
    }

    private NotificationResult sendDirect(String endpoint, String requestJson, String deviceToken) {
        // Fallback to direct connection (for testing with sandbox relay)
        log.warn("Using direct HTTP connection for push notification - this should only be used for testing");

        try {
            String fullUrl = relayBaseUrl + endpoint;

            log.info("Sending push notification via direct HTTP to: {} (device: {}...)",
                    fullUrl, deviceToken.substring(0, Math.min(10, deviceToken.length())));

            // Create HTTP client with connect timeout to prevent indefinite blocking
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            // Build the POST request with request timeout to prevent indefinite blocking
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(30))  // Overall request timeout
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // Send the request synchronously
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Successfully sent push notification via direct HTTP to device token: {}... " +
                    "Status: {}, Response: {}",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())),
                    response.statusCode(),
                    response.body().substring(0, Math.min(100, response.body().length())));

            // Consider 2xx status codes as success
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return NotificationResult.success(deviceToken);
            } else {
                // Parse error response to check if device should be unregistered
                return parseErrorResponseBody(response.body(), deviceToken);
            }
        } catch (Exception e) {
            log.error("Error sending push notification via direct HTTP to device: {}...",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())), e);
            return parseErrorResponse(e, deviceToken);
        }
    }

    /**
     * Parse error response from exception to determine if device should be unregistered.
     */
    private NotificationResult parseErrorResponse(Exception e, String deviceToken) {
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            return parseErrorResponseBody(errorMessage, deviceToken);
        }
        return NotificationResult.failed(deviceToken);
    }

    /**
     * Parse error response body to check for BadDeviceToken or isUnregistered.
     * Relay returns JSON like: {"wasAccepted":false,"errorCode":"BadDeviceToken","isUnregistered":true}
     */
    private NotificationResult parseErrorResponseBody(String responseBody, String deviceToken) {
        if (responseBody == null) {
            return NotificationResult.failed(deviceToken);
        }

        // Check for BadDeviceToken or isUnregistered indicators
        boolean isBadDeviceToken = responseBody.contains("\"errorCode\":\"BadDeviceToken\"");
        boolean isUnregistered = responseBody.contains("\"isUnregistered\":true");

        if (isBadDeviceToken || isUnregistered) {
            log.warn("Device token is invalid or unregistered, will auto-unregister: {}... Response: {}",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())),
                    responseBody.substring(0, Math.min(200, responseBody.length())));
            return NotificationResult.failedShouldUnregister(deviceToken);
        }

        return NotificationResult.failed(deviceToken);
    }

    /**
     * Encrypt the payload using the device's public key (ECIES for EC keys).
     *
     * @param payload         The payload to encrypt
     * @param publicKeyBase64 The Base64 encoded public key
     * @return Base64 encoded encrypted payload
     */
    private String encryptPayload(Map<String, Object> payload, String publicKeyBase64) throws Exception {
        // Convert payload to JSON
        String payloadJson = objectMapper.writeValueAsString(payload);

        // Decode the public key
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Encrypt with ECIES (Elliptic Curve Integrated Encryption Scheme)
        // Using AES-128-CBC with HMAC-SHA1 for MAC (default BouncyCastle ECIES parameters)
        byte[] derivation = new byte[0];
        byte[] encoding = new byte[0];
        // IESParameterSpec(derivation, encoding, macKeySize)
        IESParameterSpec iesSpec = new IESParameterSpec(derivation, encoding, 128);

        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, iesSpec);
        byte[] encryptedBytes = cipher.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Return Base64 encoded encrypted data
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Shutdown the I/O executor and release resources.
     * Should be called when the BisqRelayClient is no longer needed.
     */
    public void shutdown() {
        log.info("Shutting down BisqRelayClient I/O executor");
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("I/O executor did not terminate within 10 seconds, forcing shutdown");
                ioExecutor.shutdownNow();
                if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("I/O executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for I/O executor to terminate", e);
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
