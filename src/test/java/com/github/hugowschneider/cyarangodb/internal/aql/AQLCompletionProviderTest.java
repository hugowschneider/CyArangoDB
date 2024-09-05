
package com.github.hugowschneider.cyarangodb.internal.aql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionDetails;
import com.github.hugowschneider.cyarangodb.internal.connection.ConnectionManager;
import com.github.hugowschneider.cyarangodb.internal.test.Helper;

@TestInstance(Lifecycle.PER_CLASS)
public class AQLCompletionProviderTest {

    private AQLCompletionProvider completionProvider;
    private JTextComponent textComponent;
    private ConnectionManager connectionManager;
    private static final String CONNECTION_NAME = "test-connection";

    @BeforeAll
    public void setUpAll() {
        ConnectionDetails connectionDetails = Helper.createConnectionDetails();
        connectionManager = new ConnectionManager("./target/test-resources");
        connectionManager.addConnection(CONNECTION_NAME, connectionDetails);

    }

    @AfterAll
    public void tearDownAll() {
        File file = new File(connectionManager.getFilePath());
        file.delete();
    }

    @BeforeEach
    public void setUp() {
        completionProvider = new AQLCompletionProvider(connectionManager.getArangoDatabase(CONNECTION_NAME));
        textComponent = new JTextArea();
    }

    @Test
    @DisplayName("AQLCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the same line ")
    public void testGetAlreadyEnteredTextSameLine() throws BadLocationException {

        String text = "FOR IN A";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
        assertEquals("IN", completionProvider.getPreviousWord());

    }

    @Test
    @DisplayName("AQLCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the previous line ")
    public void testGetAlreadyEnteredTextPreviousLine() throws BadLocationException {

        String text = "FOR IN\nA";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
        assertEquals("IN", completionProvider.getPreviousWord());

    }

    @Test
    @DisplayName("AQLCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the 2 lines before")
    public void testGetAlreadyEnteredText2LinesBefore() throws BadLocationException {

        String text = "FOR IN\n\nA";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
        assertEquals("IN", completionProvider.getPreviousWord());

    }

