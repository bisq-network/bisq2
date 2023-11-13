# Security manager

The security manager can send a `AuthorizedAlertData` to the network which contains:

- AlertType alertType
- Optional message
- Boolean haltTrading
- Boolean requireVersionForTrading
- Optional minVersion,
- Optional bannedRole

AlertType:

- INFO: Informational message
- WARN: Warning message
- EMERGENCY: Emergency message with options to halt trading or require an update to a min version for trading
- BAN: Ban an `AuthorizedBondedRole` (e.g. other roles or nodes)

User can ignore data from the security manager by adding the JVM argument:
`-Dapplication.bondedRoles.ignoreSecurityManager=true`

After successful registration the security manager will see the Authorized role menu item and the Security manager
screen visible.

<img src="img/alert.png" width="1200"/>
<img src="img/ban.png" width="1200"/>