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
package com.helger.phase4.server.spi;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.incoming.spi.IAS4IncomingPullRequestProcessorSPI;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.test.profile.TestPMode;

@IsSPIImplementation
public class MockPullRequestProcessorSPI implements IAS4IncomingPullRequestProcessorSPI
{
  @Nonnull
  public IPMode findPMode (@Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    final PMode aPMode = TestPMode.createTestPMode ("pullinitiator",
                                                    "pullresponder",
                                                    "http://test.address.non/existing",
                                                    (i, r) -> "PullPMode",
                                                    false);
    if (aSignalMessage.getPullRequest () != null)
    {
      if (!aSignalMessage.getAny ().isEmpty ())
      {
        final Element aElement = (Element) aSignalMessage.getAnyAtIndex (0);
        if (aElement.getTextContent ().contains ("pushPull"))
        {
          aPMode.setMEPBinding (EMEPBinding.PUSH_PULL);
        }
        else
          if (aElement.getTextContent ().contains ("pullPush"))
          {
            aPMode.setMEPBinding (EMEPBinding.PULL_PUSH);
          }
      }
      else
      {
        aPMode.setMEPBinding (EMEPBinding.PULL);
      }
    }
    return aPMode;
  }
}
