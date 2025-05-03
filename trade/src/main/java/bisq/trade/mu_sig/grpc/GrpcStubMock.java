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

package bisq.trade.mu_sig.grpc;

import java.util.Iterator;

public class GrpcStubMock {
    public PubKeySharesResponse initTrade(PubKeySharesRequest request) {
        byte[] buyerOutputPubKeyShare = {};
        byte[] sellerOutputPubKeyShare = {};
        int currentBlockHeight = 1;

        return new PubKeySharesResponse(buyerOutputPubKeyShare, sellerOutputPubKeyShare, currentBlockHeight);
    }

    public NonceSharesMessage getNonceShares(NonceSharesRequest nonceSharesRequest) {
        return new NonceSharesMessage("TODO", 
                "TODO",
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{}
                );
    }

    public PartialSignaturesMessage getPartialSignatures(PartialSignaturesRequest partialSignaturesRequest) {
        return null;
    }

    public DepositPsbt signDepositTx(DepositTxSignatureRequest depositTxSignatureRequest) {
        return null;
    }

    public Iterator<TxConfirmationStatus> publishDepositTx(PublishDepositTxRequest publishDepositTxRequest) {
        return null;
    }

    public SwapTxSignatureResponse signSwapTx(SwapTxSignatureRequest swapTxSignatureRequest) {
        return null;
    }

    public CloseTradeResponse closeTrade(CloseTradeRequest closeTradeRequest) {
        return null;
    }
}
