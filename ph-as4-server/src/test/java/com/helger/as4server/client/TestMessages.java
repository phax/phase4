package com.helger.as4server.client;

import java.io.IOException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.error.ErrorConverter;
import com.helger.as4server.message.CreateErrorMessage;
import com.helger.as4server.message.CreateMessageClient;
import com.helger.as4server.message.CreateReceiptMessage;
import com.helger.as4server.message.CreateUserMessage;
import com.helger.as4server.message.MessageHelperMethods;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public class TestMessages
{
  // TODO testMessage for developing delete if not needed anymore
  public static Document testUserMessage () throws WSSecurityException,
                                            IOException,
                                            SAXException,
                                            ParserConfigurationException
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final CreateMessageClient aClient = new CreateMessageClient ();

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

    final Document aSignedDoc = aClient.createSignedMessage (aUserMessage.createUserMessage (aUserMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com"),
                                                                                             aUserMessage.createEbms3PayloadInfo (),
                                                                                             aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                                                        "MyServiceTypes",
                                                                                                                                        "QuoteToCollect",
                                                                                                                                        "4321",
                                                                                                                                        "pm-esens-generic-resp",
                                                                                                                                        "http://agreements.holodeckb2b.org/examples/agreement0"),
                                                                                             aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                                                                                "APP_1000000101",
                                                                                                                                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                                                                                "APP_1000000101"),
                                                                                             aUserMessage.createEbms3MessageProperties (aEbms3Properties),
                                                                                             "SOAPBodyPayload.xml"));
    return aSignedDoc;
  }

  public static Document testErrorMessage () throws WSSecurityException
  {
    final CreateErrorMessage aErrorMessage = new CreateErrorMessage ();
    final CreateMessageClient aClient = new CreateMessageClient ();
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList<> ();
    aEbms3ErrorList.add (ErrorConverter.convertEnumToEbms3Error (EEbmsError.EBMS_INVALID_HEADER));
    final Document aSignedDoc = aClient.createSignedMessage (aErrorMessage.createErrorMessage (aErrorMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com"),
                                                                                               aEbms3ErrorList));
    return aSignedDoc;
  }

  public static Document testReceiptMessage () throws WSSecurityException,
                                               DOMException,
                                               IOException,
                                               SAXException,
                                               ParserConfigurationException
  {
    final ICommonsList <Ebms3Error> aEbms3ErrorList = new CommonsArrayList<> ();
    aEbms3ErrorList.add (ErrorConverter.convertEnumToEbms3Error (EEbmsError.EBMS_INVALID_HEADER));

    final Document aUserMessage = testUserMessage ();

    final CreateReceiptMessage aReceiptMessage = new CreateReceiptMessage ();
    final CreateMessageClient aClient = new CreateMessageClient ();
    final Document aDoc = aReceiptMessage.createReceiptMessage (aReceiptMessage.createEbms3MessageInfo ("UUID-2@receiver.example.com",
                                                                                                        null),
                                                                aUserMessage);

    final Document aSignedDoc = aClient.createSignedMessage (aDoc);
    return aSignedDoc;
  }

  /**
   * Starting point for the SAAJ - SOAP Client Testing
   *
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   * @throws IOException
   * @throws WSSecurityException
   * @throws DatatypeConfigurationException
   */
  public static void main (final String args[]) throws WSSecurityException,
                                                IOException,
                                                TransformerException,
                                                SAXException,
                                                ParserConfigurationException,
                                                DatatypeConfigurationException
  {
    final XMLWriterSettings aXWS = new XMLWriterSettings ();
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("ds", MessageHelperMethods.DS_NS);
    aNSCtx.addMapping ("ebms", MessageHelperMethods.EBMS_NS);
    aNSCtx.addMapping ("wsse", MessageHelperMethods.WSSE_NS);
    aNSCtx.addMapping ("S11", "http://schemas.xmlsoap.org/soap/envelope/");
    aXWS.setNamespaceContext (aNSCtx);
    System.out.println (XMLWriter.getNodeAsString (testUserMessage (), aXWS));
    System.out.println ("##############################################");
    System.out.println (XMLWriter.getNodeAsString (testReceiptMessage (), aXWS));
  }
}
