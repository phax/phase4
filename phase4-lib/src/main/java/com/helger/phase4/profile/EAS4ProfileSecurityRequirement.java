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
package com.helger.phase4.profile;

/**
 * Defines whether an AS4 profile requires or forbids a certain security feature (signature or
 * encryption) on incoming UserMessages. The requirement is checked independent of the PMode leg
 * security settings and cannot be disabled via configuration.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public enum EAS4ProfileSecurityRequirement
{
  /** The security feature must be used */
  REQUIRED,
  /** The security feature must not be used */
  FORBIDDEN,
  /** The profile makes no statement - only the PMode leg settings apply */
  UNSPECIFIED;

  /**
   * @return <code>true</code> if this is {@link #REQUIRED}, <code>false</code> otherwise.
   */
  public boolean isRequired ()
  {
    return this == REQUIRED;
  }

  /**
   * @return <code>true</code> if this is {@link #FORBIDDEN}, <code>false</code> otherwise.
   */
  public boolean isForbidden ()
  {
    return this == FORBIDDEN;
  }
}
