/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming.soap;

import com.helger.annotation.concurrent.Immutable;
import com.helger.http.CHttp;

/**
 * SOAP constants for the usage in AS4
 *
 * @author Philip Helger
 * @since 4.1.1
 */
@Immutable
public final class CAS4Soap
{
  /**
   * Source: https://www.w3.org/TR/soap12-part2/#soapinhttp chapter 7.5.2.2, "Table 20: SOAP Fault
   * to HTTP Status Mapping"
   */
  public static final int HTTP_STATUS_CODE_VERSION_MISMATCH = CHttp.HTTP_INTERNAL_SERVER_ERROR;
  public static final int HTTP_STATUS_CODE_MUST_UNDERSTAND = CHttp.HTTP_INTERNAL_SERVER_ERROR;
  public static final int HTTP_STATUS_CODE_SENDER = CHttp.HTTP_BAD_REQUEST;
  public static final int HTTP_STATUS_CODE_RECEIVER = CHttp.HTTP_INTERNAL_SERVER_ERROR;
  public static final int HTTP_STATUS_CODE_DATA_ENCODING_UNKNOWN = CHttp.HTTP_INTERNAL_SERVER_ERROR;

  private CAS4Soap ()
  {}
}
