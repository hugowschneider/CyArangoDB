package com.github.hugowschneider.cyarangodb.internal.ui.aql;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
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

public class AQLCompletionProvider extends AbstractCompletionProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AQLCompletionProvider.class);

	private final Segment segment;
	private String cachedCompletionsText;
	private List<Completion> cachedParameterizedCompletions;

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

	public AQLCompletionProvider() {
		this(null);
	}

	public AQLCompletionProvider(String[] customWords) {
		this.segment = new Segment();
		initializeAQLCompletions();
		if (customWords != null) {
			addWordCompletions(customWords);
		}
	}

	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		int caretPosition = comp.getCaretPosition();
		Document document = comp.getDocument();
		Element lineElement = document.getDefaultRootElement()
				.getElement(document.getDefaultRootElement().getElementIndex(caretPosition));

		int lineStartOffset = lineElement.getStartOffset();
		int enteredTextLength = caretPosition - lineStartOffset;

		try {
			document.getText(lineStartOffset, enteredTextLength, segment);
		} catch (BadLocationException e) {
			LOGGER.error("Failed to get text from document", e);
			return EMPTY_STRING;
		}

		return extractValidText(segment, enteredTextLength);
	}

	@Override
	public List<Completion> getCompletionsAt(JTextComponent component, Point point) {
		int offset = component.viewToModel2D(point);
		if (offset < 0 || offset >= component.getDocument().getLength()) {
			resetCachedCompletions();
			return cachedParameterizedCompletions;
		}

		try {
			String textAtPoint = getTextAtOffset(component.getDocument(), offset);
			if (textAtPoint.equals(cachedCompletionsText)) {
				return cachedParameterizedCompletions;
			}

			cachedParameterizedCompletions = getCompletionByInputText(textAtPoint);
			cachedCompletionsText = textAtPoint;
		} catch (BadLocationException e) {
			LOGGER.error("Error while getting completions at point", e);
		}

		return cachedParameterizedCompletions;
	}

	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent component) {
		if (getParameterListStart() == 0) {
			return null;
		}

		int caretPosition = component.getCaretPosition();
		Document document = component.getDocument();
		Element lineElement = document.getDefaultRootElement()
				.getElement(document.getDefaultRootElement().getElementIndex(caretPosition));

		try {
			int lineStartOffset = lineElement.getStartOffset();
			int queryTextLength = caretPosition - lineStartOffset - 1;
			if (queryTextLength <= 0) {
				return null;
			}

			document.getText(lineStartOffset, queryTextLength, segment);

			String identifier = extractIdentifier(segment, queryTextLength);
			return filterParameterizedCompletions(identifier);
		} catch (BadLocationException e) {
			LOGGER.error("Failed to get parameterized completions", e);
		}

		return null;
	}

	private void resetCachedCompletions() {
		cachedCompletionsText = null;
		cachedParameterizedCompletions = null;
	}

	private String getTextAtOffset(Document document, int offset) throws BadLocationException {
		Element rootElement = document.getDefaultRootElement();
		int lineIndex = rootElement.getElementIndex(offset);
		Element lineElement = rootElement.getElement(lineIndex);

		int lineStartOffset = lineElement.getStartOffset();
		int lineEndOffset = lineElement.getEndOffset() - 1;

		document.getText(lineStartOffset, lineEndOffset - lineStartOffset, segment);

		int validTextStart = findValidTextStart(segment, offset - lineStartOffset);
		int validTextEnd = findValidTextEnd(segment, offset - lineStartOffset);

		return new String(segment.array, validTextStart + 1, validTextEnd - validTextStart - 1);
	}

	private String extractValidText(Segment segment, int length) {
		int segmentEnd = segment.offset + length;
		int validTextStart = segmentEnd - 1;

		while (validTextStart >= segment.offset && isValidChar(segment.array[validTextStart])) {
			validTextStart--;
		}

		validTextStart++;
		int validTextLength = segmentEnd - validTextStart;

		return validTextLength == 0 ? EMPTY_STRING : new String(segment.array, validTextStart, validTextLength);
	}

	private String extractIdentifier(Segment segment, int length) {
		int endOffset = segment.offset + length - 1;

		while (endOffset >= segment.offset && Character.isWhitespace(segment.array[endOffset])) {
			endOffset--;
		}

		int startOffset = endOffset;

		while (startOffset >= segment.offset && isValidChar(segment.array[startOffset])) {
			startOffset--;
		}

		return new String(segment.array, startOffset + 1, endOffset - startOffset);
	}

	private List<ParameterizedCompletion> filterParameterizedCompletions(String identifier) {
		List<ParameterizedCompletion> parameterizedCompletions = new ArrayList<>();
		List<Completion> completions = getCompletionByInputText(identifier);

		if (completions != null) {
			for (Completion completion : completions) {
				if (completion instanceof ParameterizedCompletion) {
					parameterizedCompletions.add((ParameterizedCompletion) completion);
				}
			}
		}

		return parameterizedCompletions.isEmpty() ? null : parameterizedCompletions;
	}

	private int findValidTextStart(Segment segment, int offset) {
		int validTextStart = segment.offset + offset - 1;
		while (validTextStart >= segment.offset && isValidChar(segment.array[validTextStart])) {
			validTextStart--;
		}
		return validTextStart;
	}

	private int findValidTextEnd(Segment segment, int offset) {
		int validTextEnd = segment.offset + offset;
		while (validTextEnd < segment.offset + segment.count && isValidChar(segment.array[validTextEnd])) {
			validTextEnd++;
		}
		return validTextEnd;
	}

	protected boolean isValidChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_';
	}

	private void initializeAQLCompletions() {
		for (String keyword : AQL_KEYWORDS) {
			addCompletion(new BasicCompletion(this, keyword));
		}
		for (String function : AQL_FUNCTIONS) {
			addCompletion(new BasicCompletion(this, function + "()"));
		}
	}
}
