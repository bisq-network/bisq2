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

package bisq.security;

import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleStore;
import bisq.security.keys.TempKeyBundleService;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class KeyBundleService {
    private final TempKeyBundleService tempKeyBundleService;

    public KeyBundleService(PersistenceService persistenceService) {
        tempKeyBundleService = new TempKeyBundleService(persistenceService);
    }

    public CompletableFuture<Boolean> initialize() {
        return tempKeyBundleService.initialize();
    }

    public Optional<KeyPair> findKeyPair(String keyId) {
        return tempKeyBundleService.findKeyBundle(keyId).map(KeyBundle::getKeyPair);
    }

    public KeyPair getOrCreateKeyPair(String keyId) {
        return tempKeyBundleService.getOrCreateKeyBundle(keyId).join().getKeyPair();
    }

    public KeyPair generateKeyPair() {
        return tempKeyBundleService.generateKeyPair();
    }

    public void persistKeyPair(String keyId, KeyPair keyPair) {
        tempKeyBundleService.createAndPersistKeyBundle(keyId, keyPair);
    }


    public String getKeyIdFromTag(String tag) {
        return tempKeyBundleService.getKeyIdFromTag(tag);
    }

    public String getDefaultKeyId() {
        return tempKeyBundleService.getDefaultKeyId();
    }

    public boolean isDefaultKeyId(String keyId) {
        return tempKeyBundleService.isDefaultKeyId(keyId);
    }

    public Persistence<KeyBundleStore> getPersistence() {
        return tempKeyBundleService.getPersistence();
    }
}
