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

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.IJsonValue;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;

/**
 * JSON converter for objects of class {@link PModeLegReliability}.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeLegReliabilityJsonConverter
{
  private static final String AT_LEAST_ONCE_CONTRACT = "AtLeastOnceContract";
  private static final String AT_LEAST_ONCE_ACK_ON_DELIVERY = "AtLeastOnceAckOnDelivery";
  private static final String AT_LEAST_ONCE_CONTRACT_ACK_TO = "AtLeastOnceContractAcksTo";
  private static final String AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE = "AtLeastOnceContractAckResponse";
  private static final String AT_LEAST_ONCE_REPLY_PATTERN = "AtLeastOnceReplyPattern";
  private static final String AT_MOST_ONCE_CONTRACT = "AtMostOnceContract";
  private static final String IN_ORDER_CONTRACT = "InOrderContract";
  private static final String START_GROUP = "StartGroup";
  private static final String CORRELATION = "Correlation";
  private static final String TERMINATE_GROUP = "TerminateGroup";

  private PModeLegReliabilityJsonConverter ()
  {}

  @Nonnull
  public static IJsonObject convertToJson (@Nonnull final PModeLegReliability aValue)
  {
    final IJsonObject ret = new JsonObject ();

    if (aValue.isAtLeastOnceContractDefined ())
      ret.add (AT_LEAST_ONCE_CONTRACT, aValue.isAtLeastOnceContract ());
    if (aValue.isAtLeastOnceAckOnDeliveryDefined ())
      ret.add (AT_LEAST_ONCE_ACK_ON_DELIVERY, aValue.isAtLeastOnceAckOnDelivery ());
    if (aValue.hasAtLeastOnceContractAcksTo ())
      ret.add (AT_LEAST_ONCE_CONTRACT_ACK_TO, aValue.getAtLeastOnceContractAcksTo ());
    if (aValue.isAtLeastOnceContractAckResponseDefined ())
      ret.add (AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE, aValue.isAtLeastOnceContractAckResponse ());
    if (aValue.hasAtLeastOnceReplyPattern ())
      ret.add (AT_LEAST_ONCE_REPLY_PATTERN, aValue.getAtLeastOnceReplyPattern ());
    if (aValue.isAtMostOnceContractDefined ())
      ret.add (AT_MOST_ONCE_CONTRACT, aValue.isAtMostOnceContract ());
    if (aValue.isInOrderContractDefined ())
      ret.add (IN_ORDER_CONTRACT, aValue.isInOrderContract ());
    if (aValue.isStartGroupDefined ())
      ret.add (START_GROUP, aValue.isStartGroup ());
    if (aValue.correlations ().isNotEmpty ())
      ret.addJson (CORRELATION, new JsonArray ().addAll (aValue.correlations ()));
    if (aValue.isTerminateGroupDefined ())
      ret.add (TERMINATE_GROUP, aValue.isTerminateGroup ());
    return ret;
  }

  @Nonnull
  public static PModeLegReliability convertToNative (@Nonnull final IJsonObject aElement)
  {
    final ETriState eAtLeastOnceContract = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (AT_LEAST_ONCE_CONTRACT),
                                                                                        PModeLegReliability.DEFAULT_AT_LEAST_ONCE_CONTRACT);
    final ETriState eAtLeastOnceAckOnDelivery = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (AT_LEAST_ONCE_ACK_ON_DELIVERY),
                                                                                             PModeLegReliability.DEFAULT_AT_LEAST_ONCE_ACK_ON_DELIVERY);
    final String sAtLeastOnceContractAcksTo = aElement.getAsString (AT_LEAST_ONCE_CONTRACT_ACK_TO);
    final ETriState eAtLeastOnceContractAckResponse = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE),
                                                                                                   PModeLegReliability.DEFAULT_AT_LEAST_ONCE_CONTRACT_ACK_RESPONSE);
    final String sAtLeastOnceReplyPattern = aElement.getAsString (AT_LEAST_ONCE_REPLY_PATTERN);
    final ETriState eAtMostOnceContract = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (AT_MOST_ONCE_CONTRACT),
                                                                                       PModeLegReliability.DEFAULT_AT_MOST_ONCE_CONTRACT);
    final ETriState eInOrderContract = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (IN_ORDER_CONTRACT),
                                                                                    PModeLegReliability.DEFAULT_IN_ORDER_CONTACT);
    final ETriState eStartGroup = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (START_GROUP),
                                                                               PModeLegReliability.DEFAULT_START_GROUP);
    final ICommonsList <String> aCorrelationStrings = new CommonsArrayList <> ();
    final IJsonArray aCorrelation = aElement.getAsArray (CORRELATION);
    if (aCorrelation != null)
      for (final IJsonValue aCorrelationElement : aCorrelation.iteratorValues ())
        aCorrelationStrings.add (aCorrelationElement.getAsString ());

    final ETriState eTerminateGroup = AbstractPModeMicroTypeConverter.getTriState (aElement.getAsString (TERMINATE_GROUP),
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
