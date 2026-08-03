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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>).
 */
@Builder
public class TestNetworkTorrcGenerator implements TorrcConfigGenerator {

    private final TorrcConfigGenerator baseTorrcConfigGenerator;

    @Builder.Default
    private final Optional<String> nickname = Optional.empty();

    @Builder.Default
    private final Optional<Integer> orPort = Optional.empty();

    @Builder.Default
    private final Optional<Integer> dirPort = Optional.empty();

    @Override
    public Map<String, List<String>> generate() {
        Map<String, List<String>> torConfigMap = baseTorrcConfigGenerator.generate();

        torConfigMap.put("TestingTorNetwork", List.of("1"));
        torConfigMap.put("TestingDirAuthVoteExit", List.of("*"));
        torConfigMap.put("TestingDirAuthVoteHSDir", List.of("*"));

        torConfigMap.put("V3AuthNIntervalsValid", List.of("2"));
        torConfigMap.put("TestingDirAuthVoteGuard", List.of("*"));
        torConfigMap.put("TestingMinExitFlagThreshold", List.of("0"));


        nickname.ifPresent(nickname -> torConfigMap.put("Nickname", List.of(nickname)));
        torConfigMap.put("ShutdownWaitLength", List.of("2"));
        torConfigMap.put("DisableDebuggerAttachment", List.of("0"));

        torConfigMap.put("ProtocolWarnings", List.of("1"));
        torConfigMap.put("SafeLogging", List.of("0"));
        torConfigMap.put("LogTimeGranularity", List.of("1"));

        orPort.ifPresent(orPort -> torConfigMap.put("OrPort", List.of(String.valueOf(orPort))));
        torConfigMap.put("Address", List.of("127.0.0.1"));
        torConfigMap.put("ServerDNSDetectHijacking", List.of("0"));

        torConfigMap.put("ServerDNSTestAddresses", List.of(""));

        dirPort.ifPresent(dirPort -> torConfigMap.put("DirPort", List.of(String.valueOf(dirPort))));

        return torConfigMap;
    }
}
