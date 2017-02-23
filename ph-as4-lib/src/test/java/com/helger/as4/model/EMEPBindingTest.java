package com.helger.as4.model;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.string.StringHelper;

/**
 * Test class for class {@link EMEPBinding}
 *
 * @author Philip Helger
 */
public final class EMEPBindingTest
{
  @Test
  public void testBasic ()
  {
    for (final EMEPBinding e : EMEPBinding.values ())
    {
      assertTrue (StringHelper.hasText (e.getID ()));
      assertTrue (StringHelper.hasText (e.getURI ()));
      assertSame (e, EMEPBinding.getFromIDOrNull (e.getID ()));
      assertTrue (e.isSynchronous () || e.isAsynchronous ());
    }
  }
}
