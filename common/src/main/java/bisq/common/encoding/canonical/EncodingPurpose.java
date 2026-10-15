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
 * The purpose of a canonical encoding, for example a signature preimage. A purpose applies to the whole preimage: to
 * the object on which {@link Canonical#encodeCanonical(CanonicalEncoder, EncodingPurpose)} is called and to all
 * objects nested in it (see {@link Canonical}).
 */
public enum EncodingPurpose {
    /**
     * The complete encoding of the object, with all its encoded fields. Used when no specific purpose applies.
     */
    DEFAULT,
    /**
     * The preimage of a signature: the bytes that are signed and verified.
     */
    SIGNATURE,
    /**
     * The preimage of a storage key: the bytes that are hashed to the key under which an object is stored.
     */
    STORAGE_KEY,
    /**
     * The preimage of the proof of work payload of a network message: the bytes of the message that the proof of work
     * of the message's authorization token binds to the message.
     */
    PROOF_OF_WORK,
    /**
     * The preimage of the hash of a trade contract: the bytes of the contract that are hashed to the contract hash,
     * which the traders sign.
     */
    TRADE_CONTRACT,
}
