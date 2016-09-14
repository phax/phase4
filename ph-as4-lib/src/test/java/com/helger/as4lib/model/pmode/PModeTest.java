package com.helger.as4lib.model.pmode;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.as4lib.AS4TestRule;

/**
 * Test class for class {@link PMode}.
 *
 * @author Philip Helger
 */
public final class PModeTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testInvalidCtor ()
  {
    try
    {
      new PMode ((String) null);
      fail ();
    }
    catch (final NullPointerException ex)
    {
      // Expected
    }
    try
    {
      new PMode ("");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // Expected
    }
  }
}
