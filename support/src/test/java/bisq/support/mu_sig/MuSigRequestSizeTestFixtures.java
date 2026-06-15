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

package bisq.support.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.chat.ChatMessageType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MuSigRequestSizeTestFixtures {
    public static final String TRADE_ID = "trade-boundary";
    public static final String CHANNEL_ID = "channel-boundary";
    private static final AtomicInteger NEXT_PORT = new AtomicInteger(10_000);

    private MuSigRequestSizeTestFixtures() {
    }

    public static int findBoundaryTextLength(IntFunction<Message> innerProtoFactory) {
        int low = 1;
        int high = 8_999;
        while (low <= high) {
            int candidate = (low + high) / 2;
            Message innerProto = innerProtoFactory.apply(candidate);
            if (innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE
                    && wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE) {
                return candidate;
            }
            if (innerProto.getSerializedSize() > MAX_SERIALIZED_SIZE) {
                high = candidate - 1;
            } else {
                low = candidate + 1;
            }
        }
        throw new AssertionError("Could not find MuSig request size boundary");
    }

    public static int findLargestAcceptedTextLength(IntFunction<Message> innerProtoFactory) {
        int low = 1;
        int high = 8_999;
        int result = -1;
        while (low <= high) {
            int candidate = (low + high) / 2;
            if (wrappedEnvelopeSize(innerProtoFactory.apply(candidate)) <= MAX_SERIALIZED_SIZE) {
                result = candidate;
                low = candidate + 1;
            } else {
                high = candidate - 1;
            }
        }
        if (result < 0) {
            throw new AssertionError("Could not find accepted MuSig request size boundary");
        }
        return result;
    }

    public static int wrappedEnvelopeSize(Message innerProto) {
        return bisq.network.protobuf.EnvelopePayloadMessage.newBuilder()
                .setExternalNetworkMessage(bisq.network.protobuf.ExternalNetworkMessage.newBuilder()
                        .setPayload(Any.pack(innerProto)))
                .build()
                .getSerializedSize();
    }

    public static void assertCanBeEncrypted(byte[] serializedPayload,
                                            NetworkId receiverNetworkId) throws GeneralSecurityException {
        assertTrue(serializedPayload.length <= MAX_SERIALIZED_SIZE);
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(serializedPayload,
                receiverNetworkId.getPubKey().getPublicKey(),
                KeyGeneration.generateDefaultEcKeyPair());
        assertTrue(confidentialData.getCipherText().length <= 20_000);
    }

    public static List<MuSigOpenTradeMessage> createBoundaryChatMessages(UserProfile requester,
                                                                         UserProfile peer,
                                                                         int textLength) {
        long now = System.currentTimeMillis();
        return List.of(createChatMessage("message-1", requester, peer, "x".repeat(textLength), now),
                createChatMessage("message-2", peer, requester, "y".repeat(textLength), now + 1));
    }

    public static UserProfile createUserProfile(String nickName) {
        int port = NEXT_PORT.incrementAndGet();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static MuSigOpenTradeMessage createChatMessage(String messageId,
                                                           UserProfile sender,
                                                           UserProfile receiver,
                                                           String text,
                                                           long date) {
        return new MuSigOpenTradeMessage(TRADE_ID,
                messageId,
                CHANNEL_ID,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                text,
                Optional.empty(),
                date,
                false,
                Optional.empty(),
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                Set.of());
    }

    public static MuSigContract createContract(UserProfile maker,
                                                UserProfile taker,
                                                Optional<UserProfile> mediator,
                                                Optional<UserProfile> arbitrator,
                                                String offerId) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-" + offerId, "DE0000000001");
        AccountPayload<?> takerPayload = createNationalBankPayload("taker-" + offerId, "DE0000000002");
        List<AccountOption> accountOptions = List.of(new AccountOption(
                paymentMethod,
                "0123456789abcdef0123456789abcdef01234567",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                OfferOptionUtil.createSaltedAccountPayloadHash(makerPayload, offerId)
        ));
        MuSigOffer offer = new MuSigOffer(offerId,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                accountOptions,
                "1.0.0");
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        byte[] takerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(takerPayload, offerId);
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                100_000L,
                3_500_000L,
                quoteSidePaymentMethodSpec,
                takerSaltedAccountPayloadHash,
                mediator,
                arbitrator,
                new MarketPriceSpec(),
                0);
    }

    private static AccountPayload<?> createNationalBankPayload(String id,
                                                               String accountNr) {
        return new NationalBankAccountPayload(id,
                "DE",
                "EUR",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                accountNr,
                Optional.empty(),
                Optional.empty());
    }
}
