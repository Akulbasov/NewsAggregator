# NewsAggregator


# Start docker with sbt
1. git clone
2. go to root folder
3. sbt
4. clean
5. compile
6. test
7. docker
8. exit from sbt
9. docker-compose up

# Check filter data by kafka-rest proxy
 
 # Create a consumer for JSON data, starting at the beginning of the topic's
 
    # log. The consumer group is called "my_json_consumer" and the instance is "my_consumer_instance".
    
    $ curl -X POST -H "Content-Type: application/vnd.kafka.v1+json" -H "Accept: application/vnd.kafka.v1+json" \
    --data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "latest"}' \
    http://localhost:8082/consumers/my_json_consumer
    
    # Subscribe the consumer to a topic
    
    $ curl -X POST -H "Content-Type: application/vnd.kafka.v1+json" --data '{"topics":["filtertopic"]}' \
    http://localhost:8082/consumers/my_json_consumer/instances/my_consumer_instance/subscription
    
    # Then consume some data from a topic using the base URL in the first response.
    
    $ curl -X GET -H "Accept: application/vnd.kafka.json.v1+json" \
    http://localhost:8082/consumers/my_json_consumer/instances/my_consumer_instance/records


# Check filter data by docker

   docker ps | grep cp-kafka (get container id)
   docker exec -ti containerId bash 
   kafka-console-consumer --zookeeper zookeeper:2181 --topic filtertopic --from-beginning
