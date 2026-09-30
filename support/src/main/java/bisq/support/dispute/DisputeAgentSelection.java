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

package bisq.support.dispute;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.security.DigestUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public final class DisputeAgentSelection {
    private DisputeAgentSelection() {
    }

    // Sort by profile ID first so all peers derive the same candidate order before selecting by index.
    public static Optional<String> selectDeterministicProfileId(Set<AuthorizedBondedRole> candidates,
                                                                String makersProfileId,
                                                                String takersProfileId,
                                                                String offerId) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() == 1) {
            return Optional.of(candidates.iterator().next().getProfileId());
        }

        ArrayList<AuthorizedBondedRole> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(Comparator.comparing(AuthorizedBondedRole::getProfileId));
        int index = getDeterministicIndex(makersProfileId, takersProfileId, offerId, sortedCandidates.size());
        return Optional.of(sortedCandidates.get(index).getProfileId());
    }

    // XOR multiple 4-byte chunks so the full 20-byte hash contributes to the deterministic selection.
    private static int getDeterministicIndex(String makersProfileId,
                                            String takersProfileId,
                                            String offerId,
                                            int candidateCount) {
        String input = makersProfileId + takersProfileId + offerId;
        byte[] hash = DigestUtil.hash(input.getBytes(StandardCharsets.UTF_8)); // returns 20 bytes
        // XOR multiple 4-byte chunks to use more of the hash
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        int space = buffer.getInt(); // First 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with last 4 bytes (20 bytes total)
        return Math.floorMod(space, candidateCount);
    }
}
