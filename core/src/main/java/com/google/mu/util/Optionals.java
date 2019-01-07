package com.google.mu.util;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.google.mu.function.CheckedBiConsumer;
import com.google.mu.function.CheckedBiFunction;
import com.google.mu.function.CheckedConsumer;
import com.google.mu.function.CheckedDoubleConsumer;
import com.google.mu.function.CheckedIntConsumer;
import com.google.mu.function.CheckedLongConsumer;

/**
 * Utilities pertaining to {@link Optional}.
 *
 * @since 1.14
 */
public final class Optionals {

  /**
   * Invokes {@code consumer} if {@code optional} is present. Returns a {@code Premise}
   * object to allow {@link Premise#orElse orElse()} and friends to be chained. For example: <pre>
   *   ifPresent(findId(), System.out::print)
   *       .orElse(() -> System.out.print("id not found"));
   * </pre>
   *
   * <p>This method is very similar to JDK {@link OptionalInt#ifPresent} with a few differences: <ol>
   * <li>{@code orElse()} is chained fluently, compared to {@link OptionalInt#ifPresentOrElse}.
   * <li>Propagates checked exceptions from the {@code consumer}.
   * <li>Syntax is consistent across one-Optional and two-Optional {@code ifPresent()} overloads.
   * <li>{@code ifPresent(findId(), System.out::print)} begins the statement with "if", which may read
   *     somewhat closer to regular {@code if} statements.
   * </ol>
   */
  public static <E extends Throwable> Premise ifPresent(
      OptionalInt optional, CheckedIntConsumer<E> consumer) throws E {
    requireNonNull(optional);
    requireNonNull(consumer);
    if (optional.isPresent()) {
      consumer.accept(optional.getAsInt());
      return Conditional.TRUE;
    } else {
      return Conditional.FALSE;
    }
  }

  /**
   * Invokes {@code consumer} if {@code optional} is present. Returns a {@code Premise}
   * object to allow {@link Premise#orElse orElse()} and friends to be chained. For example: <pre>
   *   ifPresent(findId(), System.out::print)
   *       .orElse(() -> System.out.print("id not found"));
   * </pre>
   *
   * <p>This method is very similar to JDK {@link OptionalLong#ifPresent} with a few differences: <ol>
   * <li>{@code orElse()} is chained fluently, compared to {@link OptionalLong#ifPresentOrElse}.
   * <li>Propagates checked exceptions from the {@code consumer}.
   * <li>Syntax is consistent across one-Optional and two-Optional {@code ifPresent()} overloads.
   * <li>{@code ifPresent(findId(), System.out::print)} begins the statement with "if", which may read
   *     somewhat closer to regular {@code if} statements.
   * </ol>
   */
  public static <E extends Throwable> Premise ifPresent(
      OptionalLong optional, CheckedLongConsumer<E> consumer) throws E {
    requireNonNull(optional);
    requireNonNull(consumer);
    if (optional.isPresent()) {
      consumer.accept(optional.getAsLong());
      return Conditional.TRUE;
    } else {
      return Conditional.FALSE;
    }
  }

  /**
   * Invokes {@code consumer} if {@code optional} is present. Returns a {@code Premise}
   * object to allow {@link Premise#orElse orElse()} and friends to be chained. For example: <pre>
   *   ifPresent(findMileage(), System.out::print)
   *       .orElse(() -> System.out.print("id mileage found"));
   * </pre>
   *
   * <p>This method is very similar to JDK {@link OptionalDouble#ifPresent} with a few differences: <ol>
   * <li>{@code orElse()} is chained fluently, compared to {@link OptionalDouble#ifPresentOrElse}.
   * <li>Propagates checked exceptions from the {@code consumer}.
   * <li>Syntax is consistent across one-Optional and two-Optional {@code ifPresent()} overloads.
   * <li>{@code ifPresent(findMileage(), System.out::print)} begins the statement with "if", which may read
   *     somewhat closer to regular {@code if} statements.
   * </ol>
   */
  public static <E extends Throwable> Premise ifPresent(
      OptionalDouble optional, CheckedDoubleConsumer<E> consumer) throws E {
    requireNonNull(optional);
    requireNonNull(consumer);
    if (optional.isPresent()) {
      consumer.accept(optional.getAsDouble());
      return Conditional.TRUE;
    } else {
      return Conditional.FALSE;
    }
  }

