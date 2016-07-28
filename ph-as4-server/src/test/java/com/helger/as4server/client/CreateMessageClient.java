package com.helger.as4server.client;

import java.io.IOException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.ebms3header.Ebms3Error;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.error.ErrorConverter;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public class CreateMessageClient
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("as4test.properties").build ();

  // TODO testMessage for developing delete if not needed anymore
  public static Document testUserMessage () throws WSSecurityException,
                                            IOException,
                                            DatatypeConfigurationException,
                                            SAXException,
                                            ParserConfigurationException
  {
    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final CreateMessageClient aClient = new CreateMessageClient ();
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
                                                                                             aUserMessage.createEbms3MessageProperties ()));
    return aSignedDoc;
  }

  public static Document testErrorMessage () throws WSSecurityException, DatatypeConfigurationException
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
                                               ParserConfigurationException,
                                               DatatypeConfigurationException
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

  public Document createSignedMessage (final Document aDocument) throws WSSecurityException
  {
    WSSConfig.init ();
    final Crypto aCrypto = CryptoFactory.getInstance ();
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (CF.getAsString ("key.alias"), CF.getAsString ("key.password"));
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo ("http://www.w3.org/2001/04/xmlenc#sha256");
    final Document aDoc = aDocument;
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    // TODO if you set attribute with NS it adds the same namespace again since
    // it does not take the one from envelope => Change NS between S11 and S12
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNode ("S11:mustUnderstand");
    // TODO Needs to be set to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    aMustUnderstand.setValue ("0");

    final Document aSignedDoc = aBuilder.build (aDoc, aCrypto, aSecHeader);
    return aSignedDoc;
  }

}
