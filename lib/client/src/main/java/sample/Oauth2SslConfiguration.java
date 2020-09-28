package sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Configuration
@ConfigurationProperties("client.ssl")
@Slf4j
public class Oauth2SslConfiguration {
    private String keyStore;
    private String keyStorePassword;
    private String trustStore;
    private String trustStorePassword;
    private String netDebug;

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getNetDebug() {
        return netDebug;
    }

    public void setNetDebug(String netDebug) {
        this.netDebug = netDebug;
    }

    private final static String NET_DEBUG = "javax.net.debug";
    private final static String KEYSTORE = "javax.net.ssl.keyStore";
    private final static String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private final static String TRUSTSTORE = "javax.net.ssl.trustStore";
    private final static String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    @PostConstruct
    public void init() {
        //FIXME Currently the client in resource-server relies on SSL System properties javax.net.ssl. -> Use client.ssl by default
        //TODO make sure ssl key locations accept url style values, such as classpath:...
        log.info("Setting and getting ssl system properties for spring oauth2");
        useOrSetSysProp(KEYSTORE, Oauth2SslConfiguration::getKeyStore,
            Oauth2SslConfiguration::setKeyStore);
        useOrSetSysProp(KEYSTORE_PASSWORD, Oauth2SslConfiguration::getKeyStorePassword,
            Oauth2SslConfiguration::setKeyStorePassword);
        useOrSetSysProp(TRUSTSTORE, Oauth2SslConfiguration::getTrustStore,
            Oauth2SslConfiguration::setTrustStore);
        useOrSetSysProp(TRUSTSTORE_PASSWORD, Oauth2SslConfiguration::getTrustStorePassword,
            Oauth2SslConfiguration::setTrustStorePassword);
        useOrSetSysProp(NET_DEBUG, Oauth2SslConfiguration::getNetDebug,
                Oauth2SslConfiguration::setNetDebug);
    }

    private void useOrSetSysProp(String name,
           Function<Oauth2SslConfiguration, String> getter,
           BiConsumer<Oauth2SslConfiguration, String> setter) {
        final String sysProp=System.getProperty(name);
        if(sysProp!=null) {
            setter.accept(this, sysProp);
        } else {
            String value = getter.apply(this);
            if(value!=null) {
                System.setProperty(name, value);
            }
        }
    }
}
