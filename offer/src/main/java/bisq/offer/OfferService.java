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

package bisq.offer;

import bisq.account.FiatSettlement;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.contract.SwapProtocolType;
import bisq.network.NetworkId;
import bisq.network.p2p.INetworkService;
import bisq.network.p2p.MockNetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.offer.options.*;
import bisq.security.PubKey;
import io.reactivex.subjects.PublishSubject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OfferService {
    private final List<Listing> offers = new CopyOnWriteArrayList<>();
    protected final PublishSubject<Listing> offerAddedSubject;
    protected final PublishSubject<Listing> offerRemovedSubject;
    private final INetworkService networkService;

    public OfferService(INetworkService networkService) {
        this.networkService = networkService;

        offerAddedSubject = PublishSubject.create();
        offerRemovedSubject = PublishSubject.create();
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        //todo

        offers.addAll(MockOfferBuilder.makeOffers().values());

        networkService.addListener(new MockNetworkService.Listener() {
            @Override
            public void onDataAdded(Serializable serializable) {
                if (serializable instanceof Listing offer) {
                    offers.add(offer);
                    offerAddedSubject.onNext(offer);
                }
            }

            @Override
            public void onDataRemoved(Serializable serializable) {
                if (serializable instanceof Listing offer) {
                    offers.remove(offer);
                    offerRemovedSubject.onNext(offer);
                }
            }
        });

        future.complete(true);
        return future;
    }

    public List<Listing> getOffers() {
        return offers;
    }

    public PublishSubject<Listing> getOfferAddedSubject() {
        return offerAddedSubject;
    }

    public PublishSubject<Listing> getOfferRemovedSubject() {
        return offerRemovedSubject;
    }

    public SwapOffer createOffer(long askAmount) {
        NetworkId makerNetworkId = new NetworkId(Map.of(Transport.Type.CLEAR, Address.localHost(3333)), new PubKey(null, "default"), "default");
        Leg askLeg = new Leg(Coin.asBtc(askAmount), List.of());
        Leg bidLeg = new Leg(Fiat.of(5000, "USD"), List.of(FiatSettlement.ZELLE));
        return new SwapOffer(bidLeg, 
                askLeg, 
                "USD",
                List.of(SwapProtocolType.REPUTATION, SwapProtocolType.MULTISIG), 
                makerNetworkId);
    }

    public void publishOffer(SwapOffer offer) {
        networkService.addData(offer);
    }

    public void shutdown() {
    }

    public static class MockOfferBuilder {

        @Getter
        private final static Map<String, Listing> data = new HashMap<>();

        public static Map<String, Listing> makeOffers() {
            for (int i = 0; i < 10; i++) {
                SwapOffer offer = getRandomOffer();
                data.put(offer.getId(), offer);
            }
    
           /* new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    int toggle = new Random().nextInt(2);
                    if (toggle == 0) {
                        int iter = new Random().nextInt(3);
                        for (int i = 0; i < iter; i++) {
                            Serializable offer = getRandomOffer();
                            data.put(offer.getId(), offer);
                            listeners.forEach(l -> l.onOfferAdded(offer));
                        }
                    } else {
                        int iter2 = new Random().nextInt(2);
                        for (int i = 0; i < iter2; i++) {
                            if (!data.isEmpty()) {
                                Serializable offerToRemove = getOfferToRemove();
                                data.remove(offerToRemove.getId());
                                listeners.forEach(l -> l.onOfferRemoved(offerToRemove));
                            }
                        }
                    }
                }
            }, 0, 500);*/
            return data;
        }

        private static SwapOffer getRandomOffer() {
            Leg askLeg;
            Leg bidLeg;
            Optional<Double> marketBasedPrice = Optional.empty();
            Optional<Double> minAmountAsPercentage = Optional.empty();
            String baseCurrency;
            //  int rand = new Random().nextInt(3);
            int rand = new Random().nextInt(2);
            //  rand = 0;
            if (rand == 0) {
                long usdAmount = new Random().nextInt(1000) + 500000000; // precision 4 / 50k usd
                long btcAmount = new Random().nextInt(100000000) + 100000000; // precision 8 / 1 btc
                usdAmount = 370000000; // precision 4 / 50k usd
                btcAmount = 100000000; // precision 8 / 1 btc
                askLeg = getRandomFiatAsset("USD", usdAmount);
                bidLeg = getRandomCryptoAsset("BTC", btcAmount);
                baseCurrency = "BTC";
                marketBasedPrice = Optional.of(new Random().nextInt(100) / 1000d - 0.05d); // +/- 5%
                // marketBasedPrice = Optional.empty();
                minAmountAsPercentage = new Random().nextBoolean() ? Optional.empty() : Optional.of(0.1);
                // minAmountAsPercentage = Optional.empty();
            } else if (rand == 1) {
                long usdAmount = new Random().nextInt(1000) + 600000000; // precision 4 / 50k usd
                long btcAmount = new Random().nextInt(100000000) + 110000000; // precision 8 / 1 btc
                usdAmount = 370000000; // precision 4 / 50k usd
                btcAmount = 100000000; // precision 8 / 1 btc
                askLeg = getRandomCryptoAsset("BTC", btcAmount);
                bidLeg = getRandomFiatAsset("USD", usdAmount);
                baseCurrency = "BTC";
                marketBasedPrice = Optional.of(new Random().nextInt(100) / 10000d - 0.005d); // +/- 0.5%
                marketBasedPrice = Optional.empty();
                minAmountAsPercentage = new Random().nextBoolean() ? Optional.empty() : Optional.of(0.1);
                // minAmountAsPercentage = Optional.empty();
            } else if (rand == 2) {
                long usdAmount = new Random().nextInt(100000) + 1200000; // precision 4 / 120 usd
                long eurAmount = new Random().nextInt(100000) + 1000000; // precision 4 / 100 eur
                askLeg = getRandomFiatAsset("USD", usdAmount);
                bidLeg = getRandomFiatAsset("EUR", eurAmount);
                baseCurrency = "USD";

            } else {
                // ignore for now as fiat/altcoins calculations not supported and only one market price
                long btcAmount = new Random().nextInt(10000000) + 100000000; // precision 8 / 1 btc //0.007144 BTC
                long xmrAmount = new Random().nextInt(10000000) + 13800000000L; // precision 8 / 138 xmr
                bidLeg = getRandomCryptoAsset("BTC", btcAmount);
                askLeg = getRandomCryptoAsset("XMR", xmrAmount);
                baseCurrency = "XMR";
                marketBasedPrice = Optional.of(-0.02);
                minAmountAsPercentage = Optional.of(0.8);
            }
            List<SwapProtocolType> protocolTypes = new ArrayList<>();
            rand = new Random().nextInt(3);
            for (int i = 0; i < rand; i++) {
                SwapProtocolType swapProtocolType = SwapProtocolType.values()[new Random().nextInt(SwapProtocolType.values().length)];
                protocolTypes.add(swapProtocolType);
            }
            Map<Transport.Type, Address> map = Map.of(Transport.Type.CLEAR, Address.localHost(1000 + new Random().nextInt(1000)));
            NetworkId makerNetworkId = new NetworkId(map, new PubKey(null, "default"), "default");

            Set<OfferOption> options = new HashSet<>();
            ReputationProof accountCreationDateProof = new AccountCreationDateProof("hashOfAccount", "otsProof)");
            ReputationOption reputationOptions = new ReputationOption(Set.of(accountCreationDateProof));
            options.add(reputationOptions);

            TransferOption transferOptions = new Random().nextBoolean() ?
                    new TransferOption("USA", "HSBC") :
                    new Random().nextBoolean() ? new TransferOption("DE", "N26") :
                            null;
            if (transferOptions != null) {
                options.add(transferOptions);
            }
            minAmountAsPercentage.ifPresent(value -> options.add(new AmountOption(value)));
            marketBasedPrice.ifPresent(value -> options.add(new PriceOption(value)));
            return new SwapOffer(askLeg, bidLeg, askLeg.code(), protocolTypes, makerNetworkId, options);
        }

        private static Leg getRandomCryptoAsset(String code, long amount) {
            List<FiatSettlement> transfers = new ArrayList<>(List.of(FiatSettlement.SEPA, FiatSettlement.ZELLE, FiatSettlement.REVOLUT));
            Collections.shuffle(transfers);
            transfers = List.of(transfers.get(0));
            return new Leg(Coin.of(amount, code), transfers);
        }

        private static Leg getRandomFiatAsset(String code, long amount) {
            List<FiatSettlement> transfers = new ArrayList<>(List.of(FiatSettlement.SEPA, FiatSettlement.ZELLE, FiatSettlement.REVOLUT));
            Collections.shuffle(transfers);
            transfers = List.of(transfers.get(0));
            return new Leg(Fiat.of(amount, code), transfers);
        }

        private static Listing getOfferToRemove() {
            int index = new Random().nextInt(data.size());
            return new ArrayList<>(data.values()).get(index);
        }
    }
}
