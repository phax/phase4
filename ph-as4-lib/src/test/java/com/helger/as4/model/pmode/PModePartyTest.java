package com.helger.as4.model.pmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link PModeParty}
 *
 * @author Philip Helger
 */
public final class PModePartyTest
{
  @Test
  public void testBasic ()
  {
    final PModeParty p = new PModeParty ("a", "b", "c", "d", "e");
    assertEquals ("a", p.getIDType ());
    assertTrue (p.hasIDType ());
    assertEquals ("b", p.getIDValue ());
    assertEquals ("a:b", p.getID ());
    assertEquals ("c", p.getRole ());
    assertEquals ("d", p.getUserName ());
    assertTrue (p.hasUserName ());
    assertEquals ("e", p.getPassword ());
    assertTrue (p.hasPassword ());
    assertEquals (p, new PModeParty ("a", "b", "c", "d", "e"));
  }

  @Test
  public void testSimple ()
  {
    final PModeParty p = PModeParty.createSimple ("b", "c");
    assertNull (p.getIDType ());
    assertFalse (p.hasIDType ());
    assertEquals ("b", p.getIDValue ());
    assertEquals ("b", p.getID ());
    assertEquals ("c", p.getRole ());
    assertNull (p.getUserName ());
    assertFalse (p.hasUserName ());
    assertNull (p.getPassword ());
    assertFalse (p.hasPassword ());
    assertEquals (p, PModeParty.createSimple ("b", "c"));
  }
}
