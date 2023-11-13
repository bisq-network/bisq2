# Market price node operator

Same as in Bisq 1 the market price node provides market price data to the network.
In Bisq 2 the oracle node is requesting the price data on a 1 min. interval and broadcast that data to the network.
This should reduce the risk for diverging prices in times of high volatility as the propagation to the network is rather
fast.
Additionally, the users are requesting the price data themselves as well but on a 3 min. interval.
The price data are also persisted so that at the next start there are immediately price data available. Those are
potentially out-dated which is reflected in the UI with a warning icon.
What ever the most recent price data is will be used, independent of the source of delivery.

The market price providers are currently defined in the config file and not distributed as Authorized data. The BSQ
bonding is thus not yet enforced by the system but by social consensus. In future the price nodes should follow the same
pattern used for the other nodes/roles.