@SuppressWarnings("requires-transitive-automatic")
module ktrader.broker.api {
    requires transitive kotlinx.coroutines.core.jvm;
    requires transitive kevent;
    requires transitive ktrader.datatype;
    requires static org.pf4j;

    exports org.rationalityfrontline.ktrader.broker.api;
}