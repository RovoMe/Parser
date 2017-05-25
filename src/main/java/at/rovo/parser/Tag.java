package at.rovo.parser;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a HTML or XML tag which starts with a '&lt;' character and ends with a '>' character.
 * <p>
 * This class does not provide any methods to retrieve any attribute informations of the element like the href attribute
 * in &lt;a href="..."> or the class attribute in &lt;div class="..."> tags.
 *
 * @author Roman Vottner
 */
@SuppressWarnings("unused")
public class Tag extends Token
{
    /** The logger of this class **/
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Used by TSReC parser. Will contain list of appended tokens that will appear as sub-elements of this tokens. This
     * are not exactly children per se as TSReC will append Words and Tags that do not break the text flow.
     **/
    private List<Token> appendedTokens = new ArrayList<>();
    /** will contain the attributes of this tag **/
    private Hashtable<String, String> attributes = null;

    /**
     * Initializes a new instance of a HTML tag. The parameter is the full content of this tag. This means if you need
     * to specify f.e. a div element with a class attribute you can instantiate a new tag like follows:
     * <p>
     * <code>Tag div = new Tag("&lt;div class=\"...\">");</code>
     * <p>
     * or start a tag and append the rest later via invoking {@link #append(String)}
     * <p>
     * <code>Tag div = new Tag("&lt;div"); <br/>div.append(" class=\"...\">");</code>
     *
     * @param text
     *         Content of the tag
     */
    public Tag(String text)
    {
        super(text);
    }

    public Tag(int id, String name, int parent, int numSiblings, int level)
    {
        super(id, name, null, level, parent, numSiblings);
    }

    /**
     * Creates a new instance of a tag through deep copying the data of the other tag.
     *
     * @param node
     *         The tag to copy
     */
    public Tag(Token node)
    {
        super(node);

        if (node != null)
        {
            // deep copy
            if (node.matchedNode != null)
            {
                if (node.matchedNode instanceof Word)
                {
                    this.matchedNode = new Word((Word) node.matchedNode);
                }
                else
                {
                    this.matchedNode = new Tag((Tag) node.matchedNode);
                }
            }
        }
    }

    public Tag(Tag node)
    {
        super(node);
        if (node != null)
        {
            if (node.matchedNode != null)
            {
                this.matchedNode = new Tag((Tag) node.matchedNode);
            }
        }
    }

    /**
     * Appends further text to a tag unless it is not closed yet.
     *
     * @param text
     *         The text which should be added to the tag. Note that appending occurs inside the '&lt;' and '>' parts of
     *         the tag and not outside!
     *
     * @return The current content of the tag
     */
    String append(String text)
    {
        if (!this.isValid())
        {
            this.html.append(" ");
            this.html.append(text);
        }
        return this.html.toString();
    }

    /**
     * Adds a token as a sub element to this tag.
     *
     * @param token
     *         The token to add as a sub element to this tag
     */
    void append(Token token)
    {
        this.appendedTokens.add(token);
    }

    /**
     * Returns all sub elements for this tag. Note that this elements are only available for a TSReC parser.
     *
     * @return The sub elements for this tag
     */
    public List<Token> getSubElements()
    {
        return this.appendedTokens;
    }

