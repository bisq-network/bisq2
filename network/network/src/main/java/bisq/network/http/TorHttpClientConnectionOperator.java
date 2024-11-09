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

import org.apache.hc.client5.http.ConnectExceptionSupport;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.UnsupportedSchemeException;
import org.apache.hc.client5.http.impl.ConnPoolSupport;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.io.DetachedSocketFactory;
import org.apache.hc.client5.http.io.HttpClientConnectionOperator;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.Internal;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.Closer;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;

/**
 *  Adapted from https://github.com/apache/httpcomponents-client/blob/40d6ba4ee5e39a00461a30503ff9aa98a9c8f0a2/httpclient5/src/main/java/org/apache/hc/client5/http/impl/io/DefaultHttpClientConnectionOperator.java
 *  The connect() method is customized to prevent it from doing a DNS lookup on the .onion hostname.
 */
@Internal
@Contract(threading = ThreadingBehavior.STATELESS)
class TorHttpClientConnectionOperator implements HttpClientConnectionOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TorHttpClientConnectionOperator.class);

    static final DetachedSocketFactory PLAIN_SOCKET_FACTORY = socksProxy -> socksProxy == null ? new Socket() : new Socket(socksProxy);

    private final DetachedSocketFactory detachedSocketFactory;
    private final Lookup<TlsSocketStrategy> tlsSocketStrategyLookup;
    private final SchemePortResolver schemePortResolver;

    public TorHttpClientConnectionOperator(
            final DetachedSocketFactory detachedSocketFactory,
            final SchemePortResolver schemePortResolver,
            final Lookup<TlsSocketStrategy> tlsSocketStrategyLookup) {
        super();
        this.detachedSocketFactory = Args.notNull(detachedSocketFactory, "Plain socket factory");
        this.tlsSocketStrategyLookup = Args.notNull(tlsSocketStrategyLookup, "Socket factory registry");
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver :
                DefaultSchemePortResolver.INSTANCE;
    }

    public TorHttpClientConnectionOperator(
            final SchemePortResolver schemePortResolver,
            final Lookup<TlsSocketStrategy> tlsSocketStrategyLookup) {
        this(PLAIN_SOCKET_FACTORY, schemePortResolver, tlsSocketStrategyLookup);
    }

    @Override
    public void connect(
            final ManagedHttpClientConnection conn,
            final HttpHost host,
            final InetSocketAddress localAddress,
            final TimeValue connectTimeout,
            final SocketConfig socketConfig,
            final HttpContext context) throws IOException {
        final Timeout timeout = connectTimeout != null ? Timeout.of(connectTimeout.getDuration(), connectTimeout.getTimeUnit()) : null;
        connect(conn, host, null, localAddress, timeout, socketConfig, null, context);
    }

    @Override
    public void connect(
            final ManagedHttpClientConnection conn,
            final HttpHost endpointHost,
            final NamedEndpoint endpointName,
            final InetSocketAddress localAddress,
            final Timeout connectTimeout,
            final SocketConfig socketConfig,
            final Object attachment,
            final HttpContext context) throws IOException {
        Args.notNull(conn, "Connection");
        Args.notNull(endpointHost, "Host");
        Args.notNull(socketConfig, "Socket config");
        Args.notNull(context, "Context");
        final Timeout soTimeout = socketConfig.getSoTimeout();
        final SocketAddress socksProxyAddress = socketConfig.getSocksProxyAddress();
        final Proxy socksProxy = socksProxyAddress != null ? new Proxy(Proxy.Type.SOCKS, socksProxyAddress) : null;
        final int port = this.schemePortResolver.resolve(endpointHost.getSchemeName(), endpointHost);

        // Hostname is an .onion address. Do not try to resolve it to an InetAddress.
        // Being able to do this is the reason to have this custom class instead of using DefaultHttpClientConnectionOperator.
        final InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved(endpointHost.getHostName(), port);

        onBeforeSocketConnect(context, endpointHost);
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} connecting {}->{} ({})", endpointHost, localAddress, remoteAddress, connectTimeout);
        }
        final Socket socket = detachedSocketFactory.create(socksProxy);
        try {
            conn.bind(socket);
            if (soTimeout != null) {
                socket.setSoTimeout(soTimeout.toMillisecondsIntBound());
            }
            socket.setReuseAddress(socketConfig.isSoReuseAddress());
            socket.setTcpNoDelay(socketConfig.isTcpNoDelay());
            socket.setKeepAlive(socketConfig.isSoKeepAlive());
            if (socketConfig.getRcvBufSize() > 0) {
                socket.setReceiveBufferSize(socketConfig.getRcvBufSize());
            }
            if (socketConfig.getSndBufSize() > 0) {
                socket.setSendBufferSize(socketConfig.getSndBufSize());
            }

            final int linger = socketConfig.getSoLinger().toMillisecondsIntBound();
            if (linger >= 0) {
                socket.setSoLinger(true, linger);
            }

            if (localAddress != null) {
                socket.bind(localAddress);
            }
            socket.connect(remoteAddress, TimeValue.isPositive(connectTimeout) ? connectTimeout.toMillisecondsIntBound() : 0);
            conn.bind(socket);
            onAfterSocketConnect(context, endpointHost);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} {} connected {}->{}", ConnPoolSupport.getId(conn), endpointHost,
                        conn.getLocalAddress(), conn.getRemoteAddress());
            }
            conn.setSocketTimeout(soTimeout);
            final TlsSocketStrategy tlsSocketStrategy = tlsSocketStrategyLookup != null ? tlsSocketStrategyLookup.lookup(endpointHost.getSchemeName()) : null;
            if (tlsSocketStrategy != null) {
                final NamedEndpoint tlsName = endpointName != null ? endpointName : endpointHost;
                onBeforeTlsHandshake(context, endpointHost);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} {} upgrading to TLS", ConnPoolSupport.getId(conn), tlsName);
                }
                final SSLSocket sslSocket = tlsSocketStrategy.upgrade(socket, tlsName.getHostName(), tlsName.getPort(), attachment, context);
                conn.bind(sslSocket, socket);
                onAfterTlsHandshake(context, endpointHost);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} {} upgraded to TLS", ConnPoolSupport.getId(conn), tlsName);
                }
            }
            return;
        } catch (final RuntimeException ex) {
            Closer.closeQuietly(socket);
            throw ex;
        } catch (final IOException ex) {
            Closer.closeQuietly(socket);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} connection to {} failed ({}); terminating operation", endpointHost, remoteAddress, ex.getClass());
            }
            throw ConnectExceptionSupport.enhance(ex, endpointHost);
        }
    }

    @Override
    public void upgrade(
            final ManagedHttpClientConnection conn,
            final HttpHost host,
            final HttpContext context) throws IOException {
        upgrade(conn, host, null, null, context);
    }

    @Override
    public void upgrade(
            final ManagedHttpClientConnection conn,
            final HttpHost endpointHost,
            final NamedEndpoint endpointName,
            final Object attachment,
            final HttpContext context) throws IOException {
        final Socket socket = conn.getSocket();
        if (socket == null) {
            throw new ConnectionClosedException("Connection is closed");
        }
        final String newProtocol = URIScheme.HTTP.same(endpointHost.getSchemeName()) ? URIScheme.HTTPS.id : endpointHost.getSchemeName();
        final TlsSocketStrategy tlsSocketStrategy = tlsSocketStrategyLookup != null ? tlsSocketStrategyLookup.lookup(newProtocol) : null;
        if (tlsSocketStrategy != null) {
            final NamedEndpoint tlsName = endpointName != null ? endpointName : endpointHost;
            onBeforeTlsHandshake(context, endpointHost);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} upgrading to TLS {}:{}", ConnPoolSupport.getId(conn), tlsName.getHostName(), tlsName.getPort());
            }
            final SSLSocket upgradedSocket = tlsSocketStrategy.upgrade(socket, tlsName.getHostName(), tlsName.getPort(), attachment, context);
            conn.bind(upgradedSocket);
            onAfterTlsHandshake(context, endpointHost);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} upgraded to TLS {}:{}", ConnPoolSupport.getId(conn), tlsName.getHostName(), tlsName.getPort());
            }
        } else {
            throw new UnsupportedSchemeException(newProtocol + " protocol is not supported");
        }
    }

    protected void onBeforeSocketConnect(final HttpContext httpContext, final HttpHost endpointHost) {
    }

    protected void onAfterSocketConnect(final HttpContext httpContext, final HttpHost endpointHost) {
    }

    protected void onBeforeTlsHandshake(final HttpContext httpContext, final HttpHost endpointHost) {
    }

    protected void onAfterTlsHandshake(final HttpContext httpContext, final HttpHost endpointHost) {
    }

}
