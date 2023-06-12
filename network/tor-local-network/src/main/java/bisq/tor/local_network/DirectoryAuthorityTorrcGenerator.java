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

package bisq.tor.local_network;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

/**
 * The configuration settings are from the Chutney (<a href="https://gitweb.torproject.org/chutney.git/">project</a>) and
 * Antitree's private-tor-network (<a href="https://github.com/antitree/private-tor-network">project</a>).
 */
public class DirectoryAuthorityTorrcGenerator {
    private final DirectoryAuthority thisDirectoryAuthority;
    private final Set<DirectoryAuthority> allDirAuthorities;

    public DirectoryAuthorityTorrcGenerator(DirectoryAuthority thisDirectoryAuthority, Set<DirectoryAuthority> allDirAuthorities) {
        this.thisDirectoryAuthority = thisDirectoryAuthority;
        this.allDirAuthorities = allDirAuthorities;
    }

    public void generate() throws IOException {
        var stringBuilder = new StringBuilder();
        stringBuilder.append("TestingTorNetwork 1\n")

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

                .append("DirPort ").append(thisDirectoryAuthority.getDirPort()).append("\n")

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

        allDirAuthorities.forEach(dirAuthority ->
                stringBuilder.append("DirAuthority ").append(dirAuthority.getNickname())
                        .append(" orport=").append(dirAuthority.getOrPort())
                        .append(" v3ident=").append(dirAuthority.getV3LongTermSigningKeyFingerprint())
                        .append(" 127.0.0.1:").append(dirAuthority.getDirPort())
                        .append(" ").append(dirAuthority.getTorKeyFingerprint())
                        .append("\n"));


        Files.writeString(thisDirectoryAuthority.getTorrcPath(), stringBuilder.toString());
    }
}
