/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.timestamp.KeyType;
import bisq.bonded_roles.release.AppType;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.mu_sig.grpc.MusigGrpcClient;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import bisq.trade.protobuf.MusigGrpc;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import com.google.protobuf.ByteString;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Builds a maker-side MuSigTradeService over a deep-stubbed ServiceProvider with a real maker
// identity, an account-backed offer, a MuSig daemon stub that answers the setup calls, and
// validly signed take offer requests.
class MuSigMakerAdmissionFixtures {
    static final String OFFER_ID = "offer-id";
    static final String OTHER_OFFER_ID = "other-offer-id";
    static final String ACCOUNT_ID = "account-id";

    final KeyPair makerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    final KeyPair takerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    final NetworkId makerNetworkId = createNetworkId("maker", 9997, makerKeyPair);
    final NetworkId takerNetworkId = createNetworkId("taker", 9999, takerKeyPair);
    final Identity makerIdentity = new Identity("maker", makerNetworkId,
            new KeyBundle("maker-key-bundle", makerKeyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair()));
    final ContractService contractService = new ContractService(mock(SecurityService.class));
    final ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
    final MusigGrpc.MusigBlockingStub daemon = mock(MusigGrpc.MusigBlockingStub.class);
    final MusigGrpcClient grpcClient = mock(MusigGrpcClient.class);
    @SuppressWarnings("unchecked")
    final Persistence<MuSigTradeStore> persistence = mock(Persistence.class);
    // Every store snapshot the service submitted for writing, in submission order.
    final List<MuSigTradeStore> persistedSnapshots = new CopyOnWriteArrayList<>();
    final PaymentMethodSpec<?> paymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
            FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
    final AchTransferAccountPayload makerAccountPayload = new AchTransferAccountPayload(ACCOUNT_ID, "Holder Name",
            "1 Main St", "Bank", "021000021", "12345678", BankAccountType.CHECKING);
    final Account<? extends PaymentMethod<?>, ?> makerAccount = new AchTransferAccount(ACCOUNT_ID,
            System.currentTimeMillis(), "acc", makerAccountPayload, KeyGeneration.generateDefaultEcKeyPair(),
            KeyType.EC, AccountOrigin.BISQ2_NEW);
    final MuSigOffer offer = createOffer(OFFER_ID);
    final MuSigOffer otherOffer = createOffer(OTHER_OFFER_ID);
    final ManualExecutorService executor = new ManualExecutorService();

