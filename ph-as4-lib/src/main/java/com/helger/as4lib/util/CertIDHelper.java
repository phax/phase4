/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.util;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;

public class CertIDHelper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (CertIDHelper.class);

  @Nullable
  public static String getClientUniqueID (@Nonnull final X509Certificate aCert)
  {
    try
    {
      // subject principal name must be in the order CN=XX,O=YY,C=ZZ
      // In some JDK versions it is O=YY,CN=XX,C=ZZ instead (e.g. 1.6.0_45)
      final LdapName aLdapName = new LdapName (aCert.getSubjectX500Principal ().getName ());

      // Make a map from type to name
      final ICommonsMap <String, Rdn> aParts = new CommonsHashMap<> ();
      for (final Rdn aRdn : aLdapName.getRdns ())
        aParts.put (aRdn.getType (), aRdn);

      if (false)
        return Rdn.escapeValue (aParts.get ("CN").getValue ());

      // Re-order - least important item comes first (=reverse order)!
      final String sCommonName = new LdapName (new CommonsArrayList<> (aParts.get ("CN"))).toString ();

      // subject-name
      return sCommonName;
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Failed to parse '" + aCert.getSubjectX500Principal ().getName () + "'", ex);
      return null;
    }
  }

}