    /**
     * Determines if this tag is valid or not.
     * <p>
     * A tag is valid if it matches either of the following patterns: <ul> <li><code>&lt;!-- ... --></code></li>
     * <li><code>&lt;![ ... ]]></code></li> <li><code>&lt; ... ></code></li> </ul> Note that this method does not check
     * for tags inside of comments!
     * <p>
     * If a tag is valid and not a comment, its attributes are extracted automatically.
     *
     * @return true if the tag is valid; false otherwise
     */
    boolean isValid()
    {
        boolean ret = false;
        boolean containsAttribute = true;
        if (this.html.toString().startsWith("<!--") && this.html.toString().endsWith("-->"))
        {
            ret = true;
            containsAttribute = false;
        }
        else if (this.html.toString().toLowerCase().startsWith("<script") &&
                 this.html.toString().toLowerCase().endsWith("/script>"))

        {
            ret = true;
            containsAttribute = false;
        }
        else if (this.html.toString().toLowerCase().startsWith("<noscript") &&
                 this.html.toString().toLowerCase().endsWith("/noscript>"))
        {
            ret = true;
            containsAttribute = false;
        }
        else if (this.html.toString().startsWith("<![") && this.html.toString().endsWith("]]>"))
        {
            ret = true;
            containsAttribute = false;
        }
        else if (!this.html.toString().startsWith("<!--") && this.html.toString().endsWith(">"))
        {
            ret = true;
        }

        if (ret && containsAttribute && this.attributes == null)
        {
            this.setAttributes();
        }
        return ret;
    }

    /**
     * After a tag validated to true it sets all attributes assigned to the tag.
     */
    private void setAttributes()
    {
        String html = this.html.toString();
        // if we are in compact mode and have a script, fetch only the
        // script start
        if (html.endsWith("</script>"))
        {
            html = html.substring(0, html.indexOf(">"));
        }
        // remove the ending signs of the token
        if (html.endsWith("/>"))
        {
            html = html.substring(0, html.length() - 2);
        }
        else if (html.endsWith(">"))
        {
            html = html.substring(0, html.length() - 1);
        }

        // split the attributes into tokens
        String[] tokens = html.split(" ");
        this.attributes = new Hashtable<>();
        String currArg = null;
        // omit the first token as it is the tag-name
        for (int i = 1; i < tokens.length; i++)
        {
            // check if the token has an assignment-character
            if (tokens[i].contains("=\""))
            {
                String[] arg = tokens[i].split("=\"");
                // catch constructs like: onClick =" window.open('...')"
                if (arg.length == 0)
                {
                    this.attributes.put(tokens[i - 1], checkToken(tokens[i + 1]));
                    i++;
                    currArg = tokens[i - 1];
                }
                else
                {
                    // catch constructs like: href=""
                    if (arg.length == 1)
                    {
                        this.attributes.put(arg[0], "");
                    }
                    else
                    {
                        this.attributes.put(arg[0], checkToken(arg[1]));
                    }
                    currArg = arg[0];
                }
            }
            // attribute is split up into multiple tokens, join them as long
            // as no ending quote is reached
            else if (currArg != null)
            {
                String val = this.attributes.get(currArg) + " " + tokens[i];
                this.attributes.put(currArg, checkToken(val));

                if (val.endsWith("\""))
                {
                    currArg = null;
                }
            }
        }
    }

