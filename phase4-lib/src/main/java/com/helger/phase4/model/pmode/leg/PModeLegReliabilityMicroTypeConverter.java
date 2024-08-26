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
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;

/**
 * XML converter for objects of class {@link PModeLegReliability}.
 *
 * @author Philip Helger
 */
public class PModeLegReliabilityMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModeLegReliability>
{
  private static final IMicroQName ATTR_AT_LEAST_ONCE_CONTRACT = new MicroQName ("AtLeastOnceContract");
  private static final IMicroQName ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY = new MicroQName ("AtLeastOnceAckOnDelivery");
  private static final IMicroQName ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO = new MicroQName ("AtLeastOnceContractAcksTo");
  private static final IMicroQName ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE = new MicroQName ("AtLeastOnceContractAckResponse");
  private static final IMicroQName ATTR_AT_LEAST_ONCE_REPLY_PATTERN = new MicroQName ("AtLeastOnceReplyPattern");
  private static final IMicroQName ATTR_AT_MOST_ONCE_CONTRACT = new MicroQName ("AtMostOnceContract");
  private static final IMicroQName ATTR_IN_ORDER_CONTRACT = new MicroQName ("InOrderContract");
  private static final IMicroQName ATTR_START_GROUP = new MicroQName ("StartGroup");
  private static final String ELEMENT_CORRELATION = "Correlation";
  private static final IMicroQName ATTR_TERMINATE_GROUP = new MicroQName ("TerminateGroup");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModeLegReliability aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    if (aValue.isAtLeastOnceContractDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT, aValue.isAtLeastOnceContract ());
    if (aValue.isAtLeastOnceAckOnDeliveryDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_ACK_ON_DELIVERY, aValue.isAtLeastOnceAckOnDelivery ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_TO, aValue.getAtLeastOnceContractAcksTo ());
    if (aValue.isAtLeastOnceContractAckResponseDefined ())
      ret.setAttribute (ATTR_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE, aValue.isAtLeastOnceContractAckResponse ());
    ret.setAttribute (ATTR_AT_LEAST_ONCE_REPLY_PATTERN, aValue.getAtLeastOnceReplyPattern ());
    if (aValue.isAtMostOnceContractDefined ())
      ret.setAttribute (ATTR_AT_MOST_ONCE_CONTRACT, aValue.isAtMostOnceContract ());
    if (aValue.isInOrderContractDefined ())
      ret.setAttribute (ATTR_IN_ORDER_CONTRACT, aValue.isInOrderContract ());
    if (aValue.isStartGroupDefined ())
      ret.setAttribute (ATTR_START_GROUP, aValue.isStartGroup ());
    for (final String sCorrelation : aValue.getAllCorrelations ())
      ret.appendElement (sNamespaceURI, ELEMENT_CORRELATION).appendText (sCorrelation);
    if (aValue.isTerminateGroupDefined ())
      ret.setAttribute (ATTR_TERMINATE_GROUP, aValue.isTerminateGroup ());
    return ret;
  }

  @Nonnull
  public PModeLegReliability convertToNative (@Nonnull final IMicroElement aElement)
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
    final ICommonsList <String> aCorrelationStrings = new CommonsArrayList <> ();
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
