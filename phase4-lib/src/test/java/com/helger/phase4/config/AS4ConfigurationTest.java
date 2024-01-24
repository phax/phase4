/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    final ConfiguredValue aCV = AS4Configuration.getConfig ().getConfiguredValue (AS4Configuration.PROPERTY_PHASE4_WSS4J_SYNCSECURITY);
    assertNotNull (aCV);

    final IConfigurationSource aCS = aCV.getConfigurationSource ();
    assertNotNull (aCS);
    assertTrue (aCS instanceof IConfigurationSourceResource);
    final IConfigurationSourceResource aCSR = (IConfigurationSourceResource) aCS;
    assertEquals ("phase4.properties", aCSR.getResource ().getPath ());
  }
}