  /**
   * Invokes {@code consumer} if {@code optional} is present. Returns a {@code Premise}
   * object to allow {@link Premise#orElse orElse()} and friends to be chained. For example: <pre>
   *   ifPresent(findStory(), Story::tell)
   *       .orElse(() -> print("no story"));
   * </pre>
   *
   * <p>This method is very similar to JDK {@link Optional#ifPresent} with a few differences: <ol>
   * <li>{@code orElse()} is chained fluently, compared to {@link Optional#ifPresentOrElse}.
   * <li>Propagates checked exceptions from the {@code consumer}.
   * <li>Syntax is consistent across one-Optional and two-Optional {@code ifPresent()} overloads.
   * <li>{@code ifPresent(findStory(), Story::tell)} begins the statement with "if", which may read
   *     somewhat closer to regular {@code if} statements.
   * </ol>
   */
  public static <T, E extends Throwable> Premise ifPresent(
      Optional<T> optional, CheckedConsumer<? super T, E> consumer) throws E {
    requireNonNull(optional);
    requireNonNull(consumer);
    if (optional.isPresent()) {
      consumer.accept(optional.get());
      return Conditional.TRUE;
    } else {
      return Conditional.FALSE;
    }
  }

  /**
   * Invokes {@code consumer} if both {@code left} and {@code right} are present. Returns a {@code Premise}
   * object to allow {@link Premise#orElse orElse()} and friends to be chained. For example: <pre>
   *   ifPresent(when, where, Story::tell)
   *       .orElse(() -> print("no story"));
   * </pre>
   */
  public static <A, B, E extends Throwable> Premise ifPresent(
      Optional<A> left, Optional<B> right, CheckedBiConsumer<? super A, ? super B, E> consumer)
      throws E {
    requireNonNull(left);
    requireNonNull(right);
    requireNonNull(consumer);
    if (left.isPresent() && right.isPresent()) {
      consumer.accept(left.get(), right.get());
      return Conditional.TRUE;
    } else {
      return Conditional.FALSE;
    }
  }

  /**
   * Maps {@code left} and {@code right} using {@code mapper} if both are present.
   * Returns an {@link Optional} wrapping the result of {@code mapper} if non-null, or else returns
   * {@code Optional.empty()}.
   */
  public static <A, B, R, E extends Throwable> Optional<R> map(
      Optional<A> left, Optional<B> right, CheckedBiFunction<? super A, ? super B, ? extends R, E> mapper)
      throws E {
    requireNonNull(left);
    requireNonNull(right);
    requireNonNull(mapper);
    if (left.isPresent() && right.isPresent()) {
      return Optional.ofNullable(mapper.apply(left.get(), right.get()));
    }
    return Optional.empty();
  }

  /**
   * Maps {@code left} and {@code right} using {@code mapper} if both are present.
   * Returns the result of {@code mapper} or {@code Optional.empty()} if either {@code left} or {@code right}
   * is empty.
   *
   * @throws NullPointerException if {@code mapper} returns null
   */
  public static <A, B, R, E extends Throwable> Optional<R> flatMap(
      Optional<A> left, Optional<B> right,
      CheckedBiFunction<? super A, ? super B, ? extends Optional<R>, E> mapper)
      throws E {
    requireNonNull(left);
    requireNonNull(right);
    requireNonNull(mapper);
    if (left.isPresent() && right.isPresent()) {
      return requireNonNull(mapper.apply(left.get(), right.get()));
    }
    return Optional.empty();
  }
  
  private Optionals() {}
}
