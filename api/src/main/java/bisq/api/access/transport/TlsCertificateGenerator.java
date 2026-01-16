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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

@Slf4j
public class TlsCertificateGenerator {

    @Getter
    private final KeyPair keyPair;
    @Getter
    private final X509Certificate certificate;

    private TlsCertificateGenerator(String commonName) {
        this.keyPair = generateKeyPair();
        this.certificate = buildSelfSignedCertificate(commonName, keyPair);
    }

    public static TlsCertificateGenerator create(String commonName) {
        return new TlsCertificateGenerator(commonName);
    }

    private static X509Certificate buildSelfSignedCertificate(String commonName, KeyPair keyPair) {
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

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(keyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate self-signed certificate", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate TLS key pair", e);
        }
    }
}
