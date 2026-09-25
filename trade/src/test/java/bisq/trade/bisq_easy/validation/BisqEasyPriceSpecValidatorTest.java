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

package bisq.trade.bisq_easy.validation;

import bisq.common.monetary.PriceQuote;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BisqEasyPriceSpecValidatorTest {
    private static Stream<PriceSpec> offerPriceSpecs() {
        return Stream.of(new MarketPriceSpec(),
                new FloatPriceSpec(0.02),
                new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR")));
    }

    private static Stream<Arguments> otherContractPriceSpecs() {
        return Stream.of(
                Arguments.of(new MarketPriceSpec(), new FixPriceSpec(PriceQuote.fromFiatPrice(30_000, "EUR"))),
                Arguments.of(new MarketPriceSpec(), new FloatPriceSpec(-0.5)),
                Arguments.of(new FloatPriceSpec(0.02), new FloatPriceSpec(-0.5)),
                Arguments.of(new FloatPriceSpec(0.02), new MarketPriceSpec()),
                Arguments.of(new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR")),
                        new FixPriceSpec(PriceQuote.fromFiatPrice(30_000, "EUR"))),
                Arguments.of(new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR")), new MarketPriceSpec()),
                Arguments.of(new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR")),
                        new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "USD"))));
    }

    @ParameterizedTest
    @MethodSource("offerPriceSpecs")
    void acceptsTheOfferPriceSpecReceivedFromTheNetwork(PriceSpec offerPriceSpec) throws UnresolvableProtobufException {
        PriceSpec contractPriceSpec = PriceSpec.fromProto(offerPriceSpec.toProto(false));

        assertDoesNotThrow(() -> BisqEasyPriceSpecValidator.validatePriceSpec(offerPriceSpec, contractPriceSpec));
    }

    @Test
    void acceptsAnEqualPriceSpecOfAnotherInstance() {
        assertDoesNotThrow(() -> BisqEasyPriceSpecValidator.validatePriceSpec(new FloatPriceSpec(0.02),
                new FloatPriceSpec(2 / 100.0)));
        assertDoesNotThrow(() -> BisqEasyPriceSpecValidator.validatePriceSpec(
                new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR")),
                new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "EUR"))));
    }

    @ParameterizedTest
    @MethodSource("otherContractPriceSpecs")
    void rejectsAnotherPriceSpecThanTheOffers(PriceSpec offerPriceSpec, PriceSpec contractPriceSpec) {
        assertThrows(IllegalArgumentException.class,
                () -> BisqEasyPriceSpecValidator.validatePriceSpec(offerPriceSpec, contractPriceSpec));
    }
}
