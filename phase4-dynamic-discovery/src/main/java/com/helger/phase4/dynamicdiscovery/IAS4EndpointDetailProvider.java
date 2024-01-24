/*
 * Copyright (C) 2020-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.dynamicdiscovery;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.util.Phase4Exception;

/**
 * An abstraction for receiving the AP certificate and the destination URL of
 * the receiver. The default case is to use an SMP lookup for this.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public interface IAS4EndpointDetailProvider
{
  /**
   * The initialization method is always called, before the details are queried.
   * This method may be called multiple times, so you may cache internally.
   *
   * @param aDocTypeID
   *        document type ID. May not be <code>null</code>.
   * @param aProcID
   *        Process ID. May not be <code>null</code>.
   * @param aReceiverID
   *        Participant ID of the receiver. May not be <code>null</code>.
   * @throws Phase4Exception
   *         in case of error
   */
  void init (@Nonnull IDocumentTypeIdentifier aDocTypeID,
             @Nonnull IProcessIdentifier aProcID,
             @Nonnull IParticipantIdentifier aReceiverID) throws Phase4Exception;

  /**
   * @return The X509 AP Certificate of the receiver. May be <code>null</code>
   *         if it could not be acquired.
   * @throws Phase4Exception
   *         In case of an error in determining the certificate.
   */
  @Nullable
  X509Certificate getReceiverAPCertificate () throws Phase4Exception;

  /**
   * @return The AS4 endpoint URL of the receiver. May neither be
   *         <code>null</code> nor empty.
   * @throws Phase4Exception
   *         In case of an error in determining the endpoint URL.
   */
  @Nonnull
  @Nonempty
  String getReceiverAPEndpointURL () throws Phase4Exception;
}
