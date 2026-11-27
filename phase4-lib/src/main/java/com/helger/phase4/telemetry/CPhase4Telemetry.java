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
package com.helger.phase4.telemetry;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for the phase4 telemetry (tracing and metrics) emitted via ph-telemetry: span names,
 * metric names, attribute keys and attribute values. All names use the <code>phase4.*</code>
 * prefix, except for the attribute keys taken from the OpenTelemetry semantic conventions.
 * <p>
 * Participant identifiers (C1 and C4) are never recorded. Attributes with a potentially high
 * cardinality (message ID, service, action, document type, process, server address) are only
 * used on spans, never on metrics.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class CPhase4Telemetry
{
  /**
   * The instrumentation scope name that an application may use when it wires an OpenTelemetry
   * binding of ph-telemetry only for phase4. phase4 itself never registers a binding.
   */
  public static final String INSTRUMENTATION_SCOPE_NAME = "com.helger.phase4";

  /** Span for the handling of one incoming AS4 request (server side) */
  public static final String SPAN_INCOMING_REQUEST = "phase4.incoming.request";
  /** Span for sending one AS4 user message and evaluating the response */
  public static final String SPAN_OUTGOING_USER_MESSAGE = "phase4.outgoing.user_message";
  /** Span for one HTTP transmission attempt */
  public static final String SPAN_HTTP_SEND = "phase4.http.send";
  /** Span for one SMP endpoint lookup */
  public static final String SPAN_SMP_LOOKUP = "phase4.smp.lookup";

  /** Span event: an incoming message was detected as a duplicate */
  public static final String EVENT_DUPLICATE = "phase4.duplicate";

  public static final String METRIC_INCOMING_REQUESTS = "phase4.incoming.requests";
  public static final String METRIC_INCOMING_DURATION = "phase4.incoming.duration";
  public static final String METRIC_INCOMING_SIZE = "phase4.incoming.size";
  public static final String METRIC_INCOMING_DUPLICATES = "phase4.incoming.duplicates";
  public static final String METRIC_INCOMING_EBMS_ERRORS = "phase4.incoming.ebms_errors";
  public static final String METRIC_OUTGOING_USER_MESSAGES = "phase4.outgoing.user_messages";
  public static final String METRIC_OUTGOING_DURATION = "phase4.outgoing.duration";
  public static final String METRIC_HTTP_ATTEMPTS = "phase4.http.attempts";
  public static final String METRIC_HTTP_DURATION = "phase4.http.duration";
  public static final String METRIC_HTTP_REQUEST_SIZE = "phase4.http.request.size";
  public static final String METRIC_HTTP_RETRIES = "phase4.http.retries";
  public static final String METRIC_SMP_LOOKUPS = "phase4.smp.lookups";
  public static final String METRIC_SMP_LOOKUP_DURATION = "phase4.smp.lookup.duration";

  /** Outcome of an operation - see the <code>OUTCOME_*</code> constants */
  public static final String ATTR_OUTCOME = "phase4.outcome";
  /** The type of the incoming AS4 message - see the <code>MESSAGE_TYPE_*</code> constants */
  public static final String ATTR_MESSAGE_TYPE = "phase4.message.type";
  /** The ID of the AS4 profile used */
  public static final String ATTR_PROFILE_ID = "phase4.profile.id";
  /** The ID of the PMode used. Span only. */
  public static final String ATTR_PMODE_ID = "phase4.pmode.id";
  /** The AS4 message ID. Span only. */
  public static final String ATTR_MESSAGE_ID = "phase4.message.id";
  /** The ebMS CollaborationInfo/ConversationId value. Span only. */
  public static final String ATTR_CONVERSATION_ID = "phase4.conversation.id";
  /**
   * The ebMS MessageInfo/MessageId value of the signal message received in response to an outgoing
   * user message. Span only.
   */
  public static final String ATTR_SIGNAL_MESSAGE_ID = "phase4.signal_message.id";
  /** The ebMS MessageInfo/RefToMessageId value of a signal message. Span only. */
  public static final String ATTR_SIGNAL_REF_TO_MESSAGE_ID = "phase4.signal_message.ref_to_message.id";
  /** The ebMS CollaborationInfo/Service value. Span only. */
  public static final String ATTR_SERVICE = "phase4.service";
  /** The ebMS CollaborationInfo/Action value. Span only. */
  public static final String ATTR_ACTION = "phase4.action";
  /** The configured handling of incoming duplicates */
  public static final String ATTR_DUPLICATE_HANDLING = "phase4.duplicate.handling";
  /** The ebMS error code (e.g. <code>EBMS:0004</code>) */
  public static final String ATTR_EBMS_ERROR_CODE = "phase4.ebms.error.code";
  /**
   * The result of sending a user message - the ID of <code>EAS4UserMessageSendResult</code> or
   * {@link #OUTCOME_EXCEPTION}
   */
  public static final String ATTR_SEND_RESULT = "phase4.send.result";
  /** The retry classification of a failed HTTP attempt - the name of the enum constant */
  public static final String ATTR_HTTP_RETRY_CLASSIFICATION = "phase4.http.retry.classification";
  /** The SMP client flavour used - see the <code>SMP_PROVIDER_*</code> constants */
  public static final String ATTR_SMP_PROVIDER = "phase4.smp.provider";
  /** The ID of the transport profile looked up in the SMP */
  public static final String ATTR_TRANSPORT_PROFILE_ID = "phase4.transport_profile.id";
  /** Whether a retry of the failed operation is feasible */
  public static final String ATTR_RETRY_FEASIBLE = "phase4.retry_feasible";
  /** The document type identifier. Span only. */
  public static final String ATTR_DOCTYPE_ID = "phase4.doctype.id";
  /** The process identifier. Span only. */
  public static final String ATTR_PROCESS_ID = "phase4.process.id";

  /** OpenTelemetry semantic convention: the class of the error that occurred */
  public static final String ATTR_ERROR_TYPE = "error.type";
  /** OpenTelemetry semantic convention: the HTTP response status code */
  public static final String ATTR_HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
  /** OpenTelemetry semantic convention: the host name of the server. Span only. */
  public static final String ATTR_SERVER_ADDRESS = "server.address";

  public static final String OUTCOME_SUCCESS = "success";
  /** An incoming request was answered with an ebMS error */
  public static final String OUTCOME_EBMS_ERROR = "ebms_error";
  /** An operation failed */
  public static final String OUTCOME_ERROR = "error";
  /** An operation was ended by an exception */
  public static final String OUTCOME_EXCEPTION = "exception";

  public static final String MESSAGE_TYPE_USER_MESSAGE = "user_message";
  public static final String MESSAGE_TYPE_PING = "ping";
  public static final String MESSAGE_TYPE_RECEIPT = "receipt";
  public static final String MESSAGE_TYPE_ERROR = "error";
  public static final String MESSAGE_TYPE_PULL_REQUEST = "pull_request";
  public static final String MESSAGE_TYPE_UNKNOWN = "unknown";

  public static final String SMP_PROVIDER_PEPPOL = "peppol";
  public static final String SMP_PROVIDER_BDXR1 = "bdxr1";
  public static final String SMP_PROVIDER_BDXR2 = "bdxr2";

  private CPhase4Telemetry ()
  {}
}
