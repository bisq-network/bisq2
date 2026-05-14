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

package bisq.trade.bisq_easy.protocol.events;

import bisq.account.accounts.stable_coin.StableCoinAccountPayload;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.handler.BisqEasyTradeEventHandlerAsMessageSender;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyAccountDataEventHandler extends BisqEasyTradeEventHandlerAsMessageSender<BisqEasyTrade, BisqEasyAccountDataEvent> {
    private String paymentAccountData;

    public BisqEasyAccountDataEventHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(BisqEasyAccountDataEvent event) {
        paymentAccountData = event.getPaymentAccountData();
        validateRailMatch();
    }

    @Override
    protected void commit() {
        trade.getPaymentAccountData().set(paymentAccountData);
    }

    @Override
    protected void sendMessage() {
        send(new BisqEasyAccountDataMessage(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                paymentAccountData,
                trade.getOffer()));
    }

    /**
     * Last line of defense: verify that the account data being sent matches the
     * stablecoin network (rail) agreed upon in the contract. If the UI filtering
     * somehow fails, this prevents sending funds to the wrong chain.
     */
    private void validateRailMatch() {
        var quoteSideSpec = trade.getContract().getQuoteSidePaymentMethodSpec();
        if (quoteSideSpec instanceof StableCoinPaymentMethodSpec stableCoinSpec) {
            String expectedNetwork = stableCoinSpec.getPaymentMethod().getNetwork().getDisplayName();
            checkArgument(StableCoinAccountPayload.containsNetworkTag(paymentAccountData, expectedNetwork),
                    "Stablecoin account data does not match contract network. " +
                            "Expected network: " + expectedNetwork +
                            ", account data: " + paymentAccountData);
        }
    }
}
