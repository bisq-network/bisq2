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

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyGenerationTest {

    @Test
    public void generatePublicReturnsSameInstanceForSameEncoding() throws GeneralSecurityException {
        byte[] encoded = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();

        PublicKey first = KeyGeneration.generatePublic(encoded.clone());
        PublicKey second = KeyGeneration.generatePublic(encoded.clone());

        assertSame(first, second);
        assertArrayEquals(encoded, first.getEncoded());
    }

    @Test
    public void generatePublicReturnsDistinctKeysForDistinctEncodings() throws GeneralSecurityException {
        PublicKey first = KeyGeneration.generatePublic(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded());
        PublicKey second = KeyGeneration.generatePublic(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded());

        assertNotEquals(first, second);
    }

    @Test
    public void generatePublicIsNotAffectedByLaterMutationOfCallersArray() throws GeneralSecurityException {
        byte[] encodedA = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        byte[] encodedB = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        byte[] callersArray = encodedA.clone();
        PublicKey publicKeyA = KeyGeneration.generatePublic(callersArray);

        System.arraycopy(encodedB, 0, callersArray, 0, encodedB.length);

        assertSame(publicKeyA, KeyGeneration.generatePublic(encodedA.clone()));
        assertArrayEquals(encodedB, KeyGeneration.generatePublic(callersArray).getEncoded());
    }

    @Test
    public void generatePublicIsNotAffectedByMutationOfReturnedEncoding() throws GeneralSecurityException {
        byte[] encoded = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        PublicKey publicKey = KeyGeneration.generatePublic(encoded.clone());

        publicKey.getEncoded()[0] ^= 1;

        assertArrayEquals(encoded, publicKey.getEncoded());
    }

    @Test
    public void publicKeyCacheKeySpreadsArraysHashCodeCollisions() {
        // Each byte pair is (0, 31) or (1, 0), which adds the same value to Arrays.hashCode
        byte[][] collidingEncodings = IntStream.range(0, 16)
                .mapToObj(bits -> {
                    byte[] encoding = new byte[8];
                    for (int pair = 0; pair < 4; pair++) {
                        boolean bit = ((bits >> pair) & 1) == 1;
                        encoding[2 * pair] = (byte) (bit ? 1 : 0);
                        encoding[2 * pair + 1] = (byte) (bit ? 0 : 31);
                    }
                    return encoding;
                })
                .toArray(byte[][]::new);
        assertEquals(1, Arrays.stream(collidingEncodings).mapToInt(Arrays::hashCode).distinct().count());

        long distinctCacheKeyHashes = Arrays.stream(collidingEncodings)
                .mapToInt(encoding -> new KeyGeneration.PublicKeyCacheKey(encoding).hashCode())
                .distinct()
                .count();

        assertTrue(distinctCacheKeyHashes > 1);
    }

    @Test
    public void generatePublicWithExplicitAlgorithmIsNotServedFromCache() throws GeneralSecurityException {
        byte[] encoded = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        KeyGeneration.generatePublic(encoded);

        assertEquals(KeyGeneration.EC, KeyGeneration.generatePublic(encoded, KeyGeneration.EC).getAlgorithm());
    }

    @Test
    public void generatePublicRejectsInvalidEncoding() {
        assertThrows(GeneralSecurityException.class, () -> KeyGeneration.generatePublic(new byte[]{1, 2, 3}));
    }
}
