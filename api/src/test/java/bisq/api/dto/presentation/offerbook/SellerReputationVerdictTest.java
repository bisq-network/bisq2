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

package bisq.api.dto.presentation.offerbook;

import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SellerReputationVerdictTest {
    private final BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
            mock(BisqEasySellersReputationBasedTradeAmountService.class);
    private final BisqEasyOffer offer = mock(BisqEasyOffer.class);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void verdictIsTheVerdictOfTheService(boolean hasSellerSufficientReputation) {
        when(sellersReputationService.hasSellerSufficientReputation(offer)).thenReturn(hasSellerSufficientReputation);

        assertThat(SellerReputationVerdict.find(sellersReputationService, offer)).contains(hasSellerSufficientReputation);
    }

    // A failing check repeats on every request and revision, so its warning carries no stack trace
    @Test
    void failingCheckGivesNoVerdictAndAShortWarning() {
        when(offer.getId()).thenReturn("offer-id");
        when(sellersReputationService.hasSellerSufficientReputation(offer)).thenThrow(new IllegalStateException("check failed"));
        Logger logger = (Logger) LoggerFactory.getLogger(SellerReputationVerdict.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        Optional<Boolean> verdict;
        try {
            verdict = SellerReputationVerdict.find(sellersReputationService, offer);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(verdict).isEmpty();
        List<ILoggingEvent> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getThrowableProxy()).isNull();
        // The simple name of an anonymous class is empty
        assertThat(warnings.get(0).getFormattedMessage()).contains("java.lang.IllegalStateException");
    }

    // The maker chooses the offer ID, so a line break in it could forge a log line. The layout prints a logged
    // throwable as it is, like every other one.
    @Test
    void logMessagesOfAFailingCheckHaveNoLineBreak() {
        when(offer.getId()).thenReturn("offer-id\nFORGED offer-id");
        when(sellersReputationService.hasSellerSufficientReputation(offer))
                .thenThrow(new IllegalStateException("check\r\nFORGED failed"));
        Logger logger = (Logger) LoggerFactory.getLogger(SellerReputationVerdict.class);
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            SellerReputationVerdict.find(sellersReputationService, offer);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN, Level.DEBUG);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> assertThat(message).doesNotContain("\n", "\r"));
    }
}
