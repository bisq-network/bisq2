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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig.messages.grpc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomPayoutPsbtTest {
    private static final String TX_ID = "ab".repeat(32);

    @Test
    void givenValidData_whenConstructing_thenAcceptsValue() {
        assertThatCode(() -> new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                89_900,
                59_900))
                .doesNotThrowAnyException();
    }

    @Test
    void givenMissingOrInvalidTransactionId_whenConstructing_thenRejectsValue() {
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                null,
                89_900,
                59_900))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                "invalid-tx-id",
                89_900,
                59_900))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenMissingOrEmptyPsbt_whenConstructing_thenRejectsValue() {
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                null,
                TX_ID,
                89_900,
                59_900))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                new byte[0],
                TX_ID,
                89_900,
                59_900))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNegativePayout_whenConstructing_thenRejectsValue() {
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                -1,
                59_900))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                89_900,
                -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
