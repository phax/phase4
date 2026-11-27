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
package com.helger.phase4.dbnalliance.servlet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.base.io.stream.HasInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.dbnalliance.xhe.DBNAlliancePayload;
import com.helger.dbnalliance.xhe.DBNAllianceXHEData;
import com.helger.peppolid.factory.DBNAllianceIdentifierFactory;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.xhe.v10.XHE10Marshaller;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link Phase4DBNAllianceIncomingXHE}.
 *
 * @author Philip Helger
 */
public final class Phase4DBNAllianceIncomingXHETest
{
  private static final String TEXT = "Hello <&> DBNAlliance\nsecond line";
  private static final byte [] BINARY = new byte [100_000];
  private static byte [] s_aXHEBytes;

  @BeforeClass
  public static void beforeClass ()
  {
    for (int i = 0; i < BINARY.length; ++i)
      BINARY[i] = (byte) i;

    final IIdentifierFactory aIF = DBNAllianceIdentifierFactory.INSTANCE;
    final Document aXML = DOMReader.readXMLDOM ("<Invoice xmlns='urn:test'><Line a='1'>x</Line></Invoice>");
    assertNotNull (aXML);
    final DBNAllianceXHEData aData = new DBNAllianceXHEData (aIF).setID ("id")
                                                                 .setCreationDateAndTimeNow ()
                                                                 .setFromParty ("fs", "fv")
                                                                 .setToParty ("ts", "tv")
                                                                 .addPayload (new DBNAlliancePayload (aIF).setContentTypeCodeXML ()
                                                                                                          .setPayloadContent (aXML.getDocumentElement ()))
                                                                 .addPayload (new DBNAlliancePayload (aIF).setContentTypeCode ("text/plain")
                                                                                                          .setPayloadContentText (TEXT))
                                                                 .addPayload (new DBNAlliancePayload (aIF).setContentTypeCode ("application/pdf")
                                                                                                          .setPayloadContentBinary (BINARY));
    s_aXHEBytes = new XHE10Marshaller ().getAsBytes (aData.getAsXHEDocument ());
    assertNotNull (s_aXHEBytes);
  }

  private static Phase4DBNAllianceIncomingXHE _create ()
  {
    return new Phase4DBNAllianceIncomingXHE (HasInputStream.create (s_aXHEBytes));
  }

  @Test
  public void testWholeDocument () throws IOException
  {
    final Phase4DBNAllianceIncomingXHE aXHE = _create ();
    assertArrayEquals (s_aXHEBytes, aXHE.getAllBytes ());
    // Can be read multiple times
    assertArrayEquals (s_aXHEBytes, aXHE.getAllBytes ());
    assertEquals ("id", aXHE.getXHE ().getHeader ().getIDValue ());
  }

  @Test
  public void testTextPayload () throws IOException
  {
    try (final Reader aReader = _create ().openPayloadTextReader (1))
    {
      assertEquals (TEXT, StreamHelper.getAllCharactersAsString (aReader));
    }
  }

  @Test
  public void testBinaryPayload () throws IOException
  {
    try (final InputStream aIS = _create ().openPayloadBinaryStream (2))
    {
      assertArrayEquals (BINARY, aIS.readAllBytes ());
    }
  }

  @Test
  public void testXMLPayload () throws Exception
  {
    final Phase4DBNAllianceIncomingXHE aXHE = _create ();
    final XMLStreamReader aReader = aXHE.openPayloadXMLStreamReader (0);
    try
    {
      assertEquals (XMLStreamConstants.START_ELEMENT, aReader.getEventType ());
      assertEquals ("urn:test", aReader.getNamespaceURI ());
      assertEquals ("Invoice", aReader.getLocalName ());
    }
    finally
    {
      aReader.close ();
    }

    final Element aElement = aXHE.getPayloadElement (0);
    assertEquals ("Invoice", aElement.getLocalName ());
    assertEquals ("urn:test", aElement.getNamespaceURI ());
    final Element aLine = (Element) aElement.getElementsByTagNameNS ("urn:test", "Line").item (0);
    assertNotNull (aLine);
    assertEquals ("1", aLine.getAttribute ("a"));
    assertEquals ("x", aLine.getTextContent ());
  }

  @Test
  public void testWrongAccess () throws Exception
  {
    final Phase4DBNAllianceIncomingXHE aXHE = _create ();
    // No such payload
    try
    {
      aXHE.openPayloadTextReader (3);
      fail ();
    }
    catch (final IOException ex)
    {
      // expected
    }
    // Text payload is not XML
    try
    {
      aXHE.openPayloadXMLStreamReader (1);
      fail ();
    }
    catch (final IOException ex)
    {
      // expected
    }
    // XML payload is not text - reported while reading
    try (final Reader aReader = aXHE.openPayloadTextReader (0))
    {
      while (aReader.read () >= 0)
      {
        // read
      }
      fail ();
    }
    catch (final IOException ex)
    {
      // expected
    }
  }
}
