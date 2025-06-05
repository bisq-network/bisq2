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

package bisq.trade.mu_sig.messages.network.handler.seller_as_taker;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_D;
import bisq.trade.mu_sig.messages.network.handler.taker.BaseSetupTradeMessage_D_Handler;

import static com.google.common.base.Preconditions.checkArgument;

public class SetupTradeMessage_D_Handler extends BaseSetupTradeMessage_D_Handler {
    public SetupTradeMessage_D_Handler(ServiceProvider serviceProvider,
                                       MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(SetupTradeMessage_D message) {
        super.verify(message);

        checkArgument(message.getPartialSignatures().getSwapTxInputPartialSignature().isEmpty(),
                "We are the seller and expect that the swapTxInputPartialSignature is redacted (optional is empty)");
    }
}
