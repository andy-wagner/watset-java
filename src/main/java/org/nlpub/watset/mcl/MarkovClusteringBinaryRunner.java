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

package org.nlpub.watset.mcl;

import org.jgrapht.Graph;
import org.nlpub.watset.graph.Clustering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * This is a weird thing. The implementation of MCL by its author is fast,
 * but is distributed under GPL. In order to use it, we need to run the separate
 * process and speak to it over standard input/output redirection.
 */
public class MarkovClusteringBinaryRunner<V, E> implements Clustering<V> {
    public static final int THREADS = 1;

    private final Graph<V, E> graph;
    private final Path mcl;
    private final int e;
    private final double r;
    private final int threads;
    private Map<V, Integer> mapping;
    private File output;

    public static final <V, E> Function<Graph<V, E>, Clustering<V>> provider(Path mcl, int e, double r, int threads) {
        return graph -> new MarkovClusteringBinaryRunner<>(graph, mcl, e, r, threads);
    }

    public MarkovClusteringBinaryRunner(Graph<V, E> graph, Path mcl, int e, double r, int threads) {
        this.graph = requireNonNull(graph);
        this.mcl = mcl;
        this.e = e;
        this.r = r;
        this.threads = threads;
    }

    @Override
    public Collection<Collection<V>> getClusters() {
        final Map<Integer, V> inverse = mapping.entrySet().stream().
                collect(toMap(Map.Entry::getValue, Map.Entry::getKey));

        try {
            return Files.lines(output.toPath()).
                    map(line -> Arrays.stream(line.split("\t")).
                            map(id -> inverse.get(Integer.valueOf(id))).
                            collect(toSet())).
                    collect(toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        throw new IllegalStateException("The clusters cannot be read.");
    }

    @Override
    public void run() {
        mapping = translate(graph);

        try {
            process();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void process() throws IOException, InterruptedException {
        output = File.createTempFile("mcl", "output");
        output.deleteOnExit();

        final File input = writeInputFile();

        final ProcessBuilder builder = new ProcessBuilder(
                mcl.toAbsolutePath().toString(),
                input.toString(),
                "-te", Integer.toString(threads),
                "--abc",
                "-o", output.toString());

        final Process process = builder.start();
        int status = process.waitFor();

        if (status != 0) {
            throw new IllegalStateException("mcl process \"" + mcl.toAbsolutePath() + "\" returned " + status);
        }
    }

    private Map<V, Integer> translate(Graph<V, E> graph) {
        final Map<V, Integer> mapping = new HashMap<>(graph.vertexSet().size());

        int i = 0;

        for (final V node : graph.vertexSet()) {
            mapping.put(node, i++);
        }

        return mapping;
    }

    private File writeInputFile() throws IOException {
        final File input = File.createTempFile("mcl", "input");
        input.deleteOnExit();

        try (final BufferedWriter writer = Files.newBufferedWriter(input.toPath())) {
            for (final E edge : graph.edgeSet()) {
                final int source = mapping.get(graph.getEdgeSource(edge));
                final int target = mapping.get(graph.getEdgeTarget(edge));
                final double weight = graph.getEdgeWeight(edge);

                writer.write(String.format(Locale.ROOT, "%d\t%d\t%f\n", source, target, weight));
            }
        }

        return input;
    }
}