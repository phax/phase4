/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.EAS4ProfileSecurityRequirement;
import com.helger.phase4.profile.IAS4Profile;

/**
 * Test class for {@link AS4IncomingHandler#_checkSecurityAgainstProfile}.
 *
 * @author Philip Helger
 */
public final class AS4IncomingHandlerProfileSecurityTest
{
  @NonNull
  private static IAS4IncomingMessageState _createState (final boolean bSigned, final boolean bEncrypted)
  {
    return (IAS4IncomingMessageState) Proxy.newProxyInstance (IAS4IncomingMessageState.class.getClassLoader (),
                                                              new Class <?> [] { IAS4IncomingMessageState.class },
                                                              (aProxy, aMethod, aArgs) -> switch (aMethod.getName ())
                                                              {
                                                                case "isSoapSignatureChecked" -> Boolean.valueOf (bSigned);
                                                                case "isSoapDecrypted" -> Boolean.valueOf (bEncrypted);
                                                                case "getLocale" -> Locale.ROOT;
                                                                case "getMessageID" -> "test@phase4";
                                                                default -> null;
                                                              });
  }

  @NonNull
  private static IAS4Profile _createProfile (@NonNull final EAS4ProfileSecurityRequirement eSign,
                                             @NonNull final EAS4ProfileSecurityRequirement eCrypt)
  {
    return new AS4Profile ("test",
                           "Test",
                           () -> null,
                           (i, r, a) -> null,
                           IPModeIDProvider.DEFAULT_DYNAMIC,
                           false,
                           false,
                           eSign,
                           eCrypt);
  }

  private static int _check (final boolean bSigned,
                             final boolean bEncrypted,
                             @NonNull final EAS4ProfileSecurityRequirement eSign,
                             @NonNull final EAS4ProfileSecurityRequirement eCrypt)
  {
    final AS4ErrorList aErrors = new AS4ErrorList ();
    AS4IncomingHandler._checkSecurityAgainstProfile (_createState (bSigned, bEncrypted),
                                                     _createProfile (eSign, eCrypt),
                                                     aErrors);
    aErrors.forEach (x -> assertEquals (EEbmsError.EBMS_POLICY_NONCOMPLIANCE.getErrorCode (),
                                        x.getEbmsError ().getErrorCode ()));
    return aErrors.size ();
  }

  @Test
  public void testUnspecified ()
  {
    final EAS4ProfileSecurityRequirement eU = EAS4ProfileSecurityRequirement.UNSPECIFIED;
    for (final boolean bSigned : new boolean [] { true, false })
      for (final boolean bEncrypted : new boolean [] { true, false })
        assertEquals (0, _check (bSigned, bEncrypted, eU, eU));

    // The default methods of the interface
    final IAS4Profile aProfile = new AS4Profile ("test",
                                                 "Test",
                                                 () -> null,
                                                 (i, r, a) -> null,
                                                 IPModeIDProvider.DEFAULT_DYNAMIC,
                                                 false,
                                                 false,
                                                 EAS4ProfileSecurityRequirement.UNSPECIFIED,
                                                 EAS4ProfileSecurityRequirement.UNSPECIFIED);
    assertEquals (eU, aProfile.getIncomingSignatureRequirement ());
    assertEquals (eU, aProfile.getIncomingEncryptionRequirement ());
  }

  @Test
  public void testRequired ()
  {
    final EAS4ProfileSecurityRequirement eR = EAS4ProfileSecurityRequirement.REQUIRED;
    assertEquals (0, _check (true, true, eR, eR));
    assertEquals (1, _check (false, true, eR, eR));
    assertEquals (1, _check (true, false, eR, eR));
    assertEquals (2, _check (false, false, eR, eR));
  }

  @Test
  public void testEncryptionForbidden ()
  {
    final EAS4ProfileSecurityRequirement eR = EAS4ProfileSecurityRequirement.REQUIRED;
    final EAS4ProfileSecurityRequirement eF = EAS4ProfileSecurityRequirement.FORBIDDEN;
    assertEquals (0, _check (true, false, eR, eF));
    assertEquals (1, _check (true, true, eR, eF));
    assertEquals (2, _check (false, true, eR, eF));
    assertEquals (0, _check (false, false, eF, eF));
    assertEquals (1, _check (true, false, eF, eF));
  }
}
