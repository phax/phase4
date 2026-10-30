/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.crypto;

import java.util.List;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.crypto.IWSSecSignatureCustomizer;

/**
 * An {@link IWSSecSignatureCustomizer} that adds the WS-Addressing headers and the
 * <code>ebint:RoutingInput</code> of a multi-hop message to the list of signed parts - R9 / D2.
 * <br>
 * It wraps an optional delegate, so that an existing customizer is preserved.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public class MultiHopSignatureCustomizer implements IWSSecSignatureCustomizer
{
  /**
   * The WSS4J encoding mode used by phase4 for all signed parts. Must match the value used in
   * {@code AS4Signer}.
   */
  public static final String ENCRYPTION_MODE_CONTENT = "Content";

  private final IWSSecSignatureCustomizer m_aDelegate;
  private final ICommonsList <String> m_aIDsToSign;

  /**
   * Constructor.
   *
   * @param aDelegate
   *        An optional existing customizer that is invoked first. May be <code>null</code>.
   * @param aIDsToSign
   *        The <code>wsu:Id</code> values of the elements to additionally sign. May not be
   *        <code>null</code>.
   */
  public MultiHopSignatureCustomizer (@Nullable final IWSSecSignatureCustomizer aDelegate,
                                      @NonNull final List <String> aIDsToSign)
  {
    ValueEnforcer.notNullNoNullValue (aIDsToSign, "IDsToSign");
    m_aDelegate = aDelegate;
    m_aIDsToSign = new CommonsArrayList <> (aIDsToSign);
  }

  /**
   * @return The optional delegate customizer. May be <code>null</code>.
   */
  @Nullable
  public final IWSSecSignatureCustomizer getDelegate ()
  {
    return m_aDelegate;
  }

  /**
   * @return A copy of all IDs that are additionally signed. Never <code>null</code>.
   */
  @NonNull
  public final ICommonsList <String> getAllIDsToSign ()
  {
    return m_aIDsToSign.getClone ();
  }

  @NonNull
  public WSSecSignature createWSSecSignature (@NonNull final WSSecHeader aSecHeader)
  {
    if (m_aDelegate != null)
      return m_aDelegate.createWSSecSignature (aSecHeader);
    return new WSSecSignature (aSecHeader);
  }

  public void customize (@NonNull final WSSecSignature aWSSecSignature)
  {
    if (m_aDelegate != null)
      m_aDelegate.customize (aWSSecSignature);

    for (final String sID : m_aIDsToSign)
      aWSSecSignature.getParts ().add (new WSEncryptionPart (sID, ENCRYPTION_MODE_CONTENT));
  }
}
