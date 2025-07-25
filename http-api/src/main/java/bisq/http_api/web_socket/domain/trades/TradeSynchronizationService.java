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

package bisq.http_api.web_socket.domain.trades;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.Service;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to synchronize trade states in the http-api server to ensure we haven't missed
 * any trade state changes while the server was running.
 * 
 * This addresses the issue where trade completion notifications are missed when the mobile
 * app is killed and restarted, but the root cause is that the http-api server itself
 * also misses trade state updates from the Bisq2 core services.
 * 
 * The service periodically checks for stale trades and sends chat messages to trigger
 * message processing in the Bisq2 core, ensuring trade states are properly synchronized.
 */
@Slf4j
public class TradeSynchronizationService implements Service {
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public TradeSynchronizationService(BisqEasyTradeService bisqEasyTradeService,
                                     BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService) {
        this.bisqEasyTradeService = bisqEasyTradeService;
        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("Initializing trade synchronization service");

        // Run immediate sync on startup, then every 30 seconds for more responsive updates
        scheduler.scheduleWithFixedDelay(this::synchronizeTradeStates, 0, 30, TimeUnit.SECONDS);

        log.info("Trade synchronization service scheduled - immediate start, then every 30 seconds");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("Shutting down trade synchronization service");
        scheduler.shutdown();
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Synchronizes trade states by checking for stale trades and sending sync requests.
     */
    private void synchronizeTradeStates() {
        try {
            log.debug("Running trade state synchronization check");

            var openTrades = bisqEasyTradeService.getTrades().stream()
                .filter(trade -> !trade.getTradeState().isFinalState())
                .toList();

            log.debug("Found {} total trades, {} open trades",
                bisqEasyTradeService.getTrades().size(), openTrades.size());

            if (openTrades.isEmpty()) {
                log.debug("No open trades to synchronize");
                return;
            }

            // Log all open trades for debugging
            openTrades.forEach(trade -> {
                long age = System.currentTimeMillis() - trade.getContract().getTakeOfferDate();
                log.debug("Open trade {} - state: {}, age: {}s",
                    trade.getShortId(), trade.getTradeState(), age/1000);
            });

            int syncCount = 0;
            for (BisqEasyTrade trade : openTrades) {
                try {
                    if (shouldSynchronizeTrade(trade)) {
                        log.debug("Trade {} needs synchronization, requesting sync", trade.getShortId());
                        requestTradeStateSync(trade);
                        syncCount++;
                    } else {
                        log.debug("Trade {} does not need synchronization", trade.getShortId());
                    }
                } catch (Exception e) {
                    log.error("Error checking trade {} for synchronization", trade.getId(), e);
                }
            }

            log.debug("Trade synchronization completed - sent {} sync requests", syncCount);
        } catch (Exception e) {
            log.error("Error during trade state synchronization", e);
        }
    }

    /**
     * Determines if a trade should be synchronized based on its state and timing.
     * Uses the same logic as the mobile implementations for consistency.
     */
    private boolean shouldSynchronizeTrade(BisqEasyTrade trade) {
        BisqEasyTradeState currentState = trade.getTradeState();
        long timeSinceCreation = System.currentTimeMillis() - trade.getContract().getTakeOfferDate();
        
        // Always sync trades that have been open for more than 5 minutes
        if (timeSinceCreation > 5 * 60 * 1000) {
            return true;
        }
        
        // Sync ongoing trades after 60 seconds
        if (timeSinceCreation > 60 * 1000) {
            return true;
        }
        
        // Sync trades in quick-progress states after 30 seconds
        Set<BisqEasyTradeState> quickProgressStates = Set.of(
            BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
        );
        
        if (quickProgressStates.contains(currentState) && timeSinceCreation > 30 * 1000) {
            return true;
        }
        
        return false;
    }

    /**
     * Requests a trade state synchronization by sending a chat message.
     * This triggers the Bisq2 core to process any pending trade protocol messages.
     */
    private void requestTradeStateSync(BisqEasyTrade trade) {
        try {
            String tradeId = trade.getId();
            var channelOptional = bisqEasyOpenTradeChannelService.findChannel(tradeId);
            
            if (channelOptional.isPresent()) {
                log.debug("Sending sync request for trade {}", tradeId);

                // Send a system message to trigger message processing
                String syncMessage = "Synchronizing trade state...";
                bisqEasyOpenTradeChannelService.sendTradeLogMessage(syncMessage, channelOptional.get());

                log.debug("Sync request sent for trade {}", tradeId);
            } else {
                log.warn("No channel found for trade {}, cannot request sync", tradeId);
            }
        } catch (Exception e) {
            log.error("Error requesting trade state sync for trade {}", trade.getId(), e);
        }
    }
}
