/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.model.soapfault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.json.IJsonObject;
import com.helger.phase4.model.ESoapVersion;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link AS4SoapFault}.
 *
 * @author Philip Helger
 */
public final class AS4SoapFaultTest
{
  private static final String SOAP11_FAULT = "<?xml version='1.0' encoding='UTF-8'?>" +
                                             "<S11:Envelope xmlns:S11='http://schemas.xmlsoap.org/soap/envelope/'>" +
                                             "<S11:Body>" +
                                             "<S11:Fault>" +
                                             "<faultcode>S11:Client</faultcode>" +
                                             "<faultstring>Message does not conform</faultstring>" +
                                             "<faultactor>urn:example:ap</faultactor>" +
                                             "<detail><myns:reason xmlns:myns='urn:example'>bad payload</myns:reason></detail>" +
                                             "</S11:Fault>" +
                                             "</S11:Body>" +
                                             "</S11:Envelope>";

  private static final String SOAP12_FAULT = "<?xml version='1.0' encoding='UTF-8'?>" +
                                             "<env:Envelope xmlns:env='http://www.w3.org/2003/05/soap-envelope'>" +
                                             "<env:Body>" +
                                             "<env:Fault>" +
                                             "<env:Code>" +
                                             "<env:Value>env:Sender</env:Value>" +
                                             "<env:Subcode><env:Value xmlns:m='urn:example:subcodes'>m:MessageTimeout</env:Value></env:Subcode>" +
                                             "</env:Code>" +
                                             "<env:Reason><env:Text xml:lang='en'>Sender timeout</env:Text></env:Reason>" +
                                             "<env:Role>urn:example:role</env:Role>" +
                                             "<env:Detail><e:cause xmlns:e='urn:example'>expired</e:cause></env:Detail>" +
                                             "</env:Fault>" +
                                             "</env:Body>" +
                                             "</env:Envelope>";

  private static final String SOAP12_RECEIPT_LIKE = "<?xml version='1.0' encoding='UTF-8'?>" +
                                                    "<env:Envelope xmlns:env='http://www.w3.org/2003/05/soap-envelope'>" +
                                                    "<env:Header/>" +
                                                    "<env:Body/>" +
                                                    "</env:Envelope>";

  private static final String HTML_ERROR_PAGE = "<html><head><title>502 Bad Gateway</title></head>" +
                                                "<body><center><h1>502 Bad Gateway</h1></center></body></html>";

  @Test
  public void testDetectSoap11Fault ()
  {
    final Document aDoc = DOMReader.readXMLDOM (SOAP11_FAULT);
    assertNotNull (aDoc);
    assertNotNull (AS4SoapFault.getSoapFaultElementOrNull (aDoc));

    final AS4SoapFault aFault = AS4SoapFault.createOrNull (aDoc);
    assertNotNull (aFault);
    assertSame (ESoapVersion.SOAP_11, aFault.getSoapVersion ());
    assertTrue (aFault.hasFaultCode ());
    assertEquals (ESoapVersion.SOAP_11.getNamespaceURI (), aFault.getFaultCode ().getNamespaceURI ());
    assertEquals ("Client", aFault.getFaultCode ().getLocalPart ());
    assertNull (aFault.getFaultSubcode ());
    assertEquals ("Message does not conform", aFault.getFaultReason ());
    assertEquals ("urn:example:ap", aFault.getFaultActorRole ());
    assertNotNull (aFault.getDetailElement ());
    assertEquals ("detail", aFault.getDetailElement ().getLocalName ());
    assertTrue (aFault.getRawXML ().contains ("faultcode"));
    assertSame (EAS4FaultDisposition.PERMANENT, aFault.getDisposition ());
  }

  @Test
  public void testDetectSoap12FaultWithSubcode ()
  {
    final Document aDoc = DOMReader.readXMLDOM (SOAP12_FAULT);
    assertNotNull (aDoc);
    assertNotNull (AS4SoapFault.getSoapFaultElementOrNull (aDoc));

    final AS4SoapFault aFault = AS4SoapFault.createOrNull (aDoc);
    assertNotNull (aFault);
    assertSame (ESoapVersion.SOAP_12, aFault.getSoapVersion ());
    assertTrue (aFault.hasFaultCode ());
    assertEquals (ESoapVersion.SOAP_12.getNamespaceURI (), aFault.getFaultCode ().getNamespaceURI ());
    assertEquals ("Sender", aFault.getFaultCode ().getLocalPart ());
    assertNotNull (aFault.getFaultSubcode ());
    assertEquals ("urn:example:subcodes", aFault.getFaultSubcode ().getNamespaceURI ());
    assertEquals ("MessageTimeout", aFault.getFaultSubcode ().getLocalPart ());
    assertEquals ("Sender timeout", aFault.getFaultReason ());
    assertEquals ("urn:example:role", aFault.getFaultActorRole ());
    assertNotNull (aFault.getDetailElement ());
    assertEquals ("Detail", aFault.getDetailElement ().getLocalName ());
    assertSame (EAS4FaultDisposition.PERMANENT, aFault.getDisposition ());
  }

