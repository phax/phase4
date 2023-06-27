package com.helger.phase4.bdew.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

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
import org.junit.Test;

import com.helger.bc.PBCProvider;
import com.helger.commons.io.stream.StringInputStream;
import com.helger.http.tls.ETLSVersion;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.httpclient.security.TrustStrategyTrustAll;
import com.helger.security.keystore.EKeyStoreType;

public class BrainpoolFuncTest
{
  @Test
  public void testCreateCert () throws Exception
  {
    final Provider provider = PBCProvider.getProvider ();
    assertNotNull (provider);

    final ECNamedCurveParameterSpec brainpoolp256r1Spec = ECNamedCurveTable.getParameterSpec ("brainpoolp256r1");
    assertNotNull (brainpoolp256r1Spec);

    final SecureRandom secureRandom = null;
    if (false)
    {
      // Create a new pair
      final KeyPairGenerator brainpoolp256r1Generator = KeyPairGenerator.getInstance ("ECDSA", provider);
      brainpoolp256r1Generator.initialize (brainpoolp256r1Spec, secureRandom);

      final KeyPair keyPair = brainpoolp256r1Generator.genKeyPair ();
      assertNotNull (keyPair);
    }

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
    aHCS.setSSLContext (SSLContexts.custom ()
                                   .setProvider (provider)
                                   .setProtocol (ETLSVersion.TLS_13.getID ())
                                   .loadKeyMaterial (aKeyStore, sKeyPassword.toCharArray ())
                                   .loadTrustMaterial (aTrustStore, new TrustStrategyTrustAll ())
                                   .build ());
    try (HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      System.out.println ("Starting GET request");
      final HttpGet aGet = new HttpGet ("https://159.69.113.243:8443/");
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
