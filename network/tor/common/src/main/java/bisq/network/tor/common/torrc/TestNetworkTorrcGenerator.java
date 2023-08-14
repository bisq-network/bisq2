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

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>).
 */
@Builder
@Getter
public class TestNetworkTorrcGenerator implements TorrcConfigGenerator {

    private final TorrcConfigGenerator baseTorrcConfigGenerator;

    private final String nickname;
    private final int orPort;
    private final int dirPort;

    public TestNetworkTorrcGenerator(TorrcConfigGenerator baseTorrcConfigGenerator,
                                     String nickname,
                                     int orPort,
                                     int dirPort) {
        this.baseTorrcConfigGenerator = baseTorrcConfigGenerator;
        this.nickname = nickname;
        this.orPort = orPort;
        this.dirPort = dirPort;
    }

    @Override
    public Map<String, String> generate() {
        Map<String, String> torConfigMap = baseTorrcConfigGenerator.generate();

        torConfigMap.put("TestingTorNetwork", "1");
        torConfigMap.put("TestingDirAuthVoteExit", "*");
        torConfigMap.put("TestingDirAuthVoteHSDir", "*");

        torConfigMap.put("V3AuthNIntervalsValid", "2");
        torConfigMap.put("TestingDirAuthVoteGuard", "*");
        torConfigMap.put("TestingMinExitFlagThreshold", "0");


        torConfigMap.put("Nickname", nickname);
        torConfigMap.put("ShutdownWaitLength", "2");
        torConfigMap.put("DisableDebuggerAttachment", "0");

        torConfigMap.put("ProtocolWarnings", "1");
        torConfigMap.put("SafeLogging", "0");
        torConfigMap.put("LogTimeGranularity", "1");

        torConfigMap.put("OrPort", String.valueOf(orPort));
        torConfigMap.put("Address", "127.0.0.1");
        torConfigMap.put("ServerDNSDetectHijacking", "0");

        torConfigMap.put("ServerDNSTestAddresses", "");

        torConfigMap.put("DirPort", String.valueOf(dirPort));

        return torConfigMap;
    }
}
