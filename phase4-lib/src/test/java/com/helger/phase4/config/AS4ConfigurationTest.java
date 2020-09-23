package com.helger.phase4.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.config.source.IConfigurationSource;
import com.helger.config.source.res.IConfigurationSourceResource;
import com.helger.config.value.ConfiguredValue;

/**
 * Test class of class {@link AS4Configuration}.
 *
 * @author Philip Helger
 */
public final class AS4ConfigurationTest
{
  @Test
  public void testBasic ()
  {
    assertTrue (AS4Configuration.isUseInMemoryManagers ());
    assertTrue (AS4Configuration.isWSS4JSynchronizedSecurity ());

    final ConfiguredValue aCV = AS4Configuration.getConfig ()
                                                .getConfiguredValue (AS4Configuration.PROPERTY_PHASE4_WSS4J_SYNCSECURITY);
    assertNotNull (aCV);

    final IConfigurationSource aCS = aCV.getConfigurationSource ();
    assertNotNull (aCS);
    assertTrue (aCS instanceof IConfigurationSourceResource);
    final IConfigurationSourceResource aCSR = (IConfigurationSourceResource) aCS;
    assertEquals ("phase4.properties", aCSR.getResource ().getPath ());
  }
}
