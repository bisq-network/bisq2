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


package bisq.application.migration;

/**
 * Thrown when a migration of the data directory failed and the app does not start, because the data directory is not
 * fully migrated. Starting on such a data directory can make the app misbehave or damage data.
 */
public class IncompleteMigrationException extends RuntimeException {
    public IncompleteMigrationException(String message) {
        super(message);
    }
}
