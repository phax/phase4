package com.helger.as4.duplicate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link AS4DuplicateItem}.
 *
 * @author Philip Helger
 */
public final class AS4DuplicateItemTest
{
  @Test
  public void testBasic ()
  {
    final AS4DuplicateItem x = new AS4DuplicateItem ("x");
    assertEquals ("x", x.getID ());
    assertEquals ("x", x.getMessageID ());
    XMLTestHelper.testMicroTypeConversion (x);
  }
}
