package at.rovo.parser;

import at.rovo.common.UrlReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A parser reads a HTML page or its text representation as {@link String} into memory and splits the text into {@link
 * Token}s - namely {@link Tag}s and {@link Word}s.
 * <p>
 * The tokens are stored within a {@link List} without adding any ancestor information. The respective fields of the
 * {@link Tag} instances are initialized with 0 therefore.
 * <p>
 * The parser allows omitting specified HTML tags through invocation of the according clean method. Cleaning a HTML tag
 * removes the tag as well as the content between the opening and closing tag, which might include further HTML tags.
 * <p>
 * Words can be combined to a single word token by invoking {@link #combineWords(boolean)} with a true parameter.
 * <p>
 * By default IFrame-, Script-, NoScript-, Link-, Style-, Form-, Doctype-Tags and Comments are removed.
 *
 * @author Roman Vottner
 */
@SuppressWarnings("unused")
public class Parser
{
    /** The logger of this class **/
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Specifies if words inside a tag should be combined into a single word-segment
     **/
    protected boolean combineWords = false;
    /**
     * Specifies to exclude word tokens. Words are set within {@link Tag#setText(String)}
     */
    protected boolean excludeWordTokens = false;
    /** Tags that collect everything between opening and closing tags **/
    protected List<String> compactTags = new ArrayList<>();

    /**
     * If set to true will result in META tags to be removed from the token list
     **/
    private boolean cleanMeta = false;
    /**
     * If set to true will result in IFRAME tags to be removed from the token list
     **/
    private boolean cleanIFrame = true;
    /**
     * If set to true will result in LINK tags to be removed from the token list
     **/
    private boolean cleanLinks = true;
    /**
     * If set to true will result in SCRIPT tags to be removed from the token list
     **/
    private boolean cleanScripts = true;
    /**
     * If set to true will result in NOSCRIPT tags to be removed from the token list
     **/
    private boolean cleanNoScripts = true;
    /**
     * If set to true will result in STYLE tags to be removed from the token list
     **/
    private boolean cleanStyles = true;
    /**
     * If set to true will result in FORM tags to be removed from the token list
     **/
    private boolean cleanFormElements = true;
    /**
     * If set to true will result in IMG tags to be removed from the token list
     **/
    private boolean cleanImages = false;
    /**
     * If set to true will result in A tags to be removed from the token list
     **/
    private boolean cleanAnchors = false;
    /**
     * If set to true will result in Comments to be removed from the token list
     **/
    private boolean cleanComments = true;
    /**
     * If set to true will result in DOCTYPE tags to be removed from the token list
     **/
    private boolean cleanDoctypes = true;
    /**
     * If set to true will include line breaks within the token sequence
     */
    private boolean includeLineBreaks = false;

    // Fields required during tag processing
    /** The last HTML tag found **/
    private Tag tag = null;

    /** The last word added to the tokenList **/
    protected Word lastWord = null;
    /**
     * The tag's short name which is currently compacted; null if no compaction is carried out at the moment
     **/
    private String compact = null;

    /** The ID of the token **/
    protected int id = 0;
    /** The number of words found **/
    protected int numWords = 0;
    /** The number of tags found **/
    protected int tagPos = 0;
    /** Meta-data information **/
    protected ParsingMetaData metaData = null;
    /**
     * Flag which indicates if a preceding tag was parsed completely or if it still open
     **/
    protected boolean tagFinished = true;

    /**
     * Initializes a new instance and sets default values for certain fields like the tags that are combined or tags
     * that ignore parenting.
     */
    public Parser()
    {
        if (this.cleanIFrame)
        {
            this.compactTags.add("iframe");
        }
        if (this.cleanScripts)
        {
            this.compactTags.add("script");
        }
        if (this.cleanNoScripts)
        {
            this.compactTags.add("noscript");
        }
        if (this.cleanFormElements)
        {
            this.compactTags.add("form");
        }
        if (this.cleanComments)
        {
            this.compactTags.add("<!--");
        }
        if (this.cleanStyles)
        {
            this.compactTags.add("style");
        }
        if (this.cleanAnchors)
        {
            this.compactTags.add("a");
        }
    }

    /**
     * Defines if a META tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove META tags; false will keep them in the result
     */
    public void cleanMeta(boolean clean)
    {
        this.cleanMeta = clean;
    }

    /**
     * Defines if a IFRAME tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove IFRAME tags; false will keep them in the result
     */
    public void cleanIFrame(boolean clean)
    {
        this.cleanIFrame = clean;
    }

    /**
     * Defines if a LINK tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove LINK tags; false will keep them in the result
     */
    public void cleanLinks(boolean clean)
    {
        this.cleanLinks = clean;
    }

    /**
     * Defines if a SCRIPT tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove SCRIPT tags; false will keep them in the result
     */
    public void cleanScripts(boolean clean)
    {
        this.cleanScripts = clean;
        if (clean && !this.compactTags.contains("script"))
        {
            this.compactTags.add("script");
        }
        else if (!clean)
        {
            this.compactTags.remove("script");
        }
    }

    /**
     * Defines if a NOSCRIPT tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove NOSCRIPT tags; false will keep them in the result
     */
    public void cleanNoScripts(boolean clean)
    {
        this.cleanNoScripts = clean;
        if (clean && !this.compactTags.contains("noscript"))
        {
            this.compactTags.add("noscript");
        }
        else if (!clean)
        {
            this.compactTags.remove("noscript");
        }
    }

    /**
     * Defines if a STYLE tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove STYLE tags; false will keep them in the result
     */
    public void cleanStyles(boolean clean)
    {
        this.cleanStyles = clean;
        if (clean && !this.compactTags.contains("style"))
        {
            this.compactTags.add("style");
        }
        else if (!clean)
        {
            this.compactTags.remove("style");
        }
    }

    /**
     * Defines if a FORM tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove FORM tags; false will keep them in the result
     */
    public void cleanFormElements(boolean clean)
    {
        this.cleanFormElements = clean;
        if (clean && !this.compactTags.contains("form"))
        {
            this.compactTags.add("form");
        }
        else if (!clean)
        {
            this.compactTags.remove("form");
        }
    }

    /**
     * Defines if a IMG tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove IMG tags; false will keep them in the result
     */
    public void cleanImages(boolean clean)
    {
        this.cleanImages = clean;
    }

    /**
     * Defines if a A tag should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove A tags; false will keep them in the result
     */
    public void cleanAnchors(boolean clean)
    {
        this.cleanAnchors = clean;
        if (clean && !this.compactTags.contains("a"))
        {
            this.compactTags.add("a");
        }
        else if (!clean)
        {
            this.compactTags.remove("a");
        }
    }

    /**
     * Defines if a comments should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove comments; false will keep them in the result
     */
    public void cleanComments(boolean clean)
    {
        this.cleanComments = clean;
        if (clean && !this.compactTags.contains("<!--"))
        {
            this.compactTags.add("<!--");
        }
        else if (!clean)
        {
            this.compactTags.remove("<!--");
        }
    }

    /**
     * Defines if DOCTYPE tags should be removed from the parsed token list.
     *
     * @param clean
     *         True specifies to remove DOCTYPE tags; false will keep them in the result
     */
    public void cleanDoctypes(boolean clean)
    {
        this.cleanDoctypes = clean;
    }

    /**
     * If set to true will include linebreak symbols within the token sequence, else linebreak symbols will be omitted.
     *
     * @param includeLineBreaks
     *         Tue specifies to include line breaks within the returned token sequence, false will omit those line
     *         breaks. By default line breaks will be omitted
     */
    public void setIncludeLineBreaks(boolean includeLineBreaks)
    {
        this.includeLineBreaks = includeLineBreaks;
    }

    /**
     * Returns if META tags are removed from the token list.
     *
     * @return True if META tags are removed from the token list; false otherwise
     */
    public boolean cleanMeta()
    {
        return this.cleanMeta;
    }

    /**
     * Returns if IFRAME tags are removed from the token list.
     *
     * @return True if IFRAME tags are removed from the token list; false otherwise
     */
    public boolean cleanIFrame()
    {
        return this.cleanIFrame;
    }

    /**
     * Returns if LINK tags are removed from the token list.
     *
     * @return True if LINK tags are removed from the token list; false otherwise
     */
    public boolean cleanLinks()
    {
        return this.cleanLinks;
    }

    /**
     * Returns if SCRIPT tags are removed from the token list.
     *
     * @return True if SCRIPT tags are removed from the token list; false otherwise
     */
    public boolean cleanScripts()
    {
        return this.cleanScripts;
    }

    /**
     * Returns if NOSCRIPT tags are removed from the token list.
     *
     * @return True if NOSCRIPT tags are removed from the token list; false otherwise
     */
    public boolean cleanNoScripts()
    {
        return this.cleanScripts;
    }

    /**
     * Returns if STYLE tags are removed from the token list.
     *
     * @return True if STYLE tags are removed from the token list; false otherwise
     */
    public boolean cleanStyles()
    {
        return this.cleanStyles;
    }

    /**
     * Returns if FORM tags are removed from the token list.
     *
     * @return True if FORM tags are removed from the token list; false otherwise
     */
    public boolean cleanFormElements()
    {
        return this.cleanFormElements;
    }

    /**
     * Returns if IMG tags are removed from the token list.
     *
     * @return True if IMG tags are removed from the token list; false otherwise
     */
    public boolean cleanImages()
    {
        return this.cleanImages;
    }

    /**
     * Returns if A tags are removed from the token list.
     *
     * @return True if A tags are removed from the token list; false otherwise
     */
    public boolean cleanAnchors()
    {
        return this.cleanAnchors;
    }

    /**
     * Returns if comments are removed from the token list.
     *
     * @return True if comments are removed from the token list; false otherwise
     */
    public boolean cleanComments()
    {
        return this.cleanComments;
    }

    /**
     * Returns if DOCTYPE tags are removed from the token list.
     *
     * @return True if DOCTYPE tags are removed from the token list; false otherwise
     */
    public boolean cleanDoctypes()
    {
        return this.cleanDoctypes;
    }

    /**
     * Specifies if words between tags should be combined to one single object (true) or if they should be kept
     * separated in the resulting token list (false).
     * <p>
     * By default words will not get combined.
     *
     * @param combineWords
     *         Set to true will combine words between tags to a single word object, false will generate a word object
     *         for each word
     */
    public void combineWords(boolean combineWords)
    {
        this.combineWords = combineWords;
    }

    /**
     * Returns if words between tags should be combined to a single word or if they are kept separated.
     *
     * @return True if words between tags are being combined to a single word; false otherwise
     */
    public boolean isWordCombined()
    {
        return this.combineWords;
    }

    /**
     * Returns if words are not included within the list of parsed tokens. If they are excluded, words can be found in
     * {@link Tag#getText()} method.
     *
     * @return True if words are excluded from the token list, false otherwise
     */
    public boolean isWordExcluded()
    {
        return this.excludeWordTokens;
    }

    /**
     * Resets the state of the currently running Parser instance to match the state of a new instance.
     */
    protected void reset()
    {
        this.tag = null;
        this.lastWord = null;
        this.compact = null;
        this.id = 0;
        this.numWords = 0;
        this.tagPos = 0;
        this.metaData = new ParsingMetaData();
        this.tagFinished = true;
    }

    /**
     * Builds a {@link List} of {@link Token}s representing the page referenced by the URL provided.
     * <p>
     * This method fetches the content of the URL provided and hands it over to {@link #tokenize(String, boolean)}
     * method.
     *
     * @param url
     *         A {@link String} representing the URL of the page to split up into tokens
     * @param formatText
     *         Indicates if the output tokens should be formatted
     *
     * @return A {@link List} of {@link Token}s representing the HTML page
     */
    public ParseResult tokenizeURL(String url, boolean formatText)
    {
        if (url != null && !url.equals(""))
        {
            LOG.debug("Reading page from URL: {}", url);

            UrlReader reader = new UrlReader();
            String html = reader.readPage(url);

            return this.tokenize(html, formatText);
        }
        else
        {
            throw new IllegalArgumentException("Invalid URL passed. Got: " + url);
        }
    }

    /**
     * Builds a {@link List} of {@link Token}s representing the provided string.
     *
     * @param html
     *         A {@link String} representing the full HTML code of a web site
     * @param formatText
     *         Indicates if the tokens should be formatted or not
     *
     * @return A {@link List} of {@link Token}s representing the HTML page
     */
    public ParseResult tokenize(String html, boolean formatText)
    {
        ParseResult result = new ParseResult();
        if (html == null || html.equals(""))
        {
            throw new IllegalArgumentException("Invalid html string passed.");
        }

        this.reset();

        // split the html into a token-array
        LOG.debug("Splitting page");
        LOG.debug("Received HTML: '{}'", html);

        // parse and process the tokens from the HTML file
        List<Token> tokenList = this.parseToTokens(html, " ", ">", "<", formatText);

        // generate the result
        result.setTitle(metaData.getTitle());
        result.setParsedTokens(tokenList);
        result.setAuthorName(metaData.getAuthorNames());
        result.setAuthors(metaData.getAuthor());
        result.setPublishDate(metaData.getDate());
        result.setByline(metaData.getByline());
        result.setNumWords(numWords);
        result.setNumTokens(tokenList.size());
        result.setNumTags(id);

        return result;
    }

    /**
     * Parses the HTML content into tokens of words and tags and calls {@link #processTokens(String, List, Stack,
     * boolean)} to further process these tokens.
     *
     * @param text
     *         The HTML text to parse
     * @param replace
     *         Splitting characters that should not get included in the resulting tokens
     * @param nonReplace
     *         Splitting characters that should get appended to the leading token
     * @param splitAndInclude
     *         Splitting character that is the fist character of the new token
     * @param formatText
     *         Indicates if the output tokens should be formatted
     *
     * @return A {@link List} of {@link Token}s representing the HTML page
     */
    protected List<Token> parseToTokens(String text, String replace, String nonReplace, String splitAndInclude,
                                        boolean formatText)
    {
        List<Token> tokenList = new ArrayList<>();
        Stack<Tag> stack = new Stack<>();
        stack.add(new Tag(0, "", 0, 0, 0));

        char[] replaceChars = replace.toCharArray();
        char[] nonReplaceChars = nonReplace.toCharArray();
        char[] splitAndIncludeChars = splitAndInclude.toCharArray();

        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < text.length(); i++)
        {
            if (this.includeLineBreaks)
            {
                if (text.charAt(i) == '\n' || text.charAt(i) == '\r')
                {
                    tokenList.add(new LineBreak());
                    LOG.debug("\tadded LineBreak");
                    continue;
                }
            }
            for (char c : replaceChars)
            {
                // we found a separating character - split the token at this
                // position - This token should not get included in the
                // final token so continue with the next iteration
                if (text.charAt(i) == c)
                {
                    if (!sb.toString().trim().equals(""))
                    {
                        this.processTokens(sb.toString(), tokenList, stack, formatText);
                    }
                    sb = new StringBuilder();
                    found = true;
                    break;
                }
            }

            for (char c : splitAndIncludeChars)
            {
                // we found a separating character - split the token at this
                // position and include the splitting character to the newly
                // created token
                if (text.charAt(i) == c)
                {
                    if (!sb.toString().trim().equals(""))
                    {
                        this.processTokens(sb.toString(), tokenList, stack, formatText);
                    }
                    sb = new StringBuilder();
                }
            }

            // we have already found a replaceable char - this should not be
            // added to the token
            if (found)
            {
                found = false;
                continue;
            }

            sb.append(text.charAt(i));

            for (char c : nonReplaceChars)
            {
                // we found a separating character - split the token and add
                // the separating character to the old token as its last
                // character
                if (text.charAt(i) == c)
                {
                    if (!sb.toString().trim().equals(""))
                    {
                        this.processTokens(sb.toString(), tokenList, stack, formatText);
                    }
                    sb = new StringBuilder();
                    found = true;
                    break;
                }
            }

            if (found)
            {
                found = false;
            }
        }

        return tokenList;
    }

    /**
     * Processes parsed tokens. Processing involves filtering unneeded tags and building a DOM tree.
     *
     * @param token
     *         The current token that needs to be processed
     * @param tokenList
     *         The list of parsed HTML tags and words contained in the HTML page that is currently parsed
     * @param stack
     *         This method will ignore the stack
     * @param formatText
     *         Indicates if the output tokens should be formated
     */
    protected void processTokens(String token, List<Token> tokenList, Stack<Tag> stack, boolean formatText)
    {
        // remove whitespace characters
        token = token.trim();

        // valid tokens start with a < character
        if (token.startsWith("<") &&
            // a valid tag needs at least 2 characters
            token.length() > 1 &&
            // create a new tag if either there is no tag from the previous
            // iteration or it was no script tag
            (this.tag == null || !this.tag.getHTML().toLowerCase().startsWith("<script") ||
             // create a script tag only if it is a complete tag
             // we might find a token that equals '<playlistArr.length;i++){'
             // don't treat is as a tag
             this.tag.getHTML().toLowerCase().startsWith("<script") && token.toLowerCase().endsWith("</script>"))
            // only add a new tag if the previous tag is complete and there
            // is no compact in progress
            && this.tagFinished && this.compact == null)
        {
            // we found a new tag either after a word or a preceding tag
            this.tagFinished = false;
            this.tag = new Tag(token);

            // check if we should compact tokens between certain tokens
            // together
            for (String compactTag : this.compactTags)
            {
                if (this.tag.getShortTag().toLowerCase().equals(compactTag))
                {
                    if (compactTag.equals("<!--") && !token.endsWith("-->"))
                    {
                        this.compact = "--";
                    }
                    // check if the found tag was provided as a one-line-tag
                    // f.e. <!--empty-->
                    else if (compactTag.equals("<!--") && token.endsWith("-->"))
                    {
                        this.compact = null;
                    }
                    else
                    {
                        this.compact = compactTag;
                    }

                    break;
                }
            }
            this.lastWord = null;

            if (token.endsWith(">"))
            {
                tag.setName(this.getTagName(tag.getHTML()));
            }

            LOG.trace("Processing new Tag: '{}'", this.tag.getHTML());

            this.checkTagValidity(this.tag, tokenList, stack);
        }
        else if (this.tag != null && !this.tagFinished && this.compact == null)
        {
            // the preceding tag was not yet finished so append the token to tag
            this.tag.append(token);

            if (token.endsWith(">"))
            {
                this.tag.setName(this.getTagName(this.tag.getHTML()));
            }

            LOG.trace("Appending '{}' to Tag: '{}'", token, this.tag.getHTML());

            this.checkTagValidity(this.tag, tokenList, stack);
        }
        // compact specified token sequences like scripts or iframes
        else if (this.compact != null && this.tag != null)
        {
            // as appending content to an already valid tag is not possible,
            // we have to set the content manually
            LOG.trace("Compacting Tag with '{}'", token);
            this.tag.setHTML(this.tag.getHTML() + " " + token);
            // check if the end of the tag sequence was reached
            if (this.tag.getHTML().endsWith(this.compact + ">"))
            {
                // check if there are nested script declarations
                // eg. <script ...>document.write(' <script...>...</script')
                if (compact.toLowerCase().equals("script"))
                {
                    // count the number of <script ...> tags
                    int scriptCount = 1;
                    int scriptPos = this.tag.getHTML().toLowerCase().indexOf("<script", 1);
                    while (scriptPos != -1)
                    {
                        scriptCount++;
                        scriptPos = this.tag.getHTML().toLowerCase().indexOf("<script", scriptPos + 1);
                    }
                    // count the number of </script> tags - due to special encoding
                    int endCount = 0;
                    int endPos = this.tag.getHTML().toLowerCase().indexOf("/script>");
                    while (endPos != -1)
                    {
                        endCount++;
                        endPos = this.tag.getHTML().toLowerCase().indexOf("/script>", endPos + 1);
                    }
                    // check if the number of opening tags is less or equal to
                    // the number of closing tags - sometimes an opening script
                    // is defined as <script while it has a default closing tag
                    // within a documentWrite() method
                    if (scriptCount > endCount)
                    {
                        return;
                    }
                }
                if (token.endsWith(">"))
                {
                    this.tag.setName(this.getTagName(this.tag.getHTML()));
                }
                LOG.trace("Compacting finished for Tag: '{}'", this.tag);
                this.checkTagValidity(this.tag, tokenList, stack);
                this.compact = null;
                this.tagFinished = true;
                this.tag = null;
            }
        }
        else
        {
            LOG.trace("Processing new Word: '{}'", token);
            // preceding tags are all closed and the token doesn't start with
            // an opening tag symbol - so we have a word here
            int numWords = this.addWord(token, this.id, stack, tokenList, formatText);
            this.metaData.checkToken(this.lastWord, this.combineWords);
            // keep track of the id's
            this.id += numWords;
            this.numWords += numWords;

            if (!this.combineWords && !this.excludeWordTokens)
            {
                this.lastWord = null;
            }
        }
    }

    /**
     * Returns a simple representation of the HTML tag which omits everything but the opening and closing-type and the
     * name of the HTML tag.
     *
     * @param token
     *         The HTML text of the tag
     *
     * @return A simple representation of the HTML tag omitting all information like defined classes or id's
     */
    protected String getTagName(String token)
    {
        String rawName = token.replaceAll("[<|/>]", "");
        if (rawName.contains(" "))
        {
            rawName = rawName.substring(0, rawName.indexOf(" "));
        }
        return "<" + (token.charAt(1) == '/' ? "/" : "") + rawName +
               (token.charAt(token.length() - 2) == '/' ? " /" : "") + ">";
    }

    /**
     * Creates a new HTML tag.
     *
     * @param token
     *         The HTML text of the tag to create
     * @param tokenList
     *         The list of already parsed HTML tokens
     * @param stack
     *         This method will ignore the stack
     *
     * @return The newly created HTML tag
     *
     * @throws InvalidAncestorException
     *         Thrown by overriding methods of child classes to indicate that a closing tag has no corresponding opening
     *         tag on the stack
     */
    protected Tag createNewTag(String token, List<Token> tokenList, Stack<Tag> stack) throws InvalidAncestorException
    {
        // identify the tag name
        String tagName = this.getTagName(token);

        // create the new tag
        Tag tag = new Tag(this.id++, tagName, 0, 0, 0);
        tag.setHTML(token);

        return tag;
    }

    /**
     * Checks if a tag is allowed to be added to the list of parsed tokens.
     *
     * @param tag
     *         The tag to check for its validity to be added to the list of parsed tokens
     * @param tokenList
     *         The list of already parsed HTML tokens
     * @param stack
     *         This method will ignore the stack
     */
    protected void checkTagValidity(Tag tag, List<Token> tokenList, Stack<Tag> stack)
    {
        // check if the tag is complete
        if (tag.isValid())
        {
            LOG.trace("Valid tag: '{}'\ttokenList: '{}'", tag, tokenList);
            // remove the flag
            this.tagFinished = true;

            // check if we are allowed to add the tag
            if (!this.needsRemoval(tag))
            {
                LOG.trace("Tag '{}' survived removal", tag);
                // create a new tag object with ancestor and sibling information
                try
                {
                    Tag newTag = this.createNewTag(tag.getHTML(), tokenList, stack);
                    tokenList.add(newTag);

                    newTag.setIndex(this.tagPos++);

                    LOG.debug("added Tag: '{}'", tag);

                    this.metaData.checkTag(newTag);
                }
                catch (InvalidAncestorException iaEx)
                {
                    LOG.error("No valid anchestor found for tag: '" + tag + "'", iaEx);
                }
            }
        }
    }

    /**
     * Adds a word to the list of parsed tokens and provides the possibility to format a word if <em>formatText</em> is
     * set to true.
     *
     * @param word
     *         The word to add to the list of parsed tokens
     * @param id
     *         The id of the word to add
     * @param stack
     *         This method will ignore the stack
     * @param tokenList
     *         The list of parsed tokens
     * @param formatText
     *         Indicates if the output tokens should be formatted
     *
     * @return The number of words added to the list of parsed tokens
     */
    protected int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList, boolean formatText)
    {
        int numWords = 0;

        if (formatText)
        {
            word = ParserUtil.formatText(word);
        }

        //		// split words in case they contain a / or a - but are no URL
        //		if ((word.contains("/") && !word.startsWith("http://")) || word.contains("-"))
        //		{
        //			for (String w : word.split("[/|-]"))
        //			{
        //				numWords += this.addWord(w, id++, stack, tokenList);
        //			}
        //		}
        //		else
        {
            // only a single word to add
            numWords += this.addWord(word, id, stack, tokenList);
            LOG.debug("added Word: {}", word);
        }

        return numWords;
    }

    /**
     * Adds either a new word to the list of parsed tokens or appends the word to the last added word if {@link
     * #combineWords} is set to true.
     *
     * @param word
     *         The word to add
     * @param id
     *         The id of the word to add
     * @param stack
     *         This method will ignore the stack
     * @param tokenList
     *         The list of parsed tokens
     *
     * @return The number of words added; This is 0 if the word got appended to a preceding word
     */
    protected int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList)
    {
        int ret = 0;
        if (this.excludeWordTokens)
        {
            // word tokens should not get appended to the token list, instead
            // words are appended to the last token's text
            this.lastWord.setText(this.lastWord.getText() + " " + word);
            stack.peek().setText(lastWord.getText());
        }
        // check if this word is the first word after a HTML tag and if we
        // should combine words to preceding words
        else if (!this.combineWords || this.lastWord == null || this.lastWord.getText() == null)
        {
            if (this.lastWord == null)
            {
                this.lastWord = new Word(id, word, 0, 0, 0);
                this.lastWord.setText(word);
            }
            else
            {
                this.lastWord.setNo(id);
                this.lastWord.setName(word);
                this.lastWord.setText(word);
                this.lastWord.setLevel(0);
                this.lastWord.setParentNo(0);
                this.lastWord.setSibNo(0);
            }

            // add child to the parent
            tokenList.add(this.lastWord);

            ret = 1;
        }
        else
        {
            // we should combine words and the preceding token was a word too
            // so append the content of this word to the preceding word
            this.lastWord.setText(this.lastWord.getText() + " " + word);
        }

        // update the name of the word
        this.lastWord.setName(this.lastWord.getText());

        return ret;
    }

    /**
     * Checks if a tag needs to be removed.
     *
     * @param tag
     *         The tag which should be checked for removal
     *
     * @return True if the tag needs to be removed; false otherwise
     */
    protected boolean needsRemoval(Tag tag)
    {
        // TODO: better extensibility mechanism wanted
        return (this.cleanComments && tag.isComment() ||
                this.cleanDoctypes && tag.getShortTag().toLowerCase().equals("!doctype") ||
                this.cleanMeta && tag.getShortTag().toLowerCase().equals("meta") ||
                this.cleanIFrame && tag.getShortTag().toLowerCase().equals("iframe") ||
                this.cleanScripts && tag.getShortTag().toLowerCase().equals("script") ||
                this.cleanNoScripts && tag.getShortTag().toLowerCase().equals("noscript") ||
                this.cleanLinks && tag.getShortTag().toLowerCase().equals("link") ||
                this.cleanStyles && tag.getShortTag().toLowerCase().equals("style") ||
                this.cleanFormElements && tag.getShortTag().equals("form") ||
                this.cleanImages && tag.getShortTag().toLowerCase().equals("img") ||
                this.cleanAnchors && tag.getShortTag().toLowerCase().equals("a"));
    }
}
