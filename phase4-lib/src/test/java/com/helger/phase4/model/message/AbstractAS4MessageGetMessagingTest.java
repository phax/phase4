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
package com.helger.phase4.model.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.phase4.CAS4;
import com.helger.phase4.model.ESoapVersion;
import com.helger.xml.XMLHelper;

/**
 * Test class for {@link AbstractAS4Message#getMessaging()}.
 *
 * @author Philip Helger
 */
public final class AbstractAS4MessageGetMessagingTest
{
  private static final QName QNAME_TEST = new QName ("urn:phase4:test:mock", "role", "mock");

  private static Element _getMessagingElement (final Document aSoapDoc, final ESoapVersion eSoapVersion)
  {
    final Element aHeader = XMLHelper.getFirstChildElementOfName (aSoapDoc.getDocumentElement (),
                                                                  eSoapVersion.getNamespaceURI (),
                                                                  eSoapVersion.getHeaderElementName ());
    assertNotNull ("No SOAP Header found", aHeader);
    final Element aMessaging = XMLHelper.getFirstChildElementOfName (aHeader, CAS4.EBMS_NS, "Messaging");
    assertNotNull ("No eb:Messaging found", aMessaging);
    return aMessaging;
  }

  private static void _testAttributeIsMarshalled (final ESoapVersion eSoapVersion)
  {
    final AS4ReceiptMessage aMsg = AS4ReceiptMessage.create (eSoapVersion,
                                                             MessageHelperMethods.createRandomMessageID (),
                                                             null,
                                                             null,
                                                             false,
                                                             MessageHelperMethods.createRandomMessageID ());

    // The accessor must return the very same object every time
    assertSame (aMsg.getMessaging (), aMsg.getMessaging ());

    // Put a foreign namespace attribute - this is what an AS4 profile module needs
    aMsg.getMessaging ().getOtherAttributes ().put (QNAME_TEST, "someValue");

    final Document aSoapDoc = aMsg.getAsSoapDocument ();
    assertNotNull (aSoapDoc);

    final Element aMessaging = _getMessagingElement (aSoapDoc, eSoapVersion);
    assertEquals ("The attribute set via getMessaging() was not marshalled",
                  "someValue",
                  aMessaging.getAttributeNS (QNAME_TEST.getNamespaceURI (), QNAME_TEST.getLocalPart ()));

    // The wsu:Id set by the constructor must still be present
    assertEquals (aMsg.getMessagingID (), aMessaging.getAttributeNS (CAS4.WSU_NS, "Id"));
  }

  @Test
  public void testSoap12 ()
  {
    _testAttributeIsMarshalled (ESoapVersion.SOAP_12);
  }

  @Test
  public void testSoap11 ()
  {
    _testAttributeIsMarshalled (ESoapVersion.SOAP_11);
  }

  /**
   * Without touching the accessor, nothing may change - the no-op default of this core addition.
   */
  @Test
  public void testUnmodifiedHasNoExtraAttribute ()
  {
    final AS4ReceiptMessage aMsg = AS4ReceiptMessage.create (ESoapVersion.SOAP_12,
                                                             MessageHelperMethods.createRandomMessageID (),
                                                             null,
                                                             null,
                                                             false,
                                                             MessageHelperMethods.createRandomMessageID ());
    final Element aMessaging = _getMessagingElement (aMsg.getAsSoapDocument (), ESoapVersion.SOAP_12);
    assertEquals ("", aMessaging.getAttributeNS (QNAME_TEST.getNamespaceURI (), QNAME_TEST.getLocalPart ()));
  }
}
