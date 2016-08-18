package com.helger.as4server.message;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.marshaller.Ebms3WriterBuilder;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.soap11.Soap11Body;
import com.helger.as4lib.soap11.Soap11Envelope;
import com.helger.as4lib.soap11.Soap11Header;
import com.helger.as4lib.soap12.Soap12Body;
import com.helger.as4lib.soap12.Soap12Envelope;
import com.helger.as4lib.soap12.Soap12Header;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.datetime.util.PDTXMLConverter;

/**
 * This class contains every method, static variables which are used by more
 * than one message creating classes in the package
 * com.helger.as4server.message.
 *
 * @author bayerlma
 */
public class MessageHelperMethods
{
  public static final String EBMS_NS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
  public static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  public static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

  public static Document getSoapEnvelope11ForTest (@Nonnull final String sPath) throws SAXException,
                                                                                IOException,
                                                                                ParserConfigurationException
  {
    final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true); // never forget this!
    final DocumentBuilder builder = domFactory.newDocumentBuilder ();
    return builder.parse (new ClassPathResource (sPath).getInputStream ());
  }

  // TODO Change Timestamp or do we only want the present date when the message
  // gets sent/replied
  public static Ebms3MessageInfo createEbms3MessageInfo (@Nonnull final String sMessageId,
                                                         @Nullable final String sRefToMessageID)
  {
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aMessageInfo.setMessageId (sMessageId);
    aMessageInfo.setTimestamp (PDTXMLConverter.getXMLCalendarNow ());
    aMessageInfo.setRefToMessageId (sRefToMessageID);
    return aMessageInfo;
  }

  // TODO if Payload from UserMessage fixed merge with usermessage method
  public static Document createSOAPEnvelopeAsDocument (@Nonnull final ESOAPVersion eSOAPVersion,
                                                       final Document aEbms3Message)
  {

    if (eSOAPVersion.equals (ESOAPVersion.SOAP_11))
    {
      // Creating SOAP 11 Envelope
      final Soap11Envelope aSoapEnv = new Soap11Envelope ();
      aSoapEnv.setHeader (new Soap11Header ());
      aSoapEnv.setBody (new Soap11Body ());
      aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());
      return Ebms3WriterBuilder.soap11 ().getAsDocument (aSoapEnv);
    }
    // Creating SOAP 12 Envelope
    final Soap12Envelope aSoapEnv = new Soap12Envelope ();
    aSoapEnv.setHeader (new Soap12Header ());
    aSoapEnv.setBody (new Soap12Body ());
    aSoapEnv.getHeader ().addAny (aEbms3Message.getDocumentElement ());
    return Ebms3WriterBuilder.soap12 ().getAsDocument (aSoapEnv);

  }
}
