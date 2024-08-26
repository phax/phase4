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
package com.helger.phase4;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.model.message.MessageHelperMethods;

/**
 * Reusable constants for testing.
 *
 * @author Philip Helger
 */
@Immutable
public final class AS4TestConstants
{
  // Default values
  public static final String DEFAULT_SERVER_ADDRESS = "http://localhost:8080/as4";
  public static final String DEFAULT_MPC = CAS4.DEFAULT_MPC_ID;

  // Test value that can be used
  public static final String TEST_RESPONDER = "TestResponder";
  public static final String TEST_INITIATOR = "TestInitiator";
  public static final String TEST_ACTION = "NewPurchaseOrder";
  public static final String TEST_CONVERSATION_ID = "4321";
  public static final String TEST_SERVICE_TYPE = "MyServiceTypes";
  public static final String TEST_SERVICE = "QuoteToCollect";
  public static final String TEST_SOAP_BODY_PAYLOAD_XML = "SOAPBodyPayload.xml";
  public static final String TEST_PAYLOAD_XML = "PayloadXML.xml";

  // Attachments
  public static final String ATTACHMENT_SHORTXML2_XML = "attachment/shortxml2.xml";
  public static final String ATTACHMENT_TEST_IMG_JPG = "attachment/test-img.jpg";
  public static final String ATTACHMENT_SHORTXML_XML = "attachment/shortxml.xml";
  public static final String ATTACHMENT_TEST_XML_GZ = "attachment/test.xml.gz";
  public static final String ATTACHMENT_TEST_IMG2_JPG = "attachment/test-img2.jpg";

  // CEF
  public static final String CEF_INITIATOR_ID = "CEF-Initiator";
  public static final String CEF_RESPONDER_ID = "CEF-Responder";

  // Common Asserts
  public static final String NON_REPUDIATION_INFORMATION = "NonRepudiationInformation";
  public static final String RECEIPT_ASSERTCHECK = "Receipt";
  public static final String USERMESSAGE_ASSERTCHECK = "UserMessage";

  private AS4TestConstants ()
  {}

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  public static ICommonsList <Ebms3Property> getEBMSProperties ()
  {
    return new CommonsArrayList <> (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER, "C1-test"),
                                    MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT, "C4-test"));
  }
}
