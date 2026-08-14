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

import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.Trade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 * Validates an incoming take offer request before any trade is created or persisted. Every
 * rejection carries the same generic wire reason while the log keeps the specific cause, so a
 * crafted request learns nothing from the failure mode.
 */
@Slf4j
public final class MuSigTakeOfferRequestValidator {
    private MuSigTakeOfferRequestValidator() {
    }

    public static void validateIdentity(ContractService contractService, SetupTradeMessage_A message) {
        MuSigContract contract = message.getContract();
        if (contract == null || contract.getOffer() == null) {
            throw reject("The request carries no contract or no offer");
        }
        NetworkId takerNetworkId = contract.getTaker().getNetworkId();
        if (!message.getSender().equals(takerNetworkId)) {
            throw reject("The authenticated message sender does not match the taker in the contract. sender="
                    + message.getSender() + ", contract taker=" + takerNetworkId);
        }
        if (!message.getReceiver().equals(contract.getOffer().getMakerNetworkId())) {
            // On a node owning several identities the confidential layer only binds the message
            // to the receiving identity; without this check a request naming maker A in the
            // contract but sent to identity B would build a trade mixing both identities.
            throw reject("The message receiver does not match the offer's maker network id. receiver="
                    + message.getReceiver() + ", offer maker=" + contract.getOffer().getMakerNetworkId());
        }
        String expectedTradeId = Trade.createId(contract.getOffer().getId(),
                takerNetworkId.getId(),
                contract.getTakeOfferDate());
        if (!message.getTradeId().equals(expectedTradeId)) {
            // The trade id the maker will derive from the contract must be the id the message
            // claims; a mismatch would only be rejected after the trade was persisted, leaving
            // a permanent failed trade per crafted message. The attacker-chosen id is
            // sanitized before logging - control characters would allow forged log entries.
            throw reject("The message trade id does not match the id derived from the contract. messageTradeId="
                    + sanitizeForLog(message.getTradeId()) + ", expected=" + expectedTradeId);
        }
        PublicKey takerPublicKey = takerNetworkId.getPubKey().getPublicKey();
        if (!contractService.arePublicKeysMatching(message.getSenderPublicKey(), takerPublicKey)) {
            throw reject("The message sender public key does not match the taker's public key");
        }
        ContractSignatureData signatureData = message.getContractSignatureData();
        if (signatureData == null || !contractService.arePublicKeysMatching(signatureData, takerPublicKey)) {
            throw reject("The contract signature public key does not match the taker's public key");
        }
        boolean signatureValid;
        try {
            signatureValid = contractService.verifyContractSignature(contract, signatureData);
        } catch (GeneralSecurityException | RuntimeException e) {
            // verifyContractSignature also throws on a hash mismatch between the signature data
            // and the contract; any failure mode of a crafted signature must reject, not crash.
            throw reject("The taker's contract signature could not be verified: " + e.getMessage());
        }
        if (!signatureValid) {
            throw reject("The taker's contract signature does not verify");
        }
    }

    private static String sanitizeForLog(String value) {
        return StringUtils.truncate(value.replaceAll("\\p{Cntrl}", "?"), 60);
    }

    private static TradeProtocolException reject(String logMessage) {
        log.warn("Rejecting the MuSig take offer request before trade creation: {}", logMessage);
        return new TradeProtocolException(
                "The maker has rejected the take offer request because the offer is not available anymore.",
                TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }
}
