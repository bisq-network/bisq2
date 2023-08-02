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

package bisq.common.validation;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BasicInputValidation {
    public static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2);

    public static void validateCreationDate(long date) {
        checkArgument(date < System.currentTimeMillis() + TWO_HOURS);
    }

    public static void validateHash(byte[] hash) {
        checkArgument(hash.length == 20);
    }

    public static void validateSignature(byte[] signature) {
        log.error("signature {}", signature.length);
        checkArgument(signature.length > 70 && signature.length < 74);
    }

    public static void validatePubKey(byte[] pubKey) {
        log.error("pubKey {}", pubKey.length);
        checkArgument(pubKey.length > 50 && pubKey.length < 100);
    }

    public static void validateId(String id) {
        log.error("id {}", id.length());
        checkArgument(id.length() < 50);
    }

    public static void validateProfileId(String profileId) {
        log.error("profileId {}", profileId.length());
        checkArgument(profileId.length() == 40);
    }
}