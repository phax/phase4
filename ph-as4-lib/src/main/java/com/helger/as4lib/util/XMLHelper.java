/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as4lib.util;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.xml.microdom.IMicroElement;

@Immutable
public final class XMLHelper
{
  private XMLHelper ()
  {}

  /**
   * Get all attributes of the passed element as a map with a lowercase
   * attribute name.
   *
   * @param aElement
   *        The source element to extract the attributes from. May not be
   *        <code>null</code>.
   * @return A new map and never <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static StringMap getAllAttrsWithLowercaseName (@Nonnull final IMicroElement aElement)
  {
    ValueEnforcer.notNull (aElement, "Element");

    final StringMap ret = new StringMap ();
    aElement.forAllAttributes ( (ns, name, value) -> ret.setAttribute (name.toLowerCase (Locale.US), value));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static StringMap getAllAttrsWithLowercaseNameWithRequired (@Nonnull final IMicroElement aElement,
                                                                    @Nonnull final String... aRequiredAttributes)
  {
    final StringMap aAttributes = getAllAttrsWithLowercaseName (aElement);
    for (final String sRequiredAttribute : aRequiredAttributes)
      if (!aAttributes.containsAttribute (sRequiredAttribute))
        throw new IllegalStateException (aElement.getTagName () +
                                         " is missing required attribute '" +
                                         sRequiredAttribute +
                                         "'");
    return aAttributes;
  }

  /**
   * @param aNode
   *        Start node. May not be <code>null</code>.
   * @param sNodeName
   *        The element name to be queried relative to the start node.
   * @param sNodeKeyName
   *        The attribute name of the key.
   * @param sNodeValueName
   *        The attribute name of the value.
   * @return The non-<code>null</code> {@link Map}. @ In case a node is missing
   *         a key or value attribute.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, String> mapAttributeNodes (@Nonnull final IMicroElement aNode,
                                                                       @Nonnull final String sNodeName,
                                                                       @Nonnull final String sNodeKeyName,
                                                                       @Nonnull final String sNodeValueName)
  {
    ValueEnforcer.notNull (aNode, "Node");
    ValueEnforcer.notNull (sNodeName, "NodeName");
    ValueEnforcer.notNull (sNodeKeyName, "NodeKeyName");
    ValueEnforcer.notNull (sNodeValueName, "NodeValueName");

    final ICommonsOrderedMap <String, String> ret = new CommonsLinkedHashMap<> ();
    int nIndex = 0;
    for (final IMicroElement eChild : aNode.getAllChildElements (sNodeName))
    {
      final String sName = eChild.getAttributeValue (sNodeKeyName);
      if (sName == null)
        throw new IllegalStateException (sNodeName +
                                         "[" +
                                         nIndex +
                                         "] does not have key attribute '" +
                                         sNodeKeyName +
                                         "'");

      final String sValue = eChild.getAttributeValue (sNodeValueName);
      if (sValue == null)
        throw new IllegalStateException (sNodeName +
                                         "[" +
                                         nIndex +
                                         "] does not have value attribute '" +
                                         sNodeValueName +
                                         "'");

      ret.put (sName, sValue);
      ++nIndex;
    }
    return ret;
  }
}
