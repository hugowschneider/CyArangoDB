package com.github.hugowschneider.cyarangodb.internal.ui.aql;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

public class AQLCompletionProvider extends AbstractCompletionProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AQLCompletionProvider.class);

	private static final String[] AQL_KEYWORDS_BEFORE_COLLECTIONS = {
			"IN"
	};

	private static final String[] AQL_KEYWORDS_BEFORE_NODES = {
			"ANY", "INBOUND", "OUTBOUND", "ALL_SHORTEST_PATHS", "K_PATHS", "K_SHORTEST_PATHS", "SHORTEST_PATH"
	};

	private static final String[] AQL_KEYWORDS_BEFORE_GRAPHS = {
			"GRAPH"
	};

	private static final String[] AQL_KEYWORDS = {
			"AGGREGATE", "ALL", "ALL_SHORTEST_PATHS", "AND", "ANY", "ASC", "COLLECT",
			"DESC", "DISTINCT", "FALSE", "FILTER", "FOR", "GRAPH", "IN", "INBOUND",
			"INSERT", "INTO", "K_PATHS", "K_SHORTEST_PATHS", "LET", "LIKE", "LIMIT",
			"NONE", "NOT", "NULL", "OR", "OUTBOUND", "REMOVE", "REPLACE", "RETURN",
			"SHORTEST_PATH", "SORT", "TRUE", "UPDATE", "UPSERT", "WINDOW", "WITH"
	};
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

	private ArangoDatabase database;

	private Segment seg;

	private List<String> docCollectionNames;
	private List<String> edgeCollectionNames;
	private List<String> graphNames;
	private String previousWord;

	public AQLCompletionProvider(ArangoDatabase database) {

		seg = new Segment();
		this.database = database;

		initializeAQLCompletions();
		seg = new Segment();
		this.database = database;
		updateDatabaseCompletions();
	}

	private void updateDatabaseCompletions() {
		docCollectionNames = new ArrayList<>();
		edgeCollectionNames = new ArrayList<>();
		graphNames = new ArrayList<>();
		if (this.database == null) {
			return;
		}

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

	}

	public ArangoDatabase getDatabase() {
		return database;
	}

	public void setDatabase(ArangoDatabase database) {
		this.database = database;
		updateDatabaseCompletions();
	}

	private void initializeAQLCompletions() {
		for (String keyword : AQL_KEYWORDS) {
			addCompletion(new BasicCompletion(this, keyword));
		}
		for (String function : AQL_FUNCTIONS) {
			addCompletion(new BasicCompletion(this, function));
		}
	}

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

	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(
			JTextComponent tc) {

		List<ParameterizedCompletion> list = null;

		// If this provider doesn't support parameterized completions,
		// bail out now.
		char paramListStart = getParameterListStart();
		if (paramListStart == 0) {
			return list; // null
		}

		int dot = tc.getCaretPosition();
		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(dot);
		Element elem = root.getElement(line);
		int offs = elem.getStartOffset();
		int len = dot - offs - 1/* paramListStart.length() */;
		if (len <= 0) { // Not enough chars on the line for a method.
			return list; // null
		}

		try {

			doc.getText(offs, len, s);

			// Get the identifier preceding the '(', ignoring any whitespace
			// between them.
			offs = s.offset + len - 1;
			while (offs >= s.offset && Character.isWhitespace(s.array[offs])) {
				offs--;
			}
			int end = offs;
			while (offs >= s.offset && isValidChar(s.array[offs])) {
				offs--;
			}

			String text = new String(s.array, offs + 1, end - offs);

			// Get a list of all Completions matching the text, but then
			// narrow it down to just the ParameterizedCompletions.
			List<Completion> l = getCompletionByInputText(text);
			if (l != null && !l.isEmpty()) {
				for (Object o : l) {
					if (o instanceof ParameterizedCompletion) {
						if (list == null) {
							list = new ArrayList<>(1);
						}
						list.add((ParameterizedCompletion) o);
					}
				}
			}

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return list;

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

	private List<Completion> getContexAwareeCompletions(String currentWord, String previousWord) {
		List<String> arangoCompletions = new ArrayList<>();
		final List<Completion> completions;
		if (Arrays.asList(AQL_KEYWORDS_BEFORE_COLLECTIONS).contains(previousWord)) {
			arangoCompletions.addAll(filterList(docCollectionNames, currentWord));
			arangoCompletions.addAll(filterList(edgeCollectionNames, currentWord));
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
				completions = ids.stream().map((s) -> new BasicCompletion(this, s)).collect(Collectors.toList());
			} else {
				arangoCompletions
						.addAll(filterList(docCollectionNames, currentWord.replace("'", "").replace("\"", "")));

				completions = arangoCompletions.stream().sorted()
						.map((str) -> new BasicCompletion(this, String.format(
								currentWord.startsWith("'") ? "'%1$s/"
										: (currentWord.startsWith("\"") ? "\"%1$s/" : "'%1$s/"),
								str)))
						.collect(Collectors.toList());
			}
		} else {
			completions = Collections.emptyList();
		}

		return completions;

	}

	@Override
	public List<Completion> getCompletions(JTextComponent arg0) {

		String text = getAlreadyEnteredText(arg0);
		List<Completion> completions = new ArrayList<>();
		completions.addAll(getContexAwareeCompletions(text, getPreviousWord()));
		completions.addAll(super.getCompletions(arg0));
		return completions;
	}

	protected List<String> filterList(List<String> list, String text) {

		return list.stream().filter((str) -> str.toLowerCase().startsWith(text.toLowerCase()))
				.collect(Collectors.toList());
	}

	@Override
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {

		int offset = tc.viewToModel2D(p);
		if (offset < 0 || offset >= tc.getDocument().getLength()) {
			lastCompletionsAtText = null;
			return lastParameterizedCompletionsAt = null;
		}

		Segment segment = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(offset);
		Element elem = root.getElement(line);
		int start = elem.getStartOffset();
		int end = elem.getEndOffset() - 1;

		try {

			doc.getText(start, end - start, segment);

			// Get the valid chars before the specified offset.
			int startOffs = segment.offset + (offset - start) - 1;
			while (startOffs >= segment.offset && isValidChar(segment.array[startOffs])) {
				startOffs--;
			}

			// Get the valid chars at and after the specified offset.
			int endOffs = segment.offset + (offset - start);
			while (endOffs < segment.offset + segment.count && isValidChar(segment.array[endOffs])) {
				endOffs++;
			}

			int len = endOffs - startOffs - 1;
			if (len <= 0) {
				return lastParameterizedCompletionsAt = null;
			}
			String text = new String(segment.array, startOffs + 1, len);

			if (text.equals(lastCompletionsAtText)) {
				return lastParameterizedCompletionsAt;
			}
			List<String> arangoCompletions = new ArrayList<>();
			List<Completion> completions = new ArrayList<>();
			if (Arrays.asList(AQL_KEYWORDS_BEFORE_COLLECTIONS).contains(getPreviousWord())) {
				arangoCompletions.addAll(filterList(docCollectionNames, text));
				arangoCompletions.addAll(filterList(edgeCollectionNames, text));
				completions = arangoCompletions.stream().sorted().map((str) -> new BasicCompletion(this, str))
						.collect(Collectors.toList());
			}

			// Get a list of all Completions matching the text.
			completions.addAll(getCompletionByInputText(text));
			lastCompletionsAtText = text;
			return lastParameterizedCompletionsAt = completions;

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		lastCompletionsAtText = null;
		return lastParameterizedCompletionsAt = null;

	}

	public String getPreviousWord() {
		return previousWord;
	}
}
