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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public final class SelfSignedCertificateBuilder {
    private X500Name subject = new X500Name("CN=Bisq2");
    private GeneralNames subjectAltNames = SanUtils.toGeneralNames(List.of("127.0.0.1"));
    // Add some tolerance in case client clock is not in sync
    private final Instant notBefore = Instant.now().minus(1, ChronoUnit.DAYS);
    private Instant notAfter = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1).toInstant();

    public SelfSignedCertificateBuilder commonName(String commonName) {
        this.subject = new X500Name("CN=" + commonName);
        return this;
    }

    public SelfSignedCertificateBuilder subjectAltNames(GeneralNames subjectAltNames) {
        this.subjectAltNames = subjectAltNames;
        return this;
    }

    public SelfSignedCertificateBuilder expiry(Instant notAfter) {
        this.notAfter = notAfter;
        return this;
    }

    public X509Certificate build(KeyPair keyPair) {
        if (subject == null) {
            throw new IllegalStateException("Subject must be set");
        }
        if (subjectAltNames == null) {
            throw new IllegalStateException("SAN must be set");
        }
        if (notBefore == null || notAfter == null) {
            throw new IllegalStateException("Validity date must be set");
        }

        try {
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

            X509v3CertificateBuilder certBuilder =
                    new JcaX509v3CertificateBuilder(
                            subject,                // issuer (self-signed)
                            serial,
                            Date.from(notBefore),
                            Date.from(notAfter),
                            subject,                // subject
                            keyPair.getPublic()
                    );

            CertificateExtensions.addServerExtensions(certBuilder, subjectAltNames);

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setProvider("BC")
                    .build(keyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);

            X509Certificate certificate = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certHolder);

            certificate.checkValidity();
            certificate.verify(keyPair.getPublic());

            return certificate;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build self-signed certificate", e);
        }
    }
}
