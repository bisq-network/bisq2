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

package bisq.settings;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Wrapper for CookieKey and sub key
 */
@EqualsAndHashCode
@Getter
class CookieMapKey {
    private final CookieKey cookieKey;

    @Getter
    private final Optional<String> subKey;

    CookieMapKey(CookieKey cookieKey, @Nullable String subKey) {
        this(cookieKey, Optional.ofNullable(subKey));
    }

    private CookieMapKey(CookieKey cookieKey, Optional<String> subKey) {
        this.cookieKey = cookieKey;
        this.subKey = subKey;

        if (subKey.isPresent()) {
            checkArgument(cookieKey.isUseSubKey(),
                    "If the subKey is not null, the enum must have useSubKey set to true. CookieKey=" +
                            cookieKey + ". subKey=" + subKey);
        }
    }

    static CookieMapKey fromProto(String key, Optional<String> subKey) {
        CookieKey cookieKey = CookieKey.valueOf(CookieKey.class, key);
        return new CookieMapKey(cookieKey, subKey);
    }
}
