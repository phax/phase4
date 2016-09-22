package com.helger.as4server.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.standalone.RunInJettyAS4;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.url.URLHelper;
import com.helger.photon.jetty.JettyStarter;
import com.helger.photon.jetty.JettyStopper;
import com.helger.xml.serialize.read.DOMReader;

public class PModeCheckTest extends AbstractUserMessageSetUp
{
  private static final int PORT = URLHelper.getAsURL (PROPS.getAsString ("server.address")).getPort ();
  private static final int STOP_PORT = PORT + 1000;

  @BeforeClass
  public static void startServer () throws Exception
  {
    new Thread ( () -> {
      try
      {
        new JettyStarter (RunInJettyAS4.class).setPort (PORT).setStopPort (STOP_PORT).run ();
      }
      catch (final Exception ex)
      {
        ex.printStackTrace ();
      }
    }).start ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    new JettyStopper ().setStopPort (STOP_PORT).run ();
  }

  @Test
  public void testWrongPModeID () throws Exception
  {
    final Document aDoc = _modifyUserMessage ("this-is-a-wrong-id", null, null, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testWrongSOAPVersion () throws Exception
  {
    final Document aDoc = _modifyUserMessage (null, ESOAPVersion.SOAP_11, null, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testWrongPartyIDInitiator () throws Exception
  {
    final Document aDoc = _modifyUserMessage (null, null, "random_party_id120", null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testWrongPartyIDResponder () throws Exception
  {
    final Document aDoc = _modifyUserMessage (null, null, null, "random_party_id121");

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testSigningAlgorithm () throws Exception
  {

    final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (_modifyUserMessage (null,
                                                                                                     null,
                                                                                                     null,
                                                                                                     null),
                                                                                 ESOAPVersion.AS4_DEFAULT,
                                                                                 null,
                                                                                 false,
                                                                                 ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                 ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)), true, "200");

  }

  @Nonnull
  private Document _modifyUserMessage (@Nullable final String sWrongPModeID,
                                       @Nullable final ESOAPVersion eWrongESOAPVersion,
                                       @Nullable final String sWrongPartyIdInitiator,
                                       @Nullable final String sWrongPartyIdResponder) throws Exception
  {
    // If argument is set replace the default one
    final String sSetPModeID = sWrongPModeID == null ? "pm-esens-generic-resp" : sWrongPModeID;
    final ESOAPVersion eSetESOAPVersion = eWrongESOAPVersion == null ? ESOAPVersion.AS4_DEFAULT : eWrongESOAPVersion;
    final String sSetPartyIDInitiator = sWrongPartyIdInitiator == null ? "APP_1000000101" : sWrongPartyIdInitiator;
    final String sSetPartyIDResponder = sWrongPartyIdResponder == null ? "APP_1000000101" : sWrongPartyIdResponder;

    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aEbms3PropertyProcess = new Ebms3Property ();
    aEbms3PropertyProcess.setName ("ProcessInst");
    aEbms3PropertyProcess.setValue ("PurchaseOrder:123456");
    final Ebms3Property aEbms3PropertyContext = new Ebms3Property ();
    aEbms3PropertyContext.setName ("ContextID");
    aEbms3PropertyContext.setValue ("987654321");
    aEbms3Properties.add (aEbms3PropertyContext);
    aEbms3Properties.add (aEbms3PropertyProcess);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("AS4-Server");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      sSetPModeID,
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              sSetPartyIDInitiator,
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              sSetPartyIDResponder);
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                eSetESOAPVersion)
                                            .setMustUnderstand (false);

    return aDoc.getAsSOAPDocument (aPayload);
  }
}
