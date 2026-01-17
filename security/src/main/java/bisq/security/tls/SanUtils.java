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

import bisq.common.util.StringUtils;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;

import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SanUtils {
    public static GeneralNames toGeneralNames(List<String> hosts) {
        List<GeneralName> result = new ArrayList<>();

        for (String host : hosts) {
            if (StringUtils.isEmpty(host)) {
                continue;
            }

            if (isIp(host)) {
                result.add(new GeneralName(GeneralName.iPAddress, host));
            } else {
                result.add(new GeneralName(GeneralName.dNSName, host));
            }
        }

        return new GeneralNames(result.toArray(new GeneralName[0]));
    }

    public static Set<String> readSanDnsNames(KeyStore keyStore, String alias) throws TlsException {
        Certificate cert;
        try {
            cert = keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new TlsException(e);
        }
        if (!(cert instanceof X509Certificate x509)) {
            throw new TlsException("Not an X509 certificate");
        }

        Collection<List<?>> sans;
        try {
            sans = x509.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            throw new TlsException(e);
        }
        if (sans == null) {
            return Set.of();
        }

        Set<String> result = new HashSet<>();

        for (List<?> san : sans) {
            Integer type = (Integer) san.get(0);
            Object value = san.get(1);

            if (type == GeneralName.iPAddress && value instanceof String host) {
                result.add((host));
            } else if (type == GeneralName.dNSName && value instanceof String host) {
                result.add((host).toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    private static boolean isIp(String value) {
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
