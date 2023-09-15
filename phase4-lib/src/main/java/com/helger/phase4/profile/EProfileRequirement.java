/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.phase4.profile;

/**
 * Specifies the requirement level for certain profile features.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
public enum EProfileRequirement
{
  /** It's an absolute requirement */
  MUST,
  /** It's an optional thing - so may or may not be present */
  MAY,
  /** It's forbidden to use it */
  MUST_NOT;

  public boolean requiresExistence ()
  {
    return this == MUST;
  }

  public boolean allowsExistence ()
  {
    return this == MUST || this == MAY;
  }

  public boolean allowsNonExistence ()
  {
    return this == MAY || this == MUST_NOT;
  }

  public boolean forbidsExistence ()
  {
    return this == MUST_NOT;
  }
}
