# Inventory request lifecycle

A pending inventory request belongs to one connection. When that connection closes, the node
cancels its request and releases its pending-request slot immediately. It must not wait for the
request timeout before allowing inventory requests on replacement connections.

Closing one connection leaves requests on other connections active. Repeated disconnect
notifications are safe. Observers see the number of pending requests after cancellation.

When every connection has closed, the node also resets initial inventory completion so that
reconnecting starts inventory discovery again.
Peers excluded during the previous discovery because their full response made no progress are
reconsidered. Their data or the node's ability to store it may have changed. A repeated response
which still makes no progress excludes the peer again under the normal inventory filter rules.
