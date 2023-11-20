package bisq.oracle_node.market_price;

import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.market_price.MarketPriceRequestService;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
    @Setter
    private Identity identity;
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
            if (marketPriceRequestService.getMarketPriceByCurrencyMap().isEmpty()) {
                return;
            }
            publishAuthorizedData(new AuthorizedMarketPriceData(new TreeMap<>(marketPriceRequestService.getMarketPriceByCurrencyMap()),
                    staticPublicKeysProvided));
        });

        return marketPriceRequestService.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        marketPriceByCurrencyMapPin.unbind();
        return marketPriceRequestService.shutdown();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        log.info("publish {}", StringUtils.truncate(data.toString()));
        return networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair().getKeyPair(),
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

}
