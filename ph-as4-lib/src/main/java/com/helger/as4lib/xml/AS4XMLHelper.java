/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.xml;

import javax.annotation.Nonnull;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.helger.as4lib.marshaller.Ebms3NamespaceHandler;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public final class AS4XMLHelper
{
  public static final XMLWriterSettings XWS = new XMLWriterSettings ();
  static
  {
    XWS.setNamespaceContext (new Ebms3NamespaceHandler ());
    XWS.setPutNamespaceContextPrefixesInRoot (true);
    XWS.setIndent (EXMLSerializeIndent.NONE);
  }

  private AS4XMLHelper ()
  {}

  @Nonnull
  private static String _serializePh (@Nonnull final Node aNode)
  {
    return XMLWriter.getNodeAsString (aNode, XWS);
  }

  @Nonnull
  private static String _serializeRT (@Nonnull final Node aNode) throws TransformerFactoryConfigurationError,
                                                                 TransformerException
  {
    final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
    final NonBlockingStringWriter aSW = new NonBlockingStringWriter ();
    transformer.transform (new DOMSource (aNode), new StreamResult (aSW));
    return aSW.getAsString ();
  }

  @Nonnull
  public static String serializeXML (@Nonnull final Node aNode) throws TransformerFactoryConfigurationError,
                                                                TransformerException
  {
    // Use runtime serialization otherwise XMLDsig signature wont work
    return true ? _serializeRT (aNode) : _serializePh (aNode);
  }
}
