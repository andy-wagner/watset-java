/*
 * Copyright 2018 Dmitry Ustalov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.nlpub.watset.graph;

import org.jgrapht.Graph;
import org.nlpub.watset.util.Neighbors;

import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A simple graph-based word sense induction approach that clusters node neighborhoods.
 *
 * @param <V> the type of nodes in the graph
 * @param <E> the type of edges in the graph
 * @see <a href="https://doi.org/10.3115/1654758.1654774">Biemann (TextGraphs-1)</a>
 * @see <a href="https://doi.org/10.3115/1067737.1067753">Dorow &amp; Widdows (EACL '03)</a>
 * @see <a href="https://doi.org/10.1162/COLI_a_00354">Ustalov et al. (COLI 45:3)</a>
 */
public class SenseInduction<V, E> {
    private final Graph<V, E> graph;
    private final Function<Graph<V, E>, Clustering<V>> local;

    /**
     * Create an instance of {@code SenseInduction}.
     *
     * @param graph the graph
     * @param local the neighborhood clustering algorithm supplier
     */
    public SenseInduction(Graph<V, E> graph, Function<Graph<V, E>, Clustering<V>> local) {
        this.graph = requireNonNull(graph);
        this.local = requireNonNull(local);
    }

    /**
     * Get the induced sense clusters.
     *
     * @param target the target node
     * @return a map of senses to their contexts
     */
    public Collection<Collection<V>> clusters(V target) {
        final Graph<V, E> ego = Neighbors.neighborhoodGraph(graph, requireNonNull(target));

        final Clustering<V> clustering = local.apply(ego);
        clustering.fit();

        return clustering.getClusters();
    }

    /**
     * Get the induced senses and their non-disambiguated contexts.
     *
     * @param target the target node
     * @return maps of senses to their contexts
     */
    public List<Map<V, Number>> contexts(V target) {
        final Collection<Collection<V>> clusters = clusters(target);

        final List<Map<V, Number>> senses = new ArrayList<>(clusters.size());

        for (final Collection<V> cluster : clusters) {
            final Map<V, Number> context = new HashMap<>(cluster.size());

            for (final V neighbor : cluster) {
                context.put(neighbor, graph.getEdgeWeight(graph.getEdge(target, neighbor)));
            }

            senses.add(context);
        }

        return senses;
    }
}