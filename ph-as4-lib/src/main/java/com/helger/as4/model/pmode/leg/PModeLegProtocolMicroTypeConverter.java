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
package com.helger.as4.model.pmode.leg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeLegProtocolMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLegProtocol>
{
  private static final String ATTR_ADDRESS = "Address";
  private static final String ATTR_SOAP_VERSION = "SOAPVersion";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeLegProtocol aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ADDRESS, aValue.getAddress ());
    ret.setAttribute (ATTR_SOAP_VERSION, aValue.getSOAPVersion ().getVersion ());
    return ret;
  }

  @Nonnull
  public PModeLegProtocol convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sAddress = aElement.getAttributeValue (ATTR_ADDRESS);

    final String sSOAPVersion = aElement.getAttributeValue (ATTR_SOAP_VERSION);
    final ESOAPVersion eSOAPVersion = ESOAPVersion.getFromVersionOrDefault (sSOAPVersion, ESOAPVersion.AS4_DEFAULT);

    return new PModeLegProtocol (sAddress, eSOAPVersion);
  }
}
