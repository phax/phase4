package com.helger.as4server.holodeck;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.helger.as4server.message.ReceiptMessageTests;
import com.helger.as4server.message.UserMessageOneAttachmentTests;
import com.helger.as4server.message.UserMessageSoapBodyPayloadTests;
import com.helger.as4server.settings.AS4ServerConfiguration;

@RunWith (Categories.class)
@IncludeCategory (IHolodeckTests.class)
@SuiteClasses ({ UserMessageSoapBodyPayloadTests.class,
                 UserMessageOneAttachmentTests.class,
                 ReceiptMessageTests.class })
public class HolodeckOnlineTestSuite
{
  /** The default URL where Holodeck is supposed to run */
  // TODO will be changed soon
  public static final String DEFAULT_HOLODECK_URI = "http://localhost:8080/msh";

  @BeforeClass
  public static void init ()
  {
    AS4ServerConfiguration.reinit (true);
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.jetty.enabled", false);
    AS4ServerConfiguration.getMutableSettings ().setValue ("server.address", DEFAULT_HOLODECK_URI);
  }
}
