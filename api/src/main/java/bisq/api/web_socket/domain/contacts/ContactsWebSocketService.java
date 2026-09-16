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

import bisq.api.dto.mappings.user.contact_list.ContactListEntryDtoMapping;
import bisq.api.dto.user.contact_list.ContactListEntryDto;
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pushes the node owner's contact list as a full-list REPLACE on every change — the list is small
 * (a person's curated contacts), so diffing buys nothing over the simple full snapshot, and REPLACE
 * is authoritative about absence, which covers add, remove and edit with one client-side path.
 * <p>
 * Two observers, because the domain has two change signals: the observable set fires for add and
 * remove, while tag/notes/trustScore edits mutate an entry in place and only tick
 * {@code numContactListEntryEdits} (see {@link ContactListService}).
 * <p>
 * Pushes run on their own single-threaded executor, never on the notifying thread: the set's
 * observers are notified synchronously from inside {@code ContactListStore}'s synchronized
 * add/remove methods, so building and sending the payload inline would hold that store's monitor
 * across JSON serialization and subscriber I/O, blocking every concurrent store access for the
 * push's duration. Bursts coalesce into one push, which is safe because the payload is built from
 * the live set when the task runs, not when it is queued.
 */
@Slf4j
public class ContactsWebSocketService extends SimpleObservableWebSocketService<ReadOnlyObservableSet<ContactListEntry>, List<ContactListEntryDto>> {
    private final ContactListService contactListService;
    private final ExecutorService pushExecutor;
    private final AtomicBoolean pushPending = new AtomicBoolean();

    public ContactsWebSocketService(SubscriberRepository subscriberRepository,
                                    ContactListService contactListService) {
        this(subscriberRepository, contactListService, ExecutorFactory.newSingleThreadExecutor("Contacts-push"));
    }

    /** Visible for testing, which passes a same-thread executor so pushes are observable without draining. */
    ContactsWebSocketService(SubscriberRepository subscriberRepository,
                             ContactListService contactListService,
                             ExecutorService pushExecutor) {
        super(subscriberRepository, Topic.CONTACTS);
        this.contactListService = contactListService;
        this.pushExecutor = pushExecutor;
    }

    @Override
    protected Pin setupObserver() {
        Pin entriesPin = contactListService.getContactListEntries().addObserver(this::schedulePush);
        Pin editsPin = contactListService.getNumContactListEntryEdits().addObserver(edit -> schedulePush());
        return () -> {
            entriesPin.unbind();
            editsPin.unbind();
        };
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // Unbinds first (super), so no new work can be queued behind the drain.
        return super.shutdown()
                .whenComplete((result, throwable) -> ExecutorFactory.shutdownAndAwaitTermination(pushExecutor));
    }

    @Override
    protected List<ContactListEntryDto> toPayload(ReadOnlyObservableSet<ContactListEntry> observable) {
        // Stable order (oldest first, profile id as tiebreaker), so identical states serialize
        // identically and clients render without their own sort.
        return observable.stream()
                .sorted(Comparator.comparingLong(ContactListEntry::getDate)
                        .thenComparing(entry -> entry.getUserProfile().getId()))
                .map(ContactListEntryDtoMapping::fromBisq2Model)
                .toList();
    }

    @Override
    protected ReadOnlyObservableSet<ContactListEntry> getObservable() {
        return contactListService.getContactListEntries();
    }

    /**
     * Cleared at the START of the task, not the end: a change arriving while a push is in flight
     * must be able to queue its own — clearing at the end would collapse it into the run that
     * already built its payload, and it would never be sent.
     */
    private void schedulePush() {
        if (!pushPending.compareAndSet(false, true)) {
            return;
        }
        try {
            pushExecutor.execute(() -> {
                pushPending.set(false);
                onChange();
            });
        } catch (RejectedExecutionException e) {
            // Shutdown raced the observer; the task that would have cleared the flag will never
            // run, and leaving it set would suppress nothing that matters anymore.
            pushPending.set(false);
        }
    }
}
