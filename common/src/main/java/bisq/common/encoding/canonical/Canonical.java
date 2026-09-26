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

package bisq.common.encoding.canonical;

/**
 * An object that defines its canonical encoding: the deterministic bytes of its hash and signature preimages
 * (see docs/specifications/encoding/canonical-encoding.md).
 * <p>
 * The {@link EncodingPurpose} is the purpose of the whole preimage. It applies to the object on which
 * {@code encodeCanonical} is called and to all objects nested in it: {@link CanonicalEncoder} passes it to every
 * nested schema and to the {@code encodeCanonical} of the value of a {@link CanonicalSchema.Builder#canonical
 * canonical} field. An implementation that encodes a nested {@code Canonical} itself passes the purpose that it
 * received on to the nested object.
 * <p>
 * The encoding of an object for a purpose therefore applies wherever the object appears in a preimage for that
 * purpose, also as a nested object. It may leave out only data that no such preimage must protect. For example, it
 * must not leave out the object's own signature if an enclosing preimage for the same purpose must cover that
 * signature.
 */
public interface Canonical {
    /**
     * @param encodingPurpose the purpose of the preimage. The implementation selects its encoding for the purpose and
     *                        passes the purpose on to its nested objects.
     */
    byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose);

    default byte[] encodeCanonical() {
        return encodeCanonical(CanonicalEncoder.DEFAULT, EncodingPurpose.DEFAULT);
    }

    default byte[] encodeCanonical(EncodingPurpose encodingPurpose) {
        return encodeCanonical(CanonicalEncoder.DEFAULT, encodingPurpose);
    }

    default byte[] encodeCanonical(CanonicalEncoder canonicalEncoder) {
        return encodeCanonical(canonicalEncoder, EncodingPurpose.DEFAULT);
    }
}
