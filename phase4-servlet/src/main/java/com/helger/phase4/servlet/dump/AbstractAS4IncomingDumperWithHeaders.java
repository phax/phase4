/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet.dump;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.phase4.dump.IAS4IncomingDumper;

/**
 * Abstract version of {@link IAS4IncomingDumper} that emits all headers on the
 * output stream.
 *
 * @author Philip Helger
 * @since 0.9.7
 */
public abstract class AbstractAS4IncomingDumperWithHeaders implements IAS4IncomingDumper
{
  @Nullable
  protected abstract OutputStream openOutputStream (@Nonnull final HttpHeaderMap aHttpHeaderMap) throws IOException;

  @Nullable
  public OutputStream onNewRequest (@Nonnull final HttpHeaderMap aHttpHeaderMap) throws IOException
  {
    final OutputStream ret = openOutputStream (aHttpHeaderMap);
    if (ret != null && aHttpHeaderMap.isNotEmpty ())
    {
      // At least one header is contained
      for (final Map.Entry <String, ICommonsList <String>> aEntry : aHttpHeaderMap)
      {
        final String sKey = aEntry.getKey ();
        for (final String sValue : aEntry.getValue ())
          ret.write ((sKey + HttpHeaderMap.SEPARATOR_KEY_VALUE + sValue + CHttp.EOL).getBytes (CHttp.HTTP_CHARSET));
      }
      ret.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
    return ret;
  }
}
