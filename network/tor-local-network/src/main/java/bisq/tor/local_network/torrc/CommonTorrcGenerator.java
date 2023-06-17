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

import java.io.IOException;

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>).
 */
public abstract class CommonTorrcGenerator {
    protected final DirectoryAuthority thisDirectoryAuthority;
    protected final StringBuilder torrcStringBuilder = new StringBuilder();

    public CommonTorrcGenerator(DirectoryAuthority thisDirectoryAuthority) {
        this.thisDirectoryAuthority = thisDirectoryAuthority;
    }

    public void generate() throws IOException {
        torrcStringBuilder.append("TestingTorNetwork 1\n")

                .append("PathsNeededToBuildCircuits 0.67\n")
                .append("TestingDirAuthVoteExit *\n")
                .append("TestingDirAuthVoteHSDir *\n")
                .append("V3AuthNIntervalsValid 2\n")

                .append("TestingDirAuthVoteGuard *\n")
                .append("TestingMinExitFlagThreshold 0\n")

                .append("DataDirectory ").append(thisDirectoryAuthority.getDataDir()).append("\n")
                .append("RunAsDaemon 1\n")
                .append("Nickname ").append(thisDirectoryAuthority.getNickname()).append("\n")

                .append("ShutdownWaitLength 2\n")
                .append("DisableDebuggerAttachment 0\n")

                .append("ControlPort 127.0.0.1:").append(thisDirectoryAuthority.getControlPort()).append("\n")
                .append("CookieAuthentication 1\n")

                .append("Log debug file ").append(thisDirectoryAuthority.getDataDir().resolve("debug.log").toAbsolutePath()).append("\n")
                .append("ProtocolWarnings 1\n")
                .append("SafeLogging 0\n")
                .append("LogTimeGranularity 1\n")

                .append("SocksPort 0\n")
                .append("OrPort ").append(thisDirectoryAuthority.getOrPort()).append("\n")
                .append("Address 127.0.0.1\n")

                .append("ServerDNSDetectHijacking 0\n")
                .append("ServerDNSTestAddresses\n")

                .append("DirPort ").append(thisDirectoryAuthority.getDirPort()).append("\n");
    }
}
