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
package com.helger.phase4.model.pmode.leg;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.state.ETriState;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;

/**
 * JSON converter for objects of class {@link PModeLegErrorHandling}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegErrorHandlingJsonConverter
{
  private static final String REPORT_SENDER_ERRORS_TO = "ReportSenderErrorsTo";
  private static final String REPORT_RECEIVER_ERRORS_TO = "ReportReceiverErrorsTo";
  private static final String REPORT_AS_RESPONSE = "ReportAsResponse";
  private static final String REPORT_PROCESS_ERROR_NOTFIY_CONSUMER = "ReportProcessErrorNotifyConsumer";
  private static final String REPORT_PROCESS_ERROR_NOTFIY_PRODUCER = "ReportProcessErrorNotifyProducer";
  private static final String REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER = "ReportDeliveryFailuresNotifyProducer";

  private PModeLegErrorHandlingJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLegErrorHandling aValue)
  {
    final IJsonObject ret = new JsonObject ();
    if (aValue.hasReportSenderErrorsTo ())
      ret.addJson (REPORT_SENDER_ERRORS_TO,
                   PModeAddressListJsonConverter.convertToJson (aValue.getReportSenderErrorsTo ()));
    if (aValue.hasReportReceiverErrorsTo ())
      ret.addJson (REPORT_RECEIVER_ERRORS_TO,
                   PModeAddressListJsonConverter.convertToJson (aValue.getReportReceiverErrorsTo ()));
    if (aValue.isReportAsResponseDefined ())
      ret.add (REPORT_AS_RESPONSE, aValue.isReportAsResponse ());
    if (aValue.isReportProcessErrorNotifyConsumerDefined ())
      ret.add (REPORT_PROCESS_ERROR_NOTFIY_CONSUMER, aValue.isReportProcessErrorNotifyConsumer ());
    if (aValue.isReportProcessErrorNotifyProducerDefined ())
      ret.add (REPORT_PROCESS_ERROR_NOTFIY_PRODUCER, aValue.isReportProcessErrorNotifyProducer ());
    if (aValue.isReportDeliveryFailuresNotifyProducerDefined ())
      ret.add (REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER, aValue.isReportDeliveryFailuresNotifyProducer ());
    return ret;
  }

  @Nonnull
  public static PModeLegErrorHandling convertToNative (@Nonnull final IJsonObject aElement)
  {
    final IJsonArray aSender = aElement.getAsArray (REPORT_SENDER_ERRORS_TO);
    final PModeAddressList aSenderAddresses = aSender == null ? null : PModeAddressListJsonConverter.convertToNative (
                                                                                                                      aSender);

    final IJsonArray aReceiver = aElement.getAsArray (REPORT_RECEIVER_ERRORS_TO);
    final PModeAddressList aReceiverAddresses = aReceiver == null ? null : PModeAddressListJsonConverter
                                                                                                        .convertToNative (aReceiver);

    final ETriState eReportAsResponse = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (REPORT_AS_RESPONSE),
                                                                                     PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);
    final ETriState eReportProcessErrorNotifyConsumer = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (REPORT_PROCESS_ERROR_NOTFIY_CONSUMER),
                                                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState eReportProcessErrorNotifyProducer = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (REPORT_PROCESS_ERROR_NOTFIY_PRODUCER),
                                                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    final ETriState eReportDeliveryFailuresNotifyProducer = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER),
                                                                                                         PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    return new PModeLegErrorHandling (aSenderAddresses,
                                      aReceiverAddresses,
                                      eReportAsResponse,
                                      eReportProcessErrorNotifyConsumer,
                                      eReportProcessErrorNotifyProducer,
                                      eReportDeliveryFailuresNotifyProducer);
  }
}
