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

package bisq.network.tor.local_network;

import bisq.network.tor.common.torrc.DirectoryAuthority;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.freehaven.tor.control.PasswordDigest;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Getter
public class TorNode {

    public enum Type {
        CLIENT,
        DIRECTORY_AUTHORITY,
        RELAY
    }

    private final Type type;
    private final String nickname;

    private final Path dataDir;

    private final int orPort;
    private final int dirPort;

    private final Path keysPath;

    @Getter
    private final PasswordDigest controlConnectionPassword = PasswordDigest.generateDigest();

    @Getter(AccessLevel.NONE)
    private Optional<String> identityKeyFingerprint = Optional.empty();
    @Getter(AccessLevel.NONE)
    private Optional<String> relayKeyFingerprint = Optional.empty();

    @Builder
    public TorNode(Type type, String nickname, Path dataDir, int orPort, int dirPort) {
        this.type = type;
        this.nickname = nickname;
        this.dataDir = dataDir;
        this.orPort = orPort;
        this.dirPort = dirPort;
        this.keysPath = dataDir.resolve("keys");
    }

    public Path getTorrcPath() {
        return dataDir.resolve("torrc");
    }

    public Optional<String> getAuthorityIdentityKeyFingerprint() {
        if (type != Type.DIRECTORY_AUTHORITY) {
            return Optional.empty();
        }

        if (identityKeyFingerprint.isPresent()) {
            return identityKeyFingerprint;
        }

        File certificateFile = new File(keysPath.toFile(), "authority_certificate");
        identityKeyFingerprint = readFingerprint(certificateFile, "fingerprint ");
        return identityKeyFingerprint;
    }

    public Optional<String> getRelayKeyFingerprint() {
        if (relayKeyFingerprint.isPresent()) {
            return relayKeyFingerprint;
        }

        File fingerprintFile = new File(dataDir.toFile(), "fingerprint");
        relayKeyFingerprint = readFingerprint(fingerprintFile, "Unnamed ");
        return relayKeyFingerprint;
    }

    public DirectoryAuthority toDirectoryAuthority() {
        if (type != Type.DIRECTORY_AUTHORITY) {
            throw new WrongTorNodeType("Can't convert non-directory node to directory authority.");
        }

        return DirectoryAuthority.builder()
                .nickname(nickname)
                .orPort(orPort)
                .v3Ident(getAuthorityIdentityKeyFingerprint().orElseThrow())
                .dirPort(dirPort)
                .relayFingerprint(getRelayKeyFingerprint().orElseThrow())
                .build();
    }

    private Optional<String> readFingerprint(File fingerprintFile, String linePrefix) {
        Predicate<String> lineMatcher = s -> s.startsWith(linePrefix);
        UnaryOperator<String> dataExtractor = s -> s.split(" ")[1].strip();
        var keyFingerprintReader = new KeyFingerprintReader(fingerprintFile, lineMatcher, dataExtractor);
        return keyFingerprintReader.read();
    }
}
