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

package bisq.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Provides a single, centrally configured {@link ObjectMapper} instance for the entire application.
 *
 * <p>
 * This class defines the canonical JSON configuration used across all API layers,
 * including REST endpoints, WebSocket handlers, pairing flows, and internal protocol
 * serialization. Using a single shared {@code ObjectMapper} ensures consistent
 * behavior for:
 * </p>
 *
 * <ul>
 *   <li>Date and time serialization (e.g. {@link java.time.Instant})</li>
 *   <li>JSON formatting and compatibility guarantees</li>
 *   <li>Future extensions such as custom modules or naming strategies</li>
 * </ul>
 *
 * <p>
 * All components that perform JSON serialization or deserialization <strong>must</strong>
 * obtain their mapper via {@link #get()} and must not create ad-hoc {@code ObjectMapper}
 * instances. This avoids subtle inconsistencies, duplicated configuration, and
 * version-dependent behavior differences.
 * </p>
 *
 * <p>
 * The provided mapper is immutable after initialization and safe for concurrent use.
 * </p>
 */
public final class JsonMapperProvider {
    private static final ObjectMapper MAPPER = create();

    private static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Returns the globally configured {@link ObjectMapper}.
     *
     * @return the shared application-wide ObjectMapper instance
     */
    public static ObjectMapper get() {
        return MAPPER;
    }
}
