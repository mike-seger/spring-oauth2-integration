package sample;

import nl.altindag.sslcontext.SSLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Component
public class SSLConfig {
    private final static String classPathPrefix="classpath:";

    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
    }

    @Bean
    @Scope("prototype")
    public SSLFactory sslFactory(
            @Value("${client.ssl.one-way-authentication-enabled:false}") boolean oneWayAuthenticationEnabled,
            @Value("${client.ssl.two-way-authentication-enabled:false}") boolean twoWayAuthenticationEnabled,
            @Value("${client.ssl.key-store:#{null}}") String keyStorePath,
            @Value("${client.ssl.key-store-password:}") char[] keyStorePassword,
            @Value("${client.ssl.trust-store:#{null}}") String trustStorePath,
            @Value("${client.ssl.trust-store-password:}") char[] trustStorePassword) {
        SSLFactory.Builder builder = SSLFactory.builder().withProtocol("TLSv1.3");
        if (!twoWayAuthenticationEnabled && !oneWayAuthenticationEnabled) {
            return null;
        }

        builder = withIdentityMaterial(builder, keyStorePath, keyStorePassword);
        builder = withTrustMaterial(builder, trustStorePath, trustStorePassword);

        return builder.build();
    }

    //TODO find an elegant solution for the redundant with...Materials methods
    private SSLFactory.Builder withIdentityMaterial(SSLFactory.Builder builder,
        String keyStorePath, char [] keyStorePassword) {
        if(keyStorePath==null) {
            return builder;
        }
        if(keyStorePath.startsWith(classPathPrefix)) {
            return builder.withIdentityMaterial(stripClassPath(keyStorePath), keyStorePassword);
        }
        return builder.withIdentityMaterial(new File(keyStorePath).toPath(), keyStorePassword);
    }

    //TODO find an elegant solution for the redundant with...Materials methods
    private SSLFactory.Builder withTrustMaterial(SSLFactory.Builder builder,
        String trustStorePath, char [] trustStorePassword) {
        if(trustStorePath==null) {
            return builder;
        }
        if(trustStorePath.startsWith(classPathPrefix)) {
            return builder.withTrustMaterial(stripClassPath(trustStorePath), trustStorePassword);
        }
        return builder.withTrustMaterial(new File(trustStorePath).toPath(), trustStorePassword);
    }

    private String stripClassPath(String s) {
        return s.replaceAll("^"+classPathPrefix, "");
    }
}