    @Test
    @DisplayName("AQLCompletionProvider::getAlreadyEnteredText should return the already entered word and no previous with a single word")
    public void testGetAlreadyEnteredTextShouldWorkWithASingleWord() throws BadLocationException {

        String text = "FOR";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        assertEquals("FOR", completionProvider.getAlreadyEnteredText(textComponent));
        assertEquals("", completionProvider.getPreviousWord());

    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return keywords and functions is text is empty")
    public void testGetCompletions() throws BadLocationException {
        List<Completion> completions = completionProvider.getCompletions(textComponent);
        String[] expectedCompletions = {
                "AGGREGATE", "ALL", "ALL_SHORTEST_PATHS", "AND", "ANY", "ASC", "COLLECT",
                "DESC", "DISTINCT", "FALSE", "FILTER", "FOR", "GRAPH", "IN", "INBOUND",
                "INSERT", "INTO", "K_PATHS", "K_SHORTEST_PATHS", "LET", "LIKE", "LIMIT",
                "NONE", "NOT", "NULL", "OR", "OUTBOUND", "REMOVE", "REPLACE", "RETURN",
                "SHORTEST_PATH", "SORT", "TRUE", "UPDATE", "UPSERT", "WINDOW", "WITH",
                "LENGTH", "COUNT", "SUM", "MIN", "MAX", "AVERAGE", "CONCAT", "SUBSTRING",
                "CONTAINS", "UPPER", "LOWER", "RANDOM_TOKEN",
                "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "CEIL", "COS", "DEGREES", "EXP",
                "FLOOR", "LOG", "LOG10", "PI", "POW", "RADIANS", "ROUND", "SIN", "SQRT",
                "TAN", "RAND", "DATE_NOW", "DATE_TIMESTAMP", "DATE_ISO8601", "DATE_DAYOFWEEK",
                "DATE_YEAR", "DATE_MONTH", "DATE_DAY", "DATE_HOUR", "DATE_MINUTE", "DATE_SECOND",
                "DATE_MILLISECOND", "DATE_DAYOFYEAR", "DATE_ISOWEEK", "DATE_ISOWEEKYEAR",
                "DATE_LEAPYEAR", "DATE_QUARTER", "DATE_FORMAT", "DATE_ADD", "DATE_SUBTRACT",
                "DATE_DIFF", "DATE_COMPARE", "DATE_ROUND", "DATE_TRUNC", "DATE_TIMEZONE",
                "DATE_TIMEZONE_OFFSET", "DATE_TIMEZONE_NAME", "DATE_TIMEZONE_ABBREVIATION",
                "DATE_TIMEZONE_DST", "DATE_TIMEZONE_DST_OFFSET", "DATE_TIMEZONE_DST_NAME",
                "DATE_TIMEZONE_DST_ABBREVIATION", "DATE_TIMEZONE_DST_START", "DATE_TIMEZONE_DST_END",
                "DATE_TIMEZONE_DST_NEXT", "DATE_TIMEZONE_DST_PREVIOUS", "DATE_TIMEZONE_DST_ISDST"
        };

        assertEquals(expectedCompletions.length, completions.size());
        for (Completion completion : completions) {
            assertTrue(Arrays.asList(expectedCompletions).contains(completion.getReplacementText()));
        }
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return collection names after keyword IN")
    public void testGetCompletionsReturnCollectionNames() throws BadLocationException {
        String text = "FOR v IN ";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);
        assertTrue(completions.size() > 2);

        assertEquals("imdb_edges", completions.get(0).getReplacementText());
        assertEquals("imdb_vertices", completions.get(1).getReplacementText());
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return collection names after keyword IN filtered by the already entered text")
    public void testGetCompletionsReturnCollectionNamesFiltered() throws BadLocationException {
        String text = "FOR v IN imdb_v";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() == 1);
        assertEquals("imdb_vertices", completions.get(0).getReplacementText());
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return first collection names when autocompleting node ids")
    public void testGetCompletionsReturnCollectionNamesBeforeAutocompleteNodes() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND ";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 1);
        assertEquals("'imdb_vertices/", completions.get(0).getReplacementText());
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return first collection names when autocompleting node ids considering single quotes")
    public void testGetCompletionsReturnCollectionNamesBeforeAutocompleteNodesSingleQuotes()
            throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND '";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 1);
        assertEquals("'imdb_vertices/", completions.get(0).getReplacementText());
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return first collection names when autocompleting node ids considering double quotes")
    public void testGetCompletionsReturnCollectionNamesBeforeAutocompleteNodesDoubleQuotes()
            throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND \"";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 1);
        assertEquals("\"imdb_vertices/", completions.get(0).getReplacementText());
    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return node ids after typing the colllection name ")
    public void testGetCompletionsReturnNodeAfterCollection() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND 'imdb_vertices/";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 20);
        List<String> values = Arrays.asList(
                "'imdb_vertices/1000'",
                "'imdb_vertices/10000'",
                "'imdb_vertices/10001'",
                "'imdb_vertices/10002'",
                "'imdb_vertices/10003'",
                "'imdb_vertices/10004'",
                "'imdb_vertices/10005'",
                "'imdb_vertices/10006'",
                "'imdb_vertices/10007'",
                "'imdb_vertices/10008'",
                "'imdb_vertices/10009'",
                "'imdb_vertices/1001'",
                "'imdb_vertices/10010'",
                "'imdb_vertices/10011'",
                "'imdb_vertices/10012'",
                "'imdb_vertices/10013'",
                "'imdb_vertices/10014'",
                "'imdb_vertices/10015'",
                "'imdb_vertices/10016'",
                "'imdb_vertices/10017'");
        for (int i = 0; i < 20; i++) {
            assertEquals(values.get(i), completions.get(i).getReplacementText());
        }

    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return node ids after typing the colllection name filtered")
    public void testGetCompletionsReturnNodeAfterCollectionFiltered() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND 'imdb_vertices/2";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 20);
        List<String> values = Arrays.asList(
                "'imdb_vertices/2000'",
                "'imdb_vertices/20000'",
                "'imdb_vertices/20001'",
                "'imdb_vertices/20002'",
                "'imdb_vertices/20003'",
                "'imdb_vertices/20004'",
                "'imdb_vertices/20005'",
                "'imdb_vertices/20006'",
                "'imdb_vertices/20007'",
                "'imdb_vertices/20008'",
                "'imdb_vertices/20009'",
                "'imdb_vertices/2001'",
                "'imdb_vertices/20010'",
                "'imdb_vertices/20011'",
                "'imdb_vertices/20012'",
                "'imdb_vertices/20013'",
                "'imdb_vertices/20014'",
                "'imdb_vertices/20015'",
                "'imdb_vertices/20016'",
                "'imdb_vertices/20017'");
        for (int i = 0; i < 20; i++) {
            assertEquals(values.get(i), completions.get(i).getReplacementText());
        }

    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return node ids after typing the colllection name respectiong quotes")
    public void testGetCompletionsReturnNodeAfterCollectionDoubleQuote() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND \"imdb_vertices/";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 20);
        List<String> values = Arrays.asList(
                "\"imdb_vertices/1000\"",
                "\"imdb_vertices/10000\"",
                "\"imdb_vertices/10001\"",
                "\"imdb_vertices/10002\"",
                "\"imdb_vertices/10003\"",
                "\"imdb_vertices/10004\"",
                "\"imdb_vertices/10005\"",
                "\"imdb_vertices/10006\"",
                "\"imdb_vertices/10007\"",
                "\"imdb_vertices/10008\"",
                "\"imdb_vertices/10009\"",
                "\"imdb_vertices/1001\"",
                "\"imdb_vertices/10010\"",
                "\"imdb_vertices/10011\"",
                "\"imdb_vertices/10012\"",
                "\"imdb_vertices/10013\"",
                "\"imdb_vertices/10014\"",
                "\"imdb_vertices/10015\"",
                "\"imdb_vertices/10016\"",
                "\"imdb_vertices/10017\"");
        for (int i = 0; i < 20; i++) {
            assertEquals(values.get(i), completions.get(i).getReplacementText());
        }

    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return graph names after typing the keyword GRAPH")
    public void testGetCompletionsReturnGraphNames() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND 'imdb_vertices/1000' GRAPH ";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 1);
        assertEquals("imdb", completions.get(0).getReplacementText());

    }

    @Test
    @DisplayName("AQlCompletionProvider::getCompletions should return graph names after typing the keyword GRAPH with filter")
    public void testGetCompletionsReturnGraphNamesFilter() throws BadLocationException {
        String text = "FOR v IN 1..2 INBOUND 'imdb_vertices/1000' GRAPH im";
        textComponent.setText(text);
        textComponent.setCaretPosition(text.length());
        List<Completion> completions = completionProvider.getCompletions(textComponent);

        assertTrue(completions.size() >= 1);
        assertEquals("imdb", completions.get(0).getReplacementText());

    }

}