  @Test
  public void testDetectSoap11FaultInSoap12Context ()
  {
    // A SOAP 1.1 fault must be detected even if the conversation uses SOAP 1.2 - the detection is
    // solely based on the document content
    final Document aDoc = DOMReader.readXMLDOM (SOAP11_FAULT);
    assertNotNull (aDoc);

    final AS4SoapFault aFault = AS4SoapFault.createOrNull (aDoc);
    assertNotNull (aFault);
    // The fault carries its own version, not the conversation one
    assertSame (ESoapVersion.SOAP_11, aFault.getSoapVersion ());
  }

  @Test
  public void testNonFaultSoapDocument ()
  {
    final Document aDoc = DOMReader.readXMLDOM (SOAP12_RECEIPT_LIKE);
    assertNotNull (aDoc);
    assertNull (AS4SoapFault.getSoapFaultElementOrNull (aDoc));
    assertNull (AS4SoapFault.createOrNull (aDoc));
  }

  @Test
  public void testHtmlErrorPage ()
  {
    // Even if the HTML can be parsed as XML, it's not in a SOAP namespace
    final Document aDoc = DOMReader.readXMLDOM (HTML_ERROR_PAGE);
    assertNull (AS4SoapFault.createOrNull (aDoc));
    // And a null document must be handled gracefully as well
    assertNull (AS4SoapFault.getSoapFaultElementOrNull (null));
    assertNull (AS4SoapFault.createOrNull (null));
  }

  @Test
  public void testExplicitRawXML ()
  {
    final Document aDoc = DOMReader.readXMLDOM (SOAP11_FAULT);
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (aDoc, SOAP11_FAULT);
    assertNotNull (aFault);
    assertEquals (SOAP11_FAULT, aFault.getRawXML ());
  }

