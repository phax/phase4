/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.server.spi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.incoming.spi.IAS4IncomingSignalMessagePModeProviderSPI;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.server.MockPModeGenerator;

/**
 * Mock implementation of {@link IAS4IncomingSignalMessagePModeProviderSPI}. It only resolves a
 * PMode if the <code>RefToMessageId</code> starts with {@link #KNOWN_REF_TO_MESSAGE_ID_PREFIX}, so
 * that the same test class can also verify the unchanged "no PMode found" behaviour.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MockSignalMessagePModeProviderSPI implements IAS4IncomingSignalMessagePModeProviderSPI
{
  /** Only signal messages referencing this prefix get a PMode */
  public static final String KNOWN_REF_TO_MESSAGE_ID_PREFIX = "phase4-mock-known-";

  @UsedViaReflection
  public MockSignalMessagePModeProviderSPI ()
  {}

  @Nullable
  public IPMode findPMode (@NonNull final Ebms3SignalMessage aSignalMessage)
  {
    if (aSignalMessage.getMessageInfo () == null)
      return null;

    final String sRefToMessageID = aSignalMessage.getMessageInfo ().getRefToMessageId ();
    if (sRefToMessageID == null || !sRefToMessageID.startsWith (KNOWN_REF_TO_MESSAGE_ID_PREFIX))
      return null;

    return MockPModeGenerator.getTestPModeWithSecurity (ESoapVersion.SOAP_12);
  }
}
