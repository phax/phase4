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
package com.helger.phase4.model.pmode.leg;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * XML converter for {@link PModeAddressList} objects.
 *
 * @author Philip Helger
 */
public class PModeAddressListMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeAddressList>
{
  private static final String ELEMENT_ADDRESSES = "Addresses";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final PModeAddressList aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sEncrypt : aValue.getAllAddresses ())
    {
      ret.addElementNS (sNamespaceURI, ELEMENT_ADDRESSES).addText (sEncrypt);
    }
    return ret;
  }

  @NonNull
  public PModeAddressList convertToNative (@NonNull final IMicroElement aElement)
  {
    final ICommonsList <String> aAddresses = new CommonsArrayList <> ();
    for (final IMicroElement eItem : aElement.getAllChildElements (ELEMENT_ADDRESSES))
    {
      aAddresses.add (eItem.getTextContentTrimmed ());
    }

    return new PModeAddressList (aAddresses);
  }
}
