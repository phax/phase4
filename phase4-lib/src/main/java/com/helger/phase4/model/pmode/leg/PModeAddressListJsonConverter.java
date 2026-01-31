/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonValue;
import com.helger.json.JsonArray;

/**
 * JSON converter for {@link PModeAddressList} objects.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class PModeAddressListJsonConverter
{
  private PModeAddressListJsonConverter ()
  {}

  @NonNull
  public static IJsonArray convertToJson (@NonNull final PModeAddressList aValue)
  {
    return new JsonArray ().addAll (aValue.addresses ());
  }

  @NonNull
  public static PModeAddressList convertToNative (@NonNull final IJsonArray aElement)
  {
    final ICommonsList <String> aAddresses = new CommonsArrayList <> ();
    for (final IJsonValue aItem : aElement.iteratorValues ())
    {
      aAddresses.add (aItem.getAsString ());
    }

    return new PModeAddressList (aAddresses);
  }
}
