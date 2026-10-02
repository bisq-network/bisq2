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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.release.AppType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An alert with a too long message cannot be created. The network storage therefore never holds one.
 */
class AuthorizedAlertDataTest {
    @Test
    void validAlertIsCreated() {
        assertDoesNotThrow(() -> create(Optional.of("message")));
        assertDoesNotThrow(() -> create(Optional.empty()));
    }

    @Test
    void tooLongMessageIsRejected() {
        create(Optional.of("x".repeat(AuthorizedAlertData.MAX_MESSAGE_LENGTH)));

        assertThrows(IllegalArgumentException.class,
                () -> create(Optional.of("x".repeat(AuthorizedAlertData.MAX_MESSAGE_LENGTH + 1))));
    }

    private static AuthorizedAlertData create(Optional<String> message) {
        return new AuthorizedAlertData("id",
                System.currentTimeMillis(),
                AlertType.INFO,
                Optional.of("headline"),
                message,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                "a".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }
}
