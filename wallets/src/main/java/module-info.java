module bisq.wallets {
    requires static lombok;
    requires static org.slf4j;

    requires com.fasterxml.jackson.annotation;
    requires bisq.common;

    exports bisq.wallets;
    exports bisq.wallets.exceptions;
    exports bisq.wallets.model;
}