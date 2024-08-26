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

import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;

/**
 * XML converter for objects of class {@link PModeLegProtocol}.
 *
 * @author Philip Helger
 */
public class PModeLegProtocolMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLegProtocol>
{
  private static final IMicroQName ATTR_ADDRESS = new MicroQName ("Address");
  // Legacy name
  private static final IMicroQName ATTR_SOAP_VERSION = new MicroQName ("SOAPVersion");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeLegProtocol aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.setAttribute (ATTR_ADDRESS, aValue.getAddress ());
    ret.setAttribute (ATTR_SOAP_VERSION, aValue.getSoapVersion ().getVersion ());
    return ret;
  }

  @Nonnull
  public PModeLegProtocol convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sAddress = aElement.getAttributeValue (ATTR_ADDRESS);

    final String sSoapVersion = aElement.getAttributeValue (ATTR_SOAP_VERSION);
    final ESoapVersion eSoapVersion = ESoapVersion.getFromVersionOrNull (sSoapVersion);
    if (eSoapVersion == null)
      throw new IllegalStateException ("Failed to resolve SOAP version '" + sSoapVersion + "'");

    return new PModeLegProtocol (sAddress, eSoapVersion);
  }
}
