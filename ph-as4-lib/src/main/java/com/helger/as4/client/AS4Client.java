package com.helger.as4.client;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
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
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.string.StringHelper;

/**
 * AS4 standalone client invoker.
 *
 * @author Philip Helger
 */
@WorkInProgress
public class AS4Client
{
  private final AS4ResourceManager m_aResMgr = new AS4ResourceManager ();

  private ESOAPVersion m_eSOAPVersion = ESOAPVersion.AS4_DEFAULT;
  private Node m_aPayload;
  private final ICommonsList <WSS4JAttachment> m_aAttachments = new CommonsArrayList<> ();

  // Keystore attributes
  // TODO look at AS2 Client / ClientSettinggs /ClientRequest
  private File m_aKeyStoreFile;
  private String m_sKeyStoreType = "jks";
  private String m_sKeyStoreAlias;
  private String m_sKeyStorePassword;

  // Document related attributes
  private final ICommonsList <Ebms3Property> m_aEbms3Properties = new CommonsArrayList<> ();
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

  // Signing additional attributes
  private ECryptoAlgorithmSign m_eCryptoAlgorithmSign;
  private ECryptoAlgorithmSignDigest m_eCryptoAlgorithmSignDigest;
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
   * Only returns something appropriate if the attributes got set before. Not
   * every attribute needs to be set.
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

    AS4CryptoFactory aCryptoFactory = null;
    if (bSign || bEncrypt)
    {
      _checkKeystoreAttributes ();

      final ICommonsMap <String, String> aCryptoProps = new CommonsLinkedHashMap<> ();
      aCryptoProps.put ("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.file", m_aKeyStoreFile.getPath ());
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.type", m_sKeyStoreType);
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.password", m_sKeyStorePassword);
      aCryptoProps.put ("org.apache.wss4j.crypto.merlin.keystore.alias", m_sKeyStoreAlias);
      aCryptoFactory = new AS4CryptoFactory (aCryptoProps);
    }

    // 2. sign
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

    // 3. encrypt
    MimeMessage aMimeMsg = null;
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

    if (bAttachmentsPresent && aMimeMsg == null)
    {
      // * not encrypted, not signed
      // * not encrypted, signed
      aMimeMsg = new MimeMessageCreator (m_eSOAPVersion).generateMimeMessage (aDoc, m_aAttachments);
    }

    if (aMimeMsg != null)
      return new HttpMimeMessageEntity (aMimeMsg);

    // Wrap SOAP XML
    return new StringEntity (AS4XMLHelper.serializeXML (aDoc));
  }

  @Nonnull
  public ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  public void setSOAPVersion (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
    m_eSOAPVersion = eSOAPVersion;
  }

  public Node getPayload ()
  {
    return m_aPayload;
  }

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

  public void setAllAttachments (@Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    m_aAttachments.setAll (aAttachments);
  }

  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  public void setKeyStoreFile (final File aKeyStoreFile)
  {
    m_aKeyStoreFile = aKeyStoreFile;
  }

  public String getKeyStoreType ()
  {
    return m_sKeyStoreType;
  }

  public void setKeyStoreType (final String sKeyStoreType)
  {
    m_sKeyStoreType = sKeyStoreType;
  }

  public String getKeyStoreAlias ()
  {
    return m_sKeyStoreAlias;
  }

  public void setKeyStoreAlias (final String sKeyStoreAlias)
  {
    m_sKeyStoreAlias = sKeyStoreAlias;
  }

  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  public void setKeyStorePassword (final String sKeyStorePassword)
  {
    m_sKeyStorePassword = sKeyStorePassword;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Ebms3Property> getAllEbms3Properties ()
  {
    return m_aEbms3Properties.getClone ();
  }

  public void setEbms3Properties (@Nullable final ICommonsList <Ebms3Property> aEbms3Properties)
  {
    m_aEbms3Properties.setAll (aEbms3Properties);
  }

  public String getMessageIDPrefix ()
  {
    return m_sMessageIDPrefix;
  }

  public void setMessageIDPrefix (final String sMessageIDPrefix)
  {
    m_sMessageIDPrefix = sMessageIDPrefix;
  }

  public String getAction ()
  {
    return m_sAction;
  }

  public void setAction (final String sAction)
  {
    m_sAction = sAction;
  }

  public String getServiceType ()
  {
    return m_sServiceType;
  }

  public void setServiceType (final String sServiceType)
  {
    m_sServiceType = sServiceType;
  }

  public String getServiceValue ()
  {
    return m_sServiceValue;
  }

  public void setServiceValue (final String sServiceValue)
  {
    m_sServiceValue = sServiceValue;
  }

  public String getConversationID ()
  {
    return m_sConversationID;
  }

  public void setConversationID (final String sConversationID)
  {
    m_sConversationID = sConversationID;
  }

  public String getAgreementRefPMode ()
  {
    return m_sAgreementRefPMode;
  }

  public void setAgreementRefPMode (final String sAgreementRefPMode)
  {
    m_sAgreementRefPMode = sAgreementRefPMode;
  }

  public String getAgreementRefValue ()
  {
    return m_sAgreementRefValue;
  }

  public void setAgreementRefValue (final String sAgreementRefValue)
  {
    m_sAgreementRefValue = sAgreementRefValue;
  }

  public String getFromRole ()
  {
    return m_sFromRole;
  }

  public void setFromRole (final String sFromRole)
  {
    m_sFromRole = sFromRole;
  }

  public String getFromPartyID ()
  {
    return m_sFromPartyID;
  }

  public void setFromPartyID (final String sFromPartyID)
  {
    m_sFromPartyID = sFromPartyID;
  }

  public String getToRole ()
  {
    return m_sToRole;
  }

  public void setToRole (final String sToRole)
  {
    m_sToRole = sToRole;
  }

  public String getToPartyID ()
  {
    return m_sToPartyID;
  }

  public void setToPartyID (final String sToPartyID)
  {
    m_sToPartyID = sToPartyID;
  }

  @Nullable
  public ECryptoAlgorithmSign getCryptoAlgorithmSign ()
  {
    return m_eCryptoAlgorithmSign;
  }

  public void setCryptoAlgorithmSign (@Nullable final ECryptoAlgorithmSign eCryptoAlgorithmSign)
  {
    m_eCryptoAlgorithmSign = eCryptoAlgorithmSign;
  }

  @Nullable
  public ECryptoAlgorithmSignDigest getECryptoAlgorithmSignDigest ()
  {
    return m_eCryptoAlgorithmSignDigest;
  }

  public void seeECryptoAlgorithmSignDigest (@Nullable final ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest)
  {
    m_eCryptoAlgorithmSignDigest = eECryptoAlgorithmSignDigest;
  }

  @Nullable
  public ECryptoAlgorithmCrypt getCryptoAlgorithmCrypt ()
  {
    return m_eCryptoAlgorithmCrypt;
  }

  public void setCryptoAlgorithmCrypt (@Nullable final ECryptoAlgorithmCrypt eCryptoAlgorithmCrypt)
  {
    m_eCryptoAlgorithmCrypt = eCryptoAlgorithmCrypt;
  }
}
