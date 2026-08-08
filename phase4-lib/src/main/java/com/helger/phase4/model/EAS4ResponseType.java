/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Classification of the synchronous response received for an outgoing AS4 message. The
 * classification is independent of the HTTP response status code, because non-conforming peers may
 * e.g. return SOAP Faults with HTTP 200.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public enum EAS4ResponseType implements IHasID <String>
{
  /**
   * An ebMS Signal Message containing a Receipt was received.
   */
  RECEIPT ("receipt"),
  /**
   * An ebMS Signal Message containing at least one Error was received.
   */
  EBMS_ERROR ("ebms-error"),
  /**
   * A plain SOAP Fault (SOAP 1.1 or 1.2) was received.
   */
  SOAP_FAULT ("soap-fault"),
  /**
   * The response body was empty.
   */
  EMPTY ("empty"),
  /**
   * The response body was not usable SOAP (e.g. an HTML proxy error page) or contained no usable
   * ebMS message.
   */
  UNPARSABLE ("unparsable");

  private final String m_sID;

  EAS4ResponseType (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static EAS4ResponseType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4ResponseType.class, sID);
  }
}
