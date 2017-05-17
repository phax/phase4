package com.helger.as4.CEF;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.messaging.domain.AS4UserMessage;
import com.helger.as4.messaging.domain.CreateUserMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.messaging.sign.SignedMessageCreator;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public abstract class AbstractCEFTestSetUp extends AbstractUserMessageTestSetUp
{
  protected static final String INITIATOR_ID = "CEF-Initiator";
  protected static final String RESPONDER_ID = "CEF-Responder";
  protected static final String RESPONDER_ADDRESS = "http://localhost:8080/as4";

  protected PMode m_aESENSOneWayPMode;
  protected ESOAPVersion m_eSOAPVersion;
  protected Node m_aPayload;

  @Before
  public void setUpCEF ()
  {

    m_aESENSOneWayPMode = ESENSPMode.createESENSPMode (INITIATOR_ID,
                                                       RESPONDER_ID,
                                                       RESPONDER_ADDRESS,
                                                       IPModeIDProvider.DEFAULT_DYNAMIC);

    m_eSOAPVersion = m_aESENSOneWayPMode.getLeg1 ().getProtocol ().getSOAPVersion ();
    try
    {
      m_aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));
    }
    catch (final SAXException ex)
    {
      throw new IllegalStateException ("Failed to parse example XML", ex);
    }
  }

  protected Document testSignedUserMessage (@Nonnull final ESOAPVersion eSOAPVersion,
                                            @Nullable final Node aPayload,
                                            @Nullable final ICommonsList <WSS4JAttachment> aAttachments,
                                            @Nonnull final AS4ResourceManager aResMgr) throws WSSecurityException
  {
    final SignedMessageCreator aClient = new SignedMessageCreator ();

    final Document aSignedDoc = aClient.createSignedMessage (testUserMessageSoapNotSigned (aPayload, aAttachments),
                                                             eSOAPVersion,
                                                             aAttachments,
                                                             aResMgr,
                                                             false,
                                                             ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                             ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);
    return aSignedDoc;
  }

  protected Document testUserMessageSoapNotSigned (@Nullable final Node aPayload,
                                                   @Nullable final ICommonsList <WSS4JAttachment> aAttachments)
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = MockEbmsHelper.getEBMSProperties ();

    final Ebms3MessageInfo aEbms3MessageInfo = MessageHelperMethods.createEbms3MessageInfo ();
    final Ebms3PayloadInfo aEbms3PayloadInfo = CreateUserMessage.createEbms3PayloadInfo (aPayload, aAttachments);

    final Ebms3CollaborationInfo aEbms3CollaborationInfo;
    final Ebms3PartyInfo aEbms3PartyInfo;
    aEbms3CollaborationInfo = CreateUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                              "MyServiceTypes",
                                                                              MockPModeGenerator.SOAP11_SERVICE,
                                                                              "4321",
                                                                              m_aESENSOneWayPMode.getID (),
                                                                              MockEbmsHelper.DEFAULT_AGREEMENT);
    aEbms3PartyInfo = CreateUserMessage.createEbms3PartyInfo (CAS4.DEFAULT_SENDER_URL,
                                                              INITIATOR_ID,
                                                              CAS4.DEFAULT_RESPONDER_URL,
                                                              RESPONDER_ID);

    final Ebms3MessageProperties aEbms3MessageProperties = CreateUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = CreateUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                     aEbms3PayloadInfo,
                                                                     aEbms3CollaborationInfo,
                                                                     aEbms3PartyInfo,
                                                                     aEbms3MessageProperties,
                                                                     m_eSOAPVersion)
                                                 .setMustUnderstand (true);
    return aDoc.getAsSOAPDocument (aPayload);
  }

}
