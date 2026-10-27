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

package bisq.api.offerbook;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferVisibilityTest {
    private final BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final UserIdentityService userIdentityService = mock(UserIdentityService.class);
    private final BisqEasyOffer offer = mock(BisqEasyOffer.class);
    private final BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);

    OfferVisibilityTest() {
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(message.getAuthorUserProfileId()).thenReturn("maker-id");
        when(userProfileService.findUserProfile("maker-id")).thenReturn(Optional.of(mock(UserProfile.class)));
    }

    // isValid already checks the reputation of a sell offer
    @Test
    void validOfferIsShownWithATrueVerdict() {
        when(messageService.isValid(message)).thenReturn(true);

        assertThat(find()).contains(new OfferVisibility(true, true, true));
        verify(messageService, never()).hasSellerSufficientReputation(any());
    }

    // Like the desktop offerbook; the verdict of a hidden offer is not checked
    @Test
    void invalidOfferOfAnotherMakerIsHidden() {
        when(messageService.isValid(message)).thenReturn(false);

        assertThat(find()).map(OfferVisibility::shown).contains(false);
        verify(messageService, never()).hasSellerSufficientReputation(any());
    }

    // The app warns its user, so an own offer is shown also when it is invalid
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void invalidOwnOfferIsShownWithTheVerdictOfTheService(boolean hasSellerSufficientReputation) {
        when(messageService.isValid(message)).thenReturn(false);
        when(message.isMyMessage(any())).thenReturn(true);
        when(messageService.hasSellerSufficientReputation(message)).thenReturn(hasSellerSufficientReputation);

        assertThat(find()).contains(new OfferVisibility(true, false, hasSellerSufficientReputation));
    }

    // Without the profile no item can be built, and the check would cache the offer as insufficient with a score of 0
    @Test
    void offerWithoutMakerProfileGivesNoVisibilityWithoutACheck() {
        when(userProfileService.findUserProfile("maker-id")).thenReturn(Optional.empty());
        when(messageService.isNotBanned(message)).thenReturn(true);
        when(messageService.isNotIgnored(message)).thenReturn(true);

        assertThat(find()).isEmpty();
        verify(messageService, never()).isValid(any());
        verify(messageService, never()).hasSellerSufficientReputation(any());
    }

    // The ban and ignore checks need no profile
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bannedOrIgnoredMakerWithoutProfileIsHidden(boolean isBanned) {
        when(userProfileService.findUserProfile("maker-id")).thenReturn(Optional.empty());
        when(messageService.isNotBanned(message)).thenReturn(!isBanned);
        when(messageService.isNotIgnored(message)).thenReturn(isBanned);

        assertThat(find()).map(OfferVisibility::shown).contains(false);
        verify(messageService, never()).isValid(any());
    }

    // An own offer is never hidden, with or without the profile
    @Test
    void ownOfferWithoutMakerProfileGivesNoVisibility() {
        when(userProfileService.findUserProfile("maker-id")).thenReturn(Optional.empty());
        when(messageService.isNotBanned(message)).thenReturn(false);
        when(messageService.isNotIgnored(message)).thenReturn(false);
        when(message.isMyMessage(any())).thenReturn(true);

        assertThat(find()).isEmpty();
        verify(messageService, never()).isValid(any());
    }

    // A failing check repeats on every request and revision, so its warning carries no stack trace
    @Test
    void failingCheckGivesNoVisibilityAndAShortWarning() {
        when(offer.getId()).thenReturn("offer-id");
        when(messageService.isValid(message)).thenThrow(new IllegalStateException("check failed"));
        Logger logger = (Logger) LoggerFactory.getLogger(OfferVisibility.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        Optional<OfferVisibility> visibility;
        try {
            visibility = find();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(visibility).isEmpty();
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
        when(messageService.isValid(message)).thenThrow(new IllegalStateException("check\r\nFORGED failed"));
        Logger logger = (Logger) LoggerFactory.getLogger(OfferVisibility.class);
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            find();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN, Level.DEBUG);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(logMessage -> assertThat(logMessage).doesNotContain("\n", "\r"));
    }

    private Optional<OfferVisibility> find() {
        return OfferVisibility.find(messageService, userProfileService, userIdentityService, message);
    }
}
