package com.helger.as4server.holodeck;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4server.message.ReceiptMessageTest;
import com.helger.as4server.message.UserMessageOneAttachmentTest;
import com.helger.as4server.message.UserMessageSoapBodyPayloadTest;
import com.helger.as4server.settings.AS4ServerConfiguration;

@RunWith (Categories.class)
@IncludeCategory (IHolodeckTests.class)
@SuiteClasses ({ UserMessageSoapBodyPayloadTest.class,
                 UserMessageOneAttachmentTest.class,
                 ReceiptMessageTest.class })
@Ignore
public class HolodeckOnlineTestSuite
{
  /** The default URL where Holodeck is supposed to run */
  // TODO will be changed soon
  public static final String DEFAULT_HOLODECK_URI = "http://localhost:8080/msh";

  @BeforeClass
  public static void init ()
  {
    AS4ServerConfiguration.reinitForTestOnly ();
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.jetty.enabled", false);
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.address", DEFAULT_HOLODECK_URI);
  }
}
