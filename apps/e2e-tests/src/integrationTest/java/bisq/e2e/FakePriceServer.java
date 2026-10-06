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

package bisq.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A price node on localhost. It answers getAllMarketPrices with fixed prices and the current time, in the format of
 * the Bisq price nodes, which both versions parse (MarketPriceRequestService) and which the reference time service
 * reads (the fields ending with Ts). The nodes of a test network use it instead of the real price nodes, so the prices
 * are the same on all nodes and never older than the limit of 12 hours.
 */
final class FakePriceServer implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final Map<String, Double> pricesByCurrency = new LinkedHashMap<>();
    // The nodes reject prices older than 12 hours and use the timestamps as reference time. With a fake clock of the
    // nodes, the price node uses the same offset.
    private final long clockOffsetMillis;

    FakePriceServer() throws IOException {
        this(Duration.ZERO);
    }

    FakePriceServer(Duration clockOffset) throws IOException {
        clockOffsetMillis = clockOffset.toMillis();
        pricesByCurrency.put("USD", 65_000.0);
        pricesByCurrency.put("EUR", 60_000.0);
        pricesByCurrency.put("GBP", 52_000.0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/getAllMarketPrices", exchange -> {
            byte[] body = response().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
    }

    int getPort() {
        return server.getAddress().getPort();
    }

    String getUrl() {
        return "http://127.0.0.1:" + getPort();
    }

    double getPrice(String currencyCode) {
        return pricesByCurrency.get(currencyCode);
    }

    private String response() {
        long now = System.currentTimeMillis() + clockOffsetMillis;
        ObjectNode root = JSON.createObjectNode();
        ArrayNode data = root.putArray("data");
        pricesByCurrency.forEach((currencyCode, price) -> data.addObject()
                .put("currencyCode", currencyCode)
                .put("price", price)
                // The price nodes send milliseconds in this field, as a JSON number with a fraction
                .put("timestampSec", (double) now)
                .put("provider", "COINGECKO"));
        root.put("coingeckoTs", now);
        root.put("coingeckoCount", pricesByCurrency.size());
        return root.toString();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
