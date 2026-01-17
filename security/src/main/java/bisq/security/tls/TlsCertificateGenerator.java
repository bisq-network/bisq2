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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralNames;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class TlsCertificateGenerator {
    private static final String CURVE = "secp256r1";

    public static TlsCertificateGenerator create(String commonName, List<String> hosts) {
        return new TlsCertificateGenerator(commonName, hosts);
    }

    @Getter
    private final KeyPair keyPair;
    @Getter
    private final X509Certificate certificate;

    private TlsCertificateGenerator(String commonName, List<String> hosts) {
        keyPair =  KeyGeneration.generateKeyPair(CURVE, KeyGeneration.EC);

        GeneralNames sans = SanUtils.toGeneralNames(hosts);

        Instant now = Instant.now();
        Instant notBefore = now.minus(1, ChronoUnit.HOURS);
        Instant notAfter = now.plus(10, ChronoUnit.YEARS);

        X500Name subject = new X500Name("CN=" + commonName);

        certificate = new SelfSignedCertificateBuilder()
                .subject(subject)
                .subjectAltNames(sans)
                .validity(notBefore, notAfter)
                .build(keyPair);
    }
}
