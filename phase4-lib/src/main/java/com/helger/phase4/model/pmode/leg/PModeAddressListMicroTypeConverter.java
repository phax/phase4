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
package com.helger.phase4.model.pmode.leg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
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

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeAddressList aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sEncrypt : aValue.getAllAddresses ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_ADDRESSES).appendText (sEncrypt);
    }
    return ret;
  }

  @Nonnull
  public PModeAddressList convertToNative (@Nonnull final IMicroElement aElement)
  {
    final ICommonsList <String> aAddresses = new CommonsArrayList <> ();
    for (final IMicroElement eItem : aElement.getAllChildElements (ELEMENT_ADDRESSES))
    {
      aAddresses.add (eItem.getTextContentTrimmed ());
    }

    return new PModeAddressList (aAddresses);
  }
}
