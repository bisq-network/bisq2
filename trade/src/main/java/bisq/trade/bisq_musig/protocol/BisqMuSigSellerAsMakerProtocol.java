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

package bisq.trade.bisq_musig.protocol;

import bisq.common.fsm.Fsm;
import bisq.common.fsm.FsmErrorEvent;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;

public class BisqMuSigSellerAsMakerProtocol extends BisqMuSigProtocol {

    public BisqMuSigSellerAsMakerProtocol(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {
        fromAny()
                .on(FsmErrorEvent.class)
                .run(BisqMuSigErrorEventHandler.class)
                .to(MainState.FAILED);
    }

    @Override
    public void configTransitions() {
        from(MainState.INIT)
            .on(BisqMuSigTakeOfferEvent.class)
            .run(BisqMuSigTakeOfferEventHandler.class)
            .to(MainState.DEPOSIT_PHASE)
            .withFSM()
                .initialState(DepositState.INIT)
                .errorHandler(FsmErrorEvent.class, DepositState.DEPOSIT_FAILED)
                .transition()
                .from(DepositState.INIT)
                .on(BisqMuSigConstructPreSignedTxEvent.class)
                .run(BisqMuSigConstructPreSignedTxEventHandler.class)
                .to(DepositState.PRE_SIGNED)
                .transition()
                .from(DepositState.PRE_SIGNED)
                .on(BisqMuSigKeyAggregationEvent.class)
                .run(BisqMuSigKeyAggregationEventHandler.class)
                .to(DepositState.KEY_AGGREGATED)
                .transition()
                .from(DepositState.KEY_AGGREGATED)
                .on(BisqMuSigDepositTxBroadcastEvent.class)
                .run(BisqMuSigDepositTxBroadcastEventHandler.class)
                .to(DepositState.DEPOSIT_COMPLETED)
                .onSuccess(MainState.FIAT_CONFIRMATION_PHASE)
                .onFailure(MainState.CONFLICT_RESOLUTION_PHASE)
                .then();

        from(MainState.FIAT_CONFIRMATION_PHASE)
            .on(Fsm.FsmCompletedEvent.class)
            .to(MainState.KEY_EXCHANGE_PHASE)
            .withFSM()
                .initialState(FiatState.AWAITING_FIAT)
                .errorHandler(FsmErrorEvent.class, FiatState.FIAT_FAILED)
                .transition()
                .from(FiatState.AWAITING_FIAT)
                .on(BisqMuSigFiatTransactionConfirmedEvent.class)
                .run(BisqMuSigFiatTransactionConfirmedEventHandler.class)
                .to(FiatState.FIAT_RECEIVED)
                .onSuccess(MainState.KEY_EXCHANGE_PHASE)
                .onFailure(MainState.CONFLICT_RESOLUTION_PHASE)
                .then();

        from(MainState.KEY_EXCHANGE_PHASE)
            .on(Fsm.FsmCompletedEvent.class)
            .to(MainState.FINALIZATION_PHASE)
                .withFSM()
                .initialState(KeyExchangeState.EXCHANGE_INIT)
                .errorHandler(FsmErrorEvent.class, KeyExchangeState.EXCHANGE_FAILED)
                .transition()
                .from(KeyExchangeState.EXCHANGE_INIT)
                .on(BisqMuSigKeyExchangeCompletedEvent.class)
                .run(BisqMuSigKeyExchangeCompletedEventHandler.class)
                .to(KeyExchangeState.EXCHANGE_COMPLETED)
                .onSuccess(MainState.FINALIZATION_PHASE)
                .onFailure(MainState.CONFLICT_RESOLUTION_PHASE)
                .then();

        from(MainState.FINALIZATION_PHASE)
            .on(Fsm.FsmCompletedEvent.class)
            .to(MainState.BTC_CONFIRMED)
                .withFSM()
                .initialState(FinalizationState.FINALIZE_INIT)
                .errorHandler(FsmErrorEvent.class, FinalizationState.FINALIZE_FAILED)
                .transition()
                .from(FinalizationState.FINALIZE_INIT)
                .on(BisqMuSigFinalizationCompletedEvent.class)
                .run(BisqMuSigFinalizationCompletedEventHandler.class)
                .to(FinalizationState.FINALIZE_COMPLETED)
                .onSuccess(MainState.BTC_CONFIRMED)
                .onFailure(MainState.FAILED)
                .then();

        from(MainState.CONFLICT_RESOLUTION_PHASE)
            .on(Fsm.FsmCompletedEvent.class)
            .to(MainState.CONFLICT_RESOLVED)
            .withFSM()
                .initialState(ConflictState.WARNING_TX_RECEIVED)
                .errorHandler(FsmErrorEvent.class, ConflictState.RESOLUTION_FAILURE)
                .transition()
                .from(ConflictState.WARNING_TX_RECEIVED)
                .on(BisqMuSigRedirectTxEvent.class)
                .run(BisqMuSigRedirectTxEventHandler.class)
                .to(ConflictState.REDIRECT_TX_RECEIVED)
                .transition()
                .from(ConflictState.REDIRECT_TX_RECEIVED)
                .on(BisqMuSigT1ExpiredEvent.class)
                .run(BisqMuSigT1ExpiredEventHandler.class)
                .to(ConflictState.T1_EXPIRED)
                .transition()
                .from(ConflictState.T1_EXPIRED)
                .on(BisqMuSigClaimTxEvent.class)
                .run(BisqMuSigClaimTxEventHandler.class)
                .to(ConflictState.RESOLUTION_SUCCESS)
                .onSuccess(MainState.CONFLICT_RESOLVED)
                .onFailure(MainState.FAILED)
                .then();

        // Warning transaction can interrupt the normal flow at multiple states
        fromStates(MainState.DEPOSIT_PHASE, MainState.FIAT_CONFIRMATION_PHASE, MainState.KEY_EXCHANGE_PHASE)
            .on(BisqMuSigWarningTxEvent.class)
            .run(BisqMuSigWarningTxEventHandler.class)
            .to(MainState.CONFLICT_RESOLUTION_PHASE)
            .then();

        // Direct handling of specific events that might occur at any stage
        fromAny()
            .on(BisqMuSigRedirectTxEvent.class)
            .run(BisqMuSigRedirectTxEventHandler.class)
            .to(MainState.CONFLICT_RESOLUTION_PHASE)
            .then();

        // Error handling for unexpected errors at any stage
        fromAny()
            .on(FsmErrorEvent.class)
            .run(BisqMuSigErrorEventHandler.class)
            .to(MainState.FAILED);
    }

    public enum MainState implements bisq.common.fsm.State {
        INIT,
        DEPOSIT_PHASE,
        FIAT_CONFIRMATION_PHASE,
        KEY_EXCHANGE_PHASE,
        FINALIZATION_PHASE,
        CONFLICT_RESOLUTION_PHASE,
        BTC_CONFIRMED(true),
        CONFLICT_RESOLVED(true),
        FAILED(true);

        private final boolean isFinalState;

        MainState() {
            this(false);
        }

        MainState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }

    public enum DepositState implements bisq.common.fsm.State {
        INIT,
        PRE_SIGNED,
        KEY_AGGREGATED,
        DEPOSIT_COMPLETED(true),
        DEPOSIT_FAILED(true);

        private final boolean isFinalState;

        DepositState() {
            this(false);
        }

        DepositState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }

    public enum FiatState implements bisq.common.fsm.State {
        AWAITING_FIAT,
        FIAT_RECEIVED(true),
        FIAT_FAILED(true);

        private final boolean isFinalState;

        FiatState() {
            this(false);
        }

        FiatState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }

    public enum KeyExchangeState implements bisq.common.fsm.State {
        EXCHANGE_INIT,
        EXCHANGE_COMPLETED(true),
        EXCHANGE_FAILED(true);

        private final boolean isFinalState;

        KeyExchangeState() {
            this(false);
        }

        KeyExchangeState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }

    public enum FinalizationState implements bisq.common.fsm.State {
        FINALIZE_INIT,
        FINALIZE_COMPLETED(true),
        FINALIZE_FAILED(true);

        private final boolean isFinalState;

        FinalizationState() {
            this(false);
        }

        FinalizationState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }

    public enum ConflictState implements bisq.common.fsm.State {
        WARNING_TX_RECEIVED,
        REDIRECT_TX_RECEIVED,
        T1_EXPIRED,
        RESOLUTION_SUCCESS(true),
        RESOLUTION_FAILURE(true);

        private final boolean isFinalState;

        ConflictState() {
            this(false);
        }

        ConflictState(boolean isFinalState) {
            this.isFinalState = isFinalState;
        }

        @Override
        public boolean isFinalState() {
            return isFinalState;
        }

        @Override
        public int getOrdinal() {
            return ordinal();
        }
    }
}