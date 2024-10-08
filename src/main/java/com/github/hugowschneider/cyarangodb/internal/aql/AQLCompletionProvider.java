package com.github.hugowschneider.cyarangodb.internal.aql;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;

import org.fife.ui.autocomplete.AbstractCompletionProvider;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionsReadOptions;

/**
 * AQLCompletionProvider is a class that provides auto-completion suggestions
 * for AQL (ArangoDB Query Language) in a text component.
 * It extends the AbstractCompletionProvider class.
 *
 * The AQLCompletionProvider class contains arrays of AQL keywords and
 * functions, as well as collections, edges, and graph names.
 * It also has methods to update the database completions, get the already
 * entered text, get parameterized completions, and get completions at a
 * specific position.
 * The class uses the ArangoDatabase class to interact with the ArangoDB
 * database.
 *
 * AQLCompletionProvider is typically used in conjunction with a JTextComponent
 * to provide auto-completion suggestions while typing AQL queries.
 * It can be initialized with an ArangoDatabase instance and can be updated with
 * a new ArangoDatabase instance to reflect changes in the database.
 *
 * The class provides context-aware completions based on the previous word
 * entered by the user.
 * It filters the completions based on the current word and the previous word,
 * and returns a list of completions sorted alphabetically.
 *
 * AQLCompletionProvider also inherits the basic completion functionality from
 * the AbstractCompletionProvider class.
 * It adds AQL keywords and functions as basic completions, and provides
 * parameterized completions for AQL functions.
 *
 * Note: This class requires the ArangoDB Java driver to be included in the
 * project.
 */
public class AQLCompletionProvider extends AbstractCompletionProvider {

	/**
	 * The logger for the AQLCompletionProvider class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AQLCompletionProvider.class);

	/**
	 * AQL keywords that can appear before collections in an AQL query.
	 */
	private static final String[] AQL_KEYWORDS_BEFORE_COLLECTIONS = {
			"IN"
	};

	/**
	 * AQL keywords that can appear before nodes in an AQL query.
	 */
	private static final String[] AQL_KEYWORDS_BEFORE_NODES = {
			"ANY", "INBOUND", "OUTBOUND", "ALL_SHORTEST_PATHS", "K_PATHS", "K_SHORTEST_PATHS", "SHORTEST_PATH"
	};

	/**
	 * AQL keywords that can appear before graphs in an AQL query.
	 */
	private static final String[] AQL_KEYWORDS_BEFORE_GRAPHS = {
			"GRAPH"
	};

	/**
	 * AQL keywords used in AQL queries.
	 */
	private static final String[] AQL_KEYWORDS = {
			"AGGREGATE", "ALL", "ALL_SHORTEST_PATHS", "AND", "ANY", "ASC", "COLLECT",
			"DESC", "DISTINCT", "FALSE", "FILTER", "FOR", "GRAPH", "IN", "INBOUND",
			"INSERT", "INTO", "K_PATHS", "K_SHORTEST_PATHS", "LET", "LIKE", "LIMIT",
			"NONE", "NOT", "NULL", "OR", "OUTBOUND", "REMOVE", "REPLACE", "RETURN",
			"SHORTEST_PATH", "SORT", "TRUE", "UPDATE", "UPSERT", "WINDOW", "WITH"
	};

