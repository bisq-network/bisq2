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

package bisq.protocol.bisq_easy;

import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.OfferService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.protocol.bisq_easy.messages.BisqEasyProtocolMessage;
import bisq.support.MediationService;
import bisq.support.SupportService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BisqEasyProtocolService implements PersistenceClient<BisqEasyProtocolStore>, Service, MessageListener {
    @Getter
    private final BisqEasyProtocolStore persistableStore = new BisqEasyProtocolStore();
    @Getter
    private final Persistence<BisqEasyProtocolStore> persistence;
    private final ContractService contractService;
    private final MediationService mediationService;
    private final NetworkService networkService;

    public BisqEasyProtocolService(NetworkService networkService,
                                   IdentityService identityService,
                                   PersistenceService persistenceService,
                                   OfferService offerService,
                                   ContractService contractService,
                                   SupportService supportService) {
        this.networkService = networkService;
        this.contractService = contractService;
        this.mediationService = supportService.getMediationService();
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);

    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof BisqEasyProtocolMessage) {
            processMessage((BisqEasyProtocolMessage) networkMessage);
        }
    }

    private void processMessage(BisqEasyProtocolMessage message) {

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void takeOffer(Identity takerIdentity,
                          BisqEasyOffer bisqEasyOffer,
                          Monetary baseSideAmount,
                          Monetary quoteSideAmount,
                          BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                          FiatPaymentMethodSpec fiatPaymentMethodSpec)
            throws GeneralSecurityException {
        Optional<UserProfile> mediator = mediationService.takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        BisqEasyContract bisqEasyContract = new BisqEasyContract(bisqEasyOffer,
                takerIdentity.getNetworkId(),
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator);

        ContractSignatureData contractSignatureData = contractService.signContract(bisqEasyContract, takerIdentity.getKeyPair());
        BisqEasyProtocolModel model = new BisqEasyProtocolModel(takerIdentity, bisqEasyContract, contractSignatureData);
        persistableStore.add(model);
        BisqEasyTakerProtocol protocol = (BisqEasyTakerProtocol) createProtocol(model, true);
        protocol.takeOffer();
    }

    private BisqEasyProtocol createProtocol(BisqEasyProtocolModel model, boolean isTaker) {
        BisqEasyOffer offer = model.getOffer();
        if (isTaker) {
            if (offer.getTakersDirection().isBuy()) {
                return new BisqEasyTakerAsBuyerProtocol(model);
            } else {
                return new BisqEasyTakerAsSellerProtocol(model);
            }
        } else {
            if (offer.getMakersDirection().isBuy()) {
                return new BisqEasyMakerAsBuyerProtocol(model);
            } else {
                return new BisqEasyMakerAsSellerProtocol(model);
            }
        }
    }
}