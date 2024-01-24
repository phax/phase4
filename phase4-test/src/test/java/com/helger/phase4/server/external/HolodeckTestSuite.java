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
package com.helger.phase4.server.external;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.attr.StringMap;
import com.helger.phase4.ScopedAS4Configuration;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.message.ReceiptMessageTest;
import com.helger.phase4.server.message.UserMessageOneAttachmentTest;
import com.helger.phase4.server.message.UserMessageSoapBodyPayloadTest;

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

  private static ScopedAS4Configuration s_aSC;

  @BeforeClass
  public static void init ()
  {
    final IStringMap aSettings = new StringMap ();
    aSettings.putIn (MockJettySetup.SETTINGS_SERVER_JETTY_ENABLED, false);
    aSettings.putIn (MockJettySetup.SETTINGS_SERVER_ADDRESS, DEFAULT_HOLODECK_URI);
    s_aSC = ScopedAS4Configuration.create (aSettings);
  }

  @AfterClass
  public static void clean ()
  {
    if (s_aSC != null)
      s_aSC.close ();
  }
}
