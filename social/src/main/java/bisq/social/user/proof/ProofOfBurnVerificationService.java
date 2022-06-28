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

package bisq.social.user.proof;

import bisq.common.data.Pair;
import bisq.common.encoding.Hex;
import bisq.common.util.CollectionUtil;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.http.common.HttpException;
import bisq.network.p2p.node.transport.Transport;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.social.user.role.Role;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class ProofOfBurnVerificationService implements PersistenceClient<ProofOfBurnVerificationStore> {
    // For dev testing we use hard coded txId and a pubKeyHash to get real data from Bisq explorer
    private static final boolean USE_DEV_TEST_POB_VALUES = true;

    @Getter
    @ToString
    public static final class Config {
        private final List<String> btcMemPoolProviders;
        private final List<String> bsqMemPoolProviders;

        public Config(List<String> btcMemPoolProviders,
                      List<String> bsqMemPoolProviders) {
            this.btcMemPoolProviders = btcMemPoolProviders;
            this.bsqMemPoolProviders = bsqMemPoolProviders;
        }

        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            List<String> btcMemPoolProviders = typeSafeConfig.getStringList("btcMemPoolProviders");
            List<String> bsqMemPoolProviders = typeSafeConfig.getStringList("bsqMemPoolProviders");
            return new Config(btcMemPoolProviders, bsqMemPoolProviders);
        }
    }

    @Getter
    private final ProofOfBurnVerificationStore persistableStore = new ProofOfBurnVerificationStore();
    @Getter
    private final Persistence<ProofOfBurnVerificationStore> persistence;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final Config config;
    private final Map<String, Long> publishTimeByChatUserId = new ConcurrentHashMap<>();

    public ProofOfBurnVerificationService(PersistenceService persistenceService,
                                          Config config,
                                          KeyPairService keyPairService,
                                          IdentityService identityService,
                                          NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.config = config;
        this.networkService = networkService;
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
                    BaseHttpClient httpClient = getApiHttpClient(config.getBsqMemPoolProviders());
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

    public long getMinBurnAmount(Role.Type type) {
        switch (type) {
            //todo for dev testing reduced to 6 BSQ
            case LIQUIDITY_PROVIDER:
                return USE_DEV_TEST_POB_VALUES ? 600 : 5000;
            case CHANNEL_MODERATOR:
                return 10000;
            default:
                return 0;
        }
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
