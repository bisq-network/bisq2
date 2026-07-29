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

package bisq.wallet.receive_address;

import bisq.common.application.Service;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;

import java.util.Optional;

public class ReceiveAddressService implements Service, PersistenceClient<ReceiveAddressStore> {
    public final static int RECEIVE_ADDRESS_ENTRY_NAME_MIN_LENGTH = 1;
    public final static int RECEIVE_ADDRESS_ENTRY_NAME_MAX_LENGTH = 20;

    @Getter
    private final ReceiveAddressStore persistableStore = new ReceiveAddressStore();
    @Getter
    private final Persistence<ReceiveAddressStore> persistence;

    public ReceiveAddressService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    public ReadOnlyObservableSet<ReceiveAddressEntry> getReceiveAddressEntries() {
        return persistableStore.getReceiveAddressEntries();
    }

    public boolean addReceiveAddressEntry(ReceiveAddressEntry receiveAddressEntry) {
        boolean wasAdded = persistableStore.addReceiveAddressEntry(receiveAddressEntry);
        if (wasAdded) {
            persist();
        }
        return wasAdded;
    }

    public boolean updateReceiveAddressEntry(ReceiveAddressEntry receiveAddressEntry,
                                             Optional<String> name) {
        boolean wasUpdated = false;
        Optional<ReceiveAddressEntry> receiveAddress = findReceiveAddressEntry(receiveAddressEntry);
        if (receiveAddress.isPresent() && name.isPresent() && isNameValid(name.get())) {
            ReceiveAddressEntry updatedEntry = new ReceiveAddressEntry(receiveAddressEntry.getAddress(), receiveAddressEntry.getCreatedAt(), name);
            wasUpdated = persistableStore.updateReceiveAddressEntry(updatedEntry);
            if (wasUpdated) {
                persist();
            }
        }
        return wasUpdated;
    }

    public Optional<ReceiveAddressEntry> findReceiveAddressEntry(ReceiveAddressEntry receiveAddressEntry) {
        return persistableStore.getReceiveAddressEntries().stream()
                .filter(entry -> receiveAddressEntry.getAddress().equals(entry.getAddress()))
                .findAny();
    }

    private boolean isNameValid(String name) {
        return name.length() >= RECEIVE_ADDRESS_ENTRY_NAME_MIN_LENGTH
                && name.length() <= RECEIVE_ADDRESS_ENTRY_NAME_MAX_LENGTH;
    }
}
