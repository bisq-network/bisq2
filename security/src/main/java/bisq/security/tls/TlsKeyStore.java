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

package bisq.security.tls;

import bisq.common.encoding.Hex;
import bisq.common.file.FileMutatorUtils;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TlsKeyStore {
    private static final String STORE_TYPE = "PKCS12";
    private static final String KEY_ALIAS = "tls";
    private static final String PROTOCOL = "TLSv1.3";

    public static KeyStore createAndPersistKeyStore(KeyPair keyPair,
                                                    X509Certificate certificate,
                                                    Path keyStorePath,
                                                    char[] password) throws TlsException {
        Path tmpFilePath = null;
        try {
            Path parent = keyStorePath.getParent();
            checkNotNull(parent, "keyStorePath.getParent() must not be null");
            FileMutatorUtils.createDirectories(parent);

            String fileName = keyStorePath.getFileName().toString();
            tmpFilePath = Files.createTempFile(parent, fileName, ".tmp");

            KeyStore keyStore = KeyStore.getInstance(STORE_TYPE);
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), password, new X509Certificate[]{certificate});

            try (OutputStream os = FileMutatorUtils.newOutputStream(tmpFilePath)) {
                keyStore.store(os, password);
            }

            FileMutatorUtils.renameFile(tmpFilePath, keyStorePath);
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            log.error("writeTlsIdentity failed", e);
            throw new TlsException("Failed to persist key store", e);
        } finally {
            if (tmpFilePath != null) {
                try {
                    FileMutatorUtils.releaseTempFile(tmpFilePath);
                } catch (IOException e) {
                    log.error("Releasing temp file failed for {}", tmpFilePath, e);
                }
            }
        }
    }

    public static Optional<KeyStore> readKeyStore(Path keyStorePath,
                                                  char[] password,
                                                  List<String> tlsKeyStoreSan) throws TlsException {
        try {
            if (!Files.exists(keyStorePath)) {
                return Optional.empty();
            }

            KeyStore keyStore = KeyStore.getInstance(STORE_TYPE);
            try (InputStream is = Files.newInputStream(keyStorePath, StandardOpenOption.READ)) {
                keyStore.load(is, password);

                Set<String> persisted = SanUtils.readSanDnsNames(keyStore, KEY_ALIAS).stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toCollection(TreeSet::new));

                Set<String> current = tlsKeyStoreSan.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toCollection(TreeSet::new));

                if (!persisted.equals(current)) {
                    log.warn(
                            "Persisted key store had different SAN list.\nPersisted SAN list: {}\nNew SAN list: {}",
                            persisted,
                            current
                    );
                    return Optional.empty();
                }
            }
            return Optional.of(keyStore);
        } catch (IOException e) {
            log.warn("Could not read key store.", e);
            return Optional.empty();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new TlsException("Failed to compute certificate fingerprint", e);
        }
    }

    public static String getCertificateFingerprint(KeyStore keyStore) throws TlsException {
        try {
            var certificate = loadCertificate(keyStore);
            byte[] digest = DigestUtil.sha256(certificate.getEncoded());
            return Hex.encode(digest);
        } catch (KeyStoreException | CertificateEncodingException e) {
            throw new TlsException("Failed to compute certificate fingerprint", e);
        }
    }

    private static PrivateKey loadPrivateKey(KeyStore keyStore, char[] password) throws TlsException {
        try {
            return (PrivateKey) keyStore.getKey(KEY_ALIAS, password);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new TlsException("Failed to compute certificate fingerprint", e);
        }
    }

    private static X509Certificate loadCertificate(KeyStore keyStore) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(KEY_ALIAS);
    }

}
