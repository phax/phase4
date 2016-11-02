/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as4lib.util;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.attr.MapBasedAttributeContainer;
import com.helger.commons.state.EChange;

/**
 * Base class for all kind of string-string mapping container. This
 * implementation is not thread-safe!
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class StringMap extends MapBasedAttributeContainer <String, String> implements IStringMap
{
  public StringMap ()
  {
    super ();
  }

  public StringMap (@Nonnull final Map <String, String> aMap)
  {
    super (aMap);
  }

  public StringMap (@Nonnull final IStringMap aCont)
  {
    super (aCont);
  }

  @Nonnull
  public EChange setAttribute (@Nonnull final String sName, final boolean bValue)
  {
    return setAttribute (sName, Boolean.toString (bValue));
  }

  @Nonnull
  public EChange setAttribute (@Nonnull final String sName, final int nValue)
  {
    return setAttribute (sName, Integer.toString (nValue));
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public StringMap getClone ()
  {
    return new StringMap (this);
  }
}
