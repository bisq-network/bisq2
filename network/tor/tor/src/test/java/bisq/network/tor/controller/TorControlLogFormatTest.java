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

package bisq.network.tor.controller;

import bisq.security.keys.TorKeyGeneration;
import com.google.common.io.BaseEncoding;
import net.freehaven.tor.control.PasswordDigest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TorControlLogFormatTest {
    @Test
    void addOnionHidesPrivateKeyAndKeepsPortMapping() {
        // The base64 form of the 64 byte secret scalar ends with "==", so a parser which splits at '=' would leak it
        String secretScalar = TorKeyGeneration.generateKeyPair().getBase64SecretScalar();
        String command = "ADD_ONION ED25519-V3:" + secretScalar + " Port=9999,12345\r\n";

        String loggable = TorControlLogFormat.toLoggableCommand(command);

        assertThat(loggable).isEqualTo("ADD_ONION ED25519-V3:[hidden] Port=9999,12345");
        assertThat(loggable).doesNotContain(secretScalar);
    }

    @Test
    void addOnionHidesClientAuthAndUnknownKeyTypes() {
        assertThat(TorControlLogFormat.toLoggableCommand("ADD_ONION ED25519-V3:a2V5 ClientAuth=bob:c2VjcmV0 Port=80\r\n"))
                .isEqualTo("ADD_ONION ED25519-V3:[hidden] [hidden] Port=80");
        assertThat(TorControlLogFormat.toLoggableCommand("ADD_ONION NEW:BEST Port=80\r\n"))
                .isEqualTo("ADD_ONION NEW:[hidden] Port=80");
        assertThat(TorControlLogFormat.toLoggableCommand("ADD_ONION c2VjcmV0:a2V5 Port=80\r\n"))
                .isEqualTo("ADD_ONION [hidden] Port=80");
    }

    @Test
    void authenticateHidesPasswordAndAuthCookie() {
        String password = BaseEncoding.base16().lowerCase().encode(PasswordDigest.generateDigest().getSecret());
        String authCookie = BaseEncoding.base16().lowerCase().encode(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});

        assertThat(TorControlLogFormat.toLoggableCommand("AUTHENTICATE " + password + "\r\n"))
                .isEqualTo("AUTHENTICATE [hidden]");
        assertThat(TorControlLogFormat.toLoggableCommand("AUTHENTICATE " + authCookie + "\r\n"))
                .isEqualTo("AUTHENTICATE [hidden]");
        assertThat(TorControlLogFormat.toLoggableCommand("AUTHENTICATE \"plain text password\"\r\n"))
                .isEqualTo("AUTHENTICATE [hidden]");
        // The authentication without data, used if the control port of an external Tor needs no authentication
        assertThat(TorControlLogFormat.toLoggableCommand("AUTHENTICATE \r\n"))
                .isEqualTo("AUTHENTICATE");
    }

    @Test
    void commandsWithPublicArgumentsKeepTheirArguments() {
        assertThat(TorControlLogFormat.toLoggableCommand("GETINFO net/listeners/socks\r\n"))
                .isEqualTo("GETINFO net/listeners/socks");
        assertThat(TorControlLogFormat.toLoggableCommand("HSFETCH bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd\r\n"))
                .isEqualTo("HSFETCH bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd");
        assertThat(TorControlLogFormat.toLoggableCommand("RESETCONF __OwningControllerProcess\r\n"))
                .isEqualTo("RESETCONF __OwningControllerProcess");
        assertThat(TorControlLogFormat.toLoggableCommand("SETEVENTS STATUS_CLIENT HS_DESC\r\n"))
                .isEqualTo("SETEVENTS STATUS_CLIENT HS_DESC");
        assertThat(TorControlLogFormat.toLoggableCommand("SETEVENTS\r\n"))
                .isEqualTo("SETEVENTS");
        assertThat(TorControlLogFormat.toLoggableCommand("TAKEOWNERSHIP\r\n"))
                .isEqualTo("TAKEOWNERSHIP");
    }

    @Test
    void otherCommandsHideTheirArguments() {
        // An option value can be a secret, for example a hashed password or a proxy password
        assertThat(TorControlLogFormat.toLoggableCommand("SETCONF HashedControlPassword=16:ABCDEF\r\n"))
                .isEqualTo("SETCONF [hidden]");
        assertThat(TorControlLogFormat.toLoggableCommand("SETCONF DisableNetwork=0\r\n"))
                .isEqualTo("SETCONF [hidden]");
        assertThat(TorControlLogFormat.toLoggableCommand("ONION_CLIENT_AUTH_ADD service x25519:c2VjcmV0\r\n"))
                .isEqualTo("ONION_CLIENT_AUTH_ADD [hidden]");
    }

    @Test
    void replyDataWithPublicValueIsKept() {
        assertThat(TorControlLogFormat.toLoggableReply("250-ServiceID=bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd"))
                .isEqualTo("250-ServiceID=bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd");
        assertThat(TorControlLogFormat.toLoggableReply("250-net/listeners/socks=\"127.0.0.1:9050\""))
                .isEqualTo("250-net/listeners/socks=\"127.0.0.1:9050\"");
    }

    @Test
    void replyDataWithOtherKeysIsHidden() {
        String secretScalar = TorKeyGeneration.generateKeyPair().getBase64SecretScalar();

        assertThat(TorControlLogFormat.toLoggableReply("250-PrivateKey=ED25519-V3:" + secretScalar))
                .isEqualTo("250-PrivateKey=[hidden]");
        assertThat(TorControlLogFormat.toLoggableReply("250-ClientAuth=bob:c2VjcmV0"))
                .isEqualTo("250-ClientAuth=[hidden]");
        assertThat(TorControlLogFormat.toLoggableReply("250+config-text="))
                .isEqualTo("250+config-text=[hidden]");
    }

    @Test
    void statusTextsAndEventsAreKept() {
        assertThat(TorControlLogFormat.toLoggableReply("250 OK"))
                .isEqualTo("250 OK");
        assertThat(TorControlLogFormat.toLoggableReply("515 Authentication failed: Password did not match HashedControlPassword value from configuration"))
                .isEqualTo("515 Authentication failed: Password did not match HashedControlPassword value from configuration");
        assertThat(TorControlLogFormat.toLoggableReply("650 HS_DESC UPLOADED bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd UNKNOWN $ABCD"))
                .isEqualTo("650 HS_DESC UPLOADED bedb3wpuybkuwvjat2zq5odtqbajq7x3ovllvh7l2kbxakbgkzgikqyd UNKNOWN $ABCD");
        assertThat(TorControlLogFormat.toLoggableReply("650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=100 TAG=done SUMMARY=\"Done\""))
                .isEqualTo("650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=100 TAG=done SUMMARY=\"Done\"");
    }

    @Test
    void dataLinesWithoutStatusCodeAreHidden() {
        assertThat(TorControlLogFormat.toLoggableReply("HashedControlPassword 16:ABCDEF"))
                .isEqualTo("[hidden]");
        assertThat(TorControlLogFormat.toLoggableReply("."))
                .isEqualTo("[hidden]");
    }
}
