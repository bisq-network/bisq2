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

package network.misq.network.p2p.services.data.storage.auth;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.encoding.Hex;
import network.misq.common.util.OsUtils;
import network.misq.network.p2p.services.data.NetworkData;
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.security.DigestUtil;
import network.misq.security.KeyGeneration;
import network.misq.security.SignatureUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
public class AuthorizedDataStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAddAndRemove() throws GeneralSecurityException, IOException {
        String publicKeyAsHex = "3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63";
        byte[] publicKeyBytes = Hex.decode(publicKeyAsHex);
        PublicKey publicKey = KeyGeneration.generatePublic(publicKeyBytes);

        String privateKeyAsHex = "30818d020100301006072a8648ce3d020106052b8104000a04763074020101042010c2ea3b2b1f1787f8a57d074e550b120cc04b326b43c545214434e474e5cde2a00706052b8104000aa14403420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63";
        byte[] privateKeyBytes = Hex.decode(privateKeyAsHex);

        PrivateKey privateKey = KeyGeneration.generatePrivate(privateKeyBytes);
        NetworkData networkData = new MockNetworkData("test" + UUID.randomUUID());
        byte[] signature = SignatureUtil.sign(networkData.serialize(), privateKey);
        MockAuthorizedPayload authorizedPayload = new MockAuthorizedPayload(networkData, signature, publicKey);

        KeyPair keyPair = KeyGeneration.generateKeyPair();
        AuthenticatedDataStore store = new AuthenticatedDataStore(appDirPath, authorizedPayload.getMetaData());
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authorizedPayload, keyPair);
        byte[] hash = DigestUtil.hash(authorizedPayload.serialize());
        int initialSeqNum = store.getSequenceNumber(hash);
        Result result = store.add(addRequest);
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = store.getMap();
        MapKey mapKey = new MapKey(hash);
        AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        AuthenticatedData dataFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, dataFromMap.getSequenceNumber());
        MockAuthorizedPayload payload = (MockAuthorizedPayload) dataFromMap.getPayload();
        assertEquals(payload.getNetworkData(), networkData);

        // refresh
        RefreshRequest refreshRequest = RefreshRequest.from(store, authorizedPayload, keyPair);
        Result refreshResult = store.refresh(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        dataFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, dataFromMap.getSequenceNumber());

        //remove
        RemoveRequest removeRequest = RemoveRequest.from(store, authorizedPayload, keyPair);
        Result removeDataResult = store.remove(removeRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());
    }
}
