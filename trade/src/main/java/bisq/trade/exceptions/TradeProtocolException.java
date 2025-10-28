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

package bisq.trade.exceptions;

import lombok.Getter;

public class TradeProtocolException extends RuntimeException {
    @Getter
    private final TradeProtocolFailure tradeProtocolFailure;

    public TradeProtocolException(TradeProtocolFailure tradeProtocolFailure) {
        this.tradeProtocolFailure = tradeProtocolFailure;
    }

    public TradeProtocolException(String message, TradeProtocolFailure tradeProtocolFailure) {
        super(message);
        this.tradeProtocolFailure = tradeProtocolFailure;
    }

    public TradeProtocolException(String message, TradeProtocolFailure tradeProtocolFailure, Throwable cause) {
        super(message, cause);
        this.tradeProtocolFailure = tradeProtocolFailure;
    }

    public TradeProtocolException(Throwable cause, TradeProtocolFailure tradeProtocolFailure) {
        super(cause);
        this.tradeProtocolFailure = tradeProtocolFailure;
    }

    public TradeProtocolException(String message,
                                  TradeProtocolFailure tradeProtocolFailure,
                                  Throwable cause,
                                  boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.tradeProtocolFailure = tradeProtocolFailure;
    }
}
