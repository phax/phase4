package com.helger.as4lib.partner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.as4lib.AS4TestRule;
import com.helger.as4lib.util.StringMap;
import com.helger.xml.mock.XMLTestHelper;

public class PartnerMicroTypeConverterTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testMicroTypeConversion ()
  {
    final String sID = "microtypeconvertertest";
    final StringMap aStringMap = new StringMap ();
    aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, sID);

    final Partner aPartner = new Partner (sID, aStringMap);

    XMLTestHelper.testMicroTypeConversion (aPartner);
  }
}
