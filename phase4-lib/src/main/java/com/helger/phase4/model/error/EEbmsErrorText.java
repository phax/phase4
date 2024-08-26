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
package com.helger.phase4.model.error;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.text.display.IHasDisplayText;

/**
 * Translatable error texts for EBMS error messages
 *
 * @author Philip Helger
 */
public enum EEbmsErrorText implements IHasDisplayText
{
  VALUE_NOT_RECOGNIZED ("Although the message document is well formed and schema valid, some element/attribute contains a value that could not be recognized and therefore could not be used by the MSH."),
  FEATURE_NOT_SUPPORTED ("Although the message document is well formed and schema valid, some element/attribute value cannot be processed as expected because the related feature is not supported by the MSH."),
  VALUE_INCONSISTENT ("Although the message document is well formed and schema valid, some element/attribute value is inconsistent either with the content of other element/attribute, or with the processing mode of the MSH, or with the normative requirements of the ebMS specification."),
  OTHER ("An undefined error occurred."),
  CONNECTION_FAILURE ("The MSH is expieriencing temporary or permanent failure in trying to open a transport connection with a remote MSH."),
  EMPTY_MESSAGE_PARTITION_CHANNEL ("There is no message available for pulling from this MPC at this moment."),
  MIME_INCONSISTENCY ("The use of MIME is not consistent with the required usage in this specification"),
  FEATURE_NOT_SUPPORTED_INCONSISTENT ("Although the message document is well formed and schema valid, the presence or absence of some element/attribute is not consistent with the capability of the MSH, with respect to supported features"),
  INVALID_HEADER ("The ebMS header is either not well formed as an XML document, or does not conform to the ebMS packaging rules."),
  PROCESSING_MODE_MISMATCH ("The ebMS header or another header (e.g. reliability, security) expected by the MSH is not compatible with the expected content, based on the associated P-Mode."),
  EXTERNAL_PAYLOAD_ERROR ("The MSH is unable to resolve an external payload reference (i.e. a Part that is not contained within the ebMS Message, as identified by a PartInfo/href URI)."),
  FAILED_AUTHENTICATION ("The signature in the Security header intended for the \"ebms\" SOAP actor, could not be validated by the Security module."),
  FAILED_DECRYPTION ("The encrypted data reference the Security header intended for the \"ebms\" SOAP actor could not be decrypted by the Security Module."),
  POLICY_NONCOMPLIANCE ("The processor determined that the message's security methods, parameters, scope or other security policy-level requirements or agreements were not satisfied."),
  DYSFUNCTIONAL_RELIABILITY ("Some reliability function as implemented by the Reliability module, is not operational, or the reliability state associated with this message sequence is not valid."),
  DELIVERY_FAILURE ("Although the message was send under Guaranteed delivery reuqirement, the Reliability module could not get assurance that the message was properly delivered, in spite of resending efforts."),
  MISSING_RECEIPT ("A Receipt has not been received for a message that was previously sent by the MSH generating this error."),
  INVALID_RECEIPT ("A Receipt has been received  for a message that was previously sent by the MSH generating this error, but the content does not match the message content (e.g. some part has not been acknowledged, or the digest associated does not match the signature digest, for NRR)."),
  DECOMPRESSION_FAILURE ("An error occurred during the decompression.");

  private final String m_sErrorText;

  EEbmsErrorText (@Nonnull @Nonempty final String sErrorText)
  {
    m_sErrorText = sErrorText;
  }

  @Nullable
  public String getDisplayText (@Nonnull final Locale aContentLocale)
  {
    return m_sErrorText;
  }
}
