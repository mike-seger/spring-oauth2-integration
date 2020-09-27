package in.neuw.oauth2.util.security;

import in.neuw.oauth2.exception.AppBootTimeException;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.FingerprintTrustManagerFactory;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Properties;

/**
 * @author Karanbir Singh on 07/19/2020
 */

@Service
public class SSLContextService {

    private final ResourceLoader resourceLoader;

    public SSLContextService(ResourceLoader resourceLoader) {
        this.resourceLoader=resourceLoader;
    }

    public SslContext sslContext(
            final KeyManagerFactory keyManagerFactory,
            final TrustManagerFactory trustManagerFactory) throws AppBootTimeException {
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .clientAuth(ClientAuth.REQUIRE)
                    .keyManager(keyManagerFactory)
                    // the following line is not recommended and commented out
                   //  .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .trustManager(trustManagerFactory)
                    .build();
            return sslContext;
        } catch (SSLException e) {
            e.printStackTrace();
            throw new AppBootTimeException(e.getMessage(), e);
        }
    }

    public KeyManagerFactory getKeyStore(
            final String location, final String password) throws AppBootTimeException {
        try {
            KeyStore keyStore = loadKeyStore(location, password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, password.toCharArray());
            return kmf;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
            e.printStackTrace();
            throw new AppBootTimeException(e.getMessage(), e);
        }
    }

    /*
     * Create the Trust Store.
     */
    public TrustManagerFactory getTrustStore(
            final String location, final String password) throws AppBootTimeException {
        try {
            KeyStore trustStore = loadKeyStore(location, password);
            TrustManagerFactory tmf = FingerprintTrustManagerFactory.getInstance(FingerprintTrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            throw new AppBootTimeException(e.getMessage(), e);
        }
    }

    private KeyStore loadKeyStore(final String location,
            final String password) throws KeyStoreException,
                IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            keyStore.load(inputStream, password.toCharArray());
        }
        return keyStore;
    }

}
