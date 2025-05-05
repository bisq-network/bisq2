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

package bisq.network.i2p.util;

import net.i2p.I2PAppContext;
import net.i2p.client.naming.SingleFileNamingService;
import net.i2p.client.naming.HostsTxtNamingService;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.router.RouterContext;

public class I2PNameResolver {

    public static Destination getDestinationFor(String peerName) {
        String host = peerName;
        if (host.toLowerCase().endsWith(".i2p")) {
            host = host.substring(0, host.length() - 4);
        }

        RouterContext routerCtx = (RouterContext) RouterContext.getCurrentContext();
        I2PAppContext ctx = (routerCtx != null)
                ? routerCtx
                : I2PAppContext.getGlobalContext();  // safe even if no router exists

        Destination dest = null;

        String hostsFile = ctx.getConfigDir() + System.getProperty("file.separator") + "hosts.txt";
        SingleFileNamingService fileNS = new SingleFileNamingService(ctx, hostsFile);
        dest = fileNS.lookup(host, null, null);
        if (dest != null) {
            return dest;
        }

        HostsTxtNamingService hostsNS = new HostsTxtNamingService(ctx);
        dest = hostsNS.lookup(host, null, null);
        if (dest != null) {
            return dest;
        }

        if (routerCtx != null) {
            String base32 = null;
            if (peerName.toLowerCase().endsWith(".b32.i2p")) {
                base32 = peerName.substring(0, peerName.length() - 8).toLowerCase();
            } else if (host.length() == 52 && host.matches("[a-z2-7]+")) {
                base32 = host.toLowerCase();
            }
            if (base32 != null) {
                byte[] hashBytes = decodeBase32(base32);
                if (hashBytes != null) {
                    Hash hash = new Hash(hashBytes);
                    dest = routerCtx.netDb().lookupDestinationLocally(hash);
                    if (dest != null) {
                        return dest;
                    }
                }
            }
        }
        return null;
    }

    private static byte[] decodeBase32(String s) {
        return Base32.decode(s);
    }
}
