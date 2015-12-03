A set of simple utilities for parsing metadata from tabular data files.

To build: after cloning, do `cd tabular-metadata` and then `mvn clean install`.

To run the standalone web service, `cd tabular-metadata-webapp` and `mvn jetty:run`. This will run the application at `http://localhost:8080`.
There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8080/?url={URL-of-my-tabular-data-file}`

That URL can be a `file:///path/to/my/file.csv` URL for convenience. You may also add a `hasHeaders` query parameter with value either `true` or `false` if you know whether your data has a header row.
