# Links in the desktop application

This specification defines which links the desktop application opens, and which text may create a link in a popup.

## Terms

- A web URL is a value which `java.net.URI` parses, whose scheme is `http` or `https` in any case, and which has a
  host. Examples: `https://bisq.network/x`, `http://localhost:8080/tx/abc`, `http://<onion address>.onion/tx/abc`.
- To open a link means to pass it to the operating system, which then starts a program for it.
- A link marker is the text `[HYPERLINK:<link>]` in the text of a popup. The popup replaces the marker with the link
  and a number, and it shows a link with that number below the text. The popup recognizes the marker word `HYPERLINK`
  only in upper case.
- An application text template is a text from the i18n resource files of the application, or a text which the code of
  the application writes. A template can have placeholders for values, for example `{0}`.
- Text of another user is text which the application did not write. Examples: the text of a report to a moderator,
  the name which a paired API client sends, the Bitcoin address or the transaction ID which the trade peer sends, and
  a URL in a chat message.

## Rules

### Opening links

1. The application opens only web URLs.
2. For any other value, the application does nothing: it shows no confirmation popup, opens nothing and copies nothing
   to the clipboard. It logs a warning without the value, because the value can come from another user.
3. The check of rule 1 happens before the confirmation popup and before every other step of opening a link. Each place
   which opens a link uses this one check.

### Links in popup texts

4. Only application text templates create links in a popup. Text of another user does not create a link, also when
   the application inserts it into a template.
5. Before a popup shows text of another user, and before the application inserts such text into a popup text
   template, the application replaces each `HYPERLINK` in that text with `hyperlink`. So the text `[HYPERLINK:x]` of
   another user is shown as the plain text `[hyperlink:x]`.
6. A template may have a link marker around a placeholder only if the application inserts its own value there, never
   text of another user.

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
- Rule 4: the popup applies the marker syntax to its final text, after the application inserted the values into the
  template. Without rule 5, another user could write a marker into a report, into the name of an API client, or into
  the Bitcoin address or the transaction ID of a trade. The popup then showed a link which the application did not
  write, and the reader may trust such a link because it is part of an application popup. Before rule 1, such a link
  could open a UNC path.
- Rule 5 changes the marker word, and not only complete markers, because a value can complete a marker together with
  a character of the template. Examples: a value which ends with `[HYPERLINK` before a `:` of the template, or a value
  which starts with `HYPERLINK:` after a `[` of the template. Lower case keeps the text readable. The replacement has
  no upper case letter, so it cannot form a new marker word, and a second replacement changes nothing.
- Rule 6: a marker around a placeholder turns the inserted value into a link. Rule 5 cannot prevent this, because the
  template supplies the marker.

## Enforcement

- Rules 1 to 3: `bisq.desktop.common.Browser.open` checks `Browser.isWebUrl` first. All links of the desktop
  application which open a program go through `Browser.open`: the links below a popup text, the links in views and
  the links in support chat messages.
- The support chat renderer has an additional, stricter rule for the links which it shows
  (`MarkdownSecurityUtils.isSafeOpenUrl`). That rule is a display rule of chat messages. It does not replace rule 1.
- Rule 5: `bisq.common.util.StringUtils.neutralizeHyperlinkMarkers`. The desktop application applies it to:
    - the text of a report in the report list of a moderator,
    - the name of a paired API client in the popup which revokes the client,
    - the Bitcoin address and the transaction ID in the warning of step 3b of a Bisq Easy main chain trade,
    - the URL in the confirmation popup of `Browser.open`.

## Known limitations

- Rule 5 depends on each place which builds the text of a popup. A new popup which shows text of another user must
  apply it. The popup receives a finished text, so it cannot tell which part came from another user. A popup interface
  which receives the template and the values separately would remove this limitation, but it needs a change of all
  popup calls. Rule 1 limits the effect of a missed place to a web URL.

## Compatibility

- The rules apply only to the desktop application. They change no network message and no persisted data.
- Text of another user which contains `HYPERLINK` in upper case is shown with `hyperlink` in lower case. In the
  confirmation popup of `Browser.open`, this also applies to the shown URL. The application still opens the
  unchanged URL.
