package bisq.oracle_node.market_price;

import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceRequestService;
import bisq.common.application.Service;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MarketPricePropagationService implements Service {
    private final NetworkService networkService;
    private final MarketPriceRequestService marketPriceRequestService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final boolean staticPublicKeysProvided;
    @Nullable
    private Pin marketPriceByCurrencyMapPin;

    public MarketPricePropagationService(NetworkService networkService,
                                         MarketPriceRequestService marketPriceRequestService,
                                         PrivateKey authorizedPrivateKey,
                                         PublicKey authorizedPublicKey,
                                         boolean staticPublicKeysProvided) {
        this.networkService = networkService;
        this.marketPriceRequestService = marketPriceRequestService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        marketPriceByCurrencyMapPin = marketPriceRequestService.getMarketPriceByCurrencyMap().addObserver(() -> {
            if (!marketPriceRequestService.getMarketPriceByCurrencyMap().isEmpty()) {
                TreeMap<Market, MarketPrice> marketPriceByCurrencyMap = new TreeMap<>(marketPriceRequestService.getMarketPriceByCurrencyMap());
                AuthorizedMarketPriceData data = new AuthorizedMarketPriceData(marketPriceByCurrencyMap, staticPublicKeysProvided);
                publishAuthorizedData(data);
            }
        });

        return marketPriceRequestService.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (marketPriceByCurrencyMapPin != null) {
            marketPriceByCurrencyMapPin.unbind();
            marketPriceByCurrencyMapPin = null;
        }
        return marketPriceRequestService.shutdown();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        // The authorized key owns the authorized data which this node publishes (see
        // docs/specifications/network/storage-ownership.md)
        return networkService.publishAuthorizedData(data, new KeyPair(authorizedPublicKey, authorizedPrivateKey))
                .thenApply(broadCastDataResult -> true);
    }

}
