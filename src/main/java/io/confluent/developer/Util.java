package io.confluent.developer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javafaker.Faker;

public class Util implements AutoCloseable{

    private final Logger logger = LoggerFactory.getLogger(Util.class);
    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    public class Randomizer implements AutoCloseable, Runnable{

        private Properties properties;
        private String topic;
        private boolean closed;

        

        public Randomizer(Properties properties, String topic) {
            this.properties = properties;
            this.topic = topic;
            this.closed = false;
            this.properties.setProperty("client.id", "faker");
        }

        @Override
        public void run() {
            try(KafkaProducer producer = new KafkaProducer<>(properties)){
                Faker faker = new Faker();
                while(!closed){
                    try{
                        producer.send(new ProducerRecord<>(this.topic, faker.chuckNorris().fact())).get();
                        Thread.sleep(5000);
                    }catch(InterruptedException ex){ }
                }                
            } catch (Exception e) {
                throw new UnsupportedOperationException("Unimplemented method 'run'");
            }
        }

        @Override
        public void close() throws Exception {
            closed = true;
        }       

    }
    public Randomizer startRandomizer(Properties properties, String topic){
        Randomizer rv = new Randomizer(properties, topic);
        executorService.submit(rv);
        return rv;
    }
    public void createTopics(final Properties properties, List<NewTopic> topics) throws InterruptedException, ExecutionException, TimeoutException{
        try(final AdminClient client = AdminClient.create(properties)){
            logger.info("Creating topics");
            client.createTopics(topics).values().forEach((topic, future) -> {
                try{
                    future.get();
                } catch(Exception ex){
                    logger.info(ex.toString());
                }
            });
            Collection<String> topicNames = topics.stream()
                                            .map(t -> t.name())
                                            .collect(Collectors.toCollection(LinkedList::new));
            logger.info("Asking cluster for topic descriptions");

            client.describeTopics(topicNames)
                    .allTopicNames()
                    .get(10, TimeUnit.SECONDS)
                    .forEach((name,description) -> logger.info("Topic Description : {}", description));
}
        
    }

    @Override
    public void close() throws Exception {
       if(executorService != null ){
        executorService.shutdown();
        executorService = null;
       }
    }

    
}
