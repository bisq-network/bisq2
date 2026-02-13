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

import bisq.common.encoding.Base64;
import bisq.security.DigestUtil;

import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class TlsTrustManager implements X509TrustManager {
    private final byte[] fingerprint;

    public TlsTrustManager(String fingerprintBase64) {
        if (fingerprintBase64 == null || fingerprintBase64.isEmpty()) {
            throw new IllegalArgumentException("tlsFingerprint must be a non-empty Base64 string");
        }
        byte[] fingerprintBytes;
        try {
            fingerprintBytes = Base64.decode(fingerprintBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "tlsFingerprint must be a valid Base64-encoded SHA-256 hash", e);
        }
        if (fingerprintBytes.length != 32) {
            throw new IllegalArgumentException("tlsFingerprint must be a SHA-256 hash (32 bytes)");
        }
        this.fingerprint = Arrays.copyOf(fingerprintBytes, fingerprintBytes.length);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

        if (chain == null || chain.length == 0) {
            throw new CertificateException("Empty certificate chain");
        }

        X509Certificate cert = chain[0];
        byte[] actualHash = DigestUtil.sha256(cert.getEncoded());

        if (!MessageDigest.isEqual(actualHash, fingerprint)) {
            throw new CertificateException("Certificate fingerprint not correct");
        }

        cert.checkValidity();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        throw new CertificateException("Client trust not supported");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
