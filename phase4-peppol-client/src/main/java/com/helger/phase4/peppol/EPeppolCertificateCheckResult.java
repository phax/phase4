/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.state.IValidityIndicator;

/**
 * Enumeration for all Peppol certificate checks
 *
 * @author Philip Helger
 * @since 0.9.4
 */
public enum EPeppolCertificateCheckResult implements IHasID <String>, IValidityIndicator
{
  VALID ("valid", "certificate is valid"),
  NO_CERTIFICATE_PROVIDED ("nocert", "no certificate provided"),
  NOT_YET_VALID ("notyetvalid", "certificate is not yet valid"),
  EXPIRED ("expired", "certificate is already expirted"),
  UNSUPPORTED_ISSUER ("unsupportedissuer", "unsupported certificate issuer"),
  REVOKED ("revoked", "certificate is revoked");

  private final String m_sID;
  private final String m_sReason;

  private EPeppolCertificateCheckResult (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sReason)
  {
    m_sID = sID;
    m_sReason = sReason;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getReason ()
  {
    return m_sReason;
  }

  public boolean isValid ()
  {
    return this == VALID;
  }
}
