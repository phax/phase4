package com.helger.as4.client;

import java.io.File;

import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.encrypt.EncryptionCreator;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;

/**
 * AS4 standalone client invoker.
 *
 * @author Philip Helger
 */
@WorkInProgress
public class AS4Client
{
  private final AS4ResourceManager aResMgr = new AS4ResourceManager ();

  private ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
  private Node aPayload;
  private ESOAPVersion eSOAPVersion;

  // Keystore attributes
  // TODO look at AS2 Client / ClientSettinggs /ClientRequest
  private File m_aKeyStoreFile;
  private String m_sKeyStoreAlias;
  private String m_sKeyStorePassword;
  // org.apache.wss4j.common.crypto.Merlin is the default value
  private String m_sKeyStoreProvider = "org.apache.wss4j.common.crypto.Merlin";

  // Document related attributes
  private Document aDoc;
  private MimeMessage aMsg;
  private ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
  // For Message Info
  private String sMessageIDPrefix;
  // CollaborationInfo
  // TODO group accordingly if more then 1 parameter z.b group ServiceType and
  // Value in one setter
  private String sAction;

  private String sServiceType;
  private String sServiceValue;

  private String sConversationID;

  private String sAgreementRefPMode;
  private String sAgreementRefValue;

  private String sFromRole;
  private String sFromPartyID;

  private String sToRole;
  private String sToPartyID;

  // Signing additional attributes
  private ECryptoAlgorithmSign eECryptoAlgorithmSign = ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT;
  private ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest = ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT;

  /**
   * This method encrypts the current Document and is producing a encrypted
   * Document or a MimeMessage. Encrypted Document if only a SOAP BodyPaylod is
   * present MimeMessage if Attachments are present
   *
   * @throws Exception
   *         if something goes wrong in the encryption process
   */
  public void encryptDocument () throws Exception
  {
    if (aDoc == null)
    {
      throw new IllegalStateException ("No Document is set.");
    }
    else
    {
      _checkKeystoreAttributes ();

      final EncryptionCreator aEncCreator = new EncryptionCreator ();
      // MustUnderstand always set to true
      if (aAttachments.isNotEmpty ())
      {
        aMsg = aEncCreator.encryptMimeMessage (eSOAPVersion, aDoc, true, aAttachments, aResMgr);
      }
      else
      {
        aDoc = aEncCreator.encryptSoapBodyPayload (eSOAPVersion, aDoc, true);
      }
    }
  }

  public void signDocument () throws Exception
  {
    if (aDoc == null)
    {
      throw new IllegalStateException ("No Document is set.");
    }
    else
    {
      _checkKeystoreAttributes ();
      aDoc = new SignedMessageCreator ().createSignedMessage (aDoc,
                                                              eSOAPVersion,
                                                              aAttachments,
                                                              aResMgr,
                                                              true,
                                                              eECryptoAlgorithmSign,
                                                              eECryptoAlgorithmSignDigest);
      if (aAttachments.isNotEmpty ())
      {
        aMsg = new MimeMessageCreator (eSOAPVersion).generateMimeMessage (aDoc, aAttachments);
      }

    }
  }

  private void _checkKeystoreAttributes ()
  {
    if (StringHelper.hasNoText (m_sKeyStoreAlias) ||
        StringHelper.hasNoText (m_sKeyStorePassword) ||
        !m_aKeyStoreFile.exists ())
    {
      throw new IllegalStateException ("At least one of the following Alias: " +
                                       m_sKeyStoreAlias +
                                       ", Password: " +
                                       m_sKeyStorePassword +
                                       " or the KeyStoreFile:(rue if the file exists) " +
                                       m_aKeyStoreFile.exists () +
                                       "are not set.");
    }
  }

  /**
   * Only returns something appropriate if the attributes got set before. Not
   * every attribute needs to be set.
   *
   * @return Document that is produced with the current settings.
   */
  public Document buildMessage ()
  {
    final Ebms3MessageInfo aEbms3MessageInfo = CreateUserMessage.createEbms3MessageInfo (sMessageIDPrefix);
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo (sAction,
                                                                                                           sServiceType,
                                                                                                           sServiceValue,
                                                                                                           sConversationID,
                                                                                                           sAgreementRefPMode,
                                                                                                           sAgreementRefValue);
    final Ebms3PartyInfo aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (sFromRole,
                                                                                   sFromPartyID,
                                                                                   sToRole,
                                                                                   sToPartyID);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     eSOAPVersion)
                                                 .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

  public ICommonsList <WSS4JAttachment> getAttachments ()
  {
    return aAttachments;
  }

