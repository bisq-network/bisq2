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

package bisq.trade;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Thrown when a security-manager emergency alert restricts trading (haltTrading or
 * requireVersionForTrading). Extends IllegalArgumentException so existing catch sites keep
 * working, while carrying the restriction type and min. required version so API consumers can
 * emit a structured error instead of parsing the message text.
 * <p>
 * The message texts are a compatibility contract: released mobile clients classify these
 * failures by matching fragments of them. Do not reword without a migration path.
 */
@Getter
public class TradeRestrictedException extends IllegalArgumentException {
    public enum Restriction {
        HALT_TRADING,
        MIN_VERSION_REQUIRED
    }

    private final Restriction restriction;
    @Nullable
    private final String minRequiredVersion;

    public static TradeRestrictedException haltTrading() {
        return new TradeRestrictedException(Restriction.HALT_TRADING,
                null,
                "Trading is on halt for security reasons. " +
                        "The Bisq security manager has published an emergency alert with haltTrading set to true");
    }

    public static TradeRestrictedException minVersionRequired(String minRequiredVersion) {
        return new TradeRestrictedException(Restriction.MIN_VERSION_REQUIRED,
                minRequiredVersion,
                "For trading you need to have version " + minRequiredVersion + " installed. " +
                        "The Bisq security manager has published an emergency alert with a min. version required for trading.");
    }

    private TradeRestrictedException(Restriction restriction, @Nullable String minRequiredVersion, String message) {
        super(message);
        this.restriction = restriction;
        this.minRequiredVersion = minRequiredVersion;
    }

    public Optional<String> findMinRequiredVersion() {
        return Optional.ofNullable(minRequiredVersion);
    }
}
