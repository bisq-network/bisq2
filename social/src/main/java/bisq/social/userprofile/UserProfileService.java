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

package bisq.social.userprofile;


import bisq.common.data.Pair;
import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore> {
    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Object lock = new Object();

    public UserProfileService(PersistenceService persistenceService,
                              KeyPairService keyPairService,
                              IdentityService identityService,
                              NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyPairService = keyPairService;
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        if (persistableStore.getUserProfiles().isEmpty()) {
            return createDefaultUserProfile();
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<UserProfile> createNewInitializedUserProfile(String domainId,
                                                                          String keyId,
                                                                          KeyPair keyPair,
                                                                          Set<Entitlement> entitlements) {
        return identityService.createNewInitializedIdentity(domainId, keyId, keyPair)
                .thenApply(identity -> {
                    UserProfile userProfile = new UserProfile(identity, entitlements);
                    synchronized (lock) {
                        persistableStore.getUserProfiles().add(userProfile);
                        persistableStore.getSelectedUserProfile().set(userProfile);
                    }
                    persist();
                    return userProfile;
                });
    }

    public void selectUserProfile(UserProfile value) {
        persistableStore.getSelectedUserProfile().set(value);
        persist();
    }

    public CompletableFuture<Boolean> verifyEntitlement(Entitlement.Type entitlementType, String txId, PublicKey publicKey) {
        Stream<CompletableFuture<Boolean>> proofOfBurnFutures = entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.PROOF_OF_BURN)
                .map(e -> verifyProofOfBurn(txId, publicKey));
        Stream<CompletableFuture<Boolean>> bondedRoleFutures = entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.BONDED_ROLE)
                .map(e -> verifyBondedRole(txId, publicKey));
        Stream<CompletableFuture<Boolean>> invitationFutures = entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.CHANNEL_ADMIN_INVITATION)
                .map(e -> verifyInvitation(txId, publicKey));
        return CompletableFutureUtils.allOf(Stream.concat(proofOfBurnFutures,
                        Stream.concat(bondedRoleFutures, invitationFutures)))
                .thenApply(list -> list.stream().allMatch(success -> success));
    }

    private CompletableFuture<Boolean> verifyProofOfBurn(String txId, PublicKey publicKey) {
        // todo impl
        //  https://github.com/bisq-network/bisq2/issues/68
        // 
        String userAgent = "Bisq 2";
        //todo add providers to Bisq.config
        List<String> providers = List.of("https://bisq.mempool.emzy.de/api/tx/",
                "https://mempool.space/bisq/api/tx/",
                "https://mempool.bisq.services/bisq/api/tx/");
        String url = CollectionUtil.getRandomElement(providers);
        Set<Transport.Type> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Transport.Type transportType;
        if (supportedTransportTypes.contains(Transport.Type.CLEAR)) {
            transportType = Transport.Type.CLEAR;
        } else if (supportedTransportTypes.contains(Transport.Type.TOR)) {
            transportType = Transport.Type.TOR;
        } else {
            throw new RuntimeException("I2P is not supported yet");
        }
        BaseHttpClient httpClient = networkService.getHttpClient(url, userAgent, transportType);
        return supplyAsync(() -> {
            try {
                String json = httpClient.get(txId, Optional.of(new Pair<>("User-Agent", userAgent)));
                //todo parse json 
                boolean isBsqTx = true; //todo
                boolean isProofOfBurn = true; //todo
                byte[] publicKeyHash = DigestUtil.hash(publicKey.getEncoded());
                byte[] opReturn = publicKeyHash; //todo
                return isBsqTx && isProofOfBurn && Arrays.equals(publicKeyHash, opReturn);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    // result for proof of burn tx id 3bbb2f597e714257d4a2f573e9ebfff4ab631277186a40875dbbf4140e90b748
    // 
                    /*
                    {"txid":"3bbb2f597e714257d4a2f573e9ebfff4ab631277186a40875dbbf4140e90b748","version":1,"locktime":0,"vin":[{"txid":"706d78ae02013837ca1ea83b2ac7cae2481a1a3de2c0a1401e78d4736ac6505b","vout":1,"prevout":{"scriptpubkey":"76a91499898c703b6c37d9d753883056bca076e430293d88ac","scriptpubkey_asm":"OP_DUP OP_HASH160 OP_PUSHBYTES_20 99898c703b6c37d9d753883056bca076e430293d OP_EQUALVERIFY OP_CHECKSIG","scriptpubkey_type":"p2pkh","scriptpubkey_address":"1EzqAPpc7MqTncf6atHMMfCS4UBX8vwzvB","value":1133333},"scriptsig":"473044022030e579d8282f15743824ab44aa43baf44b865c8bc9b0098115c59cd44a4a6f8d02205b4152057fab450e23ab65918267ef7d0f84948621a122ed3093b58e8c2dcd2c01210285f726704fd47100df1921f47bda36d266d31a38040bde3b2ffc0639b069bda6","scriptsig_asm":"OP_PUSHBYTES_71 3044022030e579d8282f15743824ab44aa43baf44b865c8bc9b0098115c59cd44a4a6f8d02205b4152057fab450e23ab65918267ef7d0f84948621a122ed3093b58e8c2dcd2c01 OP_PUSHBYTES_33 0285f726704fd47100df1921f47bda36d266d31a38040bde3b2ffc0639b069bda6","is_coinbase":false,"sequence":4294967295},{"txid":"706d78ae02013837ca1ea83b2ac7cae2481a1a3de2c0a1401e78d4736ac6505b","vout":2,"prevout":{"scriptpubkey":"76a914754b3c2861c4a28fbf993bf938c70b1429cb58b188ac","scriptpubkey_asm":"OP_DUP OP_HASH160 OP_PUSHBYTES_20 754b3c2861c4a28fbf993bf938c70b1429cb58b1 OP_EQUALVERIFY OP_CHECKSIG","scriptpubkey_type":"p2pkh","scriptpubkey_address":"1BhCBs46WpMkG9vYxyma9eiT5BMPJrbEiK","value":52393250},"scriptsig":"483045022100ea0aa77e1e58005996c7c62f08b933271c33c0c0d8ff5b0e6fdc171338d0ec5f022023ea101475313227165d0a4791273baabea5e2690cce5467c78307ec9a9d31180121029f0e29b99d820af0c0cd96bebea8a330d95b041eb3049438eec1b6e26bb5e330","scriptsig_asm":"OP_PUSHBYTES_72 3045022100ea0aa77e1e58005996c7c62f08b933271c33c0c0d8ff5b0e6fdc171338d0ec5f022023ea101475313227165d0a4791273baabea5e2690cce5467c78307ec9a9d311801 OP_PUSHBYTES_33 029f0e29b99d820af0c0cd96bebea8a330d95b041eb3049438eec1b6e26bb5e330","is_coinbase":false,"sequence":4294967295}],"vout":[{"scriptpubkey":"76a914926ed5b7088bfb50bf2a28ee782f92c76f1190fe88ac","scriptpubkey_asm":"OP_DUP OP_HASH160 OP_PUSHBYTES_20 926ed5b7088bfb50bf2a28ee782f92c76f1190fe OP_EQUALVERIFY OP_CHECKSIG","scriptpubkey_type":"p2pkh","scriptpubkey_address":"1EMGRwrmJ5EN95H5aXkjr9fG3HjnPAuM2A","value":1132333},{"scriptpubkey":"76a9140e54b44d63fa16782f11c2fe9ff580d3718d183a88ac","scriptpubkey_asm":"OP_DUP OP_HASH160 OP_PUSHBYTES_20 0e54b44d63fa16782f11c2fe9ff580d3718d183a OP_EQUALVERIFY OP_CHECKSIG","scriptpubkey_type":"p2pkh","scriptpubkey_address":"12Jmw3YwGCn83uZBzKSb5jnJWxcfaK2FnX","value":52390190},{"scriptpubkey":"6a161701544b4337cd600cad892f12383525087bebc8b591","scriptpubkey_asm":"OP_RETURN OP_PUSHBYTES_22 1701544b4337cd600cad892f12383525087bebc8b591","scriptpubkey_type":"op_return","value":0}],"size":406,"weight":1624,"fee":4060,"status":{"confirmed":true,"block_height":613546,"block_hash":"0000000000000000000496cd14e563bb739f09da09ffe42297e35a3be133383c","block_time":1579438021}}
                     */

    private CompletableFuture<Boolean> verifyBondedRole(String txId, PublicKey publicKey) {
        //todo
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> verifyInvitation(String txId, PublicKey publicKey) {
        //todo
        return CompletableFuture.completedFuture(true);
    }


    private CompletableFuture<Boolean> createDefaultUserProfile() {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.generateKeyPair();
        String useName = "Satoshi";
        return createNewInitializedUserProfile(useName, keyId, keyPair, new HashSet<>())
                .thenApply(userProfile -> true);
    }
}
