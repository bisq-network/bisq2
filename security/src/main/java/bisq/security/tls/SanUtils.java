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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public final class SanUtils {
    public static GeneralName[] toGeneralNames(List<String> hosts) {
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

        return result.toArray(new GeneralName[0]);
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
