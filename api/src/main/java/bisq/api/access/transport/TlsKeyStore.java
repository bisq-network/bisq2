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

package bisq.api.access.transport;

import bisq.common.file.FileMutatorUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Optional;

public class TlsKeyStore {

    public static void writeTlsIdentity(KeyPair keyPair, X509Certificate certificate,
                             Path keyStorePath, char[] password) throws Exception {
        Path parent = keyStorePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmpFile = Files.createTempFile(parent != null ? parent : Path.of("."), keyStorePath.getFileName().toString(), ".tmp");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("tls", keyPair.getPrivate(), password, new X509Certificate[]{certificate});

        try (OutputStream os = FileMutatorUtils.newOutputStream(tmpFile)) {
            ks.store(os, password);
        }

        // TODO: error handling
        FileMutatorUtils.renameFile(tmpFile, keyStorePath);
    }

    public static Optional<KeyStore> readTlsIdentity(Path keyStorePath, char[] password) throws Exception {
        if (!Files.exists(keyStorePath)) {
            return Optional.empty();
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(keyStorePath, StandardOpenOption.READ)) {
            ks.load(is, password);
        }
        return Optional.of(ks);
    }

    private static PrivateKey loadPrivateKey(KeyStore keyStore, char[] password) throws Exception {
        return (PrivateKey) keyStore.getKey("tls", password);
    }

    private static X509Certificate loadCertificate(KeyStore keyStore) throws Exception {
        return (X509Certificate) keyStore.getCertificate("tls");
    }

    public static SSLContext createSslContext(KeyStore keyStore, char[] password) throws Exception {
        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        var sslContext = javax.net.ssl.SSLContext.getInstance("TLSv1.3");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    public static String getPublicKeyFingerprint(KeyStore keyStore) throws Exception {
        var certificate = loadCertificate(keyStore);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certificate.getPublicKey().getEncoded());
        return HexFormat.of().formatHex(digest);
    }
}
