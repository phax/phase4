package com.helger.as4server.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public class PModeCheckTest extends AbstractUserMessageSetUp
{
  // TODO Currently not working as intended, since its not working at all
  // @BeforeClass
  // public static void startServer () throws Exception
  // {
  // new JettyStarter (RunInJettyAS4.class).run ();
  // }
  //
  // @AfterClass
  // public static void shutDownServer () throws Exception
  // {
  // new JettyStopper ().run ();
  // }

  @Test
  public void testWrongPModeID () throws Exception
  {
    final Document aDoc = _modfiyUserMessage ("this-is-a-wrong-id", null, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "400");
  }

  @Test
  public void testWrongSOAPVersion () throws Exception
  {
    final Document aDoc = _modfiyUserMessage (null, ESOAPVersion.SOAP_11, null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "400");
  }

  @Test
  public void testWrongPartyID () throws Exception
  {
    final Document aDoc = _modfiyUserMessage (null, null, "random_party_id120");

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), false, "400");
  }

  @Nonnull
  private Document _modfiyUserMessage (@Nullable final String sPModeID,
                                       @Nullable final ESOAPVersion eESOAPVersion,
                                       @Nullable final String sPartyId) throws Exception
  {
    // If argument is set replace the default one
    final String sSetPModeID = sPModeID == null ? "pm-esens-generic-resp" : sPModeID;
    final ESOAPVersion eSetESOAPVersion = eESOAPVersion == null ? ESOAPVersion.SOAP_12 : eESOAPVersion;
    final String sSetPartyID = sPartyId == null ? "APP_1000000101" : sPartyId;

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

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com");
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      sSetPModeID,
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              sSetPartyID,
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              sSetPartyID);
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
