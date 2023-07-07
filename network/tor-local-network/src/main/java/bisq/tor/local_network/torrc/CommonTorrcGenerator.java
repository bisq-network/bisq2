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

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>).
 */
@Getter
public abstract class CommonTorrcGenerator {
    protected final TorNode thisTorNode;
    protected final StringBuilder torrcStringBuilder = new StringBuilder();

    public CommonTorrcGenerator(TorNode thisTorNode) {
        this.thisTorNode = thisTorNode;
    }

    public void generate() {
        torrcStringBuilder.append("TestingTorNetwork 1\n")

                .append("PathsNeededToBuildCircuits 0.67\n")
                .append("TestingDirAuthVoteExit *\n")
                .append("TestingDirAuthVoteHSDir *\n")
                .append("V3AuthNIntervalsValid 2\n")

                .append("TestingDirAuthVoteGuard *\n")
                .append("TestingMinExitFlagThreshold 0\n")

                .append("DataDirectory ").append(thisTorNode.getDataDir()).append("\n")
                .append("RunAsDaemon 1\n")
                .append("Nickname ").append(thisTorNode.getNickname()).append("\n")

                .append("ShutdownWaitLength 2\n")
                .append("DisableDebuggerAttachment 0\n")

                .append("ControlPort 127.0.0.1:").append(thisTorNode.getControlPort()).append("\n")
                .append("CookieAuthentication 1\n")

                .append("Log debug file ").append(thisTorNode.getDataDir().resolve("debug.log").toAbsolutePath()).append("\n")
                .append("ProtocolWarnings 1\n")
                .append("SafeLogging 0\n")
                .append("LogTimeGranularity 1\n")

                .append("SocksPort 0\n")
                .append("OrPort ").append(thisTorNode.getOrPort()).append("\n")
                .append("Address 127.0.0.1\n")

                .append("ServerDNSDetectHijacking 0\n")
                .append("ServerDNSTestAddresses\n")

                .append("DirPort ").append(thisTorNode.getDirPort()).append("\n");
    }
}
