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
package com.helger.as4lib.model.pmode.leg;

import com.helger.as4lib.model.pmode.AbstractPModeMicroTypeConverter;
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
