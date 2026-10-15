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

package bisq.bonded_roles.release;

import bisq.common.platform.InvalidVersionException;
import bisq.common.proto.UnresolvableProtobufException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A release notification with an invalid version or with too long release notes cannot be created. The network storage
 * therefore never holds one.
 */
class ReleaseNotificationTest {
    private static final String RELEASE_NOTES = "Release notes";
    private static final String VERSION = "2.1.14";

    @Test
    void validReleaseNotificationIsCreated() throws UnresolvableProtobufException {
        ReleaseNotification releaseNotification = create(RELEASE_NOTES, VERSION);

        assertEquals(releaseNotification, ReleaseNotification.fromProto(releaseNotification.toProto(false)));
    }

    @Test
    void tooLongReleaseNotesAreRejected() {
        create("x".repeat(ReleaseNotification.MAX_MESSAGE_LENGTH), VERSION);

        assertThrows(IllegalArgumentException.class,
                () -> create("x".repeat(ReleaseNotification.MAX_MESSAGE_LENGTH + 1), VERSION));
    }

    @Test
    void invalidVersionIsRejected() {
        assertThrows(InvalidVersionException.class, () -> create(RELEASE_NOTES, ""));
        assertThrows(InvalidVersionException.class, () -> create(RELEASE_NOTES, "2.1.x"));
        // A valid format, but longer than a version can be
        assertThrows(IllegalArgumentException.class, () -> create(RELEASE_NOTES, "1.1.1.1.1.11"));
    }

    private static ReleaseNotification create(String releaseNotes, String versionString) {
        return new ReleaseNotification("id",
                System.currentTimeMillis(),
                false,
                false,
                releaseNotes,
                versionString,
                "a".repeat(40),
                false,
                AppType.DESKTOP);
    }
}
