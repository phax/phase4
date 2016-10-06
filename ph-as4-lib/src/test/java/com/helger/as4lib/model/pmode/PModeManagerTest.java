package com.helger.as4lib.model.pmode;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.as4lib.AS4TestRule;
import com.helger.as4lib.mgr.MetaAS4Manager;

/**
 * Test class for class {@link PModeManager}.
 *
 * @author bayerlma
 */
public final class PModeManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Test
  public void testBasic ()
  {
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    assertNotNull (aPModeMgr);
  }
}
