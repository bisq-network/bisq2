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

package bisq.social.user;

import bisq.common.data.ByteArray;
import bisq.common.data.Pair;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.http.common.HttpException;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.DataService;
import bisq.oracle.ots.OpenTimestampService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import bisq.security.KeyPairService;
import bisq.security.SignatureUtil;
import bisq.security.pow.ProofOfWork;
import bisq.social.user.role.Role;
import bisq.social.user.proof.*;
import bisq.social.user.reputation.Reputation;
import bisq.social.user.role.Role;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.security.SignatureUtil.bitcoinSigToDer;
import static bisq.security.SignatureUtil.formatMessageForSigning;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class ChatUserService implements PersistenceClient<ChatUserStore> {
    // For dev testing we use hard coded txId and a pubkeyhash to get real data from Bisq explorer
    private static final boolean USE_DEV_TEST_POB_VALUES = true;

    public static record Config(List<String> btcMemPoolProviders,
                                List<String> bsqMemPoolProviders) {
        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            List<String> btcMemPoolProviders = typeSafeConfig.getStringList("btcMemPoolProviders");
            List<String> bsqMemPoolProviders = typeSafeConfig.getStringList("bsqMemPoolProviders");
            return new ChatUserService.Config(btcMemPoolProviders, bsqMemPoolProviders);
        }
    }

    @Getter
    private final ChatUserStore persistableStore = new ChatUserStore();
    @Getter
    private final Persistence<ChatUserStore> persistence;
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final Config config;
    private final OpenTimestampService openTimestampService;
    private final Map<String, Long> publishTimeByChatUserId = new ConcurrentHashMap<>();

    public ChatUserService(PersistenceService persistenceService,
                           Config config,
                           KeyPairService keyPairService,
                           IdentityService identityService,
                           OpenTimestampService openTimestampService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.config = config;
        this.openTimestampService = openTimestampService;
        this.keyPairService = keyPairService;
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<ChatUserIdentity> createNewInitializedUserProfile(String profileId,
                                                                               String nickName,
                                                                               String keyId,
                                                                               KeyPair keyPair,
                                                                               ProofOfWork proofOfWork) {
        return createNewInitializedUserProfile(profileId, nickName, keyId, keyPair, proofOfWork, new HashSet<>(), new HashSet<>());
    }

    public CompletableFuture<ChatUserIdentity> createNewInitializedUserProfile(String profileId,
                                                                               String nickName,
                                                                               String keyId,
                                                                               KeyPair keyPair,
                                                                               ProofOfWork proofOfWork,
                                                                               Set<Reputation> reputation,
                                                                               Set<Role> roles) {
        return identityService.createNewInitializedIdentity(profileId, keyId, keyPair, proofOfWork)
                .thenApply(identity -> {
                    ChatUser chatUser = new ChatUser(nickName, proofOfWork, identity.getNodeIdAndKeyPair().networkId(), reputation, roles);
                    ChatUserIdentity chatUserIdentity = new ChatUserIdentity(identity, chatUser);
                    synchronized (lock) {
                        persistableStore.getChatUserIdentities().add(chatUserIdentity);
                        persistableStore.getSelectedChatUserIdentity().set(chatUserIdentity);
                    }
                    persist();
                    return chatUserIdentity;
                })
                .thenApply(chatUserIdentity -> {
                    // We don't wait for the reply but start an async task
                    CompletableFuture.runAsync(() -> openTimestampService.maybeCreateOrUpgradeTimestamp(
                                    new ByteArray(chatUserIdentity.getIdentity().pubKey().hash())),
                            ExecutorFactory.newSingleThreadExecutor("Request-timestamp"));
                    return chatUserIdentity;
                });
    }

    public void selectUserProfile(ChatUserIdentity value) {
        persistableStore.getSelectedChatUserIdentity().set(value);
        persist();
    }


    public CompletableFuture<Boolean> maybePublishChatUser(ChatUser chatUser, Identity identity) {
        String chatUserId = chatUser.getId();
        long currentTimeMillis = System.currentTimeMillis();
        if (!publishTimeByChatUserId.containsKey(chatUserId)) {
            publishTimeByChatUserId.put(chatUserId, currentTimeMillis);
        }
        long passed = currentTimeMillis - publishTimeByChatUserId.get(chatUserId);
        if (passed == 0 || passed > TimeUnit.HOURS.toMillis(5)) {
            return publishChatUser(chatUser, identity).thenApply(r -> true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<ChatUserIdentity> publishNewChatUser(ChatUserIdentity chatUserIdentity) {
        ChatUser chatUser = chatUserIdentity.getChatUser();
        Identity identity = chatUserIdentity.getIdentity();
        String chatUserId = chatUser.getId();
        long currentTimeMillis = System.currentTimeMillis();
        if (!publishTimeByChatuserId.containsKey(chatUserId)) {
            publishTimeByChatuserId.put(chatUserId, currentTimeMillis);
        }
        return publishChatUser(chatUser, identity).thenApply(r -> chatUserIdentity);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishChatUser(ChatUser chatUser, Identity identity) {
        return networkService.publishAuthenticatedData(chatUser, identity.getNodeIdAndKeyPair());
    }

    public boolean isDefaultUserProfileMissing() {
        return persistableStore.getChatUserIdentities().isEmpty();
    }

    public CompletableFuture<Optional<ChatUser.BurnInfo>> findBurnInfoAsync(byte[] pubKeyHash, Set<Role> roles) {
        if (roles.stream().noneMatch(e -> e.type() == Role.Type.LIQUIDITY_PROVIDER)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFutureUtils.allOf(roles.stream()
                        .filter(e -> e.proof() instanceof ProofOfBurnProof)
                        .map(e -> (ProofOfBurnProof) e.proof())
                        .map(proof -> verifyProofOfBurn(getMinBurnAmount(Role.Type.LIQUIDITY_PROVIDER), proof.txId(), pubKeyHash))
                )
                .thenApply(list -> {
                    List<ProofOfBurnProof> proofs = list.stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(Comparator.comparingLong(ProofOfBurnProof::date))
                            .collect(Collectors.toList());
                    if (proofs.isEmpty()) {
                        return Optional.empty();
                    } else {
                        long totalBsqBurned = proofs.stream()
                                .mapToLong(ProofOfBurnProof::burntAmount)
                                .sum();
                        long firstBurnDate = proofs.get(0).date();
                        return Optional.of(new ChatUser.BurnInfo(totalBsqBurned, firstBurnDate));
                    }
                });
    }

    public CompletableFuture<Optional<ProofOfBurnProof>> verifyProofOfBurn(String proofOfBurnTxId, String pubKeyHash) {
        return verifyProofOfBurn(0, proofOfBurnTxId, Hex.decode(pubKeyHash));
    }


    public CompletableFuture<Optional<ProofOfBurnProof>> verifyProofOfBurn(Role.Type type, String proofOfBurnTxId, String pubKeyHash) {
        return verifyProofOfBurn(getMinBurnAmount(type), proofOfBurnTxId, Hex.decode(pubKeyHash));
    }

    public CompletableFuture<Optional<ProofOfBurnProof>> verifyProofOfBurn(long minBurnAmount,
                                                                           String _txId,
                                                                           byte[] pubKeyHash) {
        String txId;
        // We use as preImage in the DAO String.getBytes(Charsets.UTF_8) to get bytes from the input string (not hex
        // as hex would be more restrictive for arbitrary inputs)
        byte[] preImage;
        String pubKeyHashAsHex;
        if (USE_DEV_TEST_POB_VALUES) {
            // BSQ proof of burn tx (mainnet): ac57b3d6bdda9976391217e6d0ecbea9b050177fd284c2b199ede383189123c7
            // pubkeyhash (preimage for POB tx) 6a4e52f31a24300fd2a03766b5ea6e4abf289609
            // op return hash 9593f12a86fcb6ca72ed621c208b9370ff8f5112
            txId = "ac57b3d6bdda9976391217e6d0ecbea9b050177fd284c2b199ede383189123c7";
            pubKeyHashAsHex = "6a4e52f31a24300fd2a03766b5ea6e4abf289609";
        } else {
            txId = _txId;
            pubKeyHashAsHex = Hex.encode(pubKeyHash);
        }
        preImage = pubKeyHashAsHex.getBytes(Charsets.UTF_8);

        Map<String, ProofOfBurnProof> verifiedProofOfBurnProofs = persistableStore.getVerifiedProofOfBurnProofs();
        if (verifiedProofOfBurnProofs.containsKey(pubKeyHashAsHex)) {
            return CompletableFuture.completedFuture(Optional.of(verifiedProofOfBurnProofs.get(pubKeyHashAsHex)));
        } else {
            return supplyAsync(() -> {
                try {
                    BaseHttpClient httpClient = getApiHttpClient(config.bsqMemPoolProviders());
                    String jsonBsqTx = httpClient.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                    Preconditions.checkArgument(BsqTxValidator.initialSanityChecks(txId, jsonBsqTx), txId + "Bsq tx sanity check failed");
                    checkArgument(BsqTxValidator.isBsqTx(httpClient.getBaseUrl()), txId + " is Nnt a valid Bsq tx");
                    checkArgument(BsqTxValidator.isProofOfBurn(jsonBsqTx), txId + " is not a proof of burn transaction");
                    long burntAmount = BsqTxValidator.getBurntAmount(jsonBsqTx);
                    checkArgument(burntAmount >= minBurnAmount, "Insufficient BSQ burn. burntAmount=" + burntAmount);
                    String hashOfPreImage = Hex.encode(DigestUtil.hash(preImage));
                    BsqTxValidator.getOpReturnData(jsonBsqTx).ifPresentOrElse(
                            opReturnData -> {
                                // First 2 bytes in opReturn are type and version
                                byte[] hashFromOpReturnDataAsBytes = Arrays.copyOfRange(Hex.decode(opReturnData), 2, 22);
                                String hashFromOpReturnData = Hex.encode(hashFromOpReturnDataAsBytes);
                                checkArgument(hashOfPreImage.equalsIgnoreCase(hashFromOpReturnData),
                                        "pubKeyHash does not match opReturn data");
                            },
                            () -> {
                                throw new IllegalArgumentException("no opReturn element found");
                            });
                    long date = BsqTxValidator.getValidatedTxDate(jsonBsqTx);
                    ProofOfBurnProof verifiedProof = new ProofOfBurnProof(txId, burntAmount, date);
                    verifiedProofOfBurnProofs.put(pubKeyHashAsHex, verifiedProof);
                    persist();
                    return Optional.of(verifiedProof);
                } catch (IllegalArgumentException e) {
                    log.warn("check failed: {}", e.getMessage(), e);
                } catch (IOException e) {
                    if (e.getCause() instanceof HttpException &&
                            e.getCause().getMessage() != null &&
                            e.getCause().getMessage().equalsIgnoreCase("Bisq transaction not found")) {
                        log.error("Tx might be not confirmed yet", e);
                    } else {
                        log.warn("Mem pool query failed:", e);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return Optional.empty();
            });
        }
    }

    public CompletableFuture<Optional<Proof>> verifyBondedRole(String txId, String signature, String pubKeyHash) {
        // inputs: txid, signature from Bisq1
        // process: get txinfo, grab pubkey from 1st output
        // verify provided signature matches with pubkey from 1st output and hash of provided identity pubkey
        return supplyAsync(() -> {
            try {
                BaseHttpClient httpClientBsq = getApiHttpClient(config.bsqMemPoolProviders());
                BaseHttpClient httpClientBtc = getApiHttpClient(config.btcMemPoolProviders());
                String jsonBsqTx = httpClientBsq.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBsq.userAgent)));
                String jsonBtcTx = httpClientBtc.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBtc.userAgent)));
                checkArgument(BsqTxValidator.initialSanityChecks(txId, jsonBsqTx), "bsq tx sanity checks");
                Preconditions.checkArgument(BtcTxValidator.initialSanityChecks(txId, jsonBtcTx), "btc tx sanity checks");
                checkArgument(BsqTxValidator.isBsqTx(httpClientBsq.getBaseUrl()), "isBsqTx");
                checkArgument(BsqTxValidator.isLockup(jsonBsqTx), "is lockup transaction");
                String signingPubkeyAsHex = BtcTxValidator.getFirstInputPubKey(jsonBtcTx);
                PublicKey signingPubKey = KeyGeneration.generatePublicFromCompressed(Hex.decode(signingPubkeyAsHex));
                boolean signatureMatches = SignatureUtil.verify(formatMessageForSigning(pubKeyHash), bitcoinSigToDer(signature), signingPubKey);
                checkArgument(signatureMatches, "signature");
                return Optional.of(new BondedRoleProof(txId, signature));
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

    public CompletableFuture<Optional<Proof>> verifyModerator(String invitationCode, PublicKey publicKey) {
        //todo
        return CompletableFuture.completedFuture(Optional.of(new InvitationProof(invitationCode)));
    }

    //todo work out concept how to adjust those values
    public long getMinBurnAmount(Role.Type type) {
        return switch (type) {
            //todo for dev testing reduced to 6 BSQ
            case LIQUIDITY_PROVIDER -> USE_DEV_TEST_POB_VALUES ? 600 : 5000;
            case CHANNEL_MODERATOR -> 10000;
            default -> 0;
        };
    }


    public Observable<ChatUserIdentity> getSelectedUserProfile() {
        return persistableStore.getSelectedChatUserIdentity();
    }

    public ObservableSet<ChatUserIdentity> getUserProfiles() {
        return persistableStore.getChatUserIdentities();
    }

    public Collection<ChatUser> getMentionableChatUsers() {
        // TODO: implement logic
        return getUserProfiles().stream().map(ChatUserIdentity::getChatUser).toList();
    }

    public Optional<ChatUserIdentity> findUserProfile(String profileId) {
        return getUserProfiles().stream().filter(userProfile -> userProfile.getProfileId().equals(profileId)).findAny();
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
