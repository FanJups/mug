/*****************************************************************************
 * Copyright (C) google.com                                                *
 * ------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");           *
 * you may not use this file except in compliance with the License.          *
 * You may obtain a copy of the License at                                   *
 *                                                                           *
 * http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing, software       *
 * distributed under the License is distributed on an "AS IS" BASIS,         *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 * See the License for the specific language governing permissions and       *
 * limitations under the License.                                            *
 *****************************************************************************/
package com.google.mu.function;

import static java.util.Objects.requireNonNull;

/** A binary function that can throw checked exceptions. */
@FunctionalInterface
public interface CheckedBiFunction<A, B, T, E extends Throwable> {
  T apply(A a, B b) throws E;

  /**
   * Returns a new {@code CheckedBiFunction} that maps the return value using {@code mapper}.
   * For example: {@code ((a, b) -> a + b).andThen(Object::toString).apply(1, 2) => "3"}.
   */
  default <R> CheckedBiFunction<A, B, R, E> andThen(
      CheckedFunction<? super T, ? extends R, ? extends E> mapper) {
    requireNonNull(mapper);
    return (a, b) -> mapper.apply(apply(a, b));
  }
}
