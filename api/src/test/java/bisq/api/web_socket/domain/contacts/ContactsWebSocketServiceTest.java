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

package bisq.api.web_socket.domain.contacts;

import bisq.api.dto.user.contact_list.ContactListEntryDto;
import bisq.api.dto.user.contact_list.ContactReasonDto;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.Topic;
import bisq.common.json.JsonMapperProvider;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContactsWebSocketServiceTest {
    private final ObservableSet<ContactListEntry> entries = new ObservableSet<>();
    private final Observable<Long> edits = new Observable<>(0L);
    private final SubscriberRepository subscriberRepository = new SubscriberRepository();
    private ContactsWebSocketService service;

    @BeforeEach
    void setUp() {
        ContactListService contactListService = mock(ContactListService.class);
        when(contactListService.getContactListEntries()).thenReturn(entries);
        when(contactListService.getNumContactListEntryEdits()).thenReturn(edits);
        // Same-thread executor so pushes are observable synchronously. Production uses a dedicated
        // push thread, because the set's observers fire inside the store's synchronized add/remove.
        service = new ContactsWebSocketService(subscriberRepository, contactListService, new SameThreadExecutorService());
    }

    @Test
    void payloadCarriesTheAnnotationsAndSortsOldestFirst() throws Exception {
        entries.add(entry("younger", 2000L, Optional.of("friend"), Optional.of("note"), Optional.of(0.5)));
        entries.add(entry("older", 1000L, Optional.empty(), Optional.empty(), Optional.empty()));

        String json = service.getJsonPayload().orElseThrow();

        List<ContactListEntryDto> payload = JsonMapperProvider.get().readValue(json, new TypeReference<>() {
        });
        assertThat(payload).extracting(dto -> dto.userProfile().nickName()).containsExactly("older", "younger");
        ContactListEntryDto younger = payload.get(1);
        assertThat(younger.contactReason()).isEqualTo(ContactReasonDto.MANUALLY_ADDED);
        assertThat(younger.tag()).isEqualTo("friend");
        assertThat(younger.notes()).isEqualTo("note");
        assertThat(younger.trustScore()).isEqualTo(0.5);
        ContactListEntryDto older = payload.get(0);
        assertThat(older.tag()).isNull();
        assertThat(older.notes()).isNull();
        assertThat(older.trustScore()).isNull();
    }

    /**
     * The wire deliberately omits {@code myUserProfile}: a client renders the contact, not which of
     * the owner's identities added it — and the identity's profile has no business on the wire.
     */
    @Test
    void payloadDoesNotLeakMyUserProfile() throws Exception {
        entries.add(entry("peer", 1000L, Optional.empty(), Optional.empty(), Optional.empty()));

        String json = service.getJsonPayload().orElseThrow();

        assertThat(json).doesNotContain("myUserProfile");
    }

    /**
     * Both change signals must be wired: the set fires for add/remove, while tag/notes/trustScore
     * edits mutate an entry in place and only tick {@code numContactListEntryEdits} — an unwired
     * edit signal would strand annotation edits on every other paired device. And the composite pin
     * must unbind both, or the service keeps pushing after shutdown.
     */
    @Test
    void bothChangeSignalsPushTheFullListAsReplaceUntilShutdown() throws Exception {
        List<String> sent = Collections.synchronizedList(new ArrayList<>());
        WebSocket webSocket = mock(WebSocket.class);
        when(webSocket.send(anyString())).thenAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        });
        SubscriptionRequest request = mock(SubscriptionRequest.class);
        when(request.getTopic()).thenReturn(Topic.CONTACTS);
        when(request.getRequestId()).thenReturn("sub-1");
        subscriberRepository.add(request, Optional.empty(), webSocket);
        service.initialize().join();
        // Both observers fire once on registration, pushing the (empty) baseline.
        awaitSends(sent, 2);
        sent.clear();

        entries.add(entry("added", 1000L, Optional.empty(), Optional.empty(), Optional.empty()));
        awaitSends(sent, 1);
        edits.set(edits.get() + 1);
        awaitSends(sent, 2);
        assertThat(sent).allMatch(json -> json.contains("\"REPLACE\""),
                "every push is the authoritative full list");

        service.shutdown().join();
        entries.add(entry("late", 2000L, Optional.empty(), Optional.empty(), Optional.empty()));
        edits.set(edits.get() + 1);
        // Both observers fire synchronously into the (now unbound) pins, so nothing new can have
        // been queued; any extra send would already be visible or in the subscriber executor.
        Thread.sleep(50);
        assertThat(sent).hasSize(2);
    }

    /** The subscriber executor delivers asynchronously, so pushes are awaited with a deadline. */
    private static void awaitSends(List<String> sent, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (sent.size() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertThat(sent).hasSize(expected);
    }

    private static class SameThreadExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }
    }

    private ContactListEntry entry(String nickName,
                                   long date,
                                   Optional<String> tag,
                                   Optional<String> notes,
                                   Optional<Double> trustScore) {
        return new ContactListEntry(mockUserProfile(nickName),
                mockUserProfile("me"),
                date,
                ContactReason.MANUALLY_ADDED,
                trustScore,
                tag,
                notes);
    }

    /**
     * Deep stub plus the byte[] getters the dto mapping digests — a stubbed byte[] getter hands back
     * null and the mapping NPEs; see {@code PrivateChatTestMocks#mockUserProfile}.
     */
    private UserProfile mockUserProfile(String nickName) {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getNickName()).thenReturn(nickName);
        when(profile.getId()).thenReturn(nickName + "-id");
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }
}
