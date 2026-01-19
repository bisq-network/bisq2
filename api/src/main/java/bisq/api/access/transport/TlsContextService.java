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

import bisq.api.ApiConfig;
import bisq.security.tls.SanUtils;
import bisq.security.tls.SslContextFactory;
import bisq.security.tls.TLsIdentity;
import bisq.security.tls.TlsException;
import bisq.security.tls.TlsKeyStore;
import bisq.security.tls.TlsPasswordException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TlsContextService {
    public final static int MIN_PASSWORD_LENGTH = 8;

    private final ApiConfig apiConfig;
    private final Path keyStorePath;
    private final boolean isTlsRequired;

    @Getter
    private volatile Optional<TlsContext> tlsContext = Optional.empty();

    public TlsContextService(ApiConfig apiConfig, Path appDataDirPath) {
        this.apiConfig = apiConfig;
        keyStorePath = appDataDirPath.resolve(Paths.get("db", "private")).resolve("tls_keystore.p12");
        isTlsRequired = apiConfig.isTlsRequired();
    }

    public synchronized Optional<TlsContext> getOrCreateTlsContext() throws TlsException {
        if (isTlsRequired && tlsContext.isEmpty()) {
            tlsContext = Optional.of(createTlsContext());
        }
        return tlsContext;
    }

    private TlsContext createTlsContext() throws TlsException {
        try {
            String tlsKeyStorePassword = apiConfig.getTlsKeyStorePassword();
            checkNotNull(tlsKeyStorePassword, "TLS password must not be null");
            checkArgument(tlsKeyStorePassword.length() >= MIN_PASSWORD_LENGTH,
                    "TLS password does not have required min. length of " + MIN_PASSWORD_LENGTH);
            char[] password = tlsKeyStorePassword.toCharArray();

            List<String> tlsKeyStoreSan = apiConfig.getTlsKeyStoreSan();
            checkNotNull(tlsKeyStoreSan, "tlsKeyStoreSan must not be null");
            checkArgument(!tlsKeyStoreSan.isEmpty(), "tlsKeyStoreSan must have at least one entry.");

            KeyStore keyStore;
            Optional<KeyStore> optionalKeyStore;
            try {
                optionalKeyStore = TlsKeyStore.readKeyStore(keyStorePath, password, tlsKeyStoreSan);
                if (optionalKeyStore.isPresent()) {
                    keyStore = optionalKeyStore.get();
                    if (!SanUtils.isMatchingPersistedSan(keyStore, tlsKeyStoreSan)) {
                        log.info("Persisted key store had different SAN list. We create a new key store.");
                        keyStore = createNewKeyStore(tlsKeyStoreSan, password);
                    }
                } else {
                    keyStore = createNewKeyStore(tlsKeyStoreSan, password);
                }
            } catch (TlsPasswordException e) {
                log.info("Could not decrypt key store with given password. Probably password has been changed.", e);
                keyStore = createNewKeyStore(tlsKeyStoreSan, password);
            }
            SSLContext sslContext = SslContextFactory.fromKeyStore(keyStore, password);
            String certificateFingerprint = TlsKeyStore.getCertificateFingerprint(keyStore);
            return new TlsContext(certificateFingerprint, sslContext);
        } catch (TlsException e) {
            throw e;
        } catch (Exception e) {
            throw new TlsException(e);
        }
    }

    private KeyStore createNewKeyStore(List<String> tlsKeyStoreSan, char[] password) throws TlsException {
        try {
            Instant expiryDate = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).toInstant();
            var tlsIdentity = new TLsIdentity("Bisq2 Api Certificate", tlsKeyStoreSan, expiryDate);
            KeyPair keyPair = tlsIdentity.getKeyPair();
            X509Certificate certificate = tlsIdentity.getCertificate();
            return TlsKeyStore.createAndPersistKeyStore(
                    keyPair,
                    certificate,
                    keyStorePath,
                    password);
        } catch (TlsException e) {
            log.error("Failed to create TlsKeyStore", e);
            throw e;
        }
    }
}
