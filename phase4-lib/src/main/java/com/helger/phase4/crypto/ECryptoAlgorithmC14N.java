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
package com.helger.phase4.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wss4j.common.WSS4JConstants;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;

/**
 * Enumeration with all message canonicalization algorithms supported when
 * signing.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public enum ECryptoAlgorithmC14N implements ICryptoAlgorithmC14N
{
  C14N_OMIT_COMMENTS ("c14n-incl", WSS4JConstants.C14N_OMIT_COMMENTS),
  C14N_WITH_COMMENTS ("c14n-incl-comments", WSS4JConstants.C14N_WITH_COMMENTS),
  C14N_EXCL_OMIT_COMMENTS ("c14n-excl", WSS4JConstants.C14N_EXCL_OMIT_COMMENTS),
  C14N_EXCL_WITH_COMMENTS ("c14n-excl-comments", WSS4JConstants.C14N_EXCL_WITH_COMMENTS);

  public static final ECryptoAlgorithmC14N C14N_ALGORITHM_DEFAULT = C14N_EXCL_OMIT_COMMENTS;

  private final String m_sID;
  private final String m_sAlgorithmURI;

  ECryptoAlgorithmC14N (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sAlgorithmURI)
  {
    m_sID = sID;
    m_sAlgorithmURI = sAlgorithmURI;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getAlgorithmURI ()
  {
    return m_sAlgorithmURI;
  }

  @Nullable
  public static ECryptoAlgorithmC14N getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithmC14N.class, sID);
  }

  @Nonnull
  public static ECryptoAlgorithmC14N getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (ECryptoAlgorithmC14N.class, sID);
  }

  @Nullable
  public static ECryptoAlgorithmC14N getFromIDOrDefault (@Nullable final String sID,
                                                         @Nullable final ECryptoAlgorithmC14N eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ECryptoAlgorithmC14N.class, sID, eDefault);
  }

  @Nullable
  public static ECryptoAlgorithmC14N getFromURIOrNull (@Nullable final String sURI)
  {
    if (StringHelper.hasNoText (sURI))
      return null;
    return EnumHelper.findFirst (ECryptoAlgorithmC14N.class, x -> x.getAlgorithmURI ().equals (sURI));
  }
}
