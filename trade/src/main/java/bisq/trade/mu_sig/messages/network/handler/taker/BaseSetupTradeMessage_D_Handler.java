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

package bisq.trade.mu_sig.messages.network.handler.taker;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.data.ByteArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeMessageHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadAndDepositTxMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.protobuf.DepositTxSignatureRequest;
import bisq.trade.protobuf.PublishDepositTxRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class BaseSetupTradeMessage_D_Handler extends MuSigTradeMessageHandlerAsMessageSender<MuSigTrade, SetupTradeMessage_D> {
    protected PartialSignatures peersPartialSignatures;
    protected DepositPsbt myDepositPsbt;

    public BaseSetupTradeMessage_D_Handler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(SetupTradeMessage_D message) {
    }

    @Override
    protected void process(SetupTradeMessage_D message) {
        peersPartialSignatures = message.getPartialSignatures();

        PartialSignaturesMessage peersPartialSignaturesMessage = PartialSignaturesMessage.from(peersPartialSignatures);
        DepositTxSignatureRequest depositTxSignatureRequest = DepositTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                .setPeersPartialSignatures(peersPartialSignaturesMessage.toProto(true))
                .build();
        myDepositPsbt = DepositPsbt.fromProto(blockingStub.signDepositTx(depositTxSignatureRequest));

        PublishDepositTxRequest publishDepositTxRequest = PublishDepositTxRequest.newBuilder()
                .setTradeId(trade.getId())
                .setDepositPsbt(myDepositPsbt.toProto(true))
                .build();
        blockingStub.publishDepositTx(publishDepositTxRequest);
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();

        mySelf.setMyDepositPsbt(myDepositPsbt);
        peer.setPeersPartialSignatures(peersPartialSignatures);
    }

    @Override
    protected void sendMessage() {
        // We send the deposit transaction even the maker could find it on the blockchain.
        ByteArray depositTx = new ByteArray(myDepositPsbt.getDepositPsbt());
        // Now we published the deposit transaction we send our payment account data.
        // We require that both peers exchange the account data to allow verification
        // that the buyer used the account defined in the contract to avoid fraud.

        //todo

        // The maker has added the salted account id to the AccountOptions.
        // We will use the payment method chosen by the taker to determine which account we had assigned to that offer.
        MuSigOffer offer = trade.getOffer();
        String offerId = offer.getId();
        PaymentMethod<?> selectedPaymentMethod = trade.getContract().getQuoteSidePaymentMethodSpec().getPaymentMethod();
        ObservableSet<Account<? extends PaymentMethod<?>, ?>> accounts = serviceProvider.getAccountService().getAccounts();
        Set<AccountOption> accountOptions = OfferOptionUtil.findAccountOptions(offer.getOfferOptions());
        Optional<Account<? extends PaymentMethod<?>, ?>> account = accountOptions.stream()
                .filter(accountOption -> accountOption.getPaymentMethod().equals(selectedPaymentMethod))
                .map(AccountOption::getSaltedAccountId)
                .flatMap(saltedAccountId -> OfferOptionUtil.findAccountFromSaltedAccountId(accounts, saltedAccountId, offerId).stream())
                .findAny();
        checkArgument(account.isPresent(), "No account found for the saltedAccountIds from the accountOptions");

        send(new SendAccountPayloadAndDepositTxMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                depositTx,
                account.get().getAccountPayload()));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Taker received peers partialSignatures.\n" +
                "Taker created depositPsbt.\n" +
                "Taker published deposit tx.\n" +
                "Taker sends deposit tx and account payload to maker");
    }
}
