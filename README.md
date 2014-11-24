A set of simple utilities for parsing metadata from tabular data files.

To build: after cloning, do `cd excel2tabular` and then `mvn clean install`.

To run the standalone web service, `cd excel2tabular-webapp` and `mvn jetty:run`. This will run the application at `http://localhost:8080`.
There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8080/?url={URL-of-my-Excel-file}`

That URL can be a `file:///path/to/my/file.csv` URL for convenience.
