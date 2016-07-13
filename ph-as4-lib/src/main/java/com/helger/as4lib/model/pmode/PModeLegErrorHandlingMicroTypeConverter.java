package com.helger.as4lib.model.pmode;

import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

public class PModeLegErrorHandlingMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_REPORT_RECEIVER_ERRORS_TO = "ReportReceiverErrorsTo";
  private static final String ELEMENT_REPORT_SENDER_ERRORS_TO = "ReportSenderErrorsTo";
  private static final String ATTR_REPORT_AS_RESPONSE = "ReportAsResponse";
  private static final String ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER = "ReportDeliveryFailuresNotifyProducer";
  private static final String ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER = "ReportProcessErrorNotifyConsumer";
  private static final String ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER = "ReportProcessErrorNotifyProducer";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegErrorHandling aValue = (PModeLegErrorHandling) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReportReceiverErrorsTo (),
                                                               sNamespaceURI,
                                                               ELEMENT_REPORT_RECEIVER_ERRORS_TO));
    ret.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getReportSenderErrorsTo (),
                                                               sNamespaceURI,
                                                               ELEMENT_REPORT_SENDER_ERRORS_TO));
    if (aValue.isReportAsResponseDefined ())
      ret.setAttribute (ATTR_REPORT_AS_RESPONSE, aValue.isReportAsResponse ());
    if (aValue.isReportDeliveryFailuresNotifyProducerDefined ())
      ret.setAttribute (ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER, aValue.isReportDeliveryFailuresNotifyProducer ());
    if (aValue.isReportProcessErrorNotifyConsumerDefined ())
      ret.setAttribute (ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER, aValue.isReportProcessErrorNotifyConsumer ());
    if (aValue.isReportProcessErrorNotifyProducerDefined ())
      ret.setAttribute (ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER, aValue.isReportProcessErrorNotifyProducer ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {

    final PModeAddressList aReceiverAddresses = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_REPORT_RECEIVER_ERRORS_TO),
                                                                                    PModeAddressList.class);
    final PModeAddressList aSenderAddresses = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_REPORT_SENDER_ERRORS_TO),
                                                                                  PModeAddressList.class);

    final ETriState eReportAsResponse = getTriState (aElement.getAttributeValue (ATTR_REPORT_AS_RESPONSE),
                                                     PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);
    final ETriState eReportDeliveryFailuresNotifyProducer = getTriState (aElement.getAttributeValue (ATTR_REPORT_DELIVERY_FAILURE_NOTFIY_PRODUCER),
                                                                         PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    final ETriState eReportProcessErrorNotifyConsumer = getTriState (aElement.getAttributeValue (ATTR_REPORT_PROCESS_ERROR_NOTFIY_CONSUMER),
                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState eReportProcessErrorNotifyProducer = getTriState (aElement.getAttributeValue (ATTR_REPORT_PROCESS_ERROR_NOTFIY_PRODUCER),
                                                                     PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    return new PModeLegErrorHandling (aReceiverAddresses,
                                      aSenderAddresses,
                                      eReportAsResponse,
                                      eReportDeliveryFailuresNotifyProducer,
                                      eReportProcessErrorNotifyConsumer,
                                      eReportProcessErrorNotifyProducer);
  }

}