  public void setAttachments (final ICommonsList <WSS4JAttachment> aAttachments)
  {
    this.aAttachments = aAttachments;
  }

  public Node getPayload ()
  {
    return aPayload;
  }

  public void setPayload (final Node aPayload)
  {
    this.aPayload = aPayload;
  }

  public ESOAPVersion geteSOAPVersion ()
  {
    return eSOAPVersion;
  }

  public void seteSOAPVersion (final ESOAPVersion eSOAPVersion)
  {
    this.eSOAPVersion = eSOAPVersion;
  }

  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  public void setKeyStoreFile (final File m_aKeyStoreFile)
  {
    this.m_aKeyStoreFile = m_aKeyStoreFile;
  }

  public String getKeyStoreAlias ()
  {
    return m_sKeyStoreAlias;
  }

  public void setKeyStoreAlias (final String m_sKeyStoreAlias)
  {
    this.m_sKeyStoreAlias = m_sKeyStoreAlias;
  }

  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  public void setKeyStorePassword (final String m_sKeyStorePassword)
  {
    this.m_sKeyStorePassword = m_sKeyStorePassword;
  }

  public String getKeyStoreProvider ()
  {
    return m_sKeyStoreProvider;
  }

  public void setKeyStoreProvider (final String m_sKeyStoreAlias)
  {
    this.m_sKeyStoreProvider = m_sKeyStoreAlias;
  }

  public Document getDoc ()
  {
    return aDoc;
  }

  public void setDoc (final Document aDoc)
  {
    this.aDoc = aDoc;
  }

  public ICommonsList <Ebms3Property> getEbms3Properties ()
  {
    return aEbms3Properties;
  }

  public void setEbms3Properties (final ICommonsList <Ebms3Property> aEbms3Properties)
  {
    this.aEbms3Properties = aEbms3Properties;
  }

  public String getMessageIDPrefix ()
  {
    return sMessageIDPrefix;
  }

  public void setMessageIDPrefix (final String sMessageIDPrefix)
  {
    this.sMessageIDPrefix = sMessageIDPrefix;
  }

  public String getction ()
  {
    return sAction;
  }

  public void setction (final String sAction)
  {
    this.sAction = sAction;
  }

  public String getServiceType ()
  {
    return sServiceType;
  }

  public void setServiceType (final String sServiceType)
  {
    this.sServiceType = sServiceType;
  }

  public String getServiceValue ()
  {
    return sServiceValue;
  }

  public void setServiceValue (final String sServiceValue)
  {
    this.sServiceValue = sServiceValue;
  }

  public String getConversationID ()
  {
    return sConversationID;
  }

  public void setConversationID (final String sConversationID)
  {
    this.sConversationID = sConversationID;
  }

  public String getgreementRefPMode ()
  {
    return sAgreementRefPMode;
  }

  public void setgreementRefPMode (final String sAgreementRefPMode)
  {
    this.sAgreementRefPMode = sAgreementRefPMode;
  }

  public String getgreementRefValue ()
  {
    return sAgreementRefValue;
  }

  public void setgreementRefValue (final String sAgreementRefValue)
  {
    this.sAgreementRefValue = sAgreementRefValue;
  }

  public String getFromRole ()
  {
    return sFromRole;
  }

  public void setFromRole (final String sFromRole)
  {
    this.sFromRole = sFromRole;
  }

  public String getFromPartyID ()
  {
    return sFromPartyID;
  }

  public void setFromPartyID (final String sFromPartyID)
  {
    this.sFromPartyID = sFromPartyID;
  }

  public String getToRole ()
  {
    return sToRole;
  }

  public void setToRole (final String sToRole)
  {
    this.sToRole = sToRole;
  }

  public String getToPartyID ()
  {
    return sToPartyID;
  }

  public void setToPartyID (final String sToPartyID)
  {
    this.sToPartyID = sToPartyID;
  }

  public ECryptoAlgorithmSign getECryptoAlgorithmSign ()
  {
    return eECryptoAlgorithmSign;
  }

  public void setECryptoAlgorithmSign (final ECryptoAlgorithmSign eECryptoAlgorithmSign)
  {
    this.eECryptoAlgorithmSign = eECryptoAlgorithmSign;
  }

  public ECryptoAlgorithmSignDigest getECryptoAlgorithmSignDigest ()
  {
    return eECryptoAlgorithmSignDigest;
  }

  public void seeECryptoAlgorithmSignDigest (final ECryptoAlgorithmSignDigest eECryptoAlgorithmSignDigest)
  {
    this.eECryptoAlgorithmSignDigest = eECryptoAlgorithmSignDigest;
  }

}
