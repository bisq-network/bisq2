module bisq.common {
    requires static lombok;
    requires static org.jetbrains.annotations;
    requires com.google.common;
    requires typesafe.config;
    requires org.slf4j;
    requires com.google.protobuf;
    requires logback.core;
    requires logback.classic;

    exports bisq.common.annotations;
    exports bisq.common.currency;
    exports bisq.common.data;
    exports bisq.common.encoding;
    exports bisq.common.locale;
    exports bisq.common.monetary;
    exports bisq.common.observable;
    exports bisq.common.options;
    exports bisq.common.threading;
    exports bisq.common.timer;
    exports bisq.common.util;
    exports bisq.common.proto;
    exports bisq.common.protobuf;
}