package com.google.mu.util.graph;

import static com.google.common.truth.Truth8.assertThat;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.NullPointerTester;
import com.google.mu.util.stream.BiStream;

public class CycleDetectorTest {
  @Test
  public void detectCycle_noChildren() {
    assertThat(CycleDetector.forGraph(n -> null).detectCycleFrom("root")).isEmpty();
  }

  @Test
  public void detectCycle_trivialCycle() {
    assertThat(CycleDetector.forGraph(Stream::of).detectCycleFrom("root"))
        .containsExactly("root", "root");
  }

  @Test
  public void detectCycle_oneUndirectedEdge() {
    Stream<String> cycle =
        detectCycle(toUndirectedGraph(ImmutableListMultimap.of("foo", "bar")), "foo");
    assertThat(cycle).containsExactly("foo", "bar", "foo").inOrder();
  }

  @Test
  public void detectCycle_oneDirectedEdge() {
    assertThat(detectCycle(toDirectedGraph(ImmutableListMultimap.of("foo", "bar")), "foo"))
        .isEmpty();
  }

  @Test
  public void detectCycle_twoDirectedEdges_noCycle() {
    assertThat(detectCycle(toDirectedGraph(
            ImmutableListMultimap.of("foo", "bar", "bar", "baz")), "foo"))
        .isEmpty();
  }

  @Test
  public void detectCycle_threeDirectedEdges_withCycle() {
    Graph<String> graph =
        toDirectedGraph(ImmutableListMultimap.of("foo", "bar", "bar", "baz", "baz", "foo"));
    Stream<String> cycle = detectCycle(graph, "foo");
    assertThat(cycle).containsExactly("foo", "bar", "baz", "foo").inOrder();
  }

  @Test
  public void detectCycle_innerDirectedEdges_withCycle() {
    Graph<String> graph = toDirectedGraph(
        ImmutableListMultimap.of("foo", "bar", "bar", "baz", "baz", "zoo", "zoo", "bar"));
    Stream<String> cycle = detectCycle(graph, "foo");
    assertThat(cycle).containsExactly("foo", "bar", "baz", "zoo", "bar").inOrder();
  }

  @Test
  public void detectCycle_dag_noCycle() {
    Graph<String> graph = toDirectedGraph(ImmutableListMultimap.of(
        "foo", "bar", "bar", "baz", "baz", "zoo", "bar", "tea", "tea", "zoo"));
    assertThat(detectCycle(graph, "foo")).isEmpty();
  }

  @Test
  public void detectCycle_diamondCycle() {
    Graph<String> graph = toDirectedGraph(ImmutableMap.of(
        "foo", asList("bar"),
        "bar", asList("baz", "tea"),
        "baz", asList("zoo"),
        "tea", asList("zoo"),
        "zoo", asList("foo")));
    Stream<String> cycle = detectCycle(graph, "bar");
    assertThat(cycle).containsExactly("bar", "baz", "zoo", "foo", "bar").inOrder();
  }

  @Test
  public void detectCycle_noStartingNodes() {
    assertThat(detectCycle(toUndirectedGraph(ImmutableListMultimap.of("foo", "bar"))))
        .isEmpty();
  }

  @Test
  public void detectCycle_multipleStartingNodes() {
    Graph<String> graph = toDirectedGraph(
        ImmutableListMultimap.of("a", "b", "foo", "bar", "bar", "baz", "baz", "foo"));
    assertThat(detectCycle(graph, "a", "foo")).containsExactly("foo", "bar", "baz", "foo")
        .inOrder();
    assertThat(detectCycle(graph, "foo", "a")).containsExactly("foo", "bar", "baz", "foo")
        .inOrder();
  }

  @Test
  public void staticMethods_nullCheck() throws Exception {
    new NullPointerTester().testAllPublicStaticMethods(CycleDetector.class);
    new ClassSanityTester().forAllPublicStaticMethods(CycleDetector.class).testNulls();
  }

  @Test
  public void instanceMethods_nullCheck()
      throws Exception {
    new NullPointerTester().testAllPublicInstanceMethods(CycleDetector.forGraph(n -> null));
  }

  @SafeVarargs
  private static <N> Stream<N> detectCycle(Graph<N> graph, N... startNodes) {
    return CycleDetector.forGraph((N n) -> graph.successors(n).stream())
        .detectCycleFrom(startNodes);
  }

  private static <N> Graph<N> toUndirectedGraph(Multimap<N, N> edges) {
    MutableGraph<N> graph = GraphBuilder.undirected().<N>build();
    BiStream.from(edges.asMap()).flatMapValues(Collection::stream).forEach(graph::putEdge);
    return graph;
  }

  private static <N> Graph<N> toDirectedGraph(Multimap<N, N> edges) {
    MutableGraph<N> graph =
        GraphBuilder.directed().incidentEdgeOrder(ElementOrder.stable()).<N>build();
    BiStream.from(edges.asMap()).flatMapValues(Collection::stream).forEach(graph::putEdge);
    return graph;
  }

  private static <N> Graph<N> toDirectedGraph(Map<N, ? extends Collection<? extends N>> edges) {
    return toDirectedGraph(toMultimap(edges));
  }

  private static <N> ImmutableListMultimap<N, N> toMultimap(
      Map<N, ? extends Collection<? extends N>> map) {
    return BiStream.from(map)
            .flatMapValues(Collection::stream)
            .collect(ImmutableListMultimap::toImmutableListMultimap);
  }
}
