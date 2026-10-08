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

package bisq.support.moderator;

import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModeratorServiceContactUserTest {
    private static final String REPORTER_PROFILE_ID = "a".repeat(40);

    private TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
    private TwoPartyPrivateChatChannel channel;
    private UserProfile reporter;
    private ModeratorService moderatorService;

    @BeforeEach
    void setUp() {
        Res.setAndApplyLanguageTag("en");

        twoPartyPrivateChatChannelService = mock(TwoPartyPrivateChatChannelService.class);
        channel = mock(TwoPartyPrivateChatChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>());
        when(twoPartyPrivateChatChannelService.findOrCreateChannel(eq(ChatChannelDomain.DISCUSSION), any()))
                .thenReturn(Optional.of(channel));
        when(twoPartyPrivateChatChannelService.sendTextMessage(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendMessageResult()));

        ChatService chatService = mock(ChatService.class);
        when(chatService.getTwoPartyPrivateChatChannelService()).thenReturn(twoPartyPrivateChatChannelService);
        when(chatService.getChatChannelSelectionServices())
                .thenReturn(Map.of(ChatChannelDomain.DISCUSSION, mock(ChatChannelSelectionService.class)));

        reporter = mock(UserProfile.class);
        when(reporter.getId()).thenReturn(REPORTER_PROFILE_ID);

        // The persistence client's shutdown hook reads the store path, so it must not be null
        Persistence<?> persistence = mock(Persistence.class);
        when(persistence.getStorePath()).thenReturn(Path.of("moderator-test"));
        PersistenceService persistenceService = mock(PersistenceService.class);
        doReturn(persistence).when(persistenceService).getOrCreatePersistence(any(), any(DbSubDirectory.class), any());

        moderatorService = new ModeratorService(new ModeratorService.Config(false),
                persistenceService,
                mock(NetworkService.class),
                mock(UserService.class),
                mock(BondedRolesService.class),
                chatService);
    }

    @Test
    void contactingTheReporterOfALongReportSendsATruncatedCitation() {
        String longReport = "x".repeat(ReportToModeratorMessage.MAX_MESSAGE_LENGTH);

        moderatorService.contactUser(reporter, Optional.of(longReport), true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Optional<Citation>> citation = ArgumentCaptor.forClass(Optional.class);
        verify(twoPartyPrivateChatChannelService).sendTextMessage(anyString(), citation.capture(), eq(channel));
        assertThat(citation.getValue()).isPresent();
        assertThat(citation.getValue().get().getText()).hasSize(Citation.MAX_TEXT_LENGTH);
        assertThat(longReport).startsWith(citation.getValue().get().getText().substring(0, Citation.MAX_TEXT_LENGTH - 1));
    }

    @Test
    void contactingTheReporterOfAShortReportCitesItUnchanged() {
        String shortReport = "spam in the offerbook";

        moderatorService.contactUser(reporter, Optional.of(shortReport), true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Optional<Citation>> citation = ArgumentCaptor.forClass(Optional.class);
        verify(twoPartyPrivateChatChannelService).sendTextMessage(anyString(), citation.capture(), eq(channel));
        assertThat(citation.getValue()).map(Citation::getText).contains(shortReport);
    }
}
