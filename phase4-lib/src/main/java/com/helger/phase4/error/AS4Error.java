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
package com.helger.phase4.error;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.base.builder.IBuilder;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.model.error.Ebms3ErrorBuilder;

/**
 * Special AS4 error summarizing the EbmsError and the HTTP status code.
 *
 * @author Philip Helger
 * @since 4.2.0
 */
public class AS4Error
{
  private @NonNull final Ebms3Error m_aEbmsError;
  private final int m_nHttpStatusCode;

  protected AS4Error (@NonNull final Ebms3Error aEbmsError, final int nHttpStatusCode)
  {
    m_aEbmsError = aEbmsError;
    m_nHttpStatusCode = nHttpStatusCode;
  }

  /**
   * @return The contained EBMS error. Never <code>null</code>.
   */
  @NonNull
  public final Ebms3Error getEbmsError ()
  {
    return m_aEbmsError;
  }

  /**
   * @return The HTTP Status Code to be returned. All values &le; 0 are interpreted as "no change".
   *         The default value is generally 200 (HTTP OK).
   */
  @CheckForSigned
  public final int getHttpStatusCode ()
  {
    return m_nHttpStatusCode;
  }

  /**
   * @return <code>true</code> of the contained status code seems to be valid, <code>false</code> if
   *         not.
   */
  public final boolean hasHttpStatusCode ()
  {
    return m_nHttpStatusCode > 0;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !o.getClass ().equals (getClass ()))
      return false;
    final AS4Error rhs = (AS4Error) o;
    return m_aEbmsError.equals (rhs.m_aEbmsError) && m_nHttpStatusCode == rhs.m_nHttpStatusCode;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aEbmsError).append (m_nHttpStatusCode).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("EbmsError", m_aEbmsError)
                                       .append ("HttpStatusCode", m_nHttpStatusCode)
                                       .getToString ();
  }

  @NonNull
  public static AS4ErrorBuilder builder ()
  {
    return new AS4ErrorBuilder ();
  }

  /**
   * Builder class for {@link AS4Error} objects.
   *
   * @author Philip Helger
   * @since 4.2.0
   */
  public static class AS4ErrorBuilder implements IBuilder <@NonNull AS4Error>
  {
    public static final int DEFAULT_HTTP_STATUS_CODE = -1;
    private @Nullable Ebms3Error m_aEbmsError;
    private @CheckForSigned int m_nHttpStatusCode = DEFAULT_HTTP_STATUS_CODE;

    public AS4ErrorBuilder ()
    {}

    @NonNull
    public AS4ErrorBuilder ebmsError (@Nullable final Ebms3ErrorBuilder a)
    {
      return ebmsError (a == null ? null : a.build ());
    }

    @NonNull
    public AS4ErrorBuilder ebmsError (@Nullable final Ebms3Error a)
    {
      m_aEbmsError = a;
      return this;
    }

    @NonNull
    public AS4ErrorBuilder httpStatusCode (final int n)
    {
      m_nHttpStatusCode = n;
      return this;
    }

    public @NonNull AS4Error build ()
    {
      if (m_aEbmsError == null)
        throw new IllegalStateException ("The ebmsError part is missing");

      return new AS4Error (m_aEbmsError, m_nHttpStatusCode);
    }
  }
}
