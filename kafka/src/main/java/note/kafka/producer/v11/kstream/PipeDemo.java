/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package note.kafka.producer.v11.kstream;

import note.kafka.KafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KeyValueMapper;

import javax.script.*;
import java.util.Properties;

public class PipeDemo {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.BOOTSTRAP_SERVERS);
        //props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        //props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        KStreamBuilder builder = new KStreamBuilder();

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        Compilable compilable = (Compilable) engine;
        Bindings bindings = engine.createBindings(); //Local级别的Binding
        String script =
                "function add(op1,op2)" +
                        "{return op1+op2} " +
                        "add(a, b)"; //定义函数并调用
        CompiledScript JSFunction = compilable.compile(script); //解析编译脚本函数


        KStream<String, String> source = builder.stream(KafkaProperties.TOPIC);//.to("streams-pipe-output");
        source.map(new KeyValueMapper<String, String, KeyValue<?, ?>>() {
            @Override
            public KeyValue<?, ?> apply(String key, String value) {
                System.out.println(key+" == "+value);
                bindings.put("a", Integer.parseInt(key));
                bindings.put("b", Integer.parseInt(value));
                Object result = null;
                try {
                    result = JSFunction.eval(bindings);
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
                return new KeyValue<>(key+" == "+value,result);
            }
        }).print();
        KafkaStreams streams = new KafkaStreams(builder, props);
        streams.start();

        // usually the stream application would be running forever,
        // in this example we just let it run for some time and stop since the input data is finite.
        Thread.sleep(500000000L);

        streams.close();
    }
}
