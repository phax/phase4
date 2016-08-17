package com.helger.as4server.client;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class AttachmentTest
{
  // private final WSSecurityEngine secEngine = new WSSecurityEngine ();
  private Crypto crypto = null;

  @Test
  public void testXMLAttachmentCompleteSignature () throws Exception
  {
    WSSConfig.init ();
    crypto = CryptoFactory.getInstance ();

    final WSSecSignature builder = new WSSecSignature ();
    builder.setUserInfo (SOAPClientSAAJ.CF.getAsString ("key.alias"), SOAPClientSAAJ.CF.getAsString ("key.password"));

    builder.getParts ().add (new WSEncryptionPart ("Body", "http://schemas.xmlsoap.org/soap/envelope/", "Content"));
    builder.getParts ().add (new WSEncryptionPart ("cid:Attachments", "Content"));

    final String attachmentId = UUID.randomUUID ().toString ();
    final Attachment attachment = new Attachment ();
    attachment.setMimeType ("application/gzip");
    attachment.addHeaders (getHeaders (attachmentId));
    attachment.setId (attachmentId);
    attachment.setSourceStream (new FileInputStream ("data/test.xml.gz"));

    final AttachmentCallbackHandler attachmentCallbackHandler = new AttachmentCallbackHandler (Collections.singletonList (attachment));
    builder.setAttachmentCallbackHandler (attachmentCallbackHandler);

    // final Document doc = SOAPUtil.toSOAPPart (SOAPUtil.SAMPLE_SOAP_MSG);
    final Document doc = TestMessages.testUserMessageSoapNotSigned ();

    final WSSecHeader secHeader = new WSSecHeader (doc);
    secHeader.insertSecurityHeader ();
    final Document signedDoc = builder.build (doc, crypto, secHeader);

    final String outputString = XMLUtils.prettyDocumentToString (signedDoc);
    System.out.println (outputString);

    final NodeList sigReferences = signedDoc.getElementsByTagNameNS (WSS4JConstants.SIG_NS, "Reference");
    Assert.assertEquals (2, sigReferences.getLength ());
  }

  protected Map <String, String> getHeaders (final String attachmentId)
  {
    final Map <String, String> headers = new HashMap<> ();
    headers.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    headers.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    headers.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + attachmentId + ">");
    headers.put (AttachmentUtils.MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    headers.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, "application/gzip");
    headers.put ("TestHeader", "testHeaderValue");
    return headers;
  }
}
