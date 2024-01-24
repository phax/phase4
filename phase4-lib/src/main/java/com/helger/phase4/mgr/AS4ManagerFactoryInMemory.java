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
package com.helger.phase4.mgr;

import javax.annotation.Nonnull;

import com.helger.phase4.duplicate.AS4DuplicateManagerInMemory;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.mpc.MPCManagerInMemory;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.model.pmode.PModeManagerInMemory;
import com.helger.phase4.profile.AS4ProfileManager;
import com.helger.phase4.profile.IAS4ProfileManager;

/**
 * Implementation of {@link IAS4ManagerFactory} creating managers that are
 * in-memory only.
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public class AS4ManagerFactoryInMemory implements IAS4ManagerFactory
{
  @Nonnull
  public IMPCManager createMPCManager ()
  {
    return new MPCManagerInMemory ();
  }

  @Nonnull
  public IPModeManager createPModeManager ()
  {
    return new PModeManagerInMemory ();
  }

  @Nonnull
  public IAS4DuplicateManager createDuplicateManager ()
  {
    return new AS4DuplicateManagerInMemory ();
  }

  @Nonnull
  public IAS4ProfileManager createProfileManager ()
  {
    return new AS4ProfileManager ();
  }

  @Nonnull
  public IAS4TimestampManager createTimestampManager ()
  {
    return IAS4TimestampManager.createDefaultInstance ();
  }
}
