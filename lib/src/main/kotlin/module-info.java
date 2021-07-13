module ktrader.broker.api {
    requires kotlin.stdlib;
    requires static org.pf4j;
    requires transitive kevent;

    exports org.rationalityfrontline.ktrader.broker.api;
}