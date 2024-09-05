# CyArangoDB

![Build](https://github.com/hugowschneider/CyArangoDB/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/github/hugowschneider/CyArangoDB/graph/badge.svg?token=GYB5PORCSX)](https://codecov.io/github/hugowschneider/CyArangoDB)


This project provides a CytoScape 3 app designed to facilitate seamless import of graphs from ArangoDB as networks, a native multi-model database. The app allows users to easily query and load network structures directly from ArangoDB into CytoScape for advanced visualization, analysis, and integration with biological networks.

Key features include:

- Direct connectivity to ArangoDB for data retrieval.
- Query capabilities with syntax highlighting and autocompletion.
  <img src="https://raw.githubusercontent.com/username/repository/master/docs/images/import.png" alt="Query capabilities" width="300">
- Support for importing and extending graphs as networks.
  <img src="https://raw.githubusercontent.com/username/repository/master/docs/images/expand.png" alt="Graph import and extension" width="300">
- Uses CytoScape 3 interface to visualize imported data.
- Node and edge data retrieval directly from ArangoDB.
  <img src="https://raw.githubusercontent.com/username/repository/master/docs/images/details.png" alt="Node and edge data retrieval" width="300">
  
Whether working with biological data or other large-scale network data, this tool enables users to leverage ArangoDB’s capabilities directly within CytoScape’s robust ecosystem for analysis and visualization.


## Running Tests

You can run tests locally using Docker Compose to start an instance of the ArangoDB database service. Follow these steps to run the tests:

1. Source the environment variables used in the tests and scripts to set up the testing environment:
    ```sh
    source .env
    ```
2. Start the testing environment:
    ```sh
    sh start-local-test-env.sh
    ```
3. Run the tests:
    ```sh
    mvn test
    ```

## JFlex

The `ArangoTokenMaker.flex` file generates a class that splits text into tokens representing ArangoDB's AQL query syntax.

If you need to regenerate this file, keep the following in mind:

- The generated `AqlTokenMaker.java` file will contain two definitions of both `zzRefill` and `yyreset`. You should manually delete the second definition of each (the ones generated by the lexer), as these generated methods modify the input buffer, which we will never need to do.

- Change the declaration/definition of `zzBuffer` to not be initialized. This avoids unnecessary memory allocation since we will be pointing the array somewhere else anyway.

- Do not call `yylex()` on the generated scanner directly; instead, use `getTokenList` as you would with any other `TokenMaker` instance.


## License

Released under [Apache 2.0](/LICENSE) by [@hugowschneider](https://github.com/hugowschneider).