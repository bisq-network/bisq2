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

package bisq.tor.local_network.torrc;

import bisq.tor.local_network.DirectoryAuthority;

public class RelayTorrcGenerator extends CommonTorrcGenerator {
    public RelayTorrcGenerator(DirectoryAuthority thisDirectoryAuthority) {
        super(thisDirectoryAuthority);
    }

    @Override
    public void generate() {
        super.generate();
        torrcStringBuilder
                .append("ExitRelay 1\n")
                .append("ExitPolicy accept 127.0.0.0/8:*\n")
                .append("ExitPolicyRejectPrivate 0\n")
                .append("ExitPolicy accept private:*\n")
                .append("ExitPolicy accept *:*\n")
                .append("ExitPolicy reject *:*\n");
    }
}
