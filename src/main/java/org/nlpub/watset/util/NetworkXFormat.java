package org.nlpub.watset.util;

import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.objects.ClassDict;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.builder.GraphBuilder;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Utilities for handling pickled NetworkX graphs.
 *
 * @see <a href="https://networkx.github.io/">NetworkX</a>
 * @see <a href="https://docs.python.org/3/library/pickle.html">Pickle</a>
 */
public interface NetworkXFormat {
    /**
     * Reconstruct a {@link Graph} object from the unpickled NetworkX graph.
     *
     * @param nx the unpickled NetworkX graph
     * @return the graph represented in the unpickled graph
     */
    static Graph<Object, DefaultWeightedEdge> load(ClassDict nx) {
        requireNonNull(nx);

        GraphBuilder<Object, DefaultWeightedEdge, ? extends SimpleWeightedGraph<Object, DefaultWeightedEdge>> builder =
                SimpleWeightedGraph.createBuilder(DefaultWeightedEdge.class);

        if (!nx.get("__class__").equals("networkx.classes.graph.Graph")) {
            throw new PickleException("graph is not networkx.classes.graph.Graph");
        }

        @SuppressWarnings("unchecked") final Map<Object, Object> nodes = (Map<Object, Object>) nx.get("_node");

        for (Object node : nodes.keySet()) {
            builder.addVertex(node);
        }

        @SuppressWarnings("unchecked") final Map<Object, Map<Object, Object>> edges = (Map<Object, Map<Object, Object>>) nx.get("_adj");

        for (Map.Entry<Object, Map<Object, Object>> source : edges.entrySet()) {
            for (Object target : source.getValue().keySet()) {
                builder.addEdge(source.getKey(), target);
            }
        }

        return builder.build();
    }
}
