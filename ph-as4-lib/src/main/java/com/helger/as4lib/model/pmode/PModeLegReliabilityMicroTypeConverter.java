package com.helger.as4lib.model.pmode;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

public class PModeLegReliabilityMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_CORRELATION = "Correlation";
  private static final String ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY = "AtLeastOnceAckOnDelivery";
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT = "AtLeastOnceContract";
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE = "AtLeastOnceContractAckResponse";
  private static final String ATTR_AT_LEAST_ONCE_REPLY_PATTERN = "AtLeastOnceReplyPattern";
  private static final String ATTR_AT_MOST_ONCE_CONTRACT = "AtMostOnceContract";
  private static final String ATTR_IN_ORDER_CONTRACT = "InOrderContract";
  private static final String ATTR_START_GROUP = "StartGroup";
  private static final String ATTR_TERMINATE_GROUP = "TerminateGroup";
  private static final String ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO = "AtLeastOnceContractAcksTo";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegReliability aValue = (PModeLegReliability) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sCorrelation : aValue.getCorrelation ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_CORRELATION).appendElement (sCorrelation);
    }
    ret.setAttribute (ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY, aValue.getAtLeastOnceAckOnDelivery ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT, aValue.getAtLeastOnceContract ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE, aValue.getAtLeastOnceContractAckResponse ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_REPLY_PATTERN, aValue.getAtLeastOnceReplyPattern ());
    ret.setAttribute (ATTR_AT_MOST_ONCE_CONTRACT, aValue.getAtMostOnceContract ());
    ret.setAttribute (ATTR_IN_ORDER_CONTRACT, aValue.getInOrderContract ());
    ret.setAttribute (ATTR_START_GROUP, aValue.getStartGroup ());
    ret.setAttribute (ATTR_TERMINATE_GROUP, aValue.getTerminateGroup ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO, aValue.getAtLeastOnceContractAcksTo ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final ICommonsList <IMicroElement> aCorrelation = aElement.getAllChildElements (ELEMENT_CORRELATION);
    final ICommonsList <String> aCorrelationStrings = new CommonsArrayList <String> ();
    for (final IMicroElement aCorrelationElement : aCorrelation)
    {
      aCorrelationStrings.add (aCorrelationElement.getNodeName ());
    }
    final ETriState eAtLeastOnceAckOnDelivery = aElement.getAttributeValueWithConversion (ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY,
                                                                                          ETriState.class);

    final ETriState eAtLeastOnceContract = aElement.getAttributeValueWithConversion (ATTR_AT_LEAST_ONCE_CONTRACT,
                                                                                     ETriState.class);
    final ETriState eAtLeastOnceContractAckResponse = aElement.getAttributeValueWithConversion (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE,
                                                                                                ETriState.class);
    final String sAtLeastOnceReplyPattern = aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_REPLY_PATTERN);
    final ETriState eAtMostOnceContract = aElement.getAttributeValueWithConversion (ATTR_AT_MOST_ONCE_CONTRACT,
                                                                                    ETriState.class);
    final ETriState eInOrderContract = aElement.getAttributeValueWithConversion (ATTR_IN_ORDER_CONTRACT,
                                                                                 ETriState.class);
    final ETriState eStartGroup = aElement.getAttributeValueWithConversion (ATTR_START_GROUP, ETriState.class);
    final ETriState eTerminateGroup = aElement.getAttributeValueWithConversion (ATTR_TERMINATE_GROUP, ETriState.class);
    final String sAtLeastOnceContractAcksTo = aElement.getAttributeValue (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO);
    return new PModeLegReliability (aCorrelationStrings,
                                    eAtLeastOnceAckOnDelivery,
                                    eAtLeastOnceContract,
                                    eAtLeastOnceContractAckResponse,
                                    sAtLeastOnceReplyPattern,
                                    eAtMostOnceContract,
                                    eInOrderContract,
                                    eStartGroup,
                                    eTerminateGroup,
                                    sAtLeastOnceContractAcksTo);
  }

}
