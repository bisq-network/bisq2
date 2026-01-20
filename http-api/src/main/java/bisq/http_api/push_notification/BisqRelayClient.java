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

/**
 * HTTP client for communicating with the Bisq Relay service.
 * Sends encrypted push notifications to iOS devices via APNs.
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

    public BisqRelayClient(String relayBaseUrl, Optional<NetworkService> networkService) {
        this.relayBaseUrl = relayBaseUrl;
        this.objectMapper = new ObjectMapper();
        this.networkService = networkService;
    }

    /**
     * Send an encrypted push notification to a device via the Bisq Relay.
     *
     * @param deviceToken    The APNs device token
     * @param publicKeyBase64 The device's public key (Base64 encoded)
     * @param payload        The notification payload to encrypt
     * @param isUrgent       Whether the notification is urgent
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> sendNotification(String deviceToken, String publicKeyBase64,
                                                        Map<String, Object> payload, boolean isUrgent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Encrypt the payload with the device's public key
                String encryptedPayload = encryptPayload(payload, publicKeyBase64);

                // Prepare the request to Bisq Relay
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("encrypted", encryptedPayload);
                requestBody.put("isUrgent", isUrgent);

                String requestJson = objectMapper.writeValueAsString(requestBody);

                // Send POST request to Bisq Relay via Tor
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
                return false;
            }
        });
    }

    private boolean sendViaTor(String endpoint, String requestJson, String deviceToken) {
        try {
            NetworkService ns = networkService.orElseThrow(() ->
                new IllegalStateException("NetworkService is required for Tor connections"));

            // Get the Tor HTTP client from NetworkService
            // For POST requests, baseUrl should include the full path
            // The TorHttpClient will extract the path from the URI
            String fullUrl = relayBaseUrl + endpoint;

            log.info("Sending push notification via Tor to: {} (device: {}...)",
                    fullUrl, deviceToken.substring(0, Math.min(10, deviceToken.length())));

            BaseHttpClient httpClient = ns.getHttpClient(
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

            // Shutdown the HTTP client
            httpClient.shutdown();

            return true;
        } catch (Exception e) {
            log.error("Error sending push notification via Tor to device: {}...",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())), e);
            return false;
        }
    }

    private boolean sendDirect(String endpoint, String requestJson, String deviceToken) {
        // Fallback to direct connection (for testing with sandbox relay)
        log.warn("Using direct HTTP connection for push notification - this should only be used for testing");

        try {
            String fullUrl = relayBaseUrl + endpoint;

            log.info("Sending push notification via direct HTTP to: {} (device: {}...)",
                    fullUrl, deviceToken.substring(0, Math.min(10, deviceToken.length())));

            // Create HTTP client
            HttpClient httpClient = HttpClient.newHttpClient();

            // Build the POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
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
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.error("Error sending push notification via direct HTTP to device: {}...",
                    deviceToken.substring(0, Math.min(10, deviceToken.length())), e);
            return false;
        }
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
}
