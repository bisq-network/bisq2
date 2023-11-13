# Explorer node operator

Same as in Bisq 1 the explorer node delivers Bitcoin blockchain data.
It is used for transaction lookup and confirmation checks in the Bisq Easy protocol.

The explorer providers are currently defined in the `ExplorerService` class and is not distributed as Authorized data.
The BSQ bonding is thus not yet enforced by the system but by social consensus. In future the explorer nodes should
follow the same pattern used for the other nodes/roles.