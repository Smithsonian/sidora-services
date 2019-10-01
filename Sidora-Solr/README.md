# Sidora Solr Service

Sidora Solr Service provides event driven create, update, and delete operation to Solr Indexes via Fedora Messaging or REST Endpoints



Sidora Solr REST API:

Base URL:
http://localhost:{port}/sidora/solr

URL Syntax
/{solrIndex}/{pid}?solrOperation={add|delete|update}

HTTP Method:
GET

HTTP Response:
200

Parameters:
solrIndex = the index we are operating on
pid = the pid of the fedora object to index
solrOperation = add, delete, or update