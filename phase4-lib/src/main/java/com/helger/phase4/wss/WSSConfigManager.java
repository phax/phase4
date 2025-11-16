/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.wss;

import java.security.Security;

import org.apache.wss4j.dom.engine.WSSConfig;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * A utility class to handle the life cycle of the {@link WSSConfig} objects.
 *
 * @author Philip Helger
 * @since 0.10.4
 */
public class WSSConfigManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (WSSConfigManager.class);

  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public WSSConfigManager ()
  {}

  @NonNull
  public static WSSConfigManager getInstance ()
  {
    return getGlobalSingleton (WSSConfigManager.class);
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    // init WSSConfig
    final boolean bContainsSTRTransform = Security.getProvider ("STRTransform") != null;
    final boolean bContainsAttachmentContentSignatureTransform = Security.getProvider ("AttachmentContentSignatureTransform") != null;
    final boolean bContainsAttachmentCompleteSignatureTransform = Security.getProvider ("AttachmentCompleteSignatureTransform") != null;
    final boolean bAddJCEProviders;
    if (bContainsSTRTransform &&
      bContainsAttachmentContentSignatureTransform &&
      bContainsAttachmentCompleteSignatureTransform)
    {
      LOGGER.info ("All WSSConfig Security Providers are installed and therefore don't need to be installed again");
      bAddJCEProviders = false;
    }
    else
    {
      // at least one is missing
      bAddJCEProviders = true;
      if (!bContainsSTRTransform &&
        !bContainsAttachmentContentSignatureTransform &&
        !bContainsAttachmentCompleteSignatureTransform)
      {
        // None of them is registered - that is understandable and we're
        // registering them now
        LOGGER.info ("None of the WSSConfig Security Providers is already installed - doing it now");
      }
      else
      {
        LOGGER.warn ("Some of the WSSConfig Security Providers are already installed - replacing them now. STRTransform=" +
                     bContainsSTRTransform +
                     "; AttachmentContentSignatureTransform=" +
                     bContainsAttachmentContentSignatureTransform +
                     "; AttachmentCompleteSignatureTransform=" +
                     bContainsAttachmentCompleteSignatureTransform);
      }
    }
    WSSConfig.setAddJceProviders (bAddJCEProviders);
    WSSConfig.init ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished initializing WSSConfig Security Providers");
  }

  @Override
  protected void onBeforeDestroy (final IScope aScopeToBeDestroyed) throws Exception
  {
    // Cleanup WSSConfig
    LOGGER.info ("Cleaning up WSSConfig." +
                 (WSSConfig.isAddJceProviders () ? " Security Providers will also be removed."
                                                 : " Security Providers were not installed by us."));
    WSSConfig.cleanUp ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished cleaning up WSSConfig");
  }

  @NonNull
  @ReturnsMutableCopy
  public static WSSConfig createStaticWSSConfig ()
  {
    // This should be the only place that creates a new WSSConfig
    // Never call WSSConfig.getNewInstance() in your code manually
    final WSSConfig ret = WSSConfig.getNewInstance ();
    ret.setIdAllocator (new AS4WsuIdAllocator ());
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public WSSConfig createWSSConfig ()
  {
    return createStaticWSSConfig ();
  }
}
