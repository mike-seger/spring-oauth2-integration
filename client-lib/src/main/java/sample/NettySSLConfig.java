package sample;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.FingerprintTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

//@Configuration
public class NettySSLConfig {
    private final ResourceLoader resourceLoader;

    public NettySSLConfig(ResourceLoader resourceLoader) {
        this.resourceLoader=resourceLoader;
    }

    @Bean
    public SslContext sslContext(Oauth2SslConfiguration sslConfiguration) {
        try {
            final KeyManagerFactory keyManagerFactory = getKeyStore(
                sslConfiguration.getKeyStore(), sslConfiguration.getKeyStorePassword());
            final TrustManagerFactory trustManagerFactory = getTrustStore(
                sslConfiguration.getTrustStore(), sslConfiguration.getTrustStorePassword());
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            if(keyManagerFactory!=null) sslContextBuilder
                .clientAuth(ClientAuth.REQUIRE)
                .keyManager(keyManagerFactory);
            if(trustManagerFactory!=null) sslContextBuilder.trustManager(trustManagerFactory);
            return sslContextBuilder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public KeyManagerFactory getKeyStore(
            final String location, final String password) throws RuntimeException {
        if(location==null) { return null; }
        try {
            char [] passwordChars = charArray(password);
            KeyStore keyStore = loadKeyStore(location, passwordChars);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, passwordChars);
            return kmf;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private TrustManagerFactory getTrustStore(
            final String location, final String password) throws RuntimeException {
        if(location==null) { return null; }
        try {
            KeyStore trustStore = loadKeyStore(location, charArray(password));
            TrustManagerFactory tmf = FingerprintTrustManagerFactory.getInstance(FingerprintTrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private KeyStore loadKeyStore(final String location,
            final char [] password) throws KeyStoreException,
            IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            keyStore.load(inputStream, password);
        }
        return keyStore;
    }

    public char [] charArray(String s) {
        return s==null?null:s.toCharArray();
    }
}

