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

import bisq.common.util.ProtobufUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
// Used for persistence of Cookie. We use enum name as key.
public enum CookieKey {
    STAGE_X,
    STAGE_Y,
    STAGE_W,
    STAGE_H,
    NAVIGATION_TARGET,
    FILE_CHOOSER_DIR,
    CREATE_OFFER_METHODS(true),
    CREATE_OFFER_USE_FIX_PRICE(true),
    BONDED_ROLES_COLLAPSED,
    CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED,
    IGNORE_VERSION(true),
    NOTIFY_FOR_PRE_RELEASE,
    BISQ_EASY_VIDEO_OPENED;

    @Setter
    @Getter
    @Nullable
    private String subKey;
    private final boolean useSubKey;

    CookieKey(boolean useSubKey) {
        this.useSubKey = useSubKey;
    }

    CookieKey() {
        this(false);
    }

    public boolean isUseSubKey() {
        if (useSubKey) {
            checkArgument(subKey != null,
                    "If the enum has useSubKey set the subKey must not be null. CookieKey=" + this);
        }
        return useSubKey;
    }

    // We do not use protobuf for the enum for more flexibility
    String getKeyForProto() {
        String key = name();
        if (isUseSubKey()) {
            key = key + "." + subKey;
        }
        return key;
    }

    @Nullable
    static CookieKey fromProto(String key) {
        String[] tokens = key.split("\\.");
        String name = tokens[0];
        CookieKey cookieKey = ProtobufUtils.enumFromProto(CookieKey.class, name);
        if (cookieKey != null && tokens.length > 1) {
            String subKey = tokens[1];
            checkArgument(cookieKey.useSubKey,
                    "If the subKey is not null, the enum must have useSubKey set to true. CookieKey=" +
                            cookieKey + ". subKey=" + subKey);
            cookieKey.setSubKey(subKey);
        }
        return cookieKey;
    }
}
