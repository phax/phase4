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

import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * XML converter for {@link PModeLeg}.
 * 
 * @author Philip Helger
 */
public class PModeLegMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLeg>
{
  private static final String ELEMENT_PROTOCOL = "Protocol";
  private static final String ELEMENT_BUSINESS_INFORMATION = "BusinessInfo";
  private static final String ELEMENT_ERROR_HANDLING = "ErrorHandling";
  private static final String ELEMENT_RELIABILITY = "Reliability";
  private static final String ELEMENT_SECURITY = "Security";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeLeg aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getProtocol (), sNamespaceURI, ELEMENT_PROTOCOL));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getBusinessInfo (),
                                                               sNamespaceURI,
                                                               ELEMENT_BUSINESS_INFORMATION));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getErrorHandling (),
                                                               sNamespaceURI,
                                                               ELEMENT_ERROR_HANDLING));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReliability (),
                                                               sNamespaceURI,
                                                               ELEMENT_RELIABILITY));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getSecurity (), sNamespaceURI, ELEMENT_SECURITY));
    return ret;
  }

  @Nonnull
  public PModeLeg convertToNative (@Nonnull final IMicroElement aElement)
  {
    final PModeLegProtocol aProtocol = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PROTOCOL),
                                                                           PModeLegProtocol.class);
    final PModeLegBusinessInformation aBusinessInformation = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_BUSINESS_INFORMATION),
                                                                                                 PModeLegBusinessInformation.class);
    final PModeLegErrorHandling aErrorHandling = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_ERROR_HANDLING),
                                                                                     PModeLegErrorHandling.class);
    final PModeLegReliability aReliability = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RELIABILITY),
                                                                                 PModeLegReliability.class);
    final PModeLegSecurity aSecurity = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_SECURITY),
                                                                           PModeLegSecurity.class);
    return new PModeLeg (aProtocol, aBusinessInformation, aErrorHandling, aReliability, aSecurity);
  }
}
