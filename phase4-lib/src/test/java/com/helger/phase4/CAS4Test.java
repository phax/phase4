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
package com.helger.phase4;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link CAS4}
 *
 * @author Philip Helger
 */
public final class CAS4Test
{
  @Test
  public void testBasic ()
  {
    assertTrue (CAS4.XSD_EBMS_HEADER.exists ());
    assertTrue (CAS4.XSD_EBBP_SIGNALS.exists ());
    assertTrue (CAS4.XSD_SOAP11.exists ());
    assertTrue (CAS4.XSD_SOAP12.exists ());
  }
}
