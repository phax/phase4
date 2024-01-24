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

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.json.IJsonObject;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link PModeLegReliability}.
 *
 * @author Philip Helger
 */
public final class PModeLegReliabilityTest
{
  private static void _testJson (@Nonnull final PModeLegReliability p)
  {
    final IJsonObject o = PModeLegReliabilityJsonConverter.convertToJson (p);
    final PModeLegReliability p2 = PModeLegReliabilityJsonConverter.convertToNative (o);
    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (p, p2);
    XMLTestHelper.testMicroTypeConversion (p);
  }

  @Test
  public void testBasic ()
  {
    final PModeLegReliability x = new PModeLegReliability ();
    _testJson (x);
  }

  @Test
  public void testWithValues ()
  {
    final PModeLegReliability x = new PModeLegReliability ();
    x.setAtLeastOnceAckOnDelivery (true);
    x.setAtLeastOnceContract (false);
    x.setAtLeastOnceReplyPattern ("bla");
    x.setAtLeastOnceContractAckResponse (true);
    x.setAtLeastOnceContractAcksTo ("egon");
    x.setAtMostOnceContract (false);
    x.setCorrelation (new CommonsArrayList <> ("gus", "tav"));
    x.setInOrderContract (true);
    x.setStartGroup (false);
    x.setTerminateGroup (true);
    _testJson (x);
  }
}
