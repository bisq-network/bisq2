# Links in the desktop application

This specification defines which links the desktop application opens.

## Terms

- A web URL is a value which `java.net.URI` parses, whose scheme is `http` or `https` in any case, and which has a
  host. Examples: `https://bisq.network/x`, `http://localhost:8080/tx/abc`, `http://<onion address>.onion/tx/abc`.
- To open a link means to pass it to the operating system, which then starts a program for it.

## Rules

1. The application opens only web URLs.
2. For any other value, the application does nothing: it shows no confirmation popup, opens nothing and copies nothing
   to the clipboard. It logs a warning without the value, because the value can come from another user.
3. The check of rule 1 happens before the confirmation popup and before every other step of opening a link. Each place
   which opens a link uses this one check.

## Reasons

- Rule 1: the operating system opens a value with the program which it registers for that kind of value. A web URL
  opens in the web browser. Other values start other programs:
    - On Windows, the shell opens a file path or a UNC path, for example `\\attacker.example\share\report.exe`. For a
      UNC path, Windows connects to the remote host. It can send the Windows user name and authentication data to that
      host, and it can start a program from the remote share.
    - Other schemes start their registered handlers, for example `file:`, `smb:`, `ms-msdt:` or `mailto:`.
- Opening a web URL reveals the IP address of the user to the web server, because the web browser does not use Tor.
  The application accepts this for web URLs. The user sees the URL of a link from another user before opening it:
  the text of such a link is its URL, and the confirmation popup shows the URL until the user turns the popup off.
- All links which the application opens today are web URLs: pages of Bisq, block explorer URLs, video URLs, the
  download page of the updater and links in support chat messages. So rule 1 changes no intended behavior.
- Rule 3: one check at the place where links are opened also covers links which a later change adds.

## Enforcement

- `bisq.desktop.common.Browser.open` checks `Browser.isWebUrl` first. All links of the desktop application which open
  a program go through `Browser.open`: the links below a popup text, the links in views and the links in support chat
  messages.
- The support chat renderer has an additional, stricter rule for the links which it shows
  (`MarkdownSecurityUtils.isSafeOpenUrl`). That rule is a display rule of chat messages. It does not replace rule 1.

## Compatibility

The rules apply only to the desktop application. They change no network message and no persisted data.
