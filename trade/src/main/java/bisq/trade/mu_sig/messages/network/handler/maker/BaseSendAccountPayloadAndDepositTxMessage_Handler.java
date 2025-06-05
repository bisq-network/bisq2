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

package bisq.trade.mu_sig.messages.network.handler.maker;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.F2FAccountPayload;
import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadAndDepositTxMessage;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseSendAccountPayloadAndDepositTxMessage_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, SendAccountPayloadAndDepositTxMessage> {
    private ByteArray depositTx;
    private AccountPayload peersAccountPayload;

    public BaseSendAccountPayloadAndDepositTxMessage_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(SendAccountPayloadAndDepositTxMessage message) {
        // todo
    }

    @Override
    protected void process(SendAccountPayloadAndDepositTxMessage message) {
        depositTx = message.getDepositTx();
        peersAccountPayload = message.getAccountPayload();

        // We observe the txConfirmationStatus to get informed once the deposit tx is confirmed (gets published by the
        // buyer when they receive the MuSigSetupTradeMessage_D).
        tradeService.observeDepositTxConfirmationStatus(trade);

        // Maybe remove makers offer
        if (serviceProvider.getSettingsService().getCloseMyOfferWhenTaken().get()) {
            MuSigOffer offer = trade.getContract().getOffer();
            serviceProvider.getOfferService().getMuSigOfferService().removeOffer(offer)
                    .whenComplete((deleteChatMessageResult, throwable) -> {
                        if (throwable == null) {
                            log.info("Offer with ID {} removed", offer.getId());
                        } else {
                            log.error("We got an error when removing offer with ID {}", offer.getId(), throwable);
                        }
                    });
        }

        // TODO should maker also try to publish deposit tx for redundancy?
    }

    @Override
    protected void commit() {
        trade.getPeer().setDepositTx(depositTx);
        trade.getPeer().setPeersAccountPayload(peersAccountPayload);
    }

    @Override
    protected void sendMessage() {
        // Now as the peer published the deposit transaction (and we have received it as well)
        // we send our payment account data.
        // We require that both peers exchange the account data to allow verification
        // that the buyer used the account defined in the contract to avoid fraud.
        //todo mock
        AccountPayload accountPayload = new F2FAccountPayload(
                "id",
                "paymentMethodName",
                "countryCode",
                "city",
                "contact",
                "extraInfo");
        send(new SendAccountPayloadMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                accountPayload));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Maker received the published deposit tx and the peers account payload.\n" +
                "Maker starts listening for deposit tx confirmation.\n" +
                "Maker sends account payload to taker.");
    }
}
