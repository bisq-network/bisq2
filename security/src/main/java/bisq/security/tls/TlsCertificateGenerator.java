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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

@Slf4j
public class TlsCertificateGenerator {
    public static TlsCertificateGenerator create(String commonName, List<String> hosts) {
        return new TlsCertificateGenerator(commonName, hosts);
    }

    @Getter
    private final KeyPair keyPair;
    @Getter
    private final X509Certificate certificate;

    private TlsCertificateGenerator(String commonName, List<String> hosts) {
        keyPair = TlsKeyPairGenerator.generateKeyPair();
        GeneralNames subjectAltNames = new GeneralNames(SanUtils.toGeneralNames(hosts));
        certificate = buildSelfSignedCertificate(commonName, keyPair, subjectAltNames);
    }

    private static X509Certificate buildSelfSignedCertificate(String commonName,
                                                              KeyPair keyPair,
                                                              GeneralNames subjectAltNames) {
        try {
            long now = System.currentTimeMillis();
            Date notBefore = new Date(now);
            Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000);

            X500Name subject = new X500Name("CN=" + commonName);
            BigInteger serial = new BigInteger(64, new SecureRandom());

            JcaX509v3CertificateBuilder certBuilder =
                    new JcaX509v3CertificateBuilder(
                            subject,
                            serial,
                            notBefore,
                            notAfter,
                            subject,
                            keyPair.getPublic()
                    );

            CertificateExtensions.addServerExtensions(certBuilder, subjectAltNames);

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(keyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate self-signed certificate", e);
        }
    }
}
