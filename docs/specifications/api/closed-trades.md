# Closed trades API

The closed trades API indexes Bisq Easy trade history for filtering, sorting and pagination.

## Optional payment details

A closed trade can lack payment account data, Bitcoin payment data or payment proof. For example,
a trader can cancel before exchanging these details. Absence of any such field must not prevent
indexing the trade or including it in API results. The DTO preserves each field as an optional
value. When present, its content is kept unchanged.

This rule changes no wire field or persisted data. It preserves the optional values of the trade
model rather than requiring payment steps that the trade might not have reached.
