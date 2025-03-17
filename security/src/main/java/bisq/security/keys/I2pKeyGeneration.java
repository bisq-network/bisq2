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

package bisq.security.keys;

public class I2pKeyGeneration {
    public static byte[] generatePrivateKey() {
        @SuppressWarnings("UnnecessaryLocalVariable") byte[] privateKey = new byte[32];
        //todo (deferred) impl
        return privateKey;
    }

    public static I2pKeyPair generateKeyPair() {
        byte[] privateKey = generatePrivateKey();
        return new I2pKeyPair(privateKey, getPublicKey(privateKey));
    }

    private static byte[] getPublicKey(byte[] privateKey) {
        @SuppressWarnings("UnnecessaryLocalVariable") byte[] publicKey = new byte[32];
        //todo (deferred) impl
        return publicKey;
    }


    public static String getDestinationFromPublicKey(byte[] publicKey) {
        // todo (deferred) impl
        return "TODO.destination";
    }
}

