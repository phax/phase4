package com.helger.as4lib.model.pmode;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.web.scope.mock.WebScopeTestRule;

public final class PModeTest
{
  @Rule
  public final TestRule m_aTestRule = new WebScopeTestRule ();

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
