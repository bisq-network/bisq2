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

package bisq.offer.options;

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.ByteArrayUtils;
import bisq.security.DigestUtil;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class OfferOptionUtilTest {

    @Test
    void createSaltedAccountPayloadHashUsesSerializedPayloadForHashAndOfferId() {
        byte[] serializedForHash = new byte[]{1, 2, 3, 4};
        TestAccountPayload accountPayload = new TestAccountPayload(serializedForHash);
        String offerId = "offer-123";

        byte[] result = OfferOptionUtil.createSaltedAccountPayloadHash(accountPayload, offerId);

        byte[] expected = DigestUtil.hash(ByteArrayUtils.concat(
                serializedForHash,
                offerId.getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals(expected, result);
    }

    @Test
    void createSaltedAccountPayloadHashChangesWhenOfferIdChanges() {
        TestAccountPayload accountPayload = new TestAccountPayload(new byte[]{9, 8, 7});

        byte[] first = OfferOptionUtil.createSaltedAccountPayloadHash(accountPayload, "offer-1");
        byte[] second = OfferOptionUtil.createSaltedAccountPayloadHash(accountPayload, "offer-2");

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void createSaltedAccountPayloadHashChangesWhenSerializedPayloadForHashChanges() {
        String offerId = "offer-123";

        byte[] first = OfferOptionUtil.createSaltedAccountPayloadHash(
                new TestAccountPayload(new byte[]{1, 2, 3}),
                offerId);
        byte[] second = OfferOptionUtil.createSaltedAccountPayloadHash(
                new TestAccountPayload(new byte[]{1, 2, 4}),
                offerId);

        assertFalse(Arrays.equals(first, second));
    }

    private static final class TestAccountPayload extends AccountPayload<PaymentMethod<?>> {
        private final byte[] serializedForHash;

        private TestAccountPayload(byte[] serializedForHash) {
            super("test-account-id", new byte[32]);
            this.serializedForHash = serializedForHash;
        }

        @Override
        public byte[] serializeForHash() {
            return serializedForHash;
        }

        @Override
        public Message.Builder getBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public byte[] getBisq1CompatibleFingerprint() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        protected byte[] getBisq2Fingerprint() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public PaymentMethod<?> getPaymentMethod() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public String getAccountDataDisplayString() {
            throw new UnsupportedOperationException("Not required for this test");
        }
    }
}
