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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SslContextFactory {
    public static SSLContext fromKeyStore(KeyStore keyStore, char[] keyPassword) throws TlsException {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyPassword);

            SSLContext context = SSLContext.getInstance(TlsKeyStore.PROTOCOL);
            context.init(kmf.getKeyManagers(), null, null);
            return context;

        } catch (GeneralSecurityException e) {
            throw new TlsException("Failed to create SSLContext", e);
        }
    }
}
