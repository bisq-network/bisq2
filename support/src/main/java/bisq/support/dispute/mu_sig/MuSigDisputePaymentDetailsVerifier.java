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

package bisq.support.dispute.mu_sig;

import bisq.account.accounts.AccountPayload;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.options.OfferOptionUtil;

import java.util.Arrays;
import java.util.Optional;

public final class MuSigDisputePaymentDetailsVerifier {
    private MuSigDisputePaymentDetailsVerifier() {
    }

    public static Result verify(MuSigContract contract,
                                AccountPayload<?> takerAccountPayload,
                                AccountPayload<?> makerAccountPayload) {
        String offerId = contract.getOffer().getId();

        byte[] takerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(takerAccountPayload, offerId);
        boolean takerAccountPayloadMatches = contract.getTaker().getSaltedAccountPayloadHash()
                .filter(expectedHash -> Arrays.equals(takerSaltedAccountPayloadHash, expectedHash))
                .isPresent();

        byte[] makerSaltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(makerAccountPayload, offerId);
        boolean makerAccountPayloadMatches = contract.getMaker().getSaltedAccountPayloadHash()
                .filter(expectedHash -> Arrays.equals(makerSaltedAccountPayloadHash, expectedHash))
                .isPresent();

        return new Result(takerAccountPayloadMatches,
                makerAccountPayloadMatches,
                takerAccountPayloadMatches
                        ? Optional.empty()
                        : Optional.of(takerAccountPayload.getAccountDataDisplayString()),
                makerAccountPayloadMatches
                        ? Optional.empty()
                        : Optional.of(makerAccountPayload.getAccountDataDisplayString()));
    }

    public record Result(boolean takerAccountPayloadMatches,
                         boolean makerAccountPayloadMatches,
                         Optional<String> takerMismatchDetails,
                         Optional<String> makerMismatchDetails) {
    }
}
