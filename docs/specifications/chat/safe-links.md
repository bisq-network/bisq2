# Safe links in chat messages

This specification defines which URLs the desktop application shows as a link which the reader can open, when it
renders the markdown of a chat message. Currently it renders markdown in messages of the support channels.

## Terms

- A link is a markdown link `[label](URL)` or a bare `http://` or `https://` URL in the text of a message (an auto
  link). The sender of the message writes the label and the URL.
- The authority of a URL is the part between `scheme://` and the next `/`, `?` or `#`. It has the form
  `[user info @] host [: port]`. The user info is the text in front of the `@`; HTTP used it for a user name and a
  password.

## Rules

1. A URL is safe only if all of the following hold:
   - it contains no control character and no bidi control character (U+202A to U+202E, U+2066 to U+2069);
   - it is a valid URI;
   - its scheme is `http` or `https`, in any letter case;
   - its authority has a host: a host name, an IPv4 address or an IPv6 address in brackets, and a port, if present,
     consists of digits;
   - its authority has no user info and no `@`.
2. A link whose URL is not safe is not a link. The application shows the text of the message as it was written, and
   creates no hyperlink for it.
3. The hyperlink of a safe link shows the URL which it opens. A label which differs from the URL is plain text in
   front of the hyperlink: `label (URL)`.
4. The application checks rule 1 again when the reader activates a hyperlink, and opens only a safe URL. Both checks
   use one definition of the rule.

## Reasons

- Rule 1, user info: a reader takes the text in front of the `@` for the host. `https://bisq.network@evil.example/`
  leads to `evil.example`, but its hyperlink would start with `bisq.network`. A link to a web page needs no user
  info. So the application loses nothing when it treats such a URL as unsafe, and a sender has no legitimate reason
  to write one. An encoded `@` (`%40`) and a second `@` are not user info. Such an
  authority has no valid host, so the host condition of rule 1 covers them. An `@` behind the authority, in the path,
  the query or the fragment, is harmless: the hyperlink shows the host in front of it.
- Rule 1, scheme: only `http` and `https` URLs open a web page. Another scheme could start another program or read a
  local file.
- Rule 1, characters: a bidi control character changes the order in which the characters of the URL are displayed,
  so the reader sees another target than the one which opens.
- Rule 2 instead of a cleaned URL: the application could remove the user info and show the rest. But the sender
  wrote the URL so that the reader sees a trusted name, and a cleaned URL could still differ from what the sender
  wants the reader to see in the message text. Plain text makes the whole URL visible as it was written.
- Rule 3: the label comes from the sender. Without rule 3, a label which looks like a trusted URL would hide the
  target of the link. Rule 1 and rule 3 together make the host of every hyperlink visible.

## Compatibility

- The rules apply to the display of a message. They do not change which messages a node accepts, stores or relays. A
  message with an unsafe URL is still shown; only the URL is plain text.
- Version 2.1.13 has no markdown rendering, so it shows every message as plain text.