    /**
     * Removes characters that indicate a tag ending.
     *
     * @param value
     *         The token to check for tag-endings
     *
     * @return The corrected token if it contained a tag ending symbol, else the origin token
     */
    private static String checkToken(String value)
    {
        if (value.endsWith("/>"))
        {
            value = value.substring(0, value.length() - 2);
        }
        else if (value.endsWith(">"))
        {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    /**
     * Returns all defined attributes and their content for this tag.
     *
     * @return All defined attributes including their content
     */
    public Map<String, String> getAttributes()
    {
        return this.attributes;
    }

    /**
     * Returns the content for a certain attribute.
     *
     * @param attribute
     *         The attribute whose content should be returned
     *
     * @return The content of the specified attribute
     */
    public String getAttribute(String attribute)
    {
        if (this.attributes.contains(attribute))
        {
            return this.attributes.get(attribute);
        }
        return "";
    }

    /**
     * Adds or replaces an existing attribute with the provided content.
     *
     * @param attribute
     *         The attribute to add or modify
     * @param content
     *         The content of the attribute to add or modify
     */
    public void setAttribute(String attribute, String content)
    {
        this.attributes.put(attribute, content);
    }

    /**
     * Determines if the tag includes an inline closing symbol &lt;... />, f.e. &lt;div /> or &lt;br/>.
     *
     * @return true if the tag contains an inline closing symbol; false otherwise
     */
    public boolean isInlineClosingTag()
    {
        return this.html.toString().endsWith("/>");
    }

    /**
     * Determines if this tag is a opening tag
     * <p>
     * Opening tags do no include any of the following patterns: <ul> <li><code>&lt;/...></code>, f.e.
     * <code>&lt;/div></code></li> <li><code>--></code></li> <li><code>]]></code></li> </ul>
     *
     * @return true if this tag is an opening tag; false otherwise
     */
    public boolean isOpeningTag()
    {
        return !(this.html.toString().startsWith("</") || this.html.toString().endsWith("-->") ||
                 this.html.toString().endsWith("]]>"));
    }

    /**
     * Determines if this tag is a comment. A comment matches either of the following patterns: <ul> <li><code>&lt;!--
     * ... --></code></li> <li><code>&lt;![ ... ]]></code></li> </ul>
     *
     * @return true if this tag is a comment; false otherwise
     */
    public boolean isComment()
    {
        return this.html != null && (this.html.toString().startsWith("<!--") || this.html.toString().endsWith("-->") ||
                                     this.html.toString().startsWith("<![") || this.html.toString().endsWith("]]>"));
    }

    /**
     * Reflects the type of HTML tag this tag represents </> The type of an HTML tag is the first part of the tag, f.e.
     * the type of <code>&lt;div class="..."></code> will be 'div' <ul> <li><code>&lt;div class="..."></code> return
     * 'div'</li> <li><code>&lt;/p></code> returns 'p'</li> <li><code>&lt;!-- ... --></code> returns ''</li> </ul>
     * <p>
     * As from the examples above can be seen, comments to not return any short tag!
     *
     * @return Returns the type of HTML tag this tag corresponds to
     */
    public String getShortTag()
    {
        String shortTag = "";
        // closing tags
        if (this.html.toString().startsWith("</"))
        {
            shortTag = this.html.substring(2);
            if (shortTag.endsWith(">"))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(">"));
            }
            if (shortTag.contains(" "))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(" "));
            }
            if (shortTag.contains("."))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf("."));
            }
            if (shortTag.contains(":"))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(":"));
            }
            // should not happen that a tag contains " - but unfortunately it
            // does
            if (shortTag.contains("\""))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf("\""));
            }
        }
        // opening tags
        else if (this.html.length() > 1 && this.html.toString().startsWith("<")
		/* && this.html.charAt(1) != '!' && this.html.charAt(1) != '[' */)
        {
            if (this.html.toString().startsWith("<!--"))
            {
                return "<!--";
            }
            // remove leading <
            shortTag = this.html.substring(1);
            if (shortTag.contains(" "))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(" "));
            }
            if (shortTag.contains("."))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf("."));
            }
            if (shortTag.contains(":"))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(":"));
            }
            if (shortTag.contains(">"))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf(">"));
            }
            // should not happen that a tag contains " - but unfortunately it
            // does
            if (shortTag.contains("\""))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf("\""));
            }
            // remove inline closings
            if (shortTag.endsWith("/"))
            {
                shortTag = shortTag.substring(0, shortTag.indexOf("/"));
            }
        }
        return shortTag;
    }

    /**
     * Renames a tag to &lt;UNKNOWN> if it is an opening tag or to &lt;/UNKNOWN> if it is a closing tag.
     */
    public void setAsUndefined()
    {
        if (this.isOpeningTag())
        {
            this.html = new StringBuilder("<unknown>");
        }
        else
        {
            this.html = new StringBuilder("</unknown>");
        }
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + this.html.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        Tag tag;
        if (obj instanceof Tag)
        {
            tag = (Tag) obj;
        }
        else
        {
            return false;
        }

        return this.html.toString().equalsIgnoreCase(tag.html.toString());
    }
}
