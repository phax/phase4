/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.error;

import javax.annotation.Nullable;

import com.helger.as4lib.ebms3header.Ebms3Description;

public interface IMutableEbmsError extends IEbmsError
{
  /**
   * Sets the value of the description property.
   *
   * @param value
   *        allowed object is {@link Ebms3Description }
   */
  void setDescription (@Nullable Ebms3Description value);

  /**
   * Sets the value of the errorDetail property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setErrorDetail (@Nullable String value);

  /**
   * Sets the value of the category property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setCategory (@Nullable String value);

  /**
   * Sets the value of the refToMessageInError property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setRefToMessageInError (@Nullable String value);

  /**
   * Sets the value of the errorCode property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setErrorCode (@Nullable String value);

  /**
   * Sets the value of the origin property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setOrigin (@Nullable String value);

  /**
   * Sets the value of the severity property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setSeverity (@Nullable String value);

  /**
   * Sets the value of the shortDescription property.
   *
   * @param value
   *        allowed object is {@link String }
   */
  void setShortDescription (@Nullable String value);
}
