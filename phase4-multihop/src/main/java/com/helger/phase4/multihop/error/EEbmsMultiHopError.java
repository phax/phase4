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
package com.helger.phase4.multihop.error;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.lang.EnumHelper;
import com.helger.diagnostics.error.IError;
import com.helger.text.display.IHasDisplayText;
import com.helger.phase4.model.error.EEbmsErrorCategory;
import com.helger.phase4.model.error.EEbmsErrorSeverity;
import com.helger.phase4.model.error.IEbmsError;

/**
 * The additional ebMS error types introduced by ebMS3 Part 2 for multi-hop - see section 2.5.6 and
 * appendix H (R12).<br>
 * These are errors an <b>intermediary</b> raises. An endpoint only has to recognise them and pass
 * them on to the application; it never generates them itself.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public enum EEbmsMultiHopError implements IEbmsError
{
  /**
   * EBMS:0020 - the intermediary could not route the message, e.g. because the destination is
   * unknown.
   */
  EBMS_ROUTING_FAILURE ("EBMS:0020",
                        EEbmsErrorSeverity.FAILURE,
                        "RoutingFailure",
                        aLocale -> "The intermediary was unable to route the message.",
                        EEbmsErrorCategory.COMMUNICATION),

  /**
   * EBMS:0021 - the message could not be stored, because the capacity of the message partition
   * channel is exhausted.
   */
  EBMS_MPC_CAPACITY_EXCEEDED ("EBMS:0021",
                              EEbmsErrorSeverity.FAILURE,
                              "MPCCapacityExceeded",
                              aLocale -> "The capacity of the message partition channel was exceeded.",
                              EEbmsErrorCategory.COMMUNICATION),

  /**
   * EBMS:0022 - the message was stored for longer than the intermediary is willing to keep it.
   */
  EBMS_MESSAGE_PERSISTENCE_TIMEOUT ("EBMS:0022",
                                    EEbmsErrorSeverity.FAILURE,
                                    "MessagePersistenceTimeout",
                                    aLocale -> "The message was persisted for longer than the allowed time.",
                                    EEbmsErrorCategory.COMMUNICATION),

  /**
   * EBMS:0023 - the message expired before it could be delivered. This is a warning, not a
   * failure.
   */
  EBMS_MESSAGE_EXPIRED ("EBMS:0023",
                        EEbmsErrorSeverity.WARNING,
                        "MessageExpired",
                        aLocale -> "The message expired before it could be delivered.",
                        EEbmsErrorCategory.COMMUNICATION);

  private final String m_sErrorCode;
  private final EEbmsErrorSeverity m_eSeverity;
  private final String m_sShortDescription;
  private final IHasDisplayText m_aDescription;
  private final EEbmsErrorCategory m_eCategory;

  EEbmsMultiHopError (@NonNull @Nonempty final String sErrorCode,
                      @NonNull final EEbmsErrorSeverity eSeverity,
                      @NonNull final String sShortDescription,
                      @NonNull final IHasDisplayText aDescription,
                      @NonNull final EEbmsErrorCategory eCategory)
  {
    m_sErrorCode = sErrorCode;
    m_eSeverity = eSeverity;
    m_sShortDescription = sShortDescription;
    m_aDescription = aDescription;
    m_eCategory = eCategory;
  }

  @NonNull
  @Nonempty
  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  @NonNull
  public EEbmsErrorSeverity getSeverity ()
  {
    return m_eSeverity;
  }

  @NonNull
  public String getShortDescription ()
  {
    return m_sShortDescription;
  }

  @NonNull
  public IHasDisplayText getDescription ()
  {
    return m_aDescription;
  }

  @NonNull
  public EEbmsErrorCategory getCategory ()
  {
    return m_eCategory;
  }

  /**
   * @param sErrorCode
   *        The error code to search. May be <code>null</code>.
   * @return <code>null</code> if no multi-hop error with that code exists.
   */
  @Nullable
  public static EEbmsMultiHopError getFromErrorCodeOrNull (@Nullable final String sErrorCode)
  {
    if (sErrorCode == null)
      return null;
    return EnumHelper.findFirst (EEbmsMultiHopError.class, x -> x.getErrorCode ().equals (sErrorCode));
  }

  /**
   * @param aError
   *        The error to check. May be <code>null</code>.
   * @return <code>null</code> if the provided error is not one of the multi-hop errors.
   */
  @Nullable
  public static EEbmsMultiHopError getFromIErrorOrNull (@Nullable final IError aError)
  {
    return aError == null ? null : getFromErrorCodeOrNull (aError.getErrorID ());
  }
}
