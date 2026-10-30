package bisq.api;

import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.api.access.transport.TlsContextService;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class HttpServerBootstrapServiceTest {

    @Test
    void tlsContextFailureDoesNotReachRunning(@TempDir Path tempDir) throws Exception {
        // A password below the min. length makes the creation of the TLS context fail
        ApiConfig apiConfig = tlsApiConfig("short", findFreePort());
        SessionAuthenticationService sessionAuthenticationService =
                new SessionAuthenticationService(mock(PairingService.class), new SessionService(60));
        HttpServerBootstrapService service = new HttpServerBootstrapService(apiConfig,
                new ResourceConfig(),
                Optional.empty(),
                sessionAuthenticationService,
                new TlsContextService(apiConfig, tempDir));

        // By design the startup does not fail, the app observes the state and error message instead
        assertThat(service.initialize().join()).isTrue();

        assertThat(service.getState().get()).isEqualTo(HttpServerBootstrapService.State.STARTING);
        assertThat(service.getErrorMessage().get()).contains("Could not create TLS context");
    }

    private static ApiConfig tlsApiConfig(String tlsKeyStorePassword, int bindPort) {
        return new ApiConfig(ApiAccessTransportType.CLEAR,
                3600,
                false,
                true,
                false,
                false,
                "127.0.0.1",
                bindPort,
                80,
                false,
                false,
                true,
                tlsKeyStorePassword,
                List.of("127.0.0.1"),
                false,
                60);
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Disabled
    @Test
    void testCallEndpointAndVerifyFingerprint() {
        assertDoesNotThrow(this::callEndpointAndVerifyPublicKeyFingerprint);
    }

    private void callEndpointAndVerifyPublicKeyFingerprint() throws Exception {
        // Replace with right fingerprint
        final String expectedFingerprint = "<expected-fingerprint>";

        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        try {
                            X509Certificate cert = chain[0];
                            // Compute SHA-256 over the public key, not the full certificate
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] digest = md.digest(cert.getPublicKey().getEncoded());
                            String fingerprint = HexFormat.of().formatHex(digest);

                            if (!fingerprint.equalsIgnoreCase(expectedFingerprint)) {
                                throw new CertificateException("Server public key fingerprint mismatch!");
                            }
                        } catch (Exception e) {
                            throw new CertificateException("Failed to validate server certificate", e);
                        }
                    }
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, trustManagers, null);

        URI uri = URI.create("https://localhost:8090/api/v1/market-price/quotes");
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();

        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        // disable hostname verification, we rely on certificate pinning
        connection.setHostnameVerifier((host, session) -> true);

        InputStream in = connection.getInputStream();
        byte[] data = in.readAllBytes();
        //convert bytes to string
        String response = new String(data);
    }
}