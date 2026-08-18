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
package com.helger.phase4.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.phase4.AS4TestRule;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;

/**
 * Test class for interface {@link IAS4Profile}.
 *
 * @author Philip Helger
 */
public final class IAS4ProfileTest
{
  @ClassRule
  public static final AS4TestRule RULE = new AS4TestRule ();

  private static final String SERVICE = "https://example.org/service";
  private static final String ACTION = "https://example.org/action";

  /**
   * An {@link IAS4Profile} that only implements the mandatory methods, so that the default
   * implementation of the widened createPModeTemplate is used.
   */
  private static final class MockProfile implements IAS4Profile
  {
    private final String m_sTemplateService;
    private final String m_sTemplateAction;

    MockProfile (@Nullable final String sTemplateService, @Nullable final String sTemplateAction)
    {
      m_sTemplateService = sTemplateService;
      m_sTemplateAction = sTemplateAction;
    }

    @NonNull
    @Nonempty
    public String getID ()
    {
      return "mock";
    }

    @NonNull
    @Nonempty
    public String getDisplayName ()
    {
      return "Mock profile";
    }

    @Nullable
    public IAS4ProfileValidator getValidator ()
    {
      return null;
    }

    @NonNull
    public PMode createPModeTemplate (@NonNull @Nonempty final String sInitiatorID,
                                      @NonNull @Nonempty final String sResponderID,
                                      @Nullable final String sAddress)
    {
      final PModeLegBusinessInformation aBI = PModeLegBusinessInformation.create (m_sTemplateService,
                                                                                  m_sTemplateAction,
                                                                                  null,
                                                                                  null);
      return new PMode ("mock-pmode",
                        new PModeParty (null, sInitiatorID, "initiator", null, null),
                        new PModeParty (null, sResponderID, "responder", null, null),
                        "agreement",
                        EMEP.ONE_WAY,
                        EMEPBinding.PUSH,
                        new PModeLeg (null, aBI, null, null, null),
                        null,
                        null,
                        null);
    }

    @NonNull
    public IPModeIDProvider getPModeIDProvider ()
    {
      return IPModeIDProvider.DEFAULT_DYNAMIC;
    }

    public boolean isDeprecated ()
    {
      return false;
    }

    public boolean isInvokeSPIForPingMessage ()
    {
      return false;
    }
  }

  @Test
  public void testTemplateWithoutServiceAndActionIsFilled ()
  {
    // This is the situation of issue #213 - the template leaves both values open
    final IAS4Profile aProfile = new MockProfile (null, null);

    final PMode aOld = aProfile.createPModeTemplate ("i", "r", "https://example.org/as4");
    assertNull (aOld.getLeg1 ().getBusinessInfo ().getService ());
    assertNull (aOld.getLeg1 ().getBusinessInfo ().getAction ());

    final PMode aNew = aProfile.createPModeTemplate ("i", "r", "https://example.org/as4", SERVICE, ACTION);
    assertEquals (SERVICE, aNew.getLeg1 ().getBusinessInfo ().getService ());
    assertEquals (ACTION, aNew.getLeg1 ().getBusinessInfo ().getAction ());
  }

  @Test
  public void testTemplateValuesAreNotOverwritten ()
  {
    // A template that predefines the values must keep them
    final IAS4Profile aProfile = new MockProfile ("template-service", "template-action");

    final PMode aPMode = aProfile.createPModeTemplate ("i", "r", null, SERVICE, ACTION);
    assertEquals ("template-service", aPMode.getLeg1 ().getBusinessInfo ().getService ());
    assertEquals ("template-action", aPMode.getLeg1 ().getBusinessInfo ().getAction ());
  }

  @Test
  public void testNullServiceAndActionAreIgnored ()
  {
    final IAS4Profile aProfile = new MockProfile (null, null);

    final PMode aPMode = aProfile.createPModeTemplate ("i", "r", null, null, null);
    assertNull (aPMode.getLeg1 ().getBusinessInfo ().getService ());
    assertNull (aPMode.getLeg1 ().getBusinessInfo ().getAction ());
  }
}