    MuSigMakerAdmissionFixtures() {
        when(serviceProvider.getContractService()).thenReturn(contractService);
        registerTaker(takerNetworkId);
        when(serviceProvider.getOfferService().getMuSigOfferService().getMyMuSigOffersService().claimActivatedOffer(OFFER_ID))
                .thenReturn(Optional.of(offer));
        when(serviceProvider.getOfferService().getMuSigOfferService().getMyMuSigOffersService().claimActivatedOffer(OTHER_OFFER_ID))
                .thenReturn(Optional.of(otherOffer));
        when(serviceProvider.getIdentityService().findAnyIdentityByNetworkId(makerNetworkId))
                .thenReturn(Optional.of(makerIdentity));
        when(serviceProvider.getUserService().getUserIdentityService().findUserIdentity(makerIdentity.getId()))
                .thenReturn(Optional.of(mock(UserIdentity.class)));
        when(serviceProvider.getAccountService().getAccounts(paymentMethodSpec.getPaymentMethod()))
                .thenReturn(Set.of(makerAccount));
        when(serviceProvider.getNetworkService().getConfidentialMessageServices()).thenReturn(Set.of());
        when(serviceProvider.getNetworkService().confidentialSend(any(), any(), any()))
                .thenAnswer(invocation -> new CompletableFuture<SendMessageResult>());
        when(grpcClient.initialize()).thenReturn(CompletableFuture.completedFuture(true));
        when(grpcClient.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
        when(grpcClient.getBlockingStub()).thenReturn(daemon);
        PersistenceService persistenceService = serviceProvider.getPersistenceService();
        doReturn(persistence).when(persistenceService).getOrCreatePersistence(any(), any(), any());
        when(persistence.persistAsync(any())).thenAnswer(invocation -> {
            persistedSnapshots.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        when(daemon.initTrade(any())).thenReturn(bisq.trade.protobuf.PubKeySharesResponse.newBuilder()
                .setBuyerOutputPubKeyShare(ByteString.copyFrom(compressedKey()))
                .setSellerOutputPubKeyShare(ByteString.copyFrom(compressedKey()))
                .setMultisigScriptKey(ByteString.copyFrom(compressedKey()))
                .build());
        when(daemon.getNonceShares(any())).thenReturn(bisq.trade.protobuf.NonceSharesMessage.getDefaultInstance());
    }

    MuSigTradeService createInitializedService() {
        MuSigTradeService service = new MuSigTradeService(serviceProvider, AppType.DESKTOP, () -> executor, grpcClient);
        // The protocol handlers reach the service, its persistence and the daemon through the
        // service provider; only the daemon boundary is stubbed.
        when(serviceProvider.getMuSigTradeService()).thenReturn(service);
        service.initialize().join();
        return service;
    }

    SetupTradeMessage_A createRequest(long takeOfferDate) throws GeneralSecurityException {
        return createRequest(offer, takerNetworkId, takerKeyPair, takeOfferDate);
    }

    SetupTradeMessage_A createRequest(MuSigOffer offer, long takeOfferDate) throws GeneralSecurityException {
        return createRequest(offer, takerNetworkId, takerKeyPair, takeOfferDate);
    }

    // A second taker the maker knows, with its own key pair.
    NetworkId registerOtherTaker(KeyPair keyPair) {
        NetworkId otherTaker = createNetworkId("other-taker", 9998, keyPair);
        registerTaker(otherTaker);
        return otherTaker;
    }

    SetupTradeMessage_A createRequest(MuSigOffer offer,
                                      NetworkId taker,
                                      KeyPair takerKeyPair,
                                      long takeOfferDate) throws GeneralSecurityException {
        MuSigContract contract = new MuSigContract(takeOfferDate, offer, taker, 111L, 222L, paymentMethodSpec,
                new byte[20], Optional.empty(), Optional.empty(), offer.getPriceSpec(), 0);
        ContractSignatureData signature = contractService.signContract(contract, takerKeyPair);
        return createRequest(contract, signature, Trade.createId(offer.getId(), taker.getId(), takeOfferDate));
    }

    // Lets a test send an accepted trade id with content that does not belong to it.
    SetupTradeMessage_A createRequest(MuSigContract contract, ContractSignatureData signature, String tradeId) {
        return new SetupTradeMessage_A("message-" + contract.getTakeOfferDate(),
                tradeId,
                "1.0.0",
                contract.getTaker().getNetworkId(),
                makerNetworkId,
                contract,
                signature,
                PubKeyShares.fromProto(bisq.trade.protobuf.PubKeyShares.newBuilder()
                        .setBuyerOutputPubKeyShare(ByteString.copyFrom(compressedKey()))
                        .setSellerOutputPubKeyShare(ByteString.copyFrom(compressedKey()))
                        .setMultisigScriptKey(ByteString.copyFrom(compressedKey()))
                        .build()));
    }

    private void registerTaker(NetworkId taker) {
        when(serviceProvider.getUserService().getUserProfileService().findUserProfile(taker.getId()))
                .thenReturn(Optional.of(mock(UserProfile.class)));
    }

    private MuSigOffer createOffer(String offerId) {
        AccountOption accountOption = new AccountOption(paymentMethodSpec.getPaymentMethod(),
                OfferOptionUtil.createdSaltedAccountId(makerAccount.getId(), offerId),
                Optional.empty(), List.of(), Optional.empty(), List.of(), new byte[20]);
        return new MuSigOffer(offerId,
                makerNetworkId,
                Direction.BUY,
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new BaseSideFixedAmountSpec(111L),
                new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")),
                List.of(paymentMethodSpec.getPaymentMethod()),
                List.of(accountOption),
                "1.0.0");
    }

    private static byte[] compressedKey() {
        byte[] key = new byte[33];
        key[0] = 0x02;
        return key;
    }

    private static NetworkId createNetworkId(String keyIdSuffix, int port, KeyPair keyPair) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), "test-key-" + keyIdSuffix));
    }
}
