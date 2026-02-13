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

import bisq.security.keys.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.GeneralNames;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
public class TlsIdentity {
    private static final String CURVE = "secp256r1";

    @Getter
    private final KeyPair keyPair;
    @Getter
    private final X509Certificate certificate;

    public TlsIdentity(String commonName, List<String> hosts) throws TlsException {
        this(commonName, hosts, ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).toInstant());
    }

    public TlsIdentity(String commonName,
                       List<String> hosts,
                       Instant expiryDate) throws TlsException {
        try {
            keyPair = KeyGeneration.generateKeyPair(CURVE, KeyGeneration.EC);
            GeneralNames sans = SanUtils.toGeneralNames(hosts);

            certificate = new SelfSignedCertificateBuilder()
                    .commonName(commonName)
                    .subjectAltNames(sans)
                    .expiry(expiryDate)
                    .build(keyPair);
        } catch (Exception e) {
            throw new TlsException(e);
        }
    }
}
