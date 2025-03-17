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

import org.apache.hc.client5.http.EndpointInfo;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.ConnPoolSupport;
import org.apache.hc.client5.http.impl.ConnectionShutdownException;
import org.apache.hc.client5.http.impl.PrefixedIncrementingId;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.io.*;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.Internal;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.*;
import org.apache.hc.core5.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Adapted from https://github.com/apache/httpcomponents-client/blob/master/httpclient5/src/main/java/org/apache/hc/client5/http/impl/io/PoolingHttpClientConnectionManager.java
 * We need this class to use our custom connection operator: {@link TorHttpClientConnectionOperator}
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
class PoolingTorHttpClientConnectionManager
        implements HttpClientConnectionManager, ConnPoolControl<HttpRoute> {

    private static final Logger LOG = LoggerFactory.getLogger(PoolingTorHttpClientConnectionManager.class);

    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 25;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

    private final HttpClientConnectionOperator connectionOperator;
    private final ManagedConnPool<HttpRoute, ManagedHttpClientConnection> pool;
    private final HttpConnectionFactory<ManagedHttpClientConnection> connFactory;
    private final AtomicBoolean closed;

    private volatile Resolver<HttpRoute, SocketConfig> socketConfigResolver;
    private volatile Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver;
    private volatile Resolver<HttpHost, TlsConfig> tlsConfigResolver;

    public PoolingTorHttpClientConnectionManager() {
        this(new TorHttpClientConnectionOperator(null,
                        RegistryBuilder.<TlsSocketStrategy>create()
                                .register(URIScheme.HTTPS.id, DefaultClientTlsStrategy.createDefault())
                                .build()),
                PoolConcurrencyPolicy.STRICT,
                PoolReusePolicy.LIFO,
                TimeValue.NEG_ONE_MILLISECOND,
                null);
    }

    @Internal
    public PoolingTorHttpClientConnectionManager(
            final HttpClientConnectionOperator httpClientConnectionOperator,
            final PoolConcurrencyPolicy poolConcurrencyPolicy,
            final PoolReusePolicy poolReusePolicy,
            final TimeValue timeToLive,
            final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        super();
        this.connectionOperator = Args.notNull(httpClientConnectionOperator, "Connection operator");
        switch (poolConcurrencyPolicy != null ? poolConcurrencyPolicy : PoolConcurrencyPolicy.STRICT) {
            case STRICT:
                this.pool = new StrictConnPool<HttpRoute, ManagedHttpClientConnection>(
                        DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                        DEFAULT_MAX_TOTAL_CONNECTIONS,
                        timeToLive,
                        poolReusePolicy,
                        null) {

                    @Override
                    public void closeExpired() {
                        enumAvailable(e -> closeIfExpired(e));
                    }

                };
                break;
            case LAX:
                this.pool = new LaxConnPool<HttpRoute, ManagedHttpClientConnection>(
                        DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                        timeToLive,
                        poolReusePolicy,
                        null) {

                    @Override
                    public void closeExpired() {
                        enumAvailable(e -> closeIfExpired(e));
                    }

                };
                break;
            default:
                throw new IllegalArgumentException("Unexpected PoolConcurrencyPolicy value: " + poolConcurrencyPolicy);
        }
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public void close() {
        close(CloseMode.GRACEFUL);
    }

    @Override
    public void close(final CloseMode closeMode) {
        if (this.closed.compareAndSet(false, true)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Shutdown connection pool {}", closeMode);
            }
            this.pool.close(closeMode);
            LOG.debug("Connection pool shut down");
        }
    }

    private InternalConnectionEndpoint cast(final ConnectionEndpoint endpoint) {
        if (endpoint instanceof InternalConnectionEndpoint) {
            return (InternalConnectionEndpoint) endpoint;
        }
        throw new IllegalStateException("Unexpected endpoint class: " + endpoint.getClass());
    }

    private SocketConfig resolveSocketConfig(final HttpRoute route) {
        final Resolver<HttpRoute, SocketConfig> resolver = this.socketConfigResolver;
        final SocketConfig socketConfig = resolver != null ? resolver.resolve(route) : null;
        return socketConfig != null ? socketConfig : SocketConfig.DEFAULT;
    }

    private ConnectionConfig resolveConnectionConfig(final HttpRoute route) {
        final Resolver<HttpRoute, ConnectionConfig> resolver = this.connectionConfigResolver;
        final ConnectionConfig connectionConfig = resolver != null ? resolver.resolve(route) : null;
        return connectionConfig != null ? connectionConfig : ConnectionConfig.DEFAULT;
    }

    private TlsConfig resolveTlsConfig(final HttpHost host) {
        final Resolver<HttpHost, TlsConfig> resolver = this.tlsConfigResolver;
        final TlsConfig tlsConfig = resolver != null ? resolver.resolve(host) : null;
        return tlsConfig != null ? tlsConfig : TlsConfig.DEFAULT;
    }

    private TimeValue resolveValidateAfterInactivity(final ConnectionConfig connectionConfig) {
        final TimeValue timeValue = connectionConfig.getValidateAfterInactivity();
        return timeValue != null ? timeValue : TimeValue.ofSeconds(2);
    }

    public LeaseRequest lease(final String id, final HttpRoute route, final Object state) {
        return lease(id, route, Timeout.DISABLED, state);
    }

    @Override
    public LeaseRequest lease(
            final String id,
            final HttpRoute route,
            final Timeout requestTimeout,
            final Object state) {
        Args.notNull(route, "HTTP route");
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} endpoint lease request ({}) {}", id, requestTimeout, ConnPoolSupport.formatStats(route, state, pool));
        }
        final Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> leaseFuture = this.pool.lease(route, state, requestTimeout, null);
        return new LeaseRequest() {
            // Using a ReentrantLock specific to each LeaseRequest instance to maintain the original
            // synchronization semantics. This ensures that each LeaseRequest has its own unique lock.
            private final ReentrantLock lock = new ReentrantLock();
            private volatile ConnectionEndpoint endpoint;

            @Override
            public ConnectionEndpoint get(
                    final Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException {
                lock.lock();
                try {
                    Args.notNull(timeout, "Operation timeout");
                    if (this.endpoint != null) {
                        return this.endpoint;
                    }
                    final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry;
                    try {
                        poolEntry = leaseFuture.get(timeout.getDuration(), timeout.getTimeUnit());
                    } catch (final TimeoutException ex) {
                        leaseFuture.cancel(true);
                        throw ex;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} endpoint leased {}", id, ConnPoolSupport.formatStats(route, state, pool));
                    }
                    final ConnectionConfig connectionConfig = resolveConnectionConfig(route);
                    try {
                        if (poolEntry.hasConnection()) {
                            final TimeValue timeToLive = connectionConfig.getTimeToLive();
                            if (TimeValue.isNonNegative(timeToLive)) {
                                if (timeToLive.getDuration() == 0
                                        || Deadline.calculate(poolEntry.getCreated(), timeToLive).isExpired()) {
                                    poolEntry.discardConnection(CloseMode.GRACEFUL);
                                }
                            }
                        }
                        if (poolEntry.hasConnection()) {
                            final TimeValue timeValue = resolveValidateAfterInactivity(connectionConfig);
                            if (TimeValue.isNonNegative(timeValue)) {
                                if (timeValue.getDuration() == 0
                                        || Deadline.calculate(poolEntry.getUpdated(), timeValue).isExpired()) {
                                    final ManagedHttpClientConnection conn = poolEntry.getConnection();
                                    boolean stale;
                                    try {
                                        stale = conn.isStale();
                                    } catch (final IOException ignore) {
                                        stale = true;
                                    }
                                    if (stale) {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("{} connection {} is stale", id, ConnPoolSupport.getId(conn));
                                        }
                                        poolEntry.discardConnection(CloseMode.IMMEDIATE);
                                    }
                                }
                            }
                        }
                        final ManagedHttpClientConnection conn = poolEntry.getConnection();
                        if (conn != null) {
                            conn.activate();
                        } else {
                            poolEntry.assignConnection(connFactory.createConnection(null));
                        }
                        this.endpoint = new InternalConnectionEndpoint(poolEntry);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("{} acquired {}", id, ConnPoolSupport.getId(endpoint));
                        }
                        return this.endpoint;
                    } catch (final Exception ex) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("{} endpoint lease failed", id);
                        }
                        pool.release(poolEntry, false);
                        throw new ExecutionException(ex.getMessage(), ex);
                    }
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public boolean cancel() {
                return leaseFuture.cancel(true);
            }

        };

    }

    @Override
    public void release(final ConnectionEndpoint endpoint, final Object state, final TimeValue keepAlive) {
        Args.notNull(endpoint, "Managed endpoint");
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> entry = cast(endpoint).detach();
        if (entry == null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} releasing endpoint", ConnPoolSupport.getId(endpoint));
        }

        if (this.isClosed()) {
            return;
        }

        final ManagedHttpClientConnection conn = entry.getConnection();
        if (conn != null && keepAlive == null) {
            conn.close(CloseMode.GRACEFUL);
        }
        boolean reusable = conn != null && conn.isOpen() && conn.isConsistent();
        try {
            if (reusable) {
                entry.updateState(state);
                entry.updateExpiry(keepAlive);
                conn.passivate();
                if (LOG.isDebugEnabled()) {
                    final String s;
                    if (TimeValue.isPositive(keepAlive)) {
                        s = "for " + keepAlive;
                    } else {
                        s = "indefinitely";
                    }
                    LOG.debug("{} connection {} can be kept alive {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.getId(conn), s);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    if (conn != null && !conn.isConsistent()) {
                        LOG.debug("{} connection is in an inconsistent state and cannot be kept alive)", ConnPoolSupport.getId(endpoint));
                    } else {
                        LOG.debug("{} connection is not kept alive)", ConnPoolSupport.getId(endpoint));
                    }
                }
            }
        } catch (final RuntimeException ex) {
            reusable = false;
            throw ex;
        } finally {
            this.pool.release(entry, reusable);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} connection released {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.formatStats(entry.getRoute(), entry.getState(), pool));
            }
        }
    }

    @Override
    public void connect(final ConnectionEndpoint endpoint,
                        final TimeValue timeout,
                        final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Managed endpoint");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        if (internalEndpoint.isConnected()) {
            return;
        }
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = internalEndpoint.getPoolEntry();
        if (!poolEntry.hasConnection()) {
            poolEntry.assignConnection(connFactory.createConnection(null));
        }
        final HttpRoute route = poolEntry.getRoute();
        final HttpHost firstHop = route.getProxyHost() != null ? route.getProxyHost() : route.getTargetHost();
        final SocketConfig socketConfig = resolveSocketConfig(route);
        final ConnectionConfig connectionConfig = resolveConnectionConfig(route);
        final Timeout connectTimeout = timeout != null ? Timeout.of(timeout.getDuration(), timeout.getTimeUnit()) : connectionConfig.getConnectTimeout();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} connecting endpoint to {} ({})", ConnPoolSupport.getId(endpoint), firstHop, connectTimeout);
        }
        final ManagedHttpClientConnection conn = poolEntry.getConnection();
        this.connectionOperator.connect(
                conn,
                firstHop,
                route.getTargetName(),
                route.getLocalSocketAddress(),
                connectTimeout,
                socketConfig,
                route.isTunnelled() ? null : resolveTlsConfig(route.getTargetHost()),
                context);
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} connected {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.getId(conn));
        }
        final Timeout socketTimeout = connectionConfig.getSocketTimeout();
        if (socketTimeout != null) {
            conn.setSocketTimeout(socketTimeout);
        }
    }

    @Override
    public void upgrade(final ConnectionEndpoint endpoint, final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Managed endpoint");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = internalEndpoint.getValidatedPoolEntry();
        final HttpRoute route = poolEntry.getRoute();
        final HttpHost target = route.getTargetHost();
        final TlsConfig tlsConfig = resolveTlsConfig(target);
        this.connectionOperator.upgrade(
                poolEntry.getConnection(),
                target,
                route.getTargetName(),
                tlsConfig,
                context);
    }

    @Override
    public void closeIdle(final TimeValue idleTime) {
        Args.notNull(idleTime, "Idle time");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing connections idle longer than {}", idleTime);
        }
        if (isClosed()) {
            return;
        }
        this.pool.closeIdle(idleTime);
    }

    @Override
    public void closeExpired() {
        if (isClosed()) {
            return;
        }
        LOG.debug("Closing expired connections");
        this.pool.closeExpired();
    }

    @Override
    public Set<HttpRoute> getRoutes() {
        return this.pool.getRoutes();
    }

    @Override
    public int getMaxTotal() {
        return this.pool.getMaxTotal();
    }

    @Override
    public void setMaxTotal(final int max) {
        this.pool.setMaxTotal(max);
    }

    @Override
    public int getDefaultMaxPerRoute() {
        return this.pool.getDefaultMaxPerRoute();
    }

    @Override
    public void setDefaultMaxPerRoute(final int max) {
        this.pool.setDefaultMaxPerRoute(max);
    }

    @Override
    public int getMaxPerRoute(final HttpRoute route) {
        return this.pool.getMaxPerRoute(route);
    }

    @Override
    public void setMaxPerRoute(final HttpRoute route, final int max) {
        this.pool.setMaxPerRoute(route, max);
    }

    @Override
    public PoolStats getTotalStats() {
        return this.pool.getTotalStats();
    }

    @Override
    public PoolStats getStats(final HttpRoute route) {
        return this.pool.getStats(route);
    }

    /**
     * Sets the same {@link SocketConfig} for all routes
     */
    public void setDefaultSocketConfig(final SocketConfig config) {
        this.socketConfigResolver = route -> config;
    }

    /**
     * Sets {@link Resolver} of {@link SocketConfig} on a per route basis.
     *
     * @since 5.2
     */
    public void setSocketConfigResolver(final Resolver<HttpRoute, SocketConfig> socketConfigResolver) {
        this.socketConfigResolver = socketConfigResolver;
    }

    /**
     * Sets the same {@link ConnectionConfig} for all routes
     *
     * @since 5.2
     */
    public void setDefaultConnectionConfig(final ConnectionConfig config) {
        this.connectionConfigResolver = route -> config;
    }

    /**
     * Sets {@link Resolver} of {@link ConnectionConfig} on a per route basis.
     *
     * @since 5.2
     */
    public void setConnectionConfigResolver(final Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver) {
        this.connectionConfigResolver = connectionConfigResolver;
    }

    /**
     * Sets the same {@link ConnectionConfig} for all hosts
     *
     * @since 5.2
     */
    public void setDefaultTlsConfig(final TlsConfig config) {
        this.tlsConfigResolver = host -> config;
    }

    /**
     * Sets {@link Resolver} of {@link TlsConfig} on a per host basis.
     *
     * @since 5.2
     */
    public void setTlsConfigResolver(final Resolver<HttpHost, TlsConfig> tlsConfigResolver) {
        this.tlsConfigResolver = tlsConfigResolver;
    }

    void closeIfExpired(final PoolEntry<HttpRoute, ManagedHttpClientConnection> entry) {
        final long now = System.currentTimeMillis();
        if (entry.getExpiryDeadline().isBefore(now)) {
            entry.discardConnection(CloseMode.GRACEFUL);
        } else {
            final ConnectionConfig connectionConfig = resolveConnectionConfig(entry.getRoute());
            final TimeValue timeToLive = connectionConfig.getTimeToLive();
            if (timeToLive != null && Deadline.calculate(entry.getCreated(), timeToLive).isBefore(now)) {
                entry.discardConnection(CloseMode.GRACEFUL);
            }
        }
    }

    /**
     * @deprecated Use custom {@link #setConnectionConfigResolver(Resolver)}
     */
    @Deprecated
    public SocketConfig getDefaultSocketConfig() {
        return SocketConfig.DEFAULT;
    }

    /**
     * @since 4.4
     * @deprecated Use {@link #setConnectionConfigResolver(Resolver)}.
     */
    @Deprecated
    public TimeValue getValidateAfterInactivity() {
        return ConnectionConfig.DEFAULT.getValidateAfterInactivity();
    }

    /**
     * Defines period of inactivity after which persistent connections must
     * be re-validated prior to being {@link #lease(String, HttpRoute, Object)} leased} to the consumer.
     * Negative values passed to this method disable connection validation. This check helps
     * detect connections that have become stale (half-closed) while kept inactive in the pool.
     *
     * @since 4.4
     * @deprecated Use {@link #setConnectionConfigResolver(Resolver)}.
     */
    @Deprecated
    public void setValidateAfterInactivity(final TimeValue validateAfterInactivity) {
        setDefaultConnectionConfig(ConnectionConfig.custom()
                .setValidateAfterInactivity(validateAfterInactivity)
                .build());
    }

    private static final PrefixedIncrementingId INCREMENTING_ID = new PrefixedIncrementingId("ep-");

    static class InternalConnectionEndpoint extends ConnectionEndpoint implements Identifiable {

        private final AtomicReference<PoolEntry<HttpRoute, ManagedHttpClientConnection>> poolEntryRef;
        private final String id;

        InternalConnectionEndpoint(
                final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry) {
            this.poolEntryRef = new AtomicReference<>(poolEntry);
            this.id = INCREMENTING_ID.getNextId();
        }

        @Override
        public String getId() {
            return id;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> getPoolEntry() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry == null) {
                throw new ConnectionShutdownException();
            }
            return poolEntry;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> getValidatedPoolEntry() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = getPoolEntry();
            final ManagedHttpClientConnection connection = poolEntry.getConnection();
            if (connection == null || !connection.isOpen()) {
                throw new ConnectionShutdownException();
            }
            return poolEntry;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> detach() {
            return poolEntryRef.getAndSet(null);
        }

        @Override
        public void close(final CloseMode closeMode) {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry != null) {
                poolEntry.discardConnection(closeMode);
            }
        }

        @Override
        public void close() throws IOException {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry != null) {
                poolEntry.discardConnection(CloseMode.GRACEFUL);
            }
        }

        @Override
        public boolean isConnected() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = getPoolEntry();
            final ManagedHttpClientConnection connection = poolEntry.getConnection();
            return connection != null && connection.isOpen();
        }

        @Override
        public void setSocketTimeout(final Timeout timeout) {
            getValidatedPoolEntry().getConnection().setSocketTimeout(timeout);
        }

        /**
         * @deprecated Use {@link #execute(String, ClassicHttpRequest, RequestExecutor, HttpContext)}
         */
        @Deprecated
        @Override
        public ClassicHttpResponse execute(
                final String exchangeId,
                final ClassicHttpRequest request,
                final HttpRequestExecutor requestExecutor,
                final HttpContext context) throws IOException, HttpException {
            Args.notNull(request, "HTTP request");
            Args.notNull(requestExecutor, "Request executor");
            final ManagedHttpClientConnection connection = getValidatedPoolEntry().getConnection();
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} executing exchange {} over {}", id, exchangeId, ConnPoolSupport.getId(connection));
            }
            return requestExecutor.execute(request, connection, context);
        }

        /**
         * @since 5.4
         */
        @Override
        public ClassicHttpResponse execute(
                final String exchangeId,
                final ClassicHttpRequest request,
                final RequestExecutor requestExecutor,
                final HttpContext context) throws IOException, HttpException {
            Args.notNull(request, "HTTP request");
            Args.notNull(requestExecutor, "Request executor");
            final ManagedHttpClientConnection connection = getValidatedPoolEntry().getConnection();
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} executing exchange {} over {}", id, exchangeId, ConnPoolSupport.getId(connection));
            }
            return requestExecutor.execute(request, connection, context);
        }

        /**
         * @since 5.4
         */
        @Override
        public EndpointInfo getInfo() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry != null) {
                final ManagedHttpClientConnection connection = poolEntry.getConnection();
                if (connection != null && connection.isOpen()) {
                    return new EndpointInfo(connection.getProtocolVersion(), connection.getSSLSession());
                }
            }
            return null;
        }

    }

    /**
     * Method that can be called to determine whether the connection manager has been shut down and
     * is closed or not.
     *
     * @return {@code true} if the connection manager has been shut down and is closed, otherwise
     * return {@code false}.
     * @since 5.4
     */
    public boolean isClosed() {
        return this.closed.get();
    }


}
