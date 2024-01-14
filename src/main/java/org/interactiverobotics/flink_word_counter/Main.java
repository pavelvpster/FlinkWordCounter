/*
 * Main.java
 *
 * Copyright (C) 2023 Pavel Prokhorov (pavelvpster@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.interactiverobotics.flink_word_counter;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Main {
    public static void main(String[] args) throws Exception {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        String input = parameterTool.get("input", "/data/input.txt");
        String output = parameterTool.get("output");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        env.setParallelism(1);

        DataStream<Tuple2<String, Long>> wordCounts = env.fromElements(input)
                .flatMap((FlatMapFunction<String, String>) (in, out) -> {
                    try {
                        // send lines (not whole contents) to 'Map' in parallel
                        Files.readAllLines(Path.of(in)).forEach(out::collect);
                    } catch (IOException ignored) {
                    }
                })
                .returns(new TypeHint<String>() {
                })
                .name("Read file")
                .flatMap((FlatMapFunction<String, Tuple2<String, Long>>) (in, out) -> {
                    String[] words = in.split("\\W+");
                    for (String word : words) {
                        out.collect(Tuple2.of(word, 1L));
                    }
                })
                .returns(new TypeHint<Tuple2<String, Long>>() {
                })
                .setParallelism(4)
                .name("Map")
                .keyBy(t -> t.f0)
                .sum(1)
                .setParallelism(4)
                .name("Reduce");

        if (output == null) {
            wordCounts.print().name("Print to Stdout");
        } else {
            // https://nightlies.apache.org/flink/flink-docs-release-1.13/docs/connectors/datastream/streamfile_sink/
            wordCounts.sinkTo(
                            FileSink.<Tuple2<String, Long>>forRowFormat(
                                            new org.apache.flink.core.fs.Path(output), new SimpleStringEncoder<>())
                                    .withRollingPolicy(
                                            DefaultRollingPolicy.builder()
                                                    .withMaxPartSize(MemorySize.ofMebiBytes(1))
                                                    .withRolloverInterval(Duration.ofSeconds(30))
                                                    .build())
                                    .build())
                    .name("Save to File");
        }

        env.execute("WordCounter");
    }
}
