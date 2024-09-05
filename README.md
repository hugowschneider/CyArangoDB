# CyArangoDB

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