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
package com.helger.phase4.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.phase4.model.soapfault.AS4SoapFault;

/**
 * Test class for class {@link AS4BidirectionalClientHelper}.
 */
public final class AS4BidirectionalClientHelperTest
{
  private static final String SOAP12_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                             "<S12:Envelope xmlns:S12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                                             "<S12:Body><S12:Fault>" +
                                             "<S12:Code><S12:Value>S12:Receiver</S12:Value></S12:Code>" +
                                             "<S12:Reason><S12:Text xml:lang=\"en\">Something failed</S12:Text></S12:Reason>" +
                                             "</S12:Fault></S12:Body></S12:Envelope>";

  private static byte [] _bytes (final String s)
  {
    return s.getBytes (StandardCharsets.UTF_8);
  }

  @Test
  public void testFindSoapFaultPlain ()
  {
    final AS4SoapFault aFault = AS4BidirectionalClientHelper._findSoapFault (_bytes (SOAP12_FAULT),
                                                                             SOAP12_FAULT,
                                                                             "application/soap+xml;charset=UTF-8");
    assertNotNull (aFault);
    assertEquals ("Something failed", aFault.getFaultReason ());
    assertEquals (SOAP12_FAULT, aFault.getRawXML ());

    // No Content-Type
    assertNotNull (AS4BidirectionalClientHelper._findSoapFault (_bytes (SOAP12_FAULT), SOAP12_FAULT, null));
  }

  @Test
  public void testFindSoapFaultMultipart ()
  {
    final String sMultipart = "\r\n------=_Part_0_123.456\r\n" +
                              "Content-Type: application/soap+xml;charset=UTF-8\r\n" +
                              "Content-Transfer-Encoding: binary\r\n" +
                              "\r\n" +
                              SOAP12_FAULT +
                              "\r\n------=_Part_0_123.456\r\n" +
                              "Content-Type: application/octet-stream\r\n" +
                              "Content-ID: <attachment@phase4>\r\n" +
                              "\r\n" +
                              "not XML\r\n" +
                              "------=_Part_0_123.456--\r\n";
    final AS4SoapFault aFault = AS4BidirectionalClientHelper._findSoapFault (_bytes (sMultipart),
                                                                             sMultipart,
                                                                             "multipart/related; boundary=\"----=_Part_0_123.456\"; type=\"application/soap+xml\"");
    assertNotNull (aFault);
    assertEquals ("Something failed", aFault.getFaultReason ());
    // The raw XML must not contain the MIME structure
    assertNotNull (aFault.getRawXML ());
    assertEquals (-1, aFault.getRawXML ().indexOf ("_Part_0_"));
  }

  @Test
  public void testFindSoapFaultMultipartWithPreamble ()
  {
    final String sMultipart = "This is a preamble\r\n" +
                              "--MIMEBoundary_abc\r\n" +
                              "Content-Type: application/soap+xml;charset=UTF-8\r\n" +
                              "\r\n" +
                              SOAP12_FAULT +
                              "\r\n--MIMEBoundary_abc--\r\n";
    final AS4SoapFault aFault = AS4BidirectionalClientHelper._findSoapFault (_bytes (sMultipart),
                                                                             sMultipart,
                                                                             "multipart/related; boundary=\"MIMEBoundary_abc\"; type=\"application/soap+xml\"");
    assertNotNull (aFault);
    assertEquals ("Something failed", aFault.getFaultReason ());
    assertEquals (-1, aFault.getRawXML ().indexOf ("MIMEBoundary_abc"));
  }

  @Test
  public void testFindSoapFaultNoFault ()
  {
    assertNull (AS4BidirectionalClientHelper._findSoapFault (null, null, null));
    assertNull (AS4BidirectionalClientHelper._findSoapFault (new byte [0], null, null));
    assertNull (AS4BidirectionalClientHelper._findSoapFault (_bytes ("not XML"), null, null));

    final String sCT = "multipart/related; boundary=b";
    // Broken MIME message
    assertNull (AS4BidirectionalClientHelper._findSoapFault (_bytes ("--b\r\nbroken"), null, sCT));
    // Missing boundary
    assertNull (AS4BidirectionalClientHelper._findSoapFault (_bytes (SOAP12_FAULT), null, "multipart/related"));

    // Multipart without a SOAP Fault in the first part
    final String sMultipart = "--b\r\nContent-Type: application/soap+xml\r\n\r\n" +
                              "<S12:Envelope xmlns:S12=\"http://www.w3.org/2003/05/soap-envelope\"><S12:Body/></S12:Envelope>" +
                              "\r\n--b--\r\n";
    assertNull (AS4BidirectionalClientHelper._findSoapFault (_bytes (sMultipart), sMultipart, sCT));
  }
}
