
import javax.swing.text.JTextComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import com.github.hugowschneider.cyarangodb.internal.ui.aql.AQLCompletionProvider;;

public class AQLCompletionProviderTest {

    private AQLCompletionProvider completionProvider;
    private JTextComponent textComponent;

    @BeforeEach
    public void setUp() {
        completionProvider = new AQLCompletionProvider(null);
        textComponent = new JTextArea();
    }

    @Test
    @DisplayName("AQlCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the same line ")
    public void testGetAlreadyEnteredTextSameLine() throws BadLocationException {
        { 
            String text = "FROM IN A";
            textComponent.setText(text);
            textComponent.setCaretPosition(text.length());
            assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
            assertEquals("IN", completionProvider.getPreviousWord());
        }
    }

    @Test
    @DisplayName("AQlCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the previous line ")
    public void testGetAlreadyEnteredTextPreviousLine() throws BadLocationException {
        { 
            String text = "FROM IN\nA";
            textComponent.setText(text);
            textComponent.setCaretPosition(text.length());
            assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
            assertEquals("IN", completionProvider.getPreviousWord());
        }
    }

    @Test
    @DisplayName("AQlCompletionProvider::getAlreadyEnteredText should return the already entered word and the previous word in the 2 lines before")
    public void testGetAlreadyEnteredText2LinesBefore() throws BadLocationException {
        { 
            String text = "FROM IN\n\nA";
            textComponent.setText(text);
            textComponent.setCaretPosition(text.length());
            assertEquals("A", completionProvider.getAlreadyEnteredText(textComponent));
            assertEquals("IN", completionProvider.getPreviousWord());
        }
    }

}
