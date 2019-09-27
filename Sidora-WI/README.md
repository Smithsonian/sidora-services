# SI Services - Sidora Wildlife Insights

### Table of contents
* [ActiveMQ Configuration](#activemq-configuration)
* [Properties Configuration](#properties-configuration)

### ActiveMQ Configuration:

* Add a new ActiveMQ Queue `sidora.wi.apim.update` for Sidora Wildlife Insights camel processes:
    ```bash
    # vi <fedora-install>/server/config/spring/activemq.xml

    ...
    <amq:destinationInterceptors>
      <amq:virtualDestinationInterceptor>
        <amq:virtualDestinations>
          <amq:compositeQueue name="fedora.apim.update">
            <amq:forwardTo>
              <amq:queue physicalName="sidora.apim.update"/>
              <amq:queue physicalName="solr.apim.update"/>
              <amq:queue physicalName="edanIds.apim.update"/>
              <amq:queue physicalName="sidora.wi.apim.update"/>
            </amq:forwardTo>
          </amq:compositeQueue>
        </amq:virtualDestinations>
      </amq:virtualDestinationInterceptor>
    </amq:destinationInterceptors>
    ...
    ```
  
### Properties Configuration:

* Edit the `/opts/sidora/smx/etc/edu.si.sidora.wi.cfg` file and verify the properties are correct.