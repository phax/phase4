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
package com.helger.phase4.model;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.phase4.model.message.EAS4MessageType;

/**
 * Message Exchange Pattern helper
 *
 * @author Philip Helger
 */
@Immutable
public final class MEPHelper
{
  private MEPHelper ()
  {}

  private static boolean _isValidResponseType (@Nonnull final EMEP eMEP,
                                               @Nonnull final EMEPBinding eMEPBinding,
                                               @Nonnull final EAS4MessageType eMsgType,
                                               final boolean bLeg1)
  {
    switch (eMEP)
    {
      case ONE_WAY:
      {
        assert bLeg1;
        switch (eMEPBinding)
        {
          case PUSH:
            return eMsgType.isReceiptOrError ();
          case PULL:
            return eMsgType.isUserMessage ();
          case SYNC:
          case PUSH_PUSH:
          case PUSH_PULL:
          case PULL_PUSH:
            // pull and sync cannot be used with a one-way protocol
            // push&|pull also not
            return false;
        }
        break;
      }
      case TWO_WAY:
      {
        switch (eMEPBinding)
        {
          case PUSH:
            assert bLeg1;
            return eMsgType.isReceiptOrError ();
          case PULL:
            assert bLeg1;
            return eMsgType.isUserMessage ();
          case SYNC:
            return eMsgType.isUserMessage ();
          case PUSH_PUSH:
          case PUSH_PULL:
            if (bLeg1)
              return eMsgType.isReceiptOrError ();
            return eMsgType.isUserMessage ();
          case PULL_PUSH:
            if (bLeg1)
              return eMsgType.isUserMessage ();
            return eMsgType.isReceiptOrError ();
        }
        break;
      }
    }
    throw new IllegalStateException ("Unhandled combination: " + eMEP + "/" + eMEPBinding + "/" + eMsgType);
  }

  public static boolean isValidResponseTypeLeg1 (@Nonnull final EMEP eMEP,
                                                 @Nonnull final EMEPBinding eMEPBinding,
                                                 @Nonnull final EAS4MessageType eMsgType)
  {
    return _isValidResponseType (eMEP, eMEPBinding, eMsgType, true);
  }

  public static boolean isValidResponseTypeLeg2 (@Nonnull final EMEP eMEP,
                                                 @Nonnull final EMEPBinding eMEPBinding,
                                                 @Nonnull final EAS4MessageType eMsgType)
  {
    return _isValidResponseType (eMEP, eMEPBinding, eMsgType, false);
  }
}
