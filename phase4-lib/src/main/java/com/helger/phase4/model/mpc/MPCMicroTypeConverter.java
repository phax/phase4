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
package com.helger.phase4.model.mpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.photon.security.object.AbstractBusinessObjectMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * XML converter for {@link MPC} objects.
 * 
 * @author Philip Helger
 */
public final class MPCMicroTypeConverter extends AbstractBusinessObjectMicroTypeConverter <MPC>
{
  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final MPC aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    setObjectFields (aValue, ret);
    return ret;
  }

  @Nonnull
  public MPC convertToNative (@Nonnull final IMicroElement aElement)
  {
    return new MPC (getStubObject (aElement));
  }
}
