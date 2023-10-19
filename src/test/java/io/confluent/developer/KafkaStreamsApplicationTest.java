package io.confluent.developer;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;

public class KafkaStreamsApplicationTest {

    Logger logger = LoggerFactory.getLogger(KafkaStreamsApplicationTest.class);

    private final static String TEST_CONFIG_FILE="configuration/test.properties";

    @Test
    public void topologyShouldUpperCaseInputs(){
        final Properties props = new Properties();
        try(InputStream inputStream = new FileInputStream(TEST_CONFIG_FILE)){
            props.load(inputStream);
        } catch (IOException ex){

        }
        final String inputTopicName = props.getProperty("input.topic.name");
        final String outputTopicName = props.getProperty("output.topic.name");

        final Topology topology = KafkaStreamsApplication.buildTopology(inputTopicName, outputTopicName);

        try(final TopologyTestDriver testDriver = new TopologyTestDriver(topology,props)){
            Serde<String> stringSerde = Serdes.String();
            final TestInputTopic<String, String> inputTopic = testDriver.createInputTopic(inputTopicName, stringSerde.serializer(), stringSerde.serializer());
            final TestOutputTopic<String, String> outputTopic = testDriver.createOutputTopic(outputTopicName, stringSerde.deserializer(), stringSerde.deserializer());

            List<String> inputMessages = List.of(
                    "Chuck Norris can write multi-threaded applications with a single thread.",
                    "No statement can catch the ChuckNorrisException.",
                    "Chuck Norris can divide by zero.",
                    "Chuck Norris can binary search unsorted data."
            );
            inputMessages.forEach(inputTopic::pipeInput);

            List<String> expectedMessages = inputMessages.stream().map(String::toUpperCase).toList();
            final List<String> outputMessages = outputTopic.readValuesToList();
            for (String outputMessage : outputMessages) {
               // logger.info(outputMessage);
            }
            assertThat(expectedMessages, equalTo(outputMessages));
        }
    }
}
