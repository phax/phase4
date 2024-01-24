/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.util;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.phase4.marshaller.Ebms3NamespaceHandler;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.EXMLSerializeXMLDeclaration;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * AS4 XML helper methods.
 *
 * @author Philip Helger
 */
public final class AS4XMLHelper
{
  public static final XMLWriterSettings XWS = XMLWriterSettings.createForCanonicalization ();
  static
  {
    XWS.setNamespaceContext (Ebms3NamespaceHandler.getInstance ());
    XWS.setIndent (EXMLSerializeIndent.NONE);
    XWS.setSerializeXMLDeclaration (EXMLSerializeXMLDeclaration.EMIT_NO_STANDALONE);
  }

  private AS4XMLHelper ()
  {}

  @Nonnull
  private static String _serializePh (@Nonnull final Node aNode)
  {
    return XMLWriter.getNodeAsString (aNode, XWS);
  }

  @Nonnull
  private static String _serializeRT (@Nonnull final Node aNode)
  {
    try
    {
      final TransformerFactory tf = TransformerFactory.newInstance ();
      tf.setAttribute (XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute (XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      final Transformer aTransformer = tf.newTransformer ();

      try (final NonBlockingStringWriter aSW = new NonBlockingStringWriter ())
      {
        aTransformer.transform (new DOMSource (aNode), new StreamResult (aSW));
        return aSW.getAsString ();
      }
    }
    catch (final TransformerException ex)
    {
      throw new IllegalStateException ("Failed to serialize XML", ex);
    }
  }

  @Nonnull
  public static String serializeXML (@Nonnull final Node aNode)
  {
    ValueEnforcer.notNull (aNode, "Node");
    // Use runtime serialization otherwise XMLDsig signature wont work
    if (true)
      return _serializeRT (aNode);
    return _serializePh (aNode);
  }
}
