# Market price node operator

Same as in Bisq 1 the market price node provides current market price data to the network.

In Bisq 2 the oracle node is requesting the price data with a 1 min. interval and broadcast that data to the network.
This should reduce the risk for diverging prices in times of high volatility as the propagation to the network is rather
fast.

Additionally, the users are requesting the price data themselves as well but with a 3 min. interval.

The price data are also persisted so that at startup there is immediately price data available. The persisted price data
is potentially out-dated which is reflected in the UI with a warning icon.

Those 3 sources for price data are all aggregated and the more recent price data will replace old ones.

The market price providers are currently defined in the config file and not distributed as Authorized data. The BSQ
bonding is not yet enforced by the system but by social consensus. In future the price nodes should follow the same
model as used for the other nodes/roles.