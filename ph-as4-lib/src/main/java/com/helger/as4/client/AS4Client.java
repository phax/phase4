package com.helger.as4.client;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.encrypt.EncryptionCreator;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.mime.MimeType;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.httpclient.HttpClientFactory;

/**
 * AS4 standalone client invoker.
 *
 * @author Philip Helger
 * @author bayerlma
 */
@WorkInProgress
public class AS4Client
{
  private final AS4ResourceManager m_aResMgr = new AS4ResourceManager ();

  private ESOAPVersion m_eSOAPVersion = ESOAPVersion.AS4_DEFAULT;
  private Node m_aPayload;
  private final ICommonsList <WSS4JAttachment> m_aAttachments = new CommonsArrayList <> ();

  // Document related attributes
  private final ICommonsList <Ebms3Property> m_aEbms3Properties = new CommonsArrayList <> ();
  // For Message Info
  private String m_sMessageIDPrefix;
  // CollaborationInfo
  // TODO group accordingly if more then 1 parameter z.b group ServiceType and
  // Value in one setter
  private String m_sAction;

  private String m_sServiceType;
  private String m_sServiceValue;

  private String m_sConversationID;

  private String m_sAgreementRefPMode;
  private String m_sAgreementRefValue;

  private String m_sFromRole;
  private String m_sFromPartyID;

  private String m_sToRole;
  private String m_sToPartyID;

  // Keystore attributes
  private File m_aKeyStoreFile;
  private String m_sKeyStoreType = "jks";
  private String m_sKeyStoreAlias;
  private String m_sKeyStorePassword;

  // Signing additional attributes
  private ECryptoAlgorithmSign m_eCryptoAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eCryptoAlgorithmSignDigest;
  // Encryption attribute
  private ECryptoAlgorithmCrypt m_eCryptoAlgorithmCrypt;

  private void _checkKeystoreAttributes ()
  {
    if (m_aKeyStoreFile == null)
      throw new IllegalStateException ("Key store file is not configured.");
    if (!m_aKeyStoreFile.exists ())
      throw new IllegalStateException ("Key store file does not exist: " + m_aKeyStoreFile.getAbsolutePath ());
    if (StringHelper.hasNoText (m_sKeyStoreType))
      throw new IllegalStateException ("Key store type is configured.");
    if (StringHelper.hasNoText (m_sKeyStoreAlias))
      throw new IllegalStateException ("Key store alias is configured.");
    if (StringHelper.hasNoText (m_sKeyStorePassword))
      throw new IllegalStateException ("Key store password is configured.");
  }

  /**
   * Build the AS4 message to be send. It uses all the attributes of this class
   * to build the final message. Compression, signing and encryption happens in
   * this methods.
   *
   * @return The HTTP entity to be send - never <code>null</code>.
   * @throws Exception
   *         in case something goes wrong
   */
  @Nonnull
  public HttpEntity buildMessage () throws Exception
  {
    final boolean bSign = m_eCryptoAlgorithmSign != null && m_eCryptoAlgorithmSignDigest != null;
    final boolean bEncrypt = m_eCryptoAlgorithmCrypt != null;
    final boolean bAttachmentsPresent = m_aAttachments.isNotEmpty ();

    final Ebms3MessageInfo aEbms3MessageInfo = CreateUserMessage.createEbms3MessageInfo (m_sMessageIDPrefix);
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (m_aPayload, m_aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo (m_sAction,
                                                                                                           m_sServiceType,
                                                                                                           m_sServiceValue,
                                                                                                           m_sConversationID,
                                                                                                           m_sAgreementRefPMode,
                                                                                                           m_sAgreementRefValue);
    final Ebms3PartyInfo aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (m_sFromRole,
                                                                                   m_sFromPartyID,
                                                                                   m_sToRole,
                                                                                   m_sToPartyID);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (m_aEbms3Properties);

    final AS4UserMessage aUserMsg = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                         aEbms3PayloadInfo,
                                                                         aEbms3CollaborationInfo,
                                                                         aEbms3PartyInfo,
                                                                         aEbms3MessageProperties,
                                                                         m_eSOAPVersion)
                                                     .setMustUnderstand (true);
    Document aDoc = aUserMsg.getAsSOAPDocument (m_aPayload);

    // 1. compress
    // Is done when the attachments are added

    // 2. sign and/or encrpyt
    MimeMessage aMimeMsg = null;
    if (bSign || bEncrypt)
    {
      _checkKeystoreAttributes ();

      final ICommonsMap <String, String> aCryptoProps = new CommonsLinkedHashMap <> ();
      aCryptoProps.put ("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.file", m_aKeyStoreFile.getPath ());
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.type", m_sKeyStoreType);
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.password", m_sKeyStorePassword);
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.alias", m_sKeyStoreAlias);
      final AS4CryptoFactory aCryptoFactory = new AS4CryptoFactory (aCryptoProps);

      // 2a. sign
      if (bSign)
      {
        final Document aSignedDoc = new SignedMessageCreator (aCryptoFactory).createSignedMessage (aDoc,
                                                                                                   m_eSOAPVersion,
                                                                                                   m_aAttachments,
                                                                                                   m_aResMgr,
                                                                                                   true,
                                                                                                   m_eCryptoAlgorithmSign,
                                                                                                   m_eCryptoAlgorithmSignDigest);
        aDoc = aSignedDoc;
      }

      // 2b. encrypt
      if (bEncrypt)
      {
        _checkKeystoreAttributes ();
        final EncryptionCreator aEncCreator = new EncryptionCreator (aCryptoFactory);
        // MustUnderstand always set to true
        if (bAttachmentsPresent)
        {
          aMimeMsg = aEncCreator.encryptMimeMessage (m_eSOAPVersion,
                                                     aDoc,
                                                     true,
                                                     m_aAttachments,
                                                     m_aResMgr,
                                                     m_eCryptoAlgorithmCrypt);
        }
        else
        {
          aDoc = aEncCreator.encryptSoapBodyPayload (m_eSOAPVersion, aDoc, true, m_eCryptoAlgorithmCrypt);
        }
      }
    }

    if (bAttachmentsPresent && aMimeMsg == null)
    {
      // * not encrypted, not signed
      // * not encrypted, signed
      aMimeMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aDoc, m_aAttachments);
    }

    if (aMimeMsg != null)
    {
      return new HttpMimeMessageEntity (aMimeMsg);
    }

    // Wrap SOAP XML
    return new StringEntity (AS4XMLHelper.serializeXML (aDoc));
  }

  public void sendMessage (@Nonnull final String sURL, @Nonnull final HttpEntity aHttpEntity) throws Exception
  {
    sendMessage (sURL, aHttpEntity, null);
  }

  public void sendMessage (@Nonnull final String sURL,
                           @Nonnull final HttpEntity aHttpEntity,
                           @Nullable final RequestConfig aRequestConfig) throws Exception
  {
    SSLContext aSSLContext = null;
    if (sURL.startsWith ("https"))
    {
      aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (null,
                        new TrustManager [] { new TrustManagerTrustAll (false) },
                        RandomHelper.getSecureRandom ());
    }

    final CloseableHttpClient aClient = new HttpClientFactory (aSSLContext).createHttpClient ();
    final HttpPost aPost = new HttpPost (sURL);

    if (aRequestConfig != null)
    {
      aPost.setConfig (aRequestConfig);
    }

    if (aHttpEntity instanceof HttpMimeMessageEntity)
      MessageHelperMethods.moveMIMEHeadersToHTTPHeader (((HttpMimeMessageEntity) aHttpEntity).getMimeMessage (), aPost);
    aPost.setEntity (aHttpEntity);

    aClient.execute (aPost);
  }

  @Nonnull
  public ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  /**
   * This method sets the SOAP Version. AS4 - Profile Default is SOAP 1.2
   *
   * @param eSOAPVersion
   *        SOAPVersion which should be set
   */
  public void setSOAPVersion (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_eSOAPVersion = eSOAPVersion;
  }

  public Node getPayload ()
  {
    return m_aPayload;
  }

  /**
   * Sets the payload for a usermessage. The payload unlike an attachment will
   * be added into the SOAP-Body of the message.
   *
   * @param aPayload
   *        the Payload to be added
   */
  public void setPayload (final Node aPayload)
  {
    m_aPayload = aPayload;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <WSS4JAttachment> getAllAttachments ()
  {
    return m_aAttachments;
  }

  /**
   * Adds a file as attachment to the message.
   *
   * @param aAttachment
   *        file which should be added
   * @param aMimeType
   *        mimetype of the given file
   * @throws IOException,
   *         if something goes wrong in the adding process
   */
  public void addAttachment (@Nonnull final File aAttachment, @Nonnull final MimeType aMimeType) throws IOException
  {
    addAttachment (aAttachment, aMimeType, null);
  }

  /**
   * Adds a file as attachment to the message.
   *
   * @param aAttachment
   *        file which should be added
   * @param aMimeType
   *        mimetype of the given file
   * @param eAS4CompressionMode
   *        which compression type should be used to compress the attachment
   * @throws IOException
   *         if something goes wrong in the adding process or the compression
   */
  public void addAttachment (@Nonnull final File aAttachment,
                             @Nonnull final MimeType aMimeType,
                             @Nullable final EAS4CompressionMode eAS4CompressionMode) throws IOException
  {
    m_aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (aAttachment,
                                                                      aMimeType,
                                                                      eAS4CompressionMode,
                                                                      m_aResMgr));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Ebms3Property> getAllEbms3Properties ()
  {
    return m_aEbms3Properties.getClone ();
  }

  /**
   * With properties optional info can be added for the receiving party. If you
   * want to be AS4 Profile conform you need to add two properties to your
   * message: originalSender and finalRecipient these two correlate to C1 and
   * C4.
   *
   * @param aEbms3Properties
   *        Properties that should be set in the current usermessage
   */
  public void setEbms3Properties (@Nullable final ICommonsList <Ebms3Property> aEbms3Properties)
  {
    m_aEbms3Properties.setAll (aEbms3Properties);
  }

  public String getMessageIDPrefix ()
  {
    return m_sMessageIDPrefix;
  }

  /**
   * If it is desired to set a MessagePrefix for the MessageID it can be done
   * here.
   *
   * @param sMessageIDPrefix
   *        Prefix that will be at the start of the MessageID
   */
  public void setMessageIDPrefix (final String sMessageIDPrefix)
  {
    m_sMessageIDPrefix = sMessageIDPrefix;
  }

  public String getAction ()
  {
    return m_sAction;
  }

  /**
   * The element is a string identifying an operation or an activity within a
   * Service that may support several of these.<br>
   * Example of what will be written in the usermessage:
   * <eb:Action>NewPurchaseOrder</eb:Action> <br>
   * This is MANDATORY.
   *
   * @param sAction
   *        the action that should be there.
   */
  public void setAction (final String sAction)
  {
    m_sAction = sAction;
  }

  public String getServiceType ()
  {
    return m_sServiceType;
  }

  /**
   * It is a string identifying the servicetype of the service specified in
   * servicevalue.<br>
   * Example of what will be written in the usermessage:
   * <eb:Service type= "MyServiceTypes">QuoteToCollect</eb:Service><br>
   *
   * @param sServiceType
   *        serviceType that should be set
   */
  public void setServiceType (final String sServiceType)
  {
    m_sServiceType = sServiceType;
  }

  public String getServiceValue ()
  {
    return m_sServiceValue;
  }

  /**
   * It is a string identifying the service that acts on the message 1639 and it
   * is specified by the designer of the service.<br>
   * Example of what will be written in the usermessage:
   * <eb:Service type= "MyServiceTypes">QuoteToCollect</eb:Service><br>
   * This is MANDATORY.
   *
   * @param sServiceValue
   *        the servicevalue that should be set
   */
  public void setServiceValue (final String sServiceValue)
  {
    m_sServiceValue = sServiceValue;
  }

  public String getConversationID ()
  {
    return m_sConversationID;
  }

  /**
   * The element is a string identifying the set of related messages that make
   * up a conversation between Parties.<br>
   * Example of what will be written in the usermessage:
   * <eb:ConversationId>4321</eb:ConversationId><br>
   * This is MANDATORY.
   *
   * @param sConversationID
   *        the conversationID that should be set
   */
  public void setConversationID (final String sConversationID)
  {
    m_sConversationID = sConversationID;
  }

  public String getAgreementRefPMode ()
  {
    return m_sAgreementRefPMode;
  }

  /**
   * The AgreementRef element requires a PModeID which can be set with this
   * method.<br>
   * Example of what will be written in the usermessage: <eb:AgreementRef pmode=
   * "pm-esens-generic-resp">http://agreements.holodeckb2b.org/examples/agreement0</eb:AgreementRef><br>
   * This is MANDATORY.
   *
   * @param sAgreementRefPMode
   *        PMode that should be used (id)
   */
  public void setAgreementRefPMode (final String sAgreementRefPMode)
  {
    m_sAgreementRefPMode = sAgreementRefPMode;
  }

  public String getAgreementRefValue ()
  {
    return m_sAgreementRefValue;
  }

  /**
   * The AgreementRef element is a string that identifies 1636 the entity or
   * artifact governing the exchange of messages between the parties.<br>
   * Example of what will be written in the usermessage: <eb:AgreementRef pmode=
   * "pm-esens-generic-resp">http://agreements.holodeckb2b.org/examples/agreement0</eb:AgreementRef><br>
   * This is MANDATORY.
   *
   * @param sAgreementRefValue
   *        agreementreference that should be set
   */
  public void setAgreementRefValue (final String sAgreementRefValue)
  {
    m_sAgreementRefValue = sAgreementRefValue;
  }

  public String getFromRole ()
  {
    return m_sFromRole;
  }

  /**
   * The value of the Role element is a non-empty string, with a default value
   * of
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole .
   *
   * @param sFromRole
   *        the role that should be set
   */
  public void setFromRole (final String sFromRole)
  {
    m_sFromRole = sFromRole;
  }

  public String getFromPartyID ()
  {
    return m_sFromPartyID;
  }

  /**
   * The PartyID is an ID that identifies the C2 over which the message gets
   * sent.<br>
   * Example of what will be written in the usermessage:
   * <eb:PartyId>ImAPartyID</eb:PartyId><br>
   * This is MANDATORY.
   *
   * @param sFromPartyID
   *        the partyID that should be set
   */
  public void setFromPartyID (final String sFromPartyID)
  {
    m_sFromPartyID = sFromPartyID;
  }

  public String getToRole ()
  {
    return m_sToRole;
  }

  /**
   * @see #setFromRole(String)
   * @param sToRole
   *        the role that should be used
   */
  public void setToRole (final String sToRole)
  {
    m_sToRole = sToRole;
  }

  public String getToPartyID ()
  {
    return m_sToPartyID;
  }

  /**
   * * @see #setFromPartyID(String)
   *
   * @param sToPartyID
   *        the PartyID that should be set
   */
  public void setToPartyID (final String sToPartyID)
  {
    m_sToPartyID = sToPartyID;
  }

  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  /**
   * The keystore that should be used can be set here.<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param aKeyStoreFile
   *        the keystore file that should be used
   */
  public void setKeyStoreFile (final File aKeyStoreFile)
  {
    m_aKeyStoreFile = aKeyStoreFile;
  }

  @Nonnull
  @Nonempty
  public String getKeyStoreType ()
  {
    return m_sKeyStoreType;
  }

  /**
   * The type of the keystore needs to be set if a keystore is used.<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStoreType
   *        keystoretype that should be set, e.g. jks
   */
  public void setKeyStoreType (@Nonnull @Nonempty final String sKeyStoreType)
  {
    ValueEnforcer.notEmpty (sKeyStoreType, "KeyStoreType");
    m_sKeyStoreType = sKeyStoreType;
  }

  public String getKeyStoreAlias ()
  {
    return m_sKeyStoreAlias;
  }

  /**
   * Keystorealias needs to be set if a keystore is used<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStoreAlias
   *        alias that should be set
   */
  public void setKeyStoreAlias (final String sKeyStoreAlias)
  {
    m_sKeyStoreAlias = sKeyStoreAlias;
  }

  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  /**
   * Keystorepassword needs to be set if a keystore is used<<br>
   * MANDATORY if you want to use sign or encryption of an usermessage.
   *
   * @param sKeyStorePassword
   *        password that should be set
   */
  public void setKeyStorePassword (final String sKeyStorePassword)
  {
    m_sKeyStorePassword = sKeyStorePassword;
  }

  @Nullable
  public ECryptoAlgorithmSign getCryptoAlgorithmSign ()
  {
    return m_eCryptoAlgorithmSign;
  }

  /**
   * A signing algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see
   * {@link #setECryptoAlgorithmSignDigest(ECryptoAlgorithmSignDigest)}
   *
   * @param eCryptoAlgorithmSign
   *        the signing algorithm that should be set
   */
  public void setCryptoAlgorithmSign (@Nullable final ECryptoAlgorithmSign eCryptoAlgorithmSign)
  {
    m_eCryptoAlgorithmSign = eCryptoAlgorithmSign;
  }

  @Nullable
  public ECryptoAlgorithmSignDigest getECryptoAlgorithmSignDigest ()
  {
    return m_eCryptoAlgorithmSignDigest;
  }

  /**
   * A signing digest algorithm can be set. <br>
   * MANDATORY if you want to use sign.<br>
   * Also @see {@link #setCryptoAlgorithmSign(ECryptoAlgorithmSign)}
   *
   * @param eECryptoAlgorithmSignDigest
   *        the signing digest algorithm that should be set
   */
  public void setECryptoAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest)
  {
    m_eCryptoAlgorithmSignDigest = eECryptoAlgorithmSignDigest;
  }

  @Nullable
  public ECryptoAlgorithmCrypt getCryptoAlgorithmCrypt ()
  {
    return m_eCryptoAlgorithmCrypt;
  }

  /**
   * A encryption algorithm can be set. <br>
   * MANDATORY if you want to use encryption.
   *
   * @param eCryptoAlgorithmCrypt
   *        the encryption algorithm that should be set
   */
  public void setCryptoAlgorithmCrypt (@Nullable final ECryptoAlgorithmCrypt eCryptoAlgorithmCrypt)
  {
    m_eCryptoAlgorithmCrypt = eCryptoAlgorithmCrypt;
  }
}
