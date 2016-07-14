package com.helger.as4lib.model.pmode;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeLegReliabilityMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT = "AtLeastOnceContract";
  private static final String ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY = "AtLeastOnceAckOnDelivery";
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO = "AtLeastOnceContractAcksTo";
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE = "AtLeastOnceContractAckResponse";
  private static final String ATTR_AT_LEAST_ONCE_REPLY_PATTERN = "AtLeastOnceReplyPattern";
  private static final String ATTR_AT_MOST_ONCE_CONTRACT = "AtMostOnceContract";
  private static final String ATTR_IN_ORDER_CONTRACT = "InOrderContract";
  private static final String ATTR_START_GROUP = "StartGroup";
  private static final String ELEMENT_CORRELATION = "Correlation";
  private static final String ATTR_TERMINATE_GROUP = "TerminateGroup";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegReliability aValue = (PModeLegReliability) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    if (aValue.isAtLeastOnceContractDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT, aValue.isAtLeastOnceContract ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO, aValue.getAtLeastOnceContractAcksTo ());
    if (aValue.isAtLeastOnceAckOnDeliveryDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY, aValue.isAtLeastOnceAckOnDelivery ());
    if (aValue.isAtLeastOnceContractAckResponseDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE, aValue.isAtLeastOnceContractAckResponse ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_REPLY_PATTERN, aValue.getAtLeastOnceReplyPattern ());
    if (aValue.isAtMostOnceContractDefined ())
      ret.setAttribute (ATTR_AT_MOST_ONCE_CONTRACT, aValue.isAtMostOnceContract ());
    if (aValue.isInOrderContractDefined ())
      ret.setAttribute (ATTR_IN_ORDER_CONTRACT, aValue.isInOrderContract ());
    if (aValue.isStartGroupDefined ())
      ret.setAttribute (ATTR_START_GROUP, aValue.isStartGroup ());
    for (final String sCorrelation : aValue.getCorrelation ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_CORRELATION).appendText (sCorrelation);
    }
    if (aValue.isTerminateGroupDefined ())
      ret.setAttribute (ATTR_TERMINATE_GROUP, aValue.isTerminateGroup ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {

    final ETriState eAtLeastOnceContract = getTriState (aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_CONTRACT),
                                                        PModeLegReliability.DEFAULT_AT_LEAST_ONCE_CONTRACT);
    final ETriState eAtLeastOnceAckOnDelivery = getTriState (aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY),
                                                             PModeLegReliability.DEFAULT_AT_LEAST_ONCE_ACK_ON_DELIVERY);

    final String sAtLeastOnceContractAcksTo = aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO);
    final ETriState eAtLeastOnceContractAckResponse = getTriState (aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE),
                                                                   PModeLegReliability.DEFAULT_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE);
    final String sAtLeastOnceReplyPattern = aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_REPLY_PATTERN);
    final ETriState eAtMostOnceContract = getTriState (aElement.getAttributeValue (ATTR_AT_MOST_ONCE_CONTRACT),
                                                       PModeLegReliability.DEFAULT_AT_MOST_ONCE_CONTRACT);
    final ETriState eInOrderContract = getTriState (aElement.getAttributeValue (ATTR_IN_ORDER_CONTRACT),
                                                    PModeLegReliability.DEFAULT_IN_ORDER_CONTACT);
    final ETriState eStartGroup = getTriState (aElement.getAttributeValue (ATTR_START_GROUP),
                                               PModeLegReliability.DEFAULT_START_GROUP);
    final ICommonsList <String> aCorrelationStrings = new CommonsArrayList<> ();
    for (final IMicroElement aCorrelationElement : aElement.getAllChildElements (ELEMENT_CORRELATION))
    {
      aCorrelationStrings.add (aCorrelationElement.getTextContentTrimmed ());
    }
    final ETriState eTerminateGroup = getTriState (aElement.getAttributeValue (ATTR_TERMINATE_GROUP),
                                                   PModeLegReliability.DEFAULT_TERMINATE_GROUP);

    return new PModeLegReliability (eAtLeastOnceContract,
                                    eAtLeastOnceAckOnDelivery,
                                    sAtLeastOnceContractAcksTo,
                                    eAtLeastOnceContractAckResponse,
                                    sAtLeastOnceReplyPattern,
                                    eAtMostOnceContract,
                                    eInOrderContract,
                                    eStartGroup,
                                    aCorrelationStrings,
                                    eTerminateGroup);
  }

}
