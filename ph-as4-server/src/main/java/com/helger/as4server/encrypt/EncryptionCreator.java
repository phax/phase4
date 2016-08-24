package com.helger.as4server.encrypt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OutputEncryptor;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.as4lib.mime.SoapMimeMultipart;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;

public class EncryptionCreator
{
  private final WSSecurityEngine m_aSecEngine = new WSSecurityEngine ();
  private byte [] m_aKeyData;
  private SecretKey m_aKey;
  private Crypto m_aCrypto = null;

  public EncryptionCreator () throws WSSecurityException
  {
    m_aCrypto = CryptoFactory.getInstance ("test.properties");
  }

  public Document encryptSoapBodyPayload (final ESOAPVersion eSOAPVersion,
                                          final Document aDoc,
                                          final boolean bMustUnderstand) throws Exception
  {

    final KeyGenerator aKeyGen = KeyGenerator.getInstance ("AES");
    aKeyGen.init (128);
    m_aKey = aKeyGen.generateKey ();
    m_aKeyData = m_aKey.getEncoded ();
    m_aSecEngine.setWssConfig (WSSConfig.getNewInstance ());

    final WSSecEncrypt aBuilder = new WSSecEncrypt ();
    aBuilder.setKeyIdentifierType (WSConstants.ISSUER_SERIAL);
    aBuilder.setSymmetricEncAlgorithm (WSS4JConstants.AES_128_GCM);
    aBuilder.setSymmetricKey (null);
    aBuilder.setUserInfo (CryptoConfigBuilder.CF.getAsString ("encrypt.alias"),
                          CryptoConfigBuilder.CF.getAsString ("encrypt.password"));

    final WSEncryptionPart encP = new WSEncryptionPart ("Body", eSOAPVersion.getNamespaceURI (), "Content");
    aBuilder.getParts ().add (encP);
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNodeNS (eSOAPVersion.getNamespaceURI (),
                                                                                     "mustUnderstand");
    if (aMustUnderstand != null)
      aMustUnderstand.setValue (eSOAPVersion.getMustUnderstandValue (bMustUnderstand));
    return aBuilder.build (aDoc, m_aCrypto, aSecHeader);
  }

  public MimeMessage encryptMimeMessageAttachments (final MimeMessage aMimeMessage) throws IOException,
                                                                                    MessagingException,
                                                                                    WSSecurityException,
                                                                                    GeneralSecurityException,
                                                                                    SMIMEException,
                                                                                    CMSException
  {
    final CryptoType aCryptoType = new CryptoType (CryptoType.TYPE.ALIAS);
    aCryptoType.setAlias (CryptoConfigBuilder.CF.getAsString ("encrypt.alias"));
    final X509Certificate [] aCertList = m_aCrypto.getX509Certificates (aCryptoType);

    final SoapMimeMultipart aMimeMultipart = (SoapMimeMultipart) aMimeMessage.getContent ();
    // Starts at 1 since the first mime message part, the user message does not
    // need to be encrypted
    for (int i = 1; i < aMimeMultipart.getCount (); i++)
    {
      final MimeBodyPart aMimeBodyPart = (MimeBodyPart) aMimeMultipart.getBodyPart (i);
      aMimeMultipart.removeBodyPart (i);

      aMimeMultipart.addBodyPart (encrypt (aMimeBodyPart, aCertList[0], ECryptoAlgorithmCrypt.AES_128_GCM));
    }
    return aMimeMessage;
  }

  @Nonnull
  public MimeBodyPart encrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final ECryptoAlgorithmCrypt eAlgorithm) throws GeneralSecurityException,
                                                                                SMIMEException,
                                                                                CMSException
  {
    ValueEnforcer.notNull (aPart, "MimeBodyPart");
    ValueEnforcer.notNull (aX509Cert, "X509Cert");
    ValueEnforcer.notNull (eAlgorithm, "Algorithm");

    // Check if the certificate is expired or active.
    aX509Cert.checkValidity ();

    final ASN1ObjectIdentifier aEncAlg = eAlgorithm.getOID ();

    final SMIMEEnvelopedGenerator aGen = new SMIMEEnvelopedGenerator ();
    aGen.addRecipientInfoGenerator (new JceKeyTransRecipientInfoGenerator (aX509Cert).setProvider (BouncyCastleProvider.PROVIDER_NAME));

    final OutputEncryptor aEncryptor = new JceCMSContentEncryptorBuilder (aEncAlg).setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                                  .build ();
    final MimeBodyPart aEncData = aGen.generate (aPart, aEncryptor);
    return aEncData;
  }
}
