# CSV export

The desktop app exports tables, for example the trade history, as CSV files. Cells can contain text of other users,
for example payment account data or chat text of a trade peer.

## Rules

1. A cell which starts with `=`, `@`, a tab or a carriage return gets a leading apostrophe. A cell which starts with
   `-` or `+` gets it too, unless it is a signed number such as `-1.00%`.
2. Inside a cell, an apostrophe is inserted after each semicolon, tab, line feed or carriage return which is followed
   by `=`, `+`, `-`, `@`, a tab or a carriage return.
3. A cell which contains a comma, a double quote, an apostrophe, a semicolon, a tab, a line feed or a carriage return
   is quoted. Double quotes inside are doubled.

## Reason

Spreadsheet applications run a cell which starts with one of these characters as a formula. A formula can open links
or read other cells when the user opens the file. The apostrophe makes the cell text. Signed numbers stay numbers, so
that exported percentages stay readable.

Rule 2: an application can split a line at another character than the comma. Excel uses the list separator of the
system, which is the semicolon in many countries. It then starts a new cell after each semicolon, and a quote in the
middle of such a cell does not stop it. So a payment proof or a Lightning invoice of a trade peer such as `x;=1+1` would
start a formula in the second cell. Rule 2 makes that cell text as well. A signed number after a semicolon also gets
the apostrophe; this is visible, but such values come only from text of users.

Rule 3: the quotes keep the cell whole for applications which split at a semicolon or a tab and respect quotes.

## Limitations

- An application which the user configures to split at another character, for example a space, is not covered.