	/**
	 * AQL functions used in AQL queries.
	 */
	private static final String[] AQL_FUNCTIONS = {
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

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)}.
	 */
	private String lastCompletionsAtText;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)},
	 * since this may be called multiple times in succession (this is usually
	 * called by {@code JTextComponent.getToolTipText()}, and if the user
	 * wiggles the mouse while a tool tip is displayed, this method gets
	 * repeatedly called. It can be costly, so we try to speed it up a tad).
	 */
	private List<Completion> lastParameterizedCompletionsAt;

	/**
	 * Arango Database connection
	 */
	private ArangoDatabase database;

	/**
	 * Segment used to get the already entered text.
	 */
	private Segment seg;

	/**
	 * List of document collection names.
	 */
	private List<String> docCollectionNames;

	/**
	 * List of edge collection names.
	 */
	private List<String> edgeCollectionNames;

	/**
	 * List of graph names.
	 */
	private List<String> graphNames;

	/**
	 * The previous word entered by the user.
	 */
	private String previousWord;

	/**
	 * AQLCompletionProvider is responsible for providing code completion
	 * suggestions for AQL (ArangoDB Query Language).
	 * It initializes the AQL completions and updates the database completions.
	 *
	 * @param database the ArangoDatabase instance used for querying the database
	 */
	public AQLCompletionProvider(ArangoDatabase database) {

		seg = new Segment();
		this.database = database;

		initializeAQLCompletions();
		seg = new Segment();
		this.database = database;
		updateDatabaseCompletions();
	}

	/**
	 * Updates the completions based on the collections and graphs in the database.
	 */
	private void updateDatabaseCompletions() {
		docCollectionNames = new ArrayList<>();
		edgeCollectionNames = new ArrayList<>();
		graphNames = new ArrayList<>();
		if (this.database == null) {
			return;
		}
		try {
			Collection<CollectionEntity> collections = this.database
					.getCollections(new CollectionsReadOptions().excludeSystem(true));
			for (CollectionEntity collection : collections) {
				if (collection.getType() == CollectionType.DOCUMENT) {
					docCollectionNames.add(collection.getName());
				} else if (collection.getType() == CollectionType.EDGES) {
					edgeCollectionNames.add(collection.getName());
				}
			}
			this.database.getGraphs().forEach(graph -> graphNames.add(graph.getName()));

			Collections.sort(docCollectionNames);
			Collections.sort(edgeCollectionNames);
			Collections.sort(graphNames);
		} catch (Exception e) {
			LOGGER.debug("Error while updating database completions", e);
		}

	}

	/**
	 * Returns the ArangoDatabase instance used for querying the database.
	 *
	 * @return the ArangoDatabase instance
	 */
	public ArangoDatabase getDatabase() {
		return database;
	}

	/**
	 * Sets the ArangoDatabase instance used for querying the database.
	 *
	 * @param database the ArangoDatabase instance
	 */
	public void setDatabase(ArangoDatabase database) {
		this.database = database;
		updateDatabaseCompletions();
	}

	/**
	 * Initializes the AQL completions with AQL keywords and functions.
	 */
	private void initializeAQLCompletions() {
		for (String keyword : AQL_KEYWORDS) {
			addCompletion(new BasicCompletion(this, keyword));
		}
		for (String function : AQL_FUNCTIONS) {
			addCompletion(new BasicCompletion(this, function));
		}
	}

	/**
	 * Returns the text that has already been entered in the text component.
	 * This method is used to determine the context for providing completions.
	 *
	 * @param comp the text component
	 * @return the text that has already been entered
	 */
	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot - start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		while (start >= seg.offset && isValidChar(seg.array[start])) {
			start--;
		}
		start++;

		len = segEnd - start;

		int prevWordEnd = start - 1;
		Segment previousSegment = seg;
		int prevWordStart = prevWordEnd;
		int previousIndex = index;

		while (previousIndex >= 0 && (prevWordStart < previousSegment.offset ||
				!isValidChar(previousSegment.array[prevWordStart]))) {
			if (prevWordEnd > 0) {
				Element prevElem = root.getElement(previousIndex);
				int prevElemStart = prevElem.getStartOffset();
				int prevSegmentEnd = prevElem.getEndOffset() - 1;
				try {
					doc.getText(prevElemStart, prevSegmentEnd - prevElemStart, previousSegment);
				} catch (BadLocationException ble) {
					ble.printStackTrace();
					return EMPTY_STRING;
				}
				while (prevWordEnd >= 0 && !isValidChar(previousSegment.array[prevWordEnd])) {
					prevWordEnd--;
				}

				prevWordStart = prevWordEnd - 1;
				while (prevWordStart >= previousSegment.offset && isValidChar(previousSegment.array[prevWordStart])) {
					prevWordStart--;
				}
			}
			previousIndex--;

		}
		prevWordStart++;

		int prevWordLen = prevWordEnd - prevWordStart + 1;

		previousWord = prevWordLen <= 0 ? EMPTY_STRING : new String(previousSegment.array, prevWordStart, prevWordLen);

		return len == 0 ? EMPTY_STRING : new String(seg.array, start, len);

	}

	/**
	 * Returns the parameterized completions for the specified text component.
	 * This method is used to provide completions for AQL functions that require
	 * parameters.
	 *
	 * @param tc the text component
	 * @return a list of parameterized completions
	 */
	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(
			JTextComponent tc) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Returns whether the specified character is valid in an auto-completion.
	 * The default implementation is equivalent to
	 * "<code>Character.isLetterOrDigit(ch) || ch=='_'</code>". Subclasses
	 * can override this method to change what characters are matched.
	 *
	 * @param ch The character.
	 * @return Whether the character is valid.
	 */
	protected boolean isValidChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '`' || ch == '\'' || ch == '"' || ch == '/';
	}

	/**
	 * Returns the completions that are context-aware based on the current word and
	 * the previous word.
	 * This method is used to provide completions that are relevant to the context
	 * in which they are used.
	 *
	 * @param currentWord  the current word being entered
	 * @param previousWord the previous word entered by the user
	 * @return a list of completions that are context-aware
	 */
	private List<Completion> getContexAwareeCompletions(String currentWord, String previousWord) {
		List<String> arangoCompletions = new ArrayList<>();
		final List<Completion> completions;
		if (Arrays.asList(AQL_KEYWORDS_BEFORE_COLLECTIONS).contains(previousWord)) {
			arangoCompletions.addAll(filterList(docCollectionNames, currentWord).collect(Collectors.toList()));
			arangoCompletions.addAll(filterList(edgeCollectionNames, currentWord).collect(Collectors.toList()));
			completions = arangoCompletions.stream().sorted().map((str) -> new BasicCompletion(this, str))
					.collect(Collectors.toList());
		} else if (Arrays.asList(AQL_KEYWORDS_BEFORE_NODES).contains(previousWord)) {
			if ((currentWord.startsWith("'") || currentWord.startsWith("\"")) && currentWord.contains("/")) {
				String collectionName = currentWord.substring(1, currentWord.indexOf("/"));
				String substr = currentWord.substring(currentWord.indexOf("/") + 1);
				StringBuilder query = new StringBuilder("FOR n IN ")
						.append(collectionName.replaceAll("[^a-zA-Z0-9_]", ""))
						.append(" ");
				Map<String, Object> bindVars = new HashMap<>();

				if (substr.length() > 0) {
					query.append("FILTER LOWER(n._id) LIKE @substr ").toString();
					bindVars.put("substr", String.format("%1$s/%2$s%%", collectionName, substr));
				}
				query.append("SORT n._id ASC LIMIT 20 RETURN n._id");
				ArangoCursor<String> ids = database.query(query.toString(), String.class, bindVars, null);
				completions = ids.stream().map((s) -> new BasicCompletion(this, String.format(
						currentWord.startsWith("'") ? "'%1$s'"
								: (currentWord.startsWith("\"") ? "\"%1$s\"" : "'%1$s'"),
						s))).collect(Collectors.toList());
			} else {
				arangoCompletions
						.addAll(filterList(docCollectionNames, currentWord.replace("'", "").replace("\"", ""))
								.collect(Collectors.toList()));

				completions = arangoCompletions.stream().sorted()
						.map((str) -> new BasicCompletion(this, String.format(
								currentWord.startsWith("'") ? "'%1$s/"
										: (currentWord.startsWith("\"") ? "\"%1$s/" : "'%1$s/"),
								str)))
						.collect(Collectors.toList());
			}
		} else if (Arrays.asList(AQL_KEYWORDS_BEFORE_GRAPHS).contains(previousWord)) {
			completions = filterList(graphNames, currentWord).sorted()
					.map((str) -> new BasicCompletion(this, str))
					.collect(Collectors.toList());
		} else {
			completions = Collections.emptyList();
		}

		return completions;

	}

	/**
	 * Returns the completions for the specified text component.
	 * This method is used to provide completions based on the text entered by the
	 * user.
	 *
	 * @param arg0 the text component
	 * @return a list of completions
	 */
	@Override
	public List<Completion> getCompletions(JTextComponent arg0) {

		String text = getAlreadyEnteredText(arg0);
		List<Completion> completions = new ArrayList<>();
		completions.addAll(getContexAwareeCompletions(text, getPreviousWord()));
		completions.addAll(super.getCompletions(arg0));
		return completions;
	}

	/**
	 * Filters the specified list based on the specified text.
	 * This method is used to filter a list of strings based on a specified text.
	 *
	 * @param list the list of strings to filter
	 * @param text the text to filter by
	 * @return a stream of strings that match the specified text
	 */
	protected Stream<String> filterList(List<String> list, String text) {

		return list.stream().filter((str) -> str.toLowerCase().startsWith(text.toLowerCase()));
	}

	/**
	 * Returns the completions at the specified position in the text component.
	 * This method is used to provide completions at a specific position in the
	 * text.
	 *
	 * @param tc the text component
	 * @param p  the position in the text component
	 * @return a list of completions at the specified position
	 */
	@Override
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {
		throw new UnsupportedOperationException("Not supported yet.");

	}

	/**
	 * Returns the previous word entered by the user.
	 *
	 * @return the previous word
	 */
	public String getPreviousWord() {
		return previousWord;
	}
}
