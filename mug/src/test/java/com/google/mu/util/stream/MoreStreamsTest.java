/*****************************************************************************
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
package com.google.mu.util.stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.mu.util.stream.BiStream.groupingValuesFrom;
import static com.google.mu.util.stream.MoreStreams.indexesFrom;
import static com.google.mu.util.stream.MoreStreams.toListAndThen;
import static com.google.mu.util.stream.MoreStreams.uniqueKeys;
import static com.google.mu.util.stream.MoreStreams.whileNotEmpty;
import static com.google.mu.util.stream.MoreStreams.whileNotNull;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.NullPointerTester;

@RunWith(JUnit4.class)
public class MoreStreamsTest {
  @Test public void generateSingleElementStream() {
    assertThat(MoreStreams.generate(1, x -> Stream.empty()).collect(toList()))
        .containsExactly(1);
  }

  @Test public void generate_functionReturnsNull() {
    assertThat(MoreStreams.generate(1, x -> null).collect(toList()))
        .containsExactly(1);
  }

  @Test public void generateFanIn() throws Exception {
    assertThat(MoreStreams.generate(100, i -> IntStream.rangeClosed(1, i / 10).boxed())
            .collect(toList()))
        .containsExactly(100, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 1);
  }

  @Test public void generateInfiniteStream() throws Exception {
    Stream<Integer> generated = MoreStreams.generate(1, i -> Stream.of(i + 1));
    assertThat(generated.limit(10).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  @Test public void generateInfiniteStreamWithGuavaIterablesLimit() throws Exception {
    Stream<Integer> generated = MoreStreams.generate(1, i -> Stream.of(i + 1));
    assertThat(Iterables.limit(MoreStreams.iterateOnce(generated), 5))
        .containsExactly(1, 2, 3, 4, 5);
  }

  @Test public void flattenEmptyStream() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.empty());
    assertThat(flattened.collect(toList())).isEmpty();
  }

  @Test public void flattenEmptyInnerStream() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.empty()));
    assertThat(flattened.collect(toList())).isEmpty();
  }

  @Test public void flattenSingleElement() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.of(1)));
    assertThat(flattened.collect(toList())).containsExactly(1);
  }

  @Test public void flattenTwoElementsFromSingleBlock() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.of(1, 2)));
    assertThat(flattened.collect(toList())).containsExactly(1, 2).inOrder();
  }

  @Test public void flattenTwoElementsFromTwoBlocks() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.of(1), Stream.of(2)));
    assertThat(flattened.collect(toList())).containsExactly(1, 2).inOrder();
  }

  @Test public void flattenMultipleElements() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.of(1, 2), Stream.of(3, 4)));
    assertThat(flattened.collect(toList())).containsExactly(1, 2, 3, 4).inOrder();
  }

  @Test public void flattenWithTrailingInfiniteStream() throws Exception {
    Stream<Integer> flattened =
        MoreStreams.flatten(Stream.of(Stream.of(1), Stream.iterate(2, i -> i + 1)));
    assertThat(flattened.limit(5).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5).inOrder();
  }

  @Test public void flattenWithLeadingInfiniteStream() throws Exception {
    Stream<Integer> flattened =
        MoreStreams.flatten(Stream.of(Stream.iterate(1, i -> i + 1), Stream.of(100)));
    assertThat(flattened.limit(5).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5).inOrder();
  }

  @Test public void flattenWithInfiniteOuterStream() throws Exception {
    Stream<List<Integer>> infinite = Stream.iterate(
        ImmutableList.of(1), l -> l.stream().map(i -> i + 1).collect(toImmutableList()));
    Stream<Integer> flattened = MoreStreams.flatten(infinite.map(l -> l.stream()));
    assertThat(flattened.limit(5).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5).inOrder();
  }

  @Test public void flattenReturnsSequentialStream() throws Exception {
    Stream<Integer> flattened = MoreStreams.flatten(Stream.of(Stream.of(1, 2), Stream.of(3)).parallel());
    assertThat(flattened.isParallel()).isFalse();
  }

  @Test public void flattenWithLeadingInfiniteStream_runInParallel() throws Exception {
    Stream<Integer> flattened =
        MoreStreams.flatten(Stream.of(Stream.iterate(1, i -> i + 1), Stream.of(100)));
    assertThat(flattened.parallel().limit(1000).collect(toList()))
        .hasSize(1000);
  }

  @Test public void diceParallelStream() {
    assertThat(MoreStreams.dice(IntStream.range(1, 8).boxed().parallel(), 2)
            .flatMap(List::stream).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5, 6, 7);
    assertThat(MoreStreams.dice(IntStream.range(1, 6).boxed(), 2).parallel()
        .flatMap(List::stream).collect(toList()))
    .containsExactly(1, 2, 3, 4, 5);
  }

  @Test public void diceSpliteratorIsNonNull() {
    Spliterator<?> spliterator = asList(1).spliterator();
    assertThat(spliterator.hasCharacteristics(Spliterator.NONNULL)).isFalse();
    assertThat(MoreStreams.dice(spliterator, 2).hasCharacteristics(Spliterator.NONNULL)).isTrue();
  }

  @Test public void diceSpliteratorIsNotSized() {
    Spliterator<?> spliterator = asList(1).spliterator();
    assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isTrue();
    assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(1);
    assertThat(MoreStreams.dice(spliterator, 2).hasCharacteristics(Spliterator.SIZED)).isFalse();
    assertThat(MoreStreams.dice(spliterator, 2).getExactSizeIfKnown()).isEqualTo(-1);
    assertThat(MoreStreams.dice(spliterator, 2).estimateSize()).isEqualTo(1);
    assertThat(MoreStreams.dice(asList(1, 2, 3).spliterator(), 2).estimateSize()).isEqualTo(2);
    assertThat(MoreStreams.dice(asList(1, 2, 3, 4, 5, 6).spliterator(), 2).estimateSize())
        .isEqualTo(3);
  }

  @Test public void diceSpliteratorIsNotSubsized() {
    Spliterator<?> spliterator = asList(1).spliterator();
    assertThat(spliterator.hasCharacteristics(Spliterator.SUBSIZED)).isTrue();
    assertThat(MoreStreams.dice(spliterator, 2).hasCharacteristics(Spliterator.SUBSIZED))
        .isFalse();
  }

  @Test public void diceSpliteratorIsNotSorted() {
    Spliterator<?> spliterator = new TreeSet<>(asList(1)).spliterator();
    assertThat(spliterator.hasCharacteristics(Spliterator.SORTED)).isTrue();
    assertThat(spliterator.getComparator()).isNull();
    assertThat(MoreStreams.dice(spliterator, 2).hasCharacteristics(Spliterator.SORTED)).isFalse();
    assertThrows(
        IllegalStateException.class, () -> MoreStreams.dice(spliterator, 2).getComparator());
  }

  @Test public void nullElementsAreOk() {
    assertThat(MoreStreams.dice(asList(null, null).stream(), 2).collect(toList()))
        .containsExactly(asList(null, null));
  }

  @Test public void maxSizeIsLargerThanDataSize() {
    assertThat(MoreStreams.dice(asList(1, 2).stream(), Integer.MAX_VALUE).collect(toList()))
        .containsExactly(asList(1, 2));
  }

  @Test public void largeMaxSizeWithUnknownSize() {
    assumeTrue(Stream.generate(() -> 1).limit(3).spliterator().getExactSizeIfKnown() == -1);
    assertThat(MoreStreams.dice(Stream.generate(() -> 1).limit(3), Integer.MAX_VALUE).
            limit(3).collect(toList()))
        .containsExactly(asList(1, 1, 1));
  }

  @Test public void invalidMaxSize() {
    assertThrows(IllegalArgumentException.class, () -> MoreStreams.dice(asList(1).stream(), -1));
    assertThrows(IllegalArgumentException.class, () -> MoreStreams.dice(asList(1).stream(), 0));
  }

  @Test public void testThrough() {
    List<String> to = new ArrayList<>();
    MoreStreams.iterateThrough(Stream.of(1, 2).map(Object::toString), to::add);
    assertThat(to).containsExactly("1", "2");
  }

  @Test public void mergingValues_emptyMaps() {
    ImmutableList<Translation> translations =
        ImmutableList.of(new Translation(ImmutableMap.of()), new Translation(ImmutableMap.of()));
    Map<Integer, String> merged = translations.stream()
        .map(Translation::dictionary)
        .collect(groupingValuesFrom(Map::entrySet, (a, b) -> a + "," + b))
        .collect(ImmutableMap::toImmutableMap);
    assertThat(merged).isEmpty();
  }

  @Test public void mergingValues_uniqueKeys() {
    ImmutableList<Translation> translations = ImmutableList.of(
        new Translation(ImmutableMap.of(1, "one")), new Translation(ImmutableMap.of(2, "two")));
    Map<Integer, String> merged = translations.stream()
        .map(Translation::dictionary)
        .collect(groupingValuesFrom(Map::entrySet, (a, b) -> a + "," + b))
        .collect(ImmutableMap::toImmutableMap);
    assertThat(merged)
        .containsExactly(1, "one", 2, "two")
        .inOrder();
  }

  @Test public void mergingValues_duplicateValues() {
    ImmutableList<Translation> translations = ImmutableList.of(
        new Translation(ImmutableMap.of(1, "one")),
        new Translation(ImmutableMap.of(2, "two", 1, "1")));
    Map<Integer, String> merged = translations.stream()
        .map(Translation::dictionary)
        .collect(groupingValuesFrom(Map::entrySet, (a, b) -> a + "," + b))
        .collect(ImmutableMap::toImmutableMap);
    assertThat(merged)
        .containsExactly(1, "one,1", 2, "two")
        .inOrder();
  }

  @Test public void uniqueKeys_emptyMaps() {
    ImmutableList<Translation> translations =
        ImmutableList.of(new Translation(ImmutableMap.of()), new Translation(ImmutableMap.of()));
    Map<Integer, String> merged = translations.stream()
        .map(Translation::dictionary)
        .collect(uniqueKeys());
    assertThat(merged).isEmpty();
  }

  @Test public void uniqueKeys_unique() {
    ImmutableList<Translation> translations = ImmutableList.of(
        new Translation(ImmutableMap.of(1, "one")), new Translation(ImmutableMap.of(2, "two")));
    Map<Integer, String> merged = translations.stream()
        .map(Translation::dictionary)
        .collect(uniqueKeys());
    assertThat(merged)
        .containsExactly(1, "one", 2, "two")
        .inOrder();
  }

  @Test public void uniqueKeys_withDuplicates() {
    ImmutableList<Translation> translations = ImmutableList.of(
        new Translation(ImmutableMap.of(1, "one")),
        new Translation(ImmutableMap.of(2, "two", 1, "1")));
    assertThrows(
        IllegalStateException.class,
        () -> translations.stream()
            .map(Translation::dictionary)
            .collect(uniqueKeys()));
  }

  @Test public void testIndexesFrom() {
    assertThat(indexesFrom(1).limit(3)).containsExactly(1, 2, 3).inOrder();
    assertThat(indexesFrom(Integer.MAX_VALUE).limit(3))
        .containsExactly(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1).inOrder();
  }

  @Test public void removingFromQueue_empty() {
    Queue<String> queue = new ArrayDeque<>();
    assertThat(whileNotEmpty(queue).map(Queue::remove)).isEmpty();
    assertThat(queue).isEmpty();
  }

  @Test public void removingFromQueue_noConcurrentModification() {
    Queue<String> queue = new ArrayDeque<>();
    queue.add("one");
    queue.add("two");
    assertThat(whileNotEmpty(queue).map(Queue::remove))
        .containsExactly("one", "two").inOrder();
    assertThat(queue).isEmpty();
  }

  @Test public void removingFromQueue_modificationUnderneath() {
    Queue<String> queue = new ArrayDeque<>();
    Stream<String> stream = whileNotEmpty(queue).map(Queue::remove);
    queue.add("one");
    queue.add("two");
    assertThat(stream).containsExactly("one", "two").inOrder();
    assertThat(queue).isEmpty();
  }

  @Test public void removingFromQueue_modificationWhileStreaming() {
    Queue<String> queue = new ArrayDeque<>();
    queue.add("one");
    assertThat(
            whileNotEmpty(queue)
                .map(Queue::remove)
                .peek(v -> {
                  if (v.equals("one")) queue.add("two");
                }))
        .containsExactly("one", "two").inOrder();
    assertThat(queue).isEmpty();
  }

  @Test public void poppingFromStack_empty() {
    Deque<String> stack = new ArrayDeque<>();
    assertThat(whileNotEmpty(stack).map(Deque::pop)).isEmpty();
    assertThat(stack).isEmpty();
  }

  @Test public void poppingFromStack_noConcurrentModification() {
    Deque<String> stack = new ArrayDeque<>();
    stack.push("one");
    stack.push("two");
    assertThat(whileNotEmpty(stack).map(Deque::pop))
        .containsExactly("two", "one").inOrder();
    assertThat(stack).isEmpty();
  }

  @Test public void poppingFromStack_modificationUnderneath() {
    Deque<String> stack = new ArrayDeque<>();
    Stream<String> stream = whileNotEmpty(stack).map(Deque::pop);
    stack.push("one");
    stack.push("two");
    assertThat(stream).containsExactly("two", "one").inOrder();
    assertThat(stack).isEmpty();
  }

  @Test public void poppingFromStack_modificationWhileStreaming() {
    Deque<String> stack = new ArrayDeque<>();
    stack.push("one");
    assertThat(
            whileNotEmpty(stack)
                .map(Deque::pop)
                .peek(v -> {
                  if (v.equals("one")) stack.push("two");
                }))
        .containsExactly("one", "two").inOrder();
    assertThat(stack).isEmpty();
  }

  @Test public void whileNotNull_null() {
    assertThat(whileNotNull(new ArrayDeque<>()::poll)).isEmpty();
  }

  @Test public void whileNotNull_notNull() {
    Deque<String> stack = new ArrayDeque<>();
    stack.push("one");
    stack.push("two");
    assertThat(whileNotNull(stack::poll))
        .containsExactly("two", "one").inOrder();
  }

  @Test public void toListAndThen_reversed() {
    assertThat(Stream.of(1, 2, 3).collect(toListAndThen(Collections::reverse)))
        .containsExactly(3, 2, 1)
        .inOrder();
  }

  @Test public void toListAndThen_nullRejected() {
    assertThrows(
        NullPointerException.class,
        () -> Stream.of(1, null).collect(toListAndThen(Collections::reverse)));
  }

  @Test public void toListAndThen_immutable() {
    List<Integer> list = Stream.of(1, 2).collect(toListAndThen(Collections::reverse));
    assertThrows(UnsupportedOperationException.class, list::clear);
  }

  @Test public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    asList(MoreStreams.class.getDeclaredMethods()).stream()
        .filter(m -> m.getName().equals("generate"))
        .forEach(tester::ignore);
    tester.testAllPublicStaticMethods(MoreStreams.class);
    new ClassSanityTester().forAllPublicStaticMethods(MoreStreams.class).testNulls();
  }

  private static class Translation {
    private final ImmutableMap<Integer, String> dictionary;

    Translation(ImmutableMap<Integer, String> dictionary) {
      this.dictionary = dictionary;
    }

    ImmutableMap<Integer, String> dictionary() {
      return dictionary;
    }
  }
}
