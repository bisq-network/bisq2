# CSV export

The desktop app exports tables, for example the trade history, as CSV files. Cells can contain text of other users,
for example payment account data or chat text of a trade peer.

## Rules

1. A cell which starts with `=`, `@`, a tab or a carriage return gets a leading apostrophe. A cell which starts with
   `-` or `+` gets it too, unless it is a signed number such as `-1.00%`.
2. A cell which contains a comma, a double quote, an apostrophe, a line feed or a carriage return is quoted. Double
   quotes inside are doubled.

## Reason

Spreadsheet applications run a cell which starts with one of these characters as a formula. A formula can open links
or read other cells when the user opens the file. The apostrophe makes the cell text. Signed numbers stay numbers, so
that exported percentages stay readable.
