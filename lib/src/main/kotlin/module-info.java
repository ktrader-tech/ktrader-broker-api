module ktrader.broker.api {
    requires transitive kotlin.stdlib;
    requires transitive kevent;
    requires static org.pf4j;

    exports org.rationalityfrontline.ktrader.broker.api;
}