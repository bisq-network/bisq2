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

package bisq.persistence;

/**
 * What the node does with a store file which this version cannot read completely (see
 * docs/specifications/persistence/unreadable-store-files.md).
 */
public enum UnreadableStoreFilePolicy {
    /** Keep a copy in the {@link UnreadableStoreArchive}. The default for the user's own data. */
    ARCHIVE,
    /**
     * As for network and cache data: a skipped entry is only logged, and a file which cannot be read at all is moved
     * into {@code corruptedFilesAtRead} next to the store.
     */
    NO_ARCHIVE,
    /**
     * Keep nothing: a skipped entry is only logged, and a file which cannot be read at all is deleted. For data which
     * must not outlive the deletion which the store does on purpose.
     */
    DELETE
}
