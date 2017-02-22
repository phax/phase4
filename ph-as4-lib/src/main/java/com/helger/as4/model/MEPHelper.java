package com.helger.as4.model;

import javax.annotation.Nonnull;

import com.helger.as4.messaging.domain.EAS4MessageType;

public final class MEPHelper
{
  public static boolean isValidResponseType (@Nonnull final EMEP eMEP,
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
          // TODO subject to change if we implement pull
          case PUSH:
            return eMsgType.isReceiptOrError ();
          case PULL:
            return eMsgType.isUserMessage ();
          case SYNC:
            return eMsgType.isUserMessage ();
          case PUSH_PUSH:
            return eMsgType.isUserMessage ();
          case PUSH_PULL:
            return eMsgType.isUserMessage ();
          case PULL_PUSH:
            return eMsgType.isUserMessage ();
        }
        break;
      }
    }
    throw new IllegalStateException ("Unhandled combination: " + eMEP + "/" + eMEPBinding + "/" + eMsgType);
  }
}
