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
import bisq.security.tls.SslContextFactory;
import bisq.security.tls.TLsIdentity;
import bisq.security.tls.TlsException;
import bisq.security.tls.TlsKeyStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TlsContextService {
    private final ApiConfig apiConfig;
    private final Path keyStorePath;
    private final boolean isTlsRequired;

    @Getter
    private volatile Optional<TlsContext> tlsContext = Optional.empty();

    public TlsContextService(ApiConfig apiConfig, Path appDataDirPath) {
        this.apiConfig = apiConfig;
        keyStorePath = appDataDirPath.resolve("api").resolve("tls_keystore.p12");
        isTlsRequired = apiConfig.isTlsRequired();
    }


    public synchronized Optional<TlsContext> getOrCreateTlsContext() throws Exception {
        if (isTlsRequired && tlsContext.isEmpty()) {
            tlsContext = Optional.of(createTlsContext());
        }
        return tlsContext;
    }

    private TlsContext createTlsContext() throws TlsException {
        char[] password = apiConfig.getTlsKeyStorePassword().toCharArray();
        List<String> hosts = apiConfig.getTlsKeyStoreSan();
        KeyStore keyStore;
        Optional<KeyStore> optionalKeyStore = TlsKeyStore.readKeyStore(keyStorePath, password);
        if (optionalKeyStore.isPresent()) {
            keyStore = optionalKeyStore.get();
        } else {
            try {
                Instant expiryDate = Instant.now().plus(10, ChronoUnit.YEARS);
                var tlsIdentity = new TLsIdentity("Bisq2 Api Certificate", hosts, expiryDate);
                KeyPair keyPair = tlsIdentity.getKeyPair();
                X509Certificate certificate = tlsIdentity.getCertificate();
                keyStore = TlsKeyStore.createAndPersistKeyStore(
                        keyPair,
                        certificate,
                        keyStorePath,
                        password);
            } catch (TlsException e) {
                log.error("Failed to create TlsKeyStore", e);
                throw e;
            }
        }
        SSLContext sslContext = SslContextFactory.fromKeyStore(keyStore, password);
        String certificateFingerprint = TlsKeyStore.getCertificateFingerprint(keyStore);
        return new TlsContext(certificateFingerprint, sslContext);
    }
}
