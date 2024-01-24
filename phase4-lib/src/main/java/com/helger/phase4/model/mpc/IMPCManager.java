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
package com.helger.phase4.model.mpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.phase4.CAS4;

/**
 * Interface for an MPC (Message Partition Channel) manager
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IMPCManager
{
  /**
   * Create a new MPC.
   *
   * @param aMPC
   *        The MPC to be added. May not be <code>null</code>.
   */
  void createMPC (@Nonnull MPC aMPC);

  /**
   * Update an existing MPC
   *
   * @param aMPC
   *        The MPC to be updated. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if something changed,
   *         {@link EChange#UNCHANGED} otherwise.
   */
  @Nonnull
  EChange updateMPC (@Nonnull IMPC aMPC);

  /**
   * Mark the MPC with the provided ID as deleted.
   *
   * @param sMPCID
   *        The ID of the MPC to be marked as deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if marking as deleted succeeded,
   *         {@link EChange#UNCHANGED} otherwise.
   */
  @Nonnull
  EChange markMPCDeleted (@Nullable String sMPCID);

  /**
   * Delete the MPC with the provided ID.
   *
   * @param sMPCID
   *        The ID of the MPC to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if deleting succeeded,
   *         {@link EChange#UNCHANGED} otherwise.
   */
  @Nonnull
  EChange deleteMPC (@Nullable String sMPCID);

  /**
   * Get the MPC with the specified ID.
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such MPC exists, the MPC otherwise.
   */
  @Nullable
  IMPC getMPCOfID (@Nullable String sID);

  /**
   * Check if an MPC with the specified ID is contained.
   * 
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>true</code> if such an MPC is contained, <code>false</code>
   *         otherwise.
   */
  boolean containsWithID (@Nullable String sID);

  /**
   * Get the MPC with the specified ID, or the default MPC.
   *
   * @param sID
   *        The ID to search. If it is <code>null</code> or empty, the default
   *        MPC will be used.
   * @return <code>null</code> if no such MPC exists, the MPC otherwise.
   * @see CAS4#DEFAULT_MPC_ID
   */
  @Nullable
  default IMPC getMPCOrDefaultOfID (@Nullable final String sID)
  {
    return getMPCOfID (StringHelper.hasNoText (sID) ? CAS4.DEFAULT_MPC_ID : sID);
  }
}
