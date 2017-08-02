/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.leg;

import com.helger.as4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeLegErrorHandlingMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLegErrorHandling>
{
  private static final String ELEMENT_REPORT_RECEIVER_ERRORS_TO = "ReportReceiverErrorsTo";
  private static final String ELEMENT_REPORT_SENDER_ERRORS_TO = "ReportSenderErrorsTo";
  private static final String ATTR_REPORT_AS_RESPONSE = "ReportAsResponse";
  private static final String ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER = "ReportProcessErrorNotifyConsumer";
  private static final String ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER = "ReportProcessErrorNotifyProducer";
  private static final String ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER = "ReportDeliveryFailuresNotifyProducer";

  public IMicroElement convertToMicroElement (final PModeLegErrorHandling aValue,
                                              final String sNamespaceURI,
                                              final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReportReceiverErrorsTo (),
                                                               sNamespaceURI,
                                                               ELEMENT_REPORT_RECEIVER_ERRORS_TO));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReportSenderErrorsTo (),
                                                               sNamespaceURI,
                                                               ELEMENT_REPORT_SENDER_ERRORS_TO));
    if (aValue.isReportAsResponseDefined ())
      ret.setAttribute (ATTR_REPORT_AS_RESPONSE, aValue.isReportAsResponse ());
    if (aValue.isReportProcessErrorNotifyConsumerDefined ())
      ret.setAttribute (ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER, aValue.isReportProcessErrorNotifyConsumer ());
    if (aValue.isReportProcessErrorNotifyProducerDefined ())
      ret.setAttribute (ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER, aValue.isReportProcessErrorNotifyProducer ());
    if (aValue.isReportDeliveryFailuresNotifyProducerDefined ())
      ret.setAttribute (ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER, aValue.isReportDeliveryFailuresNotifyProducer ());
    return ret;
  }

  public PModeLegErrorHandling convertToNative (final IMicroElement aElement)
  {
    final PModeAddressList aReceiverAddresses = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_REPORT_RECEIVER_ERRORS_TO),
                                                                                    PModeAddressList.class);
    final PModeAddressList aSenderAddresses = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_REPORT_SENDER_ERRORS_TO),
                                                                                  PModeAddressList.class);

    final ETriState eReportAsResponse = getTriState (aElement.getAttributeValue (ATTR_REPORT_AS_RESPONSE),
                                                     PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);

    final ETriState eReportProcessErrorNotifyConsumer = getTriState (aElement.getAttributeValue (ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER),
                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState eReportProcessErrorNotifyProducer = getTriState (aElement.getAttributeValue (ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER),
                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    final ETriState eReportDeliveryFailuresNotifyProducer = getTriState (aElement.getAttributeValue (ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER),
                                                                         PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    return new PModeLegErrorHandling (aReceiverAddresses,
                                      aSenderAddresses,
                                      eReportAsResponse,
                                      eReportProcessErrorNotifyConsumer,
                                      eReportProcessErrorNotifyProducer,
                                      eReportDeliveryFailuresNotifyProducer);
  }
}
