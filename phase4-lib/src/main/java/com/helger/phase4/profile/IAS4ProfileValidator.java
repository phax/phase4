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
package com.helger.phase4.profile;

import java.security.cert.X509Certificate;
import java.util.EnumSet;

import com.helger.phase4.model.ESoapVersion;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;
import org.w3c.dom.Document;

/**
 * Generic AS4 profile validator
 *
 * @author bayerlma
 * @author Philip Helger
 */
public interface IAS4ProfileValidator
{
  enum EAS4ProfileValidationMode
  {
    USER_MESSAGE,
    SIGNAL_MESSAGE;
  }

  /**
   * The parts of an incoming AS4 message that may be required to be covered by the message
   * signature.
   *
   * @since 4.6.1
   */
  enum ESignedPart
  {
    /** The ebMS <code>Messaging</code> header element */
    EBMS_MESSAGING,
    /** The SOAP <code>Body</code> element */
    SOAP_BODY,
    /** All attachments of the message */
    ATTACHMENTS;
  }

  /**
   * Get the message parts that the generic AS4 profile requires to be covered by the signature of
   * an incoming message. Chapter 5.1.4 requires the ebMS <code>Messaging</code> header element and
   * the (possibly empty) SOAP <code>Body</code> element, whereas chapter 5.1.5 requires the ebMS
   * <code>Messaging</code> header element and all MIME body parts for "SOAP with Attachments"
   * messages.
   *
   * @param bMessageHasAttachments
   *        <code>true</code> if the incoming message contains at least one attachment.
   * @return A new mutable set with all required parts. Never <code>null</code> nor empty.
   * @since 4.6.1
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  static EnumSet <ESignedPart> getDefaultRequiredSignedParts (final boolean bMessageHasAttachments)
  {
    return bMessageHasAttachments ? EnumSet.of (ESignedPart.EBMS_MESSAGING, ESignedPart.ATTACHMENTS) : EnumSet.of (
                                                                                                                   ESignedPart.EBMS_MESSAGING,
                                                                                                                   ESignedPart.SOAP_BODY);
  }

  /**
   * Get the message parts that must be covered by the signature of an incoming message. Message
   * parts that are not part of the returned set are not checked at all. This is the profile
   * specific part of the protection against XML signature wrapping attacks.
   *
   * @param bMessageHasAttachments
   *        <code>true</code> if the incoming message contains at least one attachment.
   * @return A new mutable set with all required parts. May neither be <code>null</code> nor empty.
   *         The default implementation returns {@link #getDefaultRequiredSignedParts(boolean)}.
   * @since 4.6.1
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  default EnumSet <ESignedPart> getRequiredSignedParts (final boolean bMessageHasAttachments)
  {
    return getDefaultRequiredSignedParts (bMessageHasAttachments);
  }

  /**
   * Validation a PMode
   *
   * @param aPMode
   *        The PMode to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @param eValidationMode
   *        The validation mode to use. May not be <code>null</code>. Since v3.0.0
   */
  default void validatePMode (@NonNull final IPMode aPMode,
                              @NonNull final ErrorList aErrorList,
                              @NonNull final EAS4ProfileValidationMode eValidationMode)
  {}

  /**
   * Validation the initiator identity
   *
   * @param aUserMsg
   *        The message to use for comparison. May not be <code>null</code>.
   * @param aSignCert
   *        The signature certificate used to sign the message. Can be <code>null</code>.
   * @param aMessageMetadata
   *        Metadata of the message optionally containing the TLS client certificate used. May not
   *        be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @since 2.5.0
   */
  default void validateInitiatorIdentity (@NonNull final Ebms3UserMessage aUserMsg,
                                          @Nullable final X509Certificate aSignCert,
                                          @NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                          @NonNull final ErrorList aErrorList)
  {}

  /**
   * Validation of a SoapDocument
   *
   * @param aSoapDocument
   *        The SOAP document to be validated. May not be <code>null</code>.
   * @param eSoapVersion
   *        The SOAP version of the document to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @since 4.6.2
   */
  default void validateSoapMessage (@NonNull final Document aSoapDocument, @NonNull ESoapVersion eSoapVersion, @NonNull final ErrorList aErrorList)
  {}

  /**
   * Validation of a UserMessage
   *
   * @param aUserMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateUserMessage (@NonNull final Ebms3UserMessage aUserMsg, @NonNull final ErrorList aErrorList)
  {}

  /**
   * Validation of a SignalMessage
   *
   * @param aSignalMsg
   *        The message to be validated. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   */
  default void validateSignalMessage (@NonNull final Ebms3SignalMessage aSignalMsg, @NonNull final ErrorList aErrorList)
  {}
}
