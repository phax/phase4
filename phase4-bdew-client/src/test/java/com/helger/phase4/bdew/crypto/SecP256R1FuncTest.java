package com.helger.phase4.bdew.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.ssl.SSLContexts;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import com.helger.bc.PBCProvider;
import com.helger.commons.io.stream.StringInputStream;
import com.helger.commons.system.SystemProperties;
import com.helger.http.tls.ETLSVersion;
import com.helger.http.tls.TLSConfigurationMode;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.httpclient.security.TrustStrategyTrustAll;
import com.helger.security.keystore.EKeyStoreType;

/**
 * Server-seitige TLS-Zertifikate generieren:
 *
 * <pre>
 * # Create Key
 * openssl ecparam -genkey -name prime256v1 -out man2.key -param_enc named_curve
 * # Create Certificate from key
 * openssl req -new -x509 -key man2.key -out man2.cer -days 99999
 * </pre>
 *
 * @author Philip Helger
 */
public class SecP256R1FuncTest
{
  public static void main (final String [] args) throws Exception
  {
    // Enable deep debug messages
    if (false)
      SystemProperties.setPropertyValue ("javax.net.debug", false ? "all" : "ssl,handshake");

    // Ensure BC Security Provider is installed
    final Provider provider = PBCProvider.getProvider ();
    assertNotNull (provider);

    // Make sure the curve is supported
    final ECNamedCurveParameterSpec brainpoolp256r1Spec = ECNamedCurveTable.getParameterSpec ("brainpoolp256r1");
    assertNotNull (brainpoolp256r1Spec);

    // Cert from Valentin Brandl
    final String sCert = "-----BEGIN CERTIFICATE-----\r\n" +
                         "MIICMTCCAdigAwIBAgIUF6HjYLZe6WvUtBzzRnSC7iwCLdcwCgYIKoZIzj0EAwIw\r\n" +
                         "bjELMAkGA1UEBhMCREUxEDAOBgNVBAgMB0dlcm1hbnkxEzARBgNVBAcMClJlZ2Vu\r\n" +
                         "c2J1cmcxDzANBgNVBAoMBkVCU25ldDEMMAoGA1UECwwDRGV2MRkwFwYDVQQDDBBt\r\n" +
                         "YW4xLmV4YW1wbGUuY29tMB4XDTIzMDUxNzEyMjkwMloXDTI0MDUxNjEyMjkwMlow\r\n" +
                         "bjELMAkGA1UEBhMCREUxEDAOBgNVBAgMB0dlcm1hbnkxEzARBgNVBAcMClJlZ2Vu\r\n" +
                         "c2J1cmcxDzANBgNVBAoMBkVCU25ldDEMMAoGA1UECwwDRGV2MRkwFwYDVQQDDBBt\r\n" +
                         "YW4xLmV4YW1wbGUuY29tMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABIGfAm9P\r\n" +
                         "nTZ5xpVBTW79lGQwR55MU43KGdkNAsOlz4JcU6G8xqGrUJHr3wli7SUEyfcrIii0\r\n" +
                         "+OQmv/FZvGr/NPOjUzBRMB0GA1UdDgQWBBQn5c0BF3Qz4DAmcAtwjh9jAKJUzTAf\r\n" +
                         "BgNVHSMEGDAWgBQn5c0BF3Qz4DAmcAtwjh9jAKJUzTAPBgNVHRMBAf8EBTADAQH/\r\n" +
                         "MAoGCCqGSM49BAMCA0cAMEQCIAd8OK/Q+5DDlO2d28jVrQSnwHnv7fpuCPars1tJ\r\n" +
                         "gDRsAiAKl82ZaKrU8x58GvooJ1VnwKdqdhK1Opu50bLgiRVCyg==\r\n" +
                         "-----END CERTIFICATE-----\r\n";
    final CertificateFactory aCertificateFactory = CertificateFactory.getInstance ("X.509", provider);

    final X509Certificate cert;
    try (final StringInputStream aIS = new StringInputStream (sCert, StandardCharsets.ISO_8859_1))
    {
      cert = (X509Certificate) aCertificateFactory.generateCertificate (aIS);
    }
    assertNotNull (cert);

    // Quick curve name check
    assertTrue (cert.getPublicKey () instanceof ECPublicKey);
    final ECPublicKey publicKey = (ECPublicKey) cert.getPublicKey ();
    // Bouncy Castle specific class
    final ECNamedCurveSpec certParams = (ECNamedCurveSpec) publicKey.getParams ();
    assertEquals ("brainpoolP256r1", certParams.getName ());

    // Key from Valentin Brandl
    final String sKey = "-----BEGIN EC PRIVATE KEY-----\r\n" +
                        "MHgCAQEEIJItuw6LwFdZhcwyoe/iOYcyMoyXKOyEItefcYH6WKFEoAsGCSskAwMC\r\n" +
                        "CAEBB6FEA0IABIGfAm9PnTZ5xpVBTW79lGQwR55MU43KGdkNAsOlz4JcU6G8xqGr\r\n" +
                        "UJHr3wli7SUEyfcrIii0+OQmv/FZvGr/NPM=\r\n" +
                        "-----END EC PRIVATE KEY-----\r\n";

    final Object pemObj;
    try (final PEMParser pemParser = new PEMParser (new StringReader (sKey)))
    {
      pemObj = pemParser.readObject ();
    }
    final PrivateKeyInfo privateKeyInfo = pemObj instanceof PEMKeyPair ? ((PEMKeyPair) pemObj).getPrivateKeyInfo ()
                                                                       : PrivateKeyInfo.getInstance (pemObj);
    final JcaPEMKeyConverter converter = new JcaPEMKeyConverter ();
    final BCECPrivateKey privKey = (BCECPrivateKey) converter.getPrivateKey (privateKeyInfo);
    assertNotNull (privKey);

    // Cross check
    assertTrue (privKey.getParameters () instanceof ECNamedCurveParameterSpec);
    assertEquals ("brainpoolP256r1", ((ECNamedCurveParameterSpec) privKey.getParameters ()).getName ());

    // Create in-memory Key Manager
    final KeyStore aKeyStore = EKeyStoreType.PKCS12.getKeyStore (provider);
    aKeyStore.load (null, null);
    final String sKeyPassword = "password";
    aKeyStore.setKeyEntry ("key1", privKey, sKeyPassword.toCharArray (), new Certificate [] { cert });

    // Create in-memory TrustManager
    final KeyStore aTrustStore = EKeyStoreType.PKCS12.getKeyStore (provider);
    // null stream means: create new key store
    aTrustStore.load (null, null);
    aTrustStore.setCertificateEntry ("trusted1", cert);

    // Build connection
    final HttpClientSettings aHCS = new HttpClientSettings ();
    // KeyStoreProvider stays default
    // TrustStoreProvider stays default
    // KeyManagerFactoryAlgorithm stays default
    // BouncyCastle JSSE Provider has issues
    final SSLContext aSSLCtx = SSLContexts.custom ()
                                          // .setProvider (new
                                          // BouncyCastleJsseProvider
                                          // (provider))
                                          .loadKeyMaterial (aKeyStore, sKeyPassword.toCharArray ())
                                          .loadTrustMaterial (aTrustStore, new TrustStrategyTrustAll ())
                                          .build ();
    aHCS.setSSLContext (aSSLCtx);
    if (false)
    {
      // Details for TLS 1.2

      // Tested with nginx 1.25.1 and openssl 3.0.9
      // - Works with native JSSE 11.0.16
      // - Works with native JSSE 17.0.4
      // - Doe NOT works with BouncyCastle 1.73 JSSE (TLS error 47)
      final String [] cipherSuites = { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                                       "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                       "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                                       "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };
      aHCS.setTLSConfigurationMode (new TLSConfigurationMode (new ETLSVersion [] { ETLSVersion.TLS_12 }, cipherSuites));
    }
    else
    {
      // Details for TLS 1.3

      // Tested with nginx 1.25.1 and openssl 3.0.9
      // - Works with native JSSE 11.0.16
      // - Works with native JSSE 17.0.4
      // - Works with BouncyCastle 1.73 JSSE
      final String [] cipherSuites = { "TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_AES_128_CCM_SHA256" };
      aHCS.setTLSConfigurationMode (new TLSConfigurationMode (new ETLSVersion [] { ETLSVersion.TLS_13 }, cipherSuites));
    }

    // Because we connect to an IP address
    aHCS.setHostnameVerifierVerifyAll ();

    final HttpClientFactory aHCF = new HttpClientFactory (aHCS);
    try (final HttpClientManager aHCM = new HttpClientManager (aHCF))
    {
      System.out.println ("Starting GET request");
      final HttpGet aGet = new HttpGet ("https://localhost:8443/");
      // Ignore result, we expect 404
      aHCM.execute (aGet, new ResponseHandlerByteArray ());
    }
    catch (final ExtendedHttpResponseException ex)
    {
      System.err.println ("HTTP error");
      ex.printStackTrace ();
    }
  }
}
