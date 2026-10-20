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

package bisq.trade.bisq_easy;

import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.profile.UserProfile;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The trade store leaves out a trade which it cannot serialize, and logs the failure. A trade holds private data of the
 * traders, and the message of the cause can contain such data. Users share their log files with support, so the log
 * names the trade by its ID and has only the stack trace of the cause
 * (docs/specifications/persistence/store-write-results.md).
 */
@Isolated("Captures the log of BisqEasyTradeStore")
class BisqEasyTradeStoreTest {
    private static final String PRIVATE_DATA = "private-sentinel-iban";

    private final Logger storeLogger = (Logger) LoggerFactory.getLogger(BisqEasyTradeStore.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        storeLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        storeLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void tradeWhichCannotBeSerializedIsLoggedByItsIdWithoutItsData() {
        BisqEasyTrade trade = tradeWhichCannotBeSerialized("open-trade");
        BisqEasyTradeStore store = new BisqEasyTradeStore(Set.of(trade), Set.of("open-trade"), Set.of());

        assertThat(store.getBuilder(false).getTradesList()).isEmpty();

        assertLoggedOnceWithoutPrivateData("open-trade");
    }

    @Test
    void closedTradeWhichCannotBeSerializedIsLoggedByItsIdWithoutItsData() {
        BisqEasyClosedTrade closedTrade = new BisqEasyClosedTrade(tradeWhichCannotBeSerialized("closed-trade"),
                mock(UserProfile.class),
                mock(UserProfile.class));
        BisqEasyTradeStore store = new BisqEasyTradeStore(Set.of(), Set.of("closed-trade"), Set.of(closedTrade));

        assertThat(store.getBuilder(false).getBisqEasyClosedTradesList()).isEmpty();

        assertLoggedOnceWithoutPrivateData("closed-trade");
    }

    // Like a real trade, its description and the message of the cause contain private data
    private static BisqEasyTrade tradeWhichCannotBeSerialized(String tradeId) {
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        when(trade.getId()).thenReturn(tradeId);
        when(trade.toString()).thenReturn("BisqEasyTrade with the payment account data " + PRIVATE_DATA);
        when(trade.toProto(anyBoolean())).thenThrow(new IllegalStateException("Invalid value " + PRIVATE_DATA));
        return trade;
    }

    private void assertLoggedOnceWithoutPrivateData(String tradeId) {
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getFormattedMessage())
                .contains(tradeId)
                .contains(IllegalStateException.class.getName())
                .doesNotContain(PRIVATE_DATA);
        assertThat(event.getThrowableProxy())
                .as("the cause is logged as a stack trace in the message, which leaves out the message of the cause")
                .isNull();
    }
}
