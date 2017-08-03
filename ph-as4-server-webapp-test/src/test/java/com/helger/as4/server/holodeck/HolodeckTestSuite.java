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
package com.helger.as4.server.holodeck;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4.server.message.ReceiptMessageTest;
import com.helger.as4.server.message.UserMessageOneAttachmentTest;
import com.helger.as4.server.message.UserMessageSoapBodyPayloadTest;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;

/**
 * Runs tests that are marked with the Category {@link IHolodeckTests}. <br>
 * Note: this requires a running Holodeck on {@value #DEFAULT_HOLODECK_URI}.<br>
 * Note: tested with Holodeck 2.1.2 with the esens connector enabled
 *
 * @author bayerlma
 */
@RunWith (Categories.class)
@IncludeCategory (IHolodeckTests.class)
@SuiteClasses ({ UserMessageSoapBodyPayloadTest.class, UserMessageOneAttachmentTest.class, ReceiptMessageTest.class })
@Ignore
public final class HolodeckTestSuite
{
  /** The default URL where Holodeck is supposed to run */
  public static final String DEFAULT_HOLODECK_URI = "http://localhost:8080/msh";

  @BeforeClass
  public static void init ()
  {
    AS4ServerConfiguration.internalReinitForTestOnly ();
    AS4ServerConfiguration.getMutableSettings ().putIn ("server.jetty.enabled", false);
    AS4ServerConfiguration.getMutableSettings ().putIn ("server.address", DEFAULT_HOLODECK_URI);
  }
}
