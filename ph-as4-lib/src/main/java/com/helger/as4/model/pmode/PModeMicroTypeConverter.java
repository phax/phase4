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
package com.helger.as4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.config.IPModeConfig;
import com.helger.photon.security.object.AbstractObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public final class PModeMicroTypeConverter extends AbstractObjectMicroTypeConverter
{
  private static final String ELEMENT_INITIATOR = "Initiator";
  private static final String ELEMENT_RESPONDER = "Responder";
  private static final String ATTR_PMODE_CONFIG_ID = "ConfigID";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IPMode aValue = (IPMode) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getInitiator (),
                                                               sNamespaceURI,
                                                               ELEMENT_INITIATOR));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getResponder (),
                                                               sNamespaceURI,
                                                               ELEMENT_RESPONDER));
    ret.setAttribute (ATTR_PMODE_CONFIG_ID, aValue.getConfigID ());
    return ret;
  }

  @Nonnull
  public PMode convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sConfigID = aElement.getAttributeValue (ATTR_PMODE_CONFIG_ID);
    final IPModeConfig aConfig = MetaAS4Manager.getPModeConfigMgr ().getPModeConfigOfID (sConfigID);
    if (aConfig == null)
      throw new IllegalStateException ("Failed to resolve PModeConfig with ID '" + sConfigID + "'");

    final PModeParty aInitiator = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_INITIATOR),
                                                                      PModeParty.class);
    final PModeParty aResponder = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_RESPONDER),
                                                                      PModeParty.class);

    return new PMode (getStubObject (aElement), aInitiator, aResponder, aConfig);
  }
}
