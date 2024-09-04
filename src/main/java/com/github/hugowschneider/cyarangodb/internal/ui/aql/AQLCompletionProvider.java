package com.github.hugowschneider.cyarangodb.internal.ui.aql;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import org.fife.ui.autocomplete.AbstractCompletionProvider;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;

public class AQLCompletionProvider extends AbstractCompletionProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AQLCompletionProvider.class);

	private static final String[] AQL_KEYWORDS = {
			"FOR", "IN", "RETURN", "LET", "FILTER", "SORT", "LIMIT", "COLLECT",
			"INSERT", "UPDATE", "REPLACE", "REMOVE", "UPSERT",
			"WITH", "DISTINCT", "GRAPH", "SHORTEST_PATH", "OUTBOUND", "INBOUND",
			"ANY", "ALL", "NONE", "AND", "OR", "NOT", "TRUE", "FALSE", "NULL",
			"LIKE", "INTO", "OPTIONS", "KEEP", "PRUNE", "SEARCH", "TO", "FROM",
			"TRAVERSAL", "NEIGHBORS", "K_SHORTEST_PATHS", "K_PATHS", "ALL_SHORTEST_PATHS",
			"ALL_PATHS", "ANY_PATH", "GRAPH_PATHS", "GRAPH_VERTICES", "GRAPH_EDGES",
			"GRAPH_NEIGHBORS", "GRAPH_TRAVERSAL", "GRAPH_SHORTEST_PATH", "GRAPH_K_SHORTEST_PATHS",
			"GRAPH_K_PATHS", "GRAPH_ALL_SHORTEST_PATHS", "GRAPH_ALL_PATHS", "GRAPH_ANY_PATH"
	};
	private static final String[] AQL_FUNCTIONS = {
			"LENGTH", "COUNT", "SUM", "MIN", "MAX", "AVERAGE", "CONCAT", "SUBSTRING",
			"CONTAINS", "UPPER", "LOWER", "LIKE", "RANDOM_TOKEN",
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
	}

	private void updateDatabaseCompletions() {
		docCollectionNames = new ArrayList<>();
		edgeCollectionNames = new ArrayList<>();
		graphNames = new ArrayList<>();
		if (this.database == null) {
			return;
		}

		try {
			Collection<CollectionEntity> collections = this.database.getCollections();
			for (CollectionEntity collection : collections) {
				if (collection.getType() == CollectionType.DOCUMENT) {
					docCollectionNames.add(collection.getName());
				} else if (collection.getType() == CollectionType.EDGES) {
					edgeCollectionNames.add(collection.getName());
				}
			}
			this.database.getGraphs().forEach(graph -> graphNames.add(graph.getName()));
		} catch (Exception e) {
			LOGGER.error("Failed to get collections from database", e);
		}
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
			addCompletion(new BasicCompletion(this, function + "()"));
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
			Element prevElem = root.getElement(previousIndex);
			int prevElemStart = prevElem.getStartOffset();
			int prevSegmentEnd = prevElem.getEndOffset() - 1;
			try {
				doc.getText(prevElemStart, prevSegmentEnd - prevElemStart, previousSegment);
			} catch (BadLocationException ble) {
				ble.printStackTrace();
				return EMPTY_STRING;
			}
			while (!isValidChar(previousSegment.array[prevWordEnd])) {
				prevWordEnd--;
			}

			prevWordStart = prevWordEnd - 1;
			while (prevWordStart >= previousSegment.offset && isValidChar(previousSegment.array[prevWordStart])) {
				prevWordStart--;
			}
			previousIndex--;

		}
		prevWordStart++;

		int prevWordLen = prevWordEnd - prevWordStart + 1;

		previousWord = prevWordLen == 0 ? EMPTY_STRING : new String(previousSegment.array, prevWordStart, prevWordLen);

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
		return Character.isLetterOrDigit(ch) || ch == '_';
	}

	@Override
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {

		int offset = tc.viewToModel2D(p);
		if (offset < 0 || offset >= tc.getDocument().getLength()) {
			lastCompletionsAtText = null;
			return lastParameterizedCompletionsAt = null;
		}

		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(offset);
		Element elem = root.getElement(line);
		int start = elem.getStartOffset();
		int end = elem.getEndOffset() - 1;

		try {

			doc.getText(start, end - start, s);

			// Get the valid chars before the specified offset.
			int startOffs = s.offset + (offset - start) - 1;
			while (startOffs >= s.offset && isValidChar(s.array[startOffs])) {
				startOffs--;
			}

			// Get the valid chars at and after the specified offset.
			int endOffs = s.offset + (offset - start);
			while (endOffs < s.offset + s.count && isValidChar(s.array[endOffs])) {
				endOffs++;
			}

			int len = endOffs - startOffs - 1;
			if (len <= 0) {
				return lastParameterizedCompletionsAt = null;
			}
			String text = new String(s.array, startOffs + 1, len);

			if (text.equals(lastCompletionsAtText)) {
				return lastParameterizedCompletionsAt;
			}

			// Get a list of all Completions matching the text.
			List<Completion> list = getCompletionByInputText(text);
			lastCompletionsAtText = text;
			return lastParameterizedCompletionsAt = list;

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
