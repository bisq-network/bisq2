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

package bisq.network.tor.common.torrc;

import java.util.Map;

public class DirectoryAuthorityTorrcGenerator implements TorrcConfigGenerator {
    private final TorrcConfigGenerator baseTorrcConfigGenerator;
    private final String nickname;

    public DirectoryAuthorityTorrcGenerator(TorrcConfigGenerator baseTorrcConfigGenerator, String nickname) {
        this.baseTorrcConfigGenerator = baseTorrcConfigGenerator;
        this.nickname = nickname;
    }

    @Override
    public Map<String, String> generate() {
        Map<String, String> torConfigMap = baseTorrcConfigGenerator.generate();

        torConfigMap.put("AuthoritativeDirectory", "1");
        torConfigMap.put("V3AuthoritativeDirectory", "1");
        torConfigMap.put("ContactInfo", "auth-" + nickname + "@test.test\n");

        torConfigMap.put("AssumeReachable", "1");
        torConfigMap.put("TestingV3AuthInitialVotingInterval", "20");

        torConfigMap.put("TestingV3AuthInitialVoteDelay", "4");
        torConfigMap.put("TestingV3AuthInitialDistDelay", "4");

        torConfigMap.put("V3AuthVotingInterval", "20");
        torConfigMap.put("V3AuthVoteDelay", "4");
        torConfigMap.put("V3AuthDistDelay", "4");

        torConfigMap.put("ExitPolicy", "accept *:*");

        return torConfigMap;
    }
}