  @Test
  public void testDispositionMapping ()
  {
    // SOAP 1.1 fault codes
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap11Fault ("S11:Client").getDisposition ());
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap11Fault ("S11:Client.Authentication").getDisposition ());
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap11Fault ("S11:VersionMismatch").getDisposition ());
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap11Fault ("S11:MustUnderstand").getDisposition ());
    assertSame (EAS4FaultDisposition.TRANSIENT, _createSoap11Fault ("S11:Server").getDisposition ());

    // SOAP 1.2 fault codes
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap12Fault ("env:Sender").getDisposition ());
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap12Fault ("env:VersionMismatch").getDisposition ());
    assertSame (EAS4FaultDisposition.PERMANENT, _createSoap12Fault ("env:MustUnderstand").getDisposition ());
    assertSame (EAS4FaultDisposition.TRANSIENT, _createSoap12Fault ("env:Receiver").getDisposition ());

    // Unknown fault codes are transient
    assertSame (EAS4FaultDisposition.TRANSIENT, _createSoap12Fault ("env:DataEncodingUnknown").getDisposition ());
    assertSame (EAS4FaultDisposition.TRANSIENT, _createSoap11Fault ("S11:Whatever").getDisposition ());

    // Absent fault code is transient
    final Document aDoc = DOMReader.readXMLDOM ("<S11:Envelope xmlns:S11='http://schemas.xmlsoap.org/soap/envelope/'>" +
                                                "<S11:Body><S11:Fault><faultstring>x</faultstring></S11:Fault></S11:Body>" +
                                                "</S11:Envelope>");
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (aDoc);
    assertNotNull (aFault);
    assertNull (aFault.getFaultCode ());
    assertSame (EAS4FaultDisposition.TRANSIENT, aFault.getDisposition ());
  }

  @Test
  public void testGetAsJsonObject ()
  {
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (DOMReader.readXMLDOM (SOAP12_FAULT));
    assertNotNull (aFault);

    final IJsonObject aJson = aFault.getAsJsonObject ();
    assertEquals ("1.2", aJson.getAsString ("soapVersion"));
    assertEquals ("{" + ESoapVersion.SOAP_12.getNamespaceURI () + "}Sender", aJson.getAsString ("faultCode"));
    assertEquals ("{urn:example:subcodes}MessageTimeout", aJson.getAsString ("faultSubcode"));
    assertEquals ("Sender timeout", aJson.getAsString ("faultReason"));
    assertEquals ("urn:example:role", aJson.getAsString ("faultActorRole"));
    assertTrue (aJson.getAsString ("faultDetail").contains ("expired"));
    assertEquals (EAS4FaultDisposition.PERMANENT.getID (), aJson.getAsString ("disposition"));

    // A minimal fault contains only the mandatory elements
    final IJsonObject aMinimalJson = _createSoap11Fault ("S11:Server").getAsJsonObject ();
    assertEquals ("1.1", aMinimalJson.getAsString ("soapVersion"));
    assertNull (aMinimalJson.getAsString ("faultSubcode"));
    assertNull (aMinimalJson.getAsString ("faultActorRole"));
    assertNull (aMinimalJson.getAsString ("faultDetail"));
    assertEquals (EAS4FaultDisposition.TRANSIENT.getID (), aMinimalJson.getAsString ("disposition"));
  }

  @Test
  public void testGetAsMicroElement ()
  {
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (DOMReader.readXMLDOM (SOAP12_FAULT));
    assertNotNull (aFault);

    final IMicroElement aElement = aFault.getAsMicroElement (null, "SoapFault");
    assertEquals ("SoapFault", aElement.getTagName ());
    assertEquals ("1.2", aElement.getFirstChildElement ("SoapVersion").getTextContentTrimmed ());
    assertEquals ("{" + ESoapVersion.SOAP_12.getNamespaceURI () + "}Sender",
                  aElement.getFirstChildElement ("FaultCode").getTextContentTrimmed ());
    assertEquals ("{urn:example:subcodes}MessageTimeout",
                  aElement.getFirstChildElement ("FaultSubcode").getTextContentTrimmed ());
    assertEquals ("Sender timeout", aElement.getFirstChildElement ("FaultReason").getTextContentTrimmed ());
    assertEquals ("urn:example:role", aElement.getFirstChildElement ("FaultActorRole").getTextContentTrimmed ());
    assertTrue (aElement.getFirstChildElement ("FaultDetail").getTextContentTrimmed ().contains ("expired"));
    assertEquals (EAS4FaultDisposition.PERMANENT.getID (),
                  aElement.getFirstChildElement ("Disposition").getTextContentTrimmed ());

    // A minimal fault contains only the mandatory elements
    final IMicroElement aMinimalElement = _createSoap11Fault ("S11:Server").getAsMicroElement (null, "SoapFault");
    assertEquals ("1.1", aMinimalElement.getFirstChildElement ("SoapVersion").getTextContentTrimmed ());
    assertNull (aMinimalElement.getFirstChildElement ("FaultSubcode"));
    assertNull (aMinimalElement.getFirstChildElement ("FaultActorRole"));
    assertNull (aMinimalElement.getFirstChildElement ("FaultDetail"));
    assertEquals (EAS4FaultDisposition.TRANSIENT.getID (),
                  aMinimalElement.getFirstChildElement ("Disposition").getTextContentTrimmed ());
  }

  private static AS4SoapFault _createSoap11Fault (final String sFaultCode)
  {
    final String sXML = "<S11:Envelope xmlns:S11='http://schemas.xmlsoap.org/soap/envelope/'>" +
                        "<S11:Body>" +
                        "<S11:Fault>" +
                        "<faultcode>" +
                        sFaultCode +
                        "</faultcode>" +
                        "<faultstring>test</faultstring>" +
                        "</S11:Fault>" +
                        "</S11:Body>" +
                        "</S11:Envelope>";
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (DOMReader.readXMLDOM (sXML));
    assertNotNull (aFault);
    return aFault;
  }

  private static AS4SoapFault _createSoap12Fault (final String sFaultCodeValue)
  {
    final String sXML = "<env:Envelope xmlns:env='http://www.w3.org/2003/05/soap-envelope'>" +
                        "<env:Body>" +
                        "<env:Fault>" +
                        "<env:Code><env:Value>" +
                        sFaultCodeValue +
                        "</env:Value></env:Code>" +
                        "<env:Reason><env:Text xml:lang='en'>test</env:Text></env:Reason>" +
                        "</env:Fault>" +
                        "</env:Body>" +
                        "</env:Envelope>";
    final AS4SoapFault aFault = AS4SoapFault.createOrNull (DOMReader.readXMLDOM (sXML));
    assertNotNull (aFault);
    return aFault;
  }
}
