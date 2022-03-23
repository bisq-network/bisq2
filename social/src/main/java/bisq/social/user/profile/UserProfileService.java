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

package bisq.social.user.profile;

import bisq.common.data.Pair;
import bisq.common.encoding.Hex;
import bisq.common.util.CollectionUtil;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import bisq.security.KeyPairService;
import bisq.security.SignatureUtil;
import bisq.social.user.BsqTxValidator;
import bisq.social.user.BtcTxValidator;
import bisq.social.user.Entitlement;
import bisq.social.user.UserNameGenerator;
import com.google.common.base.Preconditions;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static bisq.security.SignatureUtil.bitcoinSigToDer;
import static bisq.security.SignatureUtil.formatMessageForSigning;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore> {
    public static record Config(List<String> btcMempoolProviders,
                                List<String> bsqMempoolProviders) {
        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            List<String> btcMempoolProviders = typeSafeConfig.getStringList("btcMempoolProviders");
            List<String> bsqMempoolProviders = typeSafeConfig.getStringList("bsqMempoolProviders");
            return new UserProfileService.Config(btcMempoolProviders, bsqMempoolProviders);
        }
    }

    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final Config config;

    public UserProfileService(PersistenceService persistenceService,
                              Config config,
                              KeyPairService keyPairService,
                              IdentityService identityService,
                              NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.config = config;
        this.keyPairService = keyPairService;
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
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

   /* public CompletableFuture<Optional<? extends Entitlement.Proof>> verifyEntitlement(Entitlement.Type entitlementType,
                                                                                      String proofOfBurnTxId,
                                                                                      String bondedRoleTxId,
                                                                                      String bondedRoleSig,
                                                                                      String invitationCode,
                                                                                      PublicKey publicKey) {
        List<CompletableFuture<Optional<Entitlement.Proof>>> futures = new ArrayList<>();

        futures.addAll(entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.PROOF_OF_BURN)
                .map(e -> verifyProofOfBurn(proofOfBurnTxId, publicKey))
                .collect(Collectors.toList()));

        futures.addAll(entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.BONDED_ROLE)
                .map(e -> verifyBondedRole(bondedRoleTxId, bondedRoleSig, publicKey))
                .collect(Collectors.toList()));

        futures.addAll(entitlementType.getProofTypes().stream()
                .filter(e -> e == Entitlement.ProofType.CHANNEL_ADMIN_INVITATION)
                .map(e -> verifyInvitation(invitationCode, publicKey))
                .collect(Collectors.toList()));

        return CompletableFutureUtils.allOf(futures)
                .thenApply(list -> list.stream().filter(e->e.isPresent()).count()==3);
    }*/

    public CompletableFuture<Optional<Entitlement.Proof>> verifyProofOfBurn(Entitlement.Type type, String proofOfBurnTxId, String pubKeyHash) {
        return supplyAsync(() -> {
            try {
                BaseHttpClient httpClient = getApiHttpClient(config.bsqMempoolProviders());
                String jsonBsqTx = httpClient.get("tx/" + proofOfBurnTxId, Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                Preconditions.checkArgument(BsqTxValidator.initialSanityChecks(proofOfBurnTxId, jsonBsqTx), "bsq tx sanity checks");
                checkArgument(BsqTxValidator.isBsqTx(httpClient.getBaseUrl(), proofOfBurnTxId, jsonBsqTx), "isBsqTx");
                checkArgument(BsqTxValidator.isProofOfBurn(jsonBsqTx), "is proof of burn transaction");
                checkArgument(BsqTxValidator.getBurntAmount(jsonBsqTx) >= getMinBurnAmount(type), "insufficient burn");
                BsqTxValidator.getOpReturnData(jsonBsqTx).ifPresentOrElse(
                        (opReturn) -> checkArgument(pubKeyHash.equalsIgnoreCase(opReturn), "opReturnMatches"),
                        () -> {
                            throw new IllegalArgumentException("no opreturn found");
                        });
                return Optional.of(new Entitlement.ProofOfBurnProof(proofOfBurnTxId));
            } catch (IllegalArgumentException e) {
                log.warn("check failed: {}", e.getMessage(), e);
            } catch (IOException e) {
                log.warn("mempool query failed:", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<Entitlement.Proof>> verifyBondedRole(String txId, String signature, String pubKeyHash) {
        // inputs: txid, signature from Bisq1
        // process: get txinfo, grab pubkey from 1st output
        // verify provided signature matches with pubkey from 1st output and hash of provided identity pubkey
        return supplyAsync(() -> {
            try {
                BaseHttpClient httpClientBsq = getApiHttpClient(config.bsqMempoolProviders());
                BaseHttpClient httpClientBtc = getApiHttpClient(config.btcMempoolProviders());
                String jsonBsqTx = httpClientBsq.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBsq.userAgent)));
                String jsonBtcTx = httpClientBtc.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBtc.userAgent)));
                checkArgument(BsqTxValidator.initialSanityChecks(txId, jsonBsqTx), "bsq tx sanity checks");
                Preconditions.checkArgument(BtcTxValidator.initialSanityChecks(txId, jsonBtcTx), "btc tx sanity checks");
                checkArgument(BsqTxValidator.isBsqTx(httpClientBsq.getBaseUrl(), txId, jsonBsqTx), "isBsqTx");
                checkArgument(BsqTxValidator.isLockup(jsonBsqTx), "is lockup transaction");
                String signingPubkeyAsHex = BtcTxValidator.getFirstInputPubKey(jsonBtcTx);
                PublicKey signingPubKey = KeyGeneration.generatePublicFromCompressed(Hex.decode(signingPubkeyAsHex));
                boolean signatureMatches = SignatureUtil.verify(formatMessageForSigning(pubKeyHash), bitcoinSigToDer(signature), signingPubKey);
                checkArgument(signatureMatches, "signature");
                return Optional.of(new Entitlement.BondedRoleProof(txId, signature));
            } catch (IllegalArgumentException e) {
                log.warn("check failed: {}", e.getMessage(), e);
            } catch (IOException e) {
                log.warn("mempool query failed:", e);
            } catch (GeneralSecurityException e) {
                log.warn("signature validation failed:", e);
            } catch (JsonSyntaxException e) {
                log.warn("json decoding failed:", e);
            } catch (NullPointerException e) {
                log.error("unexpected failure:", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<Entitlement.Proof>> verifyModerator(String invitationCode, PublicKey publicKey) {
        //todo
        return CompletableFuture.completedFuture(Optional.of(new Entitlement.ChannelAdminInvitationProof(invitationCode)));
    }

    private CompletableFuture<Boolean> createDefaultUserProfile() {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.generateKeyPair();
        byte[] pubKeyBytes = keyPair.getPublic().getEncoded();
        byte[] hash = DigestUtil.hash(pubKeyBytes);
        String useName = UserNameGenerator.fromHash(hash);
        return createNewInitializedUserProfile(useName, keyId, keyPair, new HashSet<>())
                .thenApply(userProfile -> true);
    }

    //todo work out concept how to adjust those values
    public long getMinBurnAmount(Entitlement.Type type) {
        return switch (type) {
            case LIQUIDITY_PROVIDER -> 5000;
            case CHANNEL_MODERATOR -> 10000;
            default -> 0;
        };
    }

    private BaseHttpClient getApiHttpClient(List<String> providerUrls) {
        String userAgent = "Bisq 2";
        String url = CollectionUtil.getRandomElement(providerUrls);
        Set<Transport.Type> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Transport.Type transportType;
        if (supportedTransportTypes.contains(Transport.Type.CLEAR)) {
            transportType = Transport.Type.CLEAR;
        } else if (supportedTransportTypes.contains(Transport.Type.TOR)) {
            transportType = Transport.Type.TOR;
        } else {
            throw new RuntimeException("I2P is not supported yet");
        }
        return networkService.getHttpClient(url, userAgent, transportType);
    }
}
