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

import java.util.List;
import java.util.Map;

public class DirectoryAuthorityTorrcGenerator implements TorrcConfigGenerator {
    private final TorrcConfigGenerator baseTorrcConfigGenerator;
    private final String nickname;

    public DirectoryAuthorityTorrcGenerator(TorrcConfigGenerator baseTorrcConfigGenerator, String nickname) {
        this.baseTorrcConfigGenerator = baseTorrcConfigGenerator;
        this.nickname = nickname;
    }

    @Override
    public Map<String, List<String>> generate() {
        Map<String, List<String>> torConfigMap = baseTorrcConfigGenerator.generate();

        torConfigMap.put("AuthoritativeDirectory", List.of("1"));
        torConfigMap.put("V3AuthoritativeDirectory", List.of("1"));
        torConfigMap.put("ContactInfo", List.of("auth-" + nickname + "@test.test\n"));

        torConfigMap.put("AssumeReachable", List.of("1"));
        torConfigMap.put("TestingV3AuthInitialVotingInterval", List.of("20"));

        torConfigMap.put("TestingV3AuthInitialVoteDelay", List.of("4"));
        torConfigMap.put("TestingV3AuthInitialDistDelay", List.of("4"));

        torConfigMap.put("V3AuthVotingInterval", List.of("20"));
        torConfigMap.put("V3AuthVoteDelay", List.of("4"));
        torConfigMap.put("V3AuthDistDelay", List.of("4"));

        torConfigMap.put("ExitPolicy", List.of("accept *:*"));

        return torConfigMap;
    }
}
