# Release manager

The release manager can send a `ReleaseNotification` to the network which contains:

- isPreRelease
- isLauncherUpdate
- ReleaseNotes
- Version

User can ignore data from the security manager by adding the JVM argument:
`-Dapplication.bondedRoles.ignoreSecurityManager=true`

After successful registration the release manager will see the Authorized role menu item and the Release manager screen
visible.

<img src="img/release.png" width="1200"/>
<img src="img/release_popup.png" width="1200"/>