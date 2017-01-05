/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
