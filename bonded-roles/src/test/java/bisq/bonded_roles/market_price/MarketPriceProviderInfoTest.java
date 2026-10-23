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

package bisq.bonded_roles.market_price;

import bisq.common.monetary.PriceQuote;
import bisq.i18n.Res;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A market price must not need the i18n resources until its provider name is displayed. Otherwise parsing and
 * creating market prices depends on the order in which a process loads its classes, and a class initializer which
 * fails because the resources are not loaded yet makes every later use of that class fail in the same JVM.
 */
class MarketPriceProviderInfoTest {
    private static final List<bisq.bonded_roles.protobuf.MarketPriceProvider> PROTO_PROVIDERS = List.of(
            bisq.bonded_roles.protobuf.MarketPriceProvider.MARKETPRICEPROVIDER_BISQAGGREGATE,
            bisq.bonded_roles.protobuf.MarketPriceProvider.MARKETPRICEPROVIDER_COINGECKO,
            bisq.bonded_roles.protobuf.MarketPriceProvider.MARKETPRICEPROVIDER_POLO,
            bisq.bonded_roles.protobuf.MarketPriceProvider.MARKETPRICEPROVIDER_BITFINEX,
            bisq.bonded_roles.protobuf.MarketPriceProvider.MARKETPRICEPROVIDER_OTHER);

    @BeforeAll
    static void setUpResources() {
        // Only the display name needs the resources. The test without resources uses a Res of its own.
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void marketPriceIsParsedAndSerializedWithoutLoadedResources() throws Exception {
        Class<?> marketPriceClass = Class.forName(MarketPrice.class.getName(), true, new IsolatingClassLoader());
        Method fromProto = marketPriceClass.getMethod("fromProto", bisq.bonded_roles.protobuf.MarketPrice.class);
        Method toProto = marketPriceClass.getMethod("toProto", boolean.class);

        for (bisq.bonded_roles.protobuf.MarketPriceProvider protoProvider : PROTO_PROVIDERS) {
            bisq.bonded_roles.protobuf.MarketPrice proto = bisq.bonded_roles.protobuf.MarketPrice.newBuilder()
                    .setPriceQuote(PriceQuote.fromFiatPrice(60000, "USD").toProto(false))
                    .setTimestamp(System.currentTimeMillis())
                    .setMarketPriceProvider(protoProvider)
                    .build();

            Object marketPrice = fromProto.invoke(null, proto);

            assertThat(toProto.invoke(marketPrice, false)).isEqualTo(proto);
        }
    }

    @Test
    void displayNameOfAKnownProvider() {
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE).getDisplayName())
                .isEqualTo("Bisq price aggregator");
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.COINGECKO).getDisplayName()).isEqualTo("CoinGecko");
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.POLO).getDisplayName()).isEqualTo("Poloniex");
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.BITFINEX).getDisplayName()).isEqualTo("Bitfinex");
    }

    @Test
    void displayNameOfAnUnknownProvider() {
        // A price node reported a provider which MarketPriceProvider does not know
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.OTHER, "KRAKEN").getDisplayName())
                .isEqualTo("KRAKEN");
        // The network data and the persisted data carry only OTHER, not the name which the price node reported
        assertThat(new MarketPriceProviderInfo(MarketPriceProvider.OTHER).getDisplayName()).isEqualTo("N/A");
    }

    /**
     * Defines the market price classes and Res again. The test then sees a Res without bundles, and the class
     * initializers of the market price classes run with it, whatever other tests did before in this JVM.
     */
    private static final class IsolatingClassLoader extends ClassLoader {
        private static final List<String> ISOLATED_PACKAGES = List.of("bisq.bonded_roles.market_price.", "bisq.i18n.");

        private IsolatingClassLoader() {
            super(MarketPriceProviderInfoTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (ISOLATED_PACKAGES.stream().noneMatch(name::startsWith)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass == null) {
                    loadedClass = defineIsolatedClass(name);
                }
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }

        private Class<?> defineIsolatedClass(String name) throws ClassNotFoundException {
            try (InputStream inputStream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (inputStream == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = inputStream.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
