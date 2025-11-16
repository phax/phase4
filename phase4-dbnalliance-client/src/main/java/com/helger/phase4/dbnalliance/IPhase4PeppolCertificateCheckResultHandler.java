/*
 * Copyright (C) 2024-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.security.certificate.ECertificateCheckResult;

/**
 * Interface for handling certification validations results
 *
 * @author Philip Helger
 * @since 3.1.0
 */
public interface IPhase4PeppolCertificateCheckResultHandler
{
  /**
   * Invoked after certificate check.
   *
   * @param aAPCertificate
   *        The AP certificate that was checked. May be <code>null</code>.
   * @param aCheckDT
   *        The date and time that was used to check the certificate. Never <code>null</code>.
   * @param eCertCheckResult
   *        The result of the certificate check. Never <code>null</code>.
   * @throws Phase4DBNAllianceException
   *         Implementation dependent
   */
  void onCertificateCheckResult (@Nullable X509Certificate aAPCertificate,
                                 @NonNull OffsetDateTime aCheckDT,
                                 @NonNull ECertificateCheckResult eCertCheckResult) throws Phase4DBNAllianceException;
}
