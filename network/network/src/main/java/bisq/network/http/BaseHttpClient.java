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

package bisq.network.http;

import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import bisq.network.http.utils.HttpMethod;
import bisq.network.http.utils.ResponseBodyTooLargeException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class BaseHttpClient implements HttpClient {
    public final String baseUrl;
    /**
     * Redacted variant of {@link #baseUrl} used for log statements and
     * exception messages. Subclasses MUST log {@code logBaseUrl} (never
     * {@code baseUrl}) so that secrets in the path — device tokens, session
     * ids, etc. — never reach log files. When the caller has nothing to
     * redact, it can pass the same value as {@code baseUrl}.
     */
    public final String logBaseUrl;
    public final String userAgent;
    protected final String uid;
    // The largest response body which the request service reads for its endpoint
    protected final int maxResponseBodyBytes;

    public boolean hasPendingRequest;

    public BaseHttpClient(String baseUrl, String logBaseUrl, String userAgent, int maxResponseBodyBytes) {
        checkArgument(maxResponseBodyBytes > 0, "maxResponseBodyBytes must be positive");
        this.baseUrl = baseUrl;
        this.logBaseUrl = logBaseUrl;
        this.userAgent = userAgent;
        this.maxResponseBodyBytes = maxResponseBodyBytes;

        uid = StringUtils.createUid();
    }

    @Override
    public String get(String param, Optional<Pair<String, String>> optionalHeader) throws IOException {
        return doRequest(param, HttpMethod.GET, optionalHeader);
    }

    @Override
    public String post(String param, Optional<Pair<String, String>> optionalHeader) throws IOException {
        return doRequest(param, HttpMethod.POST, optionalHeader);
    }

    @Override
    public boolean hasPendingRequest() {
        return hasPendingRequest;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getLogBaseUrl() {
        return logBaseUrl;
    }

    protected abstract String doRequest(String param,
                                        HttpMethod httpMethod,
                                        Optional<Pair<String, String>> optionalHeader) throws IOException;

    /**
     * Reads the body, but throws {@link ResponseBodyTooLargeException} once it has more than
     * {@link #maxResponseBodyBytes} bytes. The limit counts the bytes which the client reads from the stream of the body,
     * after the transfer and content decoding, so a compressed body cannot grow past it. After the limit the stream is
     * not closed, see {@link LimitedInputStream#close()}: the client has to discard the connection.
     */
    protected String inputStreamToString(InputStream inputStream) throws IOException {
        InputStream limitedInputStream = new LimitedInputStream(inputStream, maxResponseBodyBytes);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(limitedInputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

    protected boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long numBytesRead;
        private boolean isLimitExceeded;

        private LimitedInputStream(InputStream inputStream, long maxBytes) {
            super(inputStream);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int numBytes = super.read(buffer, offset, length);
            if (numBytes > 0) {
                count(numBytes);
            }
            return numBytes;
        }

        @Override
        public long skip(long length) throws IOException {
            long numBytes = super.skip(length);
            if (numBytes > 0) {
                count(numBytes);
            }
            return numBytes;
        }

        // Closing the body of an Apache response reads the rest of the body, so that the connection can be reused,
        // also a body without end. Above the limit the stream stays open, and the client discards the connection.
        @Override
        public void close() throws IOException {
            if (!isLimitExceeded) {
                super.close();
            }
        }

        private void count(long numBytes) throws ResponseBodyTooLargeException {
            numBytesRead += numBytes;
            if (numBytesRead > maxBytes) {
                isLimitExceeded = true;
                throw new ResponseBodyTooLargeException(maxBytes);
            }
        }
    }
}
