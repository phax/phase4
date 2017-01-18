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
package com.helger.as4.server;

import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

/**
 * This class helps to keep the test clean from strings that can be changed.
 * Most of the test use the predefined settings here.
 *
 * @author bayerlma
 */
public final class AS4ServerTestHelper
{

  public static String DEFAULT_PARTY_ID = "APP_1000000101";
  public static String DEFAULT_INITIATOR_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender";
  public static String DEFAULT_RESPONDER_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";
  public static String DEFAULT_AGREEMENT = "http://agreements.holodeckb2b.org/examples/agreement0";

  public static String FINAL_RECIPIENT = "finalRecipient";
  public static String ORIGINAL_SENDER = "originalSender";

  private AS4ServerTestHelper ()
  {}

  public static ICommonsList <Ebms3Property> getEBMSProperties ()
  {
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList <> ();
    final Ebms3Property aOriginalSender = new Ebms3Property ();
    aOriginalSender.setName (ORIGINAL_SENDER);
    aOriginalSender.setValue ("C1-test");
    final Ebms3Property aFinalRecipient = new Ebms3Property ();
    aFinalRecipient.setName (FINAL_RECIPIENT);
    aFinalRecipient.setValue ("C4-test");
    aEbms3Properties.add (aOriginalSender);
    aEbms3Properties.add (aFinalRecipient);

    return aEbms3Properties;
  }

}
