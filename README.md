# promoted-java-delivery-client-example
Promoted Java Delivery Client Example

# How to run?

Create a properties file in `~/.app.properties`.  The flags can also be specified on the CLI.

```
metricsApiEndpointUrl=<get from promoted>/log
metricsApiKey=<get from promoted>
deliveryApiEndpointUrl=<get from promoted>/deliver
deliveryApiKey=<get from promoted>
```

Then run this command.
```
mvn install
mvn compile exec:java
```
