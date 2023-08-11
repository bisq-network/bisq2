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

import bisq.tor.local_network.TorNode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>).
 */
@Getter
public abstract class TestNetworkTorrcGenerator implements TorrcConfigGenerator {
    protected final TorNode thisTorNode;
    protected final Map<String, String> torConfigMap = new HashMap<>();

    public TestNetworkTorrcGenerator(TorNode thisTorNode) {
        this.thisTorNode = thisTorNode;
    }

    @Override
    public Map<String, String> generate() {
        torConfigMap.put("TestingTorNetwork", "1");
        torConfigMap.put("TestingDirAuthVoteExit", "*");
        torConfigMap.put("TestingDirAuthVoteHSDir", "*");

        torConfigMap.put("V3AuthNIntervalsValid", "2");
        torConfigMap.put("TestingDirAuthVoteGuard", "*");
        torConfigMap.put("TestingMinExitFlagThreshold", "0");

        torConfigMap.put("DataDirectory", thisTorNode.getDataDir().toAbsolutePath().toString());

        torConfigMap.put("Nickname", thisTorNode.getNickname());
        torConfigMap.put("ShutdownWaitLength", "2");
        torConfigMap.put("DisableDebuggerAttachment", "0");
        torConfigMap.put("ControlPort", "127.0.0.1:" + thisTorNode.getControlPort());

        torConfigMap.put("HashedControlPassword",
                thisTorNode.getControlConnectionPassword()
                        .getHashedPassword()
        );

        torConfigMap.put("Log",
                "debug file " + thisTorNode.getDataDir().resolve("debug.log").toAbsolutePath()
        );

        torConfigMap.put("ProtocolWarnings", "1");
        torConfigMap.put("SafeLogging", "0");
        torConfigMap.put("LogTimeGranularity", "1");

        if (thisTorNode.getType() != TorNode.Type.CLIENT) {
            torConfigMap.put("SocksPort", "0");
        }

        torConfigMap.put("OrPort", String.valueOf(thisTorNode.getOrPort()));
        torConfigMap.put("Address", "127.0.0.1");
        torConfigMap.put("ServerDNSDetectHijacking", "0");

        torConfigMap.put("ServerDNSTestAddresses", "");

        torConfigMap.put("DirPort", String.valueOf(thisTorNode.getDirPort()));

        return torConfigMap;
    }
}
