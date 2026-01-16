package bisq.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class HttpServerBootstrapServiceTest {

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