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

public class DirectoryAuthorityTorrcGenerator extends CommonTorrcGenerator {
    public DirectoryAuthorityTorrcGenerator(DirectoryAuthority thisDirectoryAuthority) {
        super(thisDirectoryAuthority);
    }

    @Override
    public void generate() {
        super.generate();

        torrcStringBuilder
                .append("AuthoritativeDirectory 1\n")
                .append("V3AuthoritativeDirectory 1\n")
                .append("ContactInfo auth-").append(thisDirectoryAuthority.getNickname()).append("@test.test\n")

                .append("AssumeReachable 1\n")

                .append("TestingV3AuthInitialVotingInterval 20\n")
                .append("TestingV3AuthInitialVoteDelay 4\n")
                .append("TestingV3AuthInitialDistDelay 4\n")

                .append("V3AuthVotingInterval 20\n")
                .append("V3AuthVoteDelay 4\n")
                .append("V3AuthDistDelay 4\n")

                .append(thisDirectoryAuthority.getExitPolicy()).append("\n");
    }
}
