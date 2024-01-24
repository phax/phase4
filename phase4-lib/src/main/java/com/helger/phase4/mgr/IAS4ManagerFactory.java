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

import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.model.mpc.IMPCManager;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.profile.IAS4ProfileManager;
import com.helger.phase4.util.Phase4Exception;

/**
 * Factory for global managers
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IAS4ManagerFactory
{
  /**
   * @return A new {@link IMPCManager} instance
   * @throws Phase4Exception
   *         on error
   */
  @Nonnull
  IMPCManager createMPCManager () throws Phase4Exception;

  /**
   * @return A new {@link IPModeManager} instance
   * @throws Phase4Exception
   *         on error
   */
  @Nonnull
  IPModeManager createPModeManager () throws Phase4Exception;

  /**
   * @return A new {@link IAS4DuplicateManager} instance
   * @throws Phase4Exception
   *         on error
   */
  @Nonnull
  IAS4DuplicateManager createDuplicateManager () throws Phase4Exception;

  /**
   * @return A new {@link IAS4ProfileManager} instance
   * @throws Phase4Exception
   *         on error
   * @since 0.10.4
   */
  @Nonnull
  IAS4ProfileManager createProfileManager () throws Phase4Exception;

  /**
   * @return A new {@link IAS4TimestampManager} instance
   */
  @Nonnull
  IAS4TimestampManager createTimestampManager ();
}
