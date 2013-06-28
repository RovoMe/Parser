package at.rovo.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.rovo.UrlReader;

/**
 * <p>A parser analyzes text and does something with the provided text. This class
 * divides a provided {@link String} into {@link Token}s - namely {@link Tag}s and 
 * {@link Word}s.</p>
 * 
 * @author Roman Vottner
 */
public class Parser 
{
	/** A static logger instance **/
	protected static Logger logger = LogManager.getLogger(Parser.class.getName());
	
	/** Specifies if the erased tags should be removed completely or just the 
	 * content of those tags**/
	protected boolean cleanFully = false; // TODO: currently everything is cleaned fully
	/** Specifies if words inside a tag should be combined into a single 
	 * word-segment **/
	protected boolean combineWords = false;
	/** Tags that collect everything between opening and closing tags **/
	protected List<String> compactTags = new ArrayList<String>();
	/** Tags contained in this list will ignore parenting. This means that tags
	 * following one of these tags will be on the same tree-level like the tag
	 * contained in this list **/
	protected List<String> ignoreParentingTags = new ArrayList<String>();
	
	/** If set to true will result in META tags to be removed from the token 
	 * list **/
	private boolean cleanMeta = false;
	/** If set to true will result in LINK tags to be removed from the token 
	 * list **/
	private boolean cleanLinks = true;
	/** If set to true will result in SCRIPT tags to be removed from the token
	 * list **/
	private boolean cleanScripts = true;
	/** If set to true will result in STYLE tags to be removed from the token
	 * list **/
	private boolean cleanStyles = true;
	/** If set to true will result in FORM tags to be removed from the token 
	 * list **/
	private boolean cleanFormElements = true;
	/** If set to true will result in IMG tags to be removed from the token
	 * list **/
	private boolean cleanImages = false;
	/** If set to true will result in A tags to be removed from the token 
	 * list **/
	private boolean cleanAnchors = false;
	/** If set to true will result in Comments to be removed from the token
	 * list **/
	private boolean cleanComments = true;
	/** If set to true will result in DOCTYPE tags to be removed from the token
	 * list **/
	private boolean cleanDoctypes = true;
	
	// Fields required during tag processing
	
	/** The last word added to the tokenList **/
	protected Word lastWord = null;
	/** The last HTML tag found **/
	protected Tag tag = null;
	/** The position of the token **/
	protected int tokenPos = 0;
	/** The ID of the token **/
	protected int id = 0;
	/** The number of words found **/
	protected int numWords = 0;
	/** The tag's short name which is currently compacted; null if no compaction 
	 * is carried out at the moment **/
	protected String compact = null;
	/** Meta-data informations **/
	protected ParsingMetaData metaData = new ParsingMetaData();
	/** Flag which indicates if a preceding tag was parsed completely or if it
	 * still open **/
	protected boolean tagFinished = true;
	
	/**
	 * <p>Initializes a new instance and sets default values for certain fields
	 * like the tags that are combined or tags that ignore parenting.</p>
	 */
	public Parser()
	{
		this.compactTags.add("iframe");
		if (this.cleanScripts)
			this.compactTags.add("script");
		if (this.cleanFormElements)		
			this.compactTags.add("form");
		if (this.cleanComments)
			this.compactTags.add("<!--");
		if (this.cleanStyles)
			this.compactTags.add("style");
		if (this.cleanAnchors)
			this.compactTags.add("a");
		
		// single line tags
		this.ignoreParentingTags.add("hr");
		this.ignoreParentingTags.add("br");
		this.ignoreParentingTags.add("meta");
		this.ignoreParentingTags.add("link");
		this.ignoreParentingTags.add("img");
		this.ignoreParentingTags.add("!doctype");
		this.ignoreParentingTags.add("input");
	}
	
	/**
	 * <p>Defines if a META tag should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove META tags; false will keep them in 
	 *              the result
	 */
	public void cleanMeta(boolean clean) 
	{ 
		this.cleanMeta = clean; 
	}
	
	/**
	 * <p>Defines if a LINK tag should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove LINK tags; false will keep them in 
	 *              the result
	 */
	public void cleanLinks(boolean clean) 
	{ 
		this.cleanLinks = clean; 
	}
	
	/**
	 * <p>Defines if a SCRIPT tag should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove SCRIPT tags; false will keep them 
	 *              in the result
	 */
	public void cleanScripts(boolean clean) 
	{ 
		this.cleanScripts = clean; 
		if (clean && !this.compactTags.contains("script"))
			this.compactTags.add("script");
		else if (!clean)
			this.compactTags.remove("script");
	}
	
	/**
	 * <p>Defines if a STYLE tag should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove STYLE tags; false will keep them in 
	 *              the result
	 */
	public void cleanStyles(boolean clean) 
	{ 
		this.cleanStyles = clean; 
		if (clean && !this.compactTags.contains("style"))
			this.compactTags.add("style");
		else if (!clean)
			this.compactTags.remove("style");
	}
	
	/**
	 * <p>Defines if a FORM tag should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove FORM tags; false will keep them in 
	 *              the result
	 */
	public void cleanFormElements(boolean clean) 
	{ 
		this.cleanFormElements = clean; 
		if (clean && !this.compactTags.contains("form"))
			this.compactTags.add("form");
		else if (!clean)
			this.compactTags.remove("form");
	}
	
	/**
	 * <p>Defines if a IMG tag should be removed from the parsed token list.</p>
	 * 
	 * @param clean True specifies to remove IMG tags; false will keep them in 
	 *              the result
	 */
	public void cleanImages(boolean clean) 
	{ 
		this.cleanImages = clean; 
	}
	
	/**
	 * <p>Defines if a A tag should be removed from the parsed token list.</p>
	 * 
	 * @param clean True specifies to remove A tags; false will keep them in 
	 *              the result
	 */
	public void cleanAnchors(boolean clean) 
	{ 
		this.cleanAnchors = clean; 
		if (clean && !this.compactTags.contains("a"))
			this.compactTags.add("a");
		else if (!clean)
			this.compactTags.remove("a");
	}
	
	/**
	 * <p>Defines if a comments should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove comments; false will keep them in 
	 *              the result
	 */
	public void cleanComments(boolean clean) 
	{ 
		this.cleanComments = clean; 
		if (clean && !this.compactTags.contains("<!--"))
			this.compactTags.add("<!--");
		else if (!clean)
			this.compactTags.remove("<!--");
	}
	
	/**
	 * <p>Defines if DOCTYPE tags should be removed from the parsed token list.
	 * </p>
	 * 
	 * @param clean True specifies to remove DOCTYPE tags; false will keep them 
	 *              in the result
	 */
	public void cleanDoctypes(boolean clean)
	{
		this.cleanDoctypes = clean;
	}
	
	/**
	 * <p>Returns if META tags are removed from the token list.</p>
	 * 
	 * @return True if META tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanMeta() { return this.cleanMeta; }
	
	/**
	 * <p>Returns if LINK tags are removed from the token list.</p>
	 * 
	 * @return True if LINK tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanLinks() { return this.cleanLinks; }
	
	/**
	 * <p>Returns if SCRIPT tags are removed from the token list.</p>
	 * 
	 * @return True if SCRIPT tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanScripts() { return this.cleanScripts; }
	
	/**
	 * <p>Returns if STYLE tags are removed from the token list.</p>
	 * 
	 * @return True if STYLE tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanStyles() { return this.cleanStyles; }
	
	/**
	 * <p>Returns if FORM tags are removed from the token list.</p>
	 * 
	 * @return True if FORM tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanFormElements() { return this.cleanFormElements; }
	
	/**
	 * <p>Returns if IMG tags are removed from the token list.</p>
	 * 
	 * @return True if IMG tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanImages() { return this.cleanImages; }
	
	/**
	 * <p>Returns if A tags are removed from the token list.</p>
	 * 
	 * @return True if A tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanAnchors() { return this.cleanAnchors; }
	
	/**
	 * <p>Returns if comments are removed from the token list.</p>
	 * 
	 * @return True if comments are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanComments() { return this.cleanComments; }
	
	/**
	 * <p>Returns if DOCTYPE tags are removed from the token list.</p>
	 * 
	 * @return True if DOCTYPE tags are removed from the token list; false 
	 *         otherwise
	 */
	public boolean cleanDoctypes() { return this.cleanDoctypes; }
	
	/**
	 * <p>Add a tag which ignores parenting. This means the proximating tag will
	 * be on the same tree level like this tag.</p>
	 * 
	 * @param tagName The tag name which should ignore parenting
	 */
	public void addTagToIgnoreForParenting(String tagName)
	{
		if (!this.ignoreParentingTags.contains(tagName))
			this.ignoreParentingTags.add(tagName);
	}
	
	/**
	 * <p>Removes a tag from the list of tags that ignore parenting.</p>
	 * 
	 * @param tagName The tag to remove from the list to ignore parenting
	 */
	public void removeTagToIgnoreForParenting(String tagName)
	{
		if (this.ignoreParentingTags.contains(tagName))
			this.ignoreParentingTags.remove(tagName);
	}
	
	/**
	 * <p>Specifies if tags should be removed completely or only their contend.</p>
	 * 
	 * @param cleanFully True will delete the complete tag; false will only 
	 *                   clean the content of the tags which should be removed
	 */
	public void cleanFully(boolean cleanFully)
	{
		this.cleanFully = cleanFully;
	}
	
	/**
	 * <p>Returns if tags get removed completely or only their content.</p>
	 * 
	 * @return True if the tag is removed completely; false if only the content
	 *         is removed
	 */
	public boolean isCleanedFully()
	{
		return this.cleanFully;
	}
	
	/**
	 * <p>Specifies if words between tags should be combined to one single 
	 * object (true) or if they should be kept separated in the resulting
	 * token list (false).</p>
	 * <p>By default words will not get combined.</p>
	 * 
	 * @param combineWords Set to true will combine words between tags to a 
	 *                     single word object, false will generate a word 
	 *                     object for each word
	 */
 	public void combineWords(boolean combineWords)
	{
		this.combineWords = combineWords;
	}
	
 	/**
 	 * <p>Returns if words between tags should be combined to a single word or
 	 * if they are kept separated.</p>
 	 * 
 	 * @return True if words between tags are being combined to a single word; 
 	 *         false otherwise
 	 */
	public boolean isWordCombined()
	{
		return this.combineWords;
	}
				
	/**
	 * <p>Builds a {@link List} of {@link Token}s representing the page referenced
	 * by the URL provided.</p>
	 * <p>This method fetches the content of the URL provided and hands it
	 * over to {@link #tokenize(String)} method.</p>
	 *  
	 * @param html A {@link String} representing the URL of the page to 
	 *             split up into tokens
	 * @param formatText Indicates if the output tokens should be formated
	 * @return A {@link List} of {@link Token}s representing the HTML page
	 */
	public ParseResult tokenizeURL(String url, boolean formatText)
	{
		if (url != null && !url.equals(""))
		{
			if (logger.isDebugEnabled())
				logger.debug("Reading page from URL: "+url);
			
			UrlReader reader = new UrlReader();
			String html = reader.readPage(url);
			
			return this.tokenize(html, formatText);
		}
		else
			throw new IllegalArgumentException("Invalid URL passed. Got: "+url);
	}
	
	/**
	 * <p>Builds a {@link List} of {@link Token}s representing the provided
	 * string.</p>
	 *  
	 * @param html A {@link String} representing the full HTML code of a
	 *             web site
	 * @param formatText Indicates if the tokens should be formated or not
	 * @return A {@link List} of {@link Token}s representing the HTML page
	 */
	public ParseResult tokenize(String html, boolean formatText)
	{
		ParseResult result = new ParseResult();
		if (html == null || html.equals(""))
			throw new IllegalArgumentException("Invalid html string passed.");
				
		// split the html into a token-array
		if (logger.isDebugEnabled())
			logger.debug("Splitting page");
		
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
	 * <p>Parses the HTML content into tokens of words and tags and calls 
	 * {@link #processTokens(String, List, Stack, boolean)} to further process
	 * these tokens.</p>
	 * 
	 * @param text The HTML text to parse
	 * @param replace Splitting characters that should not get included in the 
	 *                resulting tokens
	 * @param nonReplace Splitting characters that should get appended to the
	 *                   leading token
	 * @param splitAndInclude Splitting character that is the fist character of
	 *                        the new token
	 * @param formatText Indicates if the output tokens should be formated
	 * @return A {@link List} of {@link Token}s representing the HTML page
	 */
	protected List<Token> parseToTokens(String text, String replace, String nonReplace, String splitAndInclude, boolean formatText)
	{
		List<Token> tokenList = new ArrayList<Token>();
		Stack<Tag> stack = new Stack<Tag>();
		stack.add(new Tag(0, "", 0, 0, 0));
		
		char[] replaceChars = replace.toCharArray();
		char[] nonReplaceChars = nonReplace.toCharArray();
		char[] splitAndIncludeChars = splitAndInclude.toCharArray();
		
		StringBuilder sb = new StringBuilder();
		boolean found = false;
		for(int i=0; i < text.length(); i++)
		{			
			for (char c : replaceChars)
			{
				// we found a separating character - split the token at this 
				// position - This token should not get included in the final
				// token so jump continue with the next iteration
				if (text.charAt(i) == c)
				{
					if (!sb.toString().trim().equals(""))
						this.processTokens(sb.toString(), tokenList, stack, formatText);
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
						this.processTokens(sb.toString(), tokenList, stack, formatText);
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
				// we found a separating character - split the token and add the
				// separating character to the old token as its last character
				if (text.charAt(i) == c)
				{
					if (!sb.toString().trim().equals(""))
						this.processTokens(sb.toString(), tokenList, stack, formatText);
					sb = new StringBuilder();
					found = true;
					break;
				}
			}
			
			if (found)
				found = false;
		}
		
		return tokenList;
	}
	
	/**
	 * <p>Processes parsed tokens. Processing involves filtering unneeded tags
	 * and building a DOM tree.</p>
	 * 
	 * @param token The current token that needs to be processed
	 * @param tokenList The list of parsed HTML tags and words contained in the
	 *                  HTML page that is currently parsed
	 * @param stack The stack that keeps track of the ancestors of a certain 
	 *              HTML node
	 * @param formatText Indicates if the output tokens should be formated
	 */
	protected void processTokens(String token, List<Token> tokenList, Stack<Tag> stack, boolean formatText)
	{
		// remove whitespace characters
		token = token.trim();
		
		if (token.startsWith("<") && this.tagFinished && this.compact == null)
		{
			// we found a new tag either after a word or a preceding tag
			this.tagFinished = false;
			this.tag = new Tag(token);
			
			// check if we should compact tokens between certain tokens 
			// together
			for (String compactTag : this.compactTags)
			{
				if (this.tag.getShortTag().equals(compactTag))
				{
					if (compactTag.equals("<!--"))
						this.compact = "-->";
					else
						this.compact = compactTag;
					break;
				}
			}
			this.lastWord = null;
			
			this.checkTagValidity(this.tag, tokenList, stack);
		}
		else if (this.tag != null && !this.tagFinished && this.compact == null)
		{
			// the preceding tag was not yet finished so append the token to 
			// tag
			this.tag.append(token);
			
			if (token.endsWith("/>"))
				this.tag.setName(this.getTagName(this.tag.getHTML()));
			
			this.checkTagValidity(this.tag, tokenList, stack);
		}
		// compact specified token sequences like scripts or iframes
		else if (this.compact != null && this.tag != null)
		{
			// as appending content to an already valid tag is not possible,
			// we have to set the content manually
			this.tag.setHTML(this.tag.getHTML()+" "+token);
			// check if the end of the tag sequence was reached
			if (this.tag.getHTML().endsWith(this.compact+">") || 
					this.tag.getHTML().endsWith(this.compact))
			{
				this.checkTagValidity(this.tag, tokenList, stack);
				this.compact = null;
				this.tagFinished = true;
				this.tag = null;
			}
		}
		else
		{
			// preceding tags are all closed and the token doesn't start with
			// an opening tag symbol - so we have a word here
			int numWords = this.addWord(token, this.id, stack, tokenList, formatText);
			this.metaData.checkToken(this.lastWord, this.combineWords);
			// keep track of the id's
			this.id += numWords;
			this.numWords += numWords;
			
			if (!this.combineWords)
				this.lastWord = null;			
		}
	}
	
	/**
	 * <p>Returns a simple representation of the HTML tag which omits everything
	 * but the opening and closing-type and the name of the HTML tag.</p>
	 * 
	 * @param token The HTML text of the tag
	 * @return A simple representation of the HTML tag omitting all informations
	 *         like defined classes or id's
	 */
	protected String getTagName(String token)
	{
		String rawName = token.replaceAll("[<|/|>]", "");
		if (rawName.contains(" "))
			rawName = rawName.substring(0,rawName.indexOf(" "));
		String tagName = "<"+(token.charAt(1)=='/' ? "/" : "") +	rawName +
				(token.charAt(token.length()-2) == '/' ? " /" : "") +">";
		return tagName;
	}
	
	/**
	 * <p>Creates a new HTML tag and keeps track of ancestor information as well
	 * as the indentation level of the current token.</p>
	 * 
	 * @param token The HTML text of the tag to create
	 * @param tokenList The list of already parsed HTML tokens
	 * @param stack The stack that keeps track of the ancestors of a certain 
	 *              HTML node
	 * @return The newly created HTML tag
	 */
	protected Tag createNewTag(String token, List<Token> tokenList, Stack<Tag> stack)
	{
		Tag tag;
		
		int parent;
		// identify the tag name
		String tagName = this.getTagName(token);
		
		// detect the id of the parent and the position in the tree-
		// hierarchy so we can initialize the new Tag appropriately
		if (!stack.isEmpty() && stack.peek() != null)
		{
			parent = stack.peek().getNo();
			int level = stack.size()-1;
			if (tagName.startsWith("</"))
			{
				// closing tag found so go back one step in the tree
				// (closing tags are on the same level as opening tags)
				level--;
				parent = tokenList.get(parent).getParentNo();
			}
			
			if (logger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				for (int _i=0; _i<level; _i++)
					builder.append("\t");
				logger.debug(builder.toString()+tagName+" id: "+this.id+" parent: "+parent);
			}
			
			// initialize the new tag
			if (stack.peek().getChildren() != null)
				tag = new Tag(this.id++, tagName, parent, stack.peek().getChildren().length, level);
			else
				tag = new Tag(this.id++, tagName, parent, 0, level);
		}
		else
		{
			// initial tag found!
			tag = new Tag(this.id++, tagName, 0, 0, 0);
			parent = 0;
		}
		tag.setHTML(token);
		
		// build the ancestor tree
		this.addAncestorInformation(tag, tokenList, stack, parent);
		
		return tag;
	}

	/**
	 * <p>Adds ancestor information to <em>tag</em> and adds the tag as a child
	 * to it's parent.</p>
	 * 
	 * @param tag The tag to build the ancestor information for
	 * @param tokenList The list of already parsed HTML tokens
	 * @param stack The stack that keeps track of the ancestors of a certain 
	 *              HTML node
	 * @param parent The ID of the parent of <em>tag</em>
	 */
	protected void addAncestorInformation(Tag tag, List<Token> tokenList, Stack<Tag> stack, int parent)
	{
		boolean addTag = true;
		// check end tags for a corresponding opening tag on the stack
		// if none could be found the tag will not be added to the tokenList
		if ((!tag.isOpeningTag() && !tag.isInlineCloseingTag()) && !stack.isEmpty())
		{
			stack.peek().setEndNo(this.id-1);
			if (!this.ignoreParentingTags.contains(tag.getShortTag().toLowerCase()))
			if (this.checkElementsOnStack(tag, stack, tokenList))
			{
				this.id--;
				addTag = false;
			}
		}
		
		// decides if a tag should be added to the tokenList
		if (addTag)
		{
			// add child to the parent
			if (tokenList.size() > parent && !stack.isEmpty())
				tokenList.get(parent).addChild(tag);
			// opening tags will get added to the stack if they do not occur
			// in the ignoreParentingTags list
			if (tag.getHTML().startsWith("</") || tag.getHTML().endsWith("/>") 
				|| this.ignoreParentingTags.contains(tag.getShortTag().toLowerCase()))
				tag.setEndNo(this.id-1);				
		}
	}
	
	/**
	 * <p>Checks if a tag is allowed to be added to the list of parsed tokens.
	 * </p>
	 * 
	 * @param tag The tag to check for its validity to be added to the list of
	 *            parsed tokens
	 * @param tokenList The list of already parsed HTML tokens
	 * @param stack The stack that keeps track of the ancestors of a certain 
	 *              HTML node
	 */
	protected void checkTagValidity(Tag tag, List<Token> tokenList, Stack<Tag> stack)
	{
		// check if the tag is complete
		if (tag.isValid())
		{
			// remove the flag
			this.tagFinished = true;
			
			// check if we are allowed to add the tag
			if (!this.needsRemoval(tag))
			{
				// create a new tag object with ancestor and sibling informations
				Tag newTag = this.createNewTag(tag.getHTML(), tokenList, stack);
				tokenList.add(newTag);
				
				if (logger.isDebugEnabled())
					logger.debug("\tadded Tag: "+tag);
				
				// put the tag on the stack if it does not appear within the
				// ignoreParentingTags list and either the stack is empty or
				// the tag is an opening tag
				if (!this.ignoreParentingTags.contains(newTag.getShortTag().toLowerCase()) 
						&& (stack.isEmpty() || newTag.isOpeningTag()) 
						&& !newTag.isInlineCloseingTag())
					stack.push(newTag);
				
				this.metaData.checkTag(newTag);
			}
		}
	}

	/**
	 * <p>Adds a word to the list of parsed tokens and provides the possibility
	 * to format a word if <em>formatText</em> is set to true.</p>
	 * 
	 * @param word The word to add to the list of parsed tokens
	 * @param id The id of the word to add
	 * @param stack The ancestors of the word
	 * @param tokenList The list of parsed tokens
	 * @param formatText Indicates if the output tokens should be formated
	 * @return The number of words added to the list of parsed tokens
	 */
	protected int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList, boolean formatText)
	{
		int numWords = 0;
		
		if (formatText)
			word = Util.formatText(word);
		
		// split words in case they contain a / or a - but are no URL
		if ((word.contains("/") && !word.startsWith("http://")) 
				|| word.contains("-"))
		{
			for (String w : word.split("[/|-]"))
			{
				numWords += this.addWord(w, id++,  stack,  tokenList);
			}
		}
		else
		{
			// only a single word to add
			numWords += this.addWord(word, id,  stack,  tokenList);
		}

		return numWords;
	}
	
	/**
	 * <p>Adds either a new word to the list of parsed tokens or appends the
	 * word to the last added word if {@link #combineWords} is set to true.</p>
	 * 
	 * @param word The word to add
	 * @param id The id of the word to add
	 * @param stack The ancestors of the word
	 * @param tokenList The list of parsed tokens
	 * @return The number of words added; This is 0 if the word got appended to
	 *         a preceding word
	 */
	private int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList)
	{
		int ret = 0;
		// check if this word is the first word after a HTML tag and if we should
		// combine words to preceding words
		if (!this.combineWords || (this.combineWords && 
				(this.lastWord == null || this.lastWord.getText()==null) ))
		{
			// either we have a word following a HTML tag or we should not 
			// combine words
			int parent;
			if (!stack.isEmpty() && stack.peek() != null)
			{
				// not the first element on the stack :)
				parent = stack.peek().getNo();
				int level = stack.size()-1;
				if (stack.peek().getChildren() != null)
				{
					// there has at least been one word for the parent HTML tag
					// before
					if (this.lastWord == null)
						this.lastWord = new Word(id, word, parent, stack.peek().getChildren().length, level);
					else
					{
						this.lastWord.setNo(id);
						this.lastWord.setName(word);
						this.lastWord.setText(word);
						this.lastWord.setLevel(level);
						this.lastWord.setParentNo(parent);
						this.lastWord.setSibNo(stack.peek().getChildren().length);
					}
				}
				else
				{
					// this is the first word after a HTML tag
					if (this.lastWord == null)
						this.lastWord = new Word(id, word, parent, 0, level);
					else
					{
						this.lastWord.setNo(id);
						this.lastWord.setName(word);
						this.lastWord.setText(word);
						this.lastWord.setLevel(level);
						this.lastWord.setParentNo(parent);
						this.lastWord.setSibNo(0);
					}
				}
			}
			else
			{
				// this is the first word on the stack!
				parent = 0;
				if (this.lastWord == null)
					this.lastWord = new Word(id, word, 0, 0, 0);
				else
				{
					this.lastWord.setNo(id);
					this.lastWord.setName(word);
					this.lastWord.setText(word);
					this.lastWord.setLevel(0);
					this.lastWord.setParentNo(0);
					this.lastWord.setSibNo(0);
				}
			}
			
			// add child to the parent
			if (tokenList.size() > parent)
				tokenList.get(parent).addChild(this.lastWord);
			tokenList.add(this.lastWord);
							
			ret = 1;
		}
		else
		{
			// we should combine words and the preceding token was a word too
			// so append the content of this word to the preceding word
			this.lastWord.setText(this.lastWord.getText()+" "+word);
		}
		
		// update the name of the word
		this.lastWord.setName(this.lastWord.getText());
		
		return ret;
	}
	
	/**
	 * <p>Checks if a HTML node has a corresponding parent on the stack. If so
	 * nodes are taken from the stack until the parent is reached. The parent is
	 * now the last entry on the stack.</p>
	 * <p>If no matching parent could be found, the algorithm assumes that the
	 * tag itself is a wild tag and should not be included in the final output,
	 * therefore the tag is removed from the tokenList and the reference of the
	 * parent pointing to this node is removed.</p>
	 * 
	 * @param node The node to check if a corresponding parent is on the stack
	 * @param stack The stack that includes all ancestors
	 * @param tokenList The list containing all HTML nodes
	 * @param childEndTag Defines if the end tag is on the same level as the start 
	 *                    tag (true) or the end tag is a child of the start tag 
	 *                    (false)
	 * @return Returns true if the element is a wild node and has no ancestor 
	 *         on the stack, false otherwise
	 */
	protected boolean checkElementsOnStack(Tag node, Stack<Tag> stack, List<Token> tokenList)
	{
		// first element on the stack is the root-element
		for (int i=stack.size()-1; i>0; i--)
		{
			Token curNode = stack.elementAt(i);
			if (curNode.getName().equals(node.getName().replace("/", "")))
			{
				// match found
				int numPopRequired = node.getLevel()+1 - curNode.getLevel();
				for (int j=0; j<numPopRequired; j++)
					stack.pop();
				return false;
			}
		}
		if (logger.isWarnEnabled())
			logger.warn("Ignoring "+node.getNo()+" "+node.getName());
		return true;
	}
				
	/**
	 * <p>Checks if a tag needs to be removed.</p>
	 * 
	 * @param tag The tag which should be checked for removal
	 * @return True if the tag needs to be removed; false otherwise
	 */
	private boolean needsRemoval(Tag tag)
	{
		// TODO: better extensibility mechanism wanted
		if (this.cleanComments && tag.isComment() ||
				this.cleanDoctypes && tag.getShortTag().toLowerCase().equals("!doctype") ||
				this.cleanMeta && tag.getShortTag().toLowerCase().equals("meta") ||
				this.cleanScripts && tag.getShortTag().toLowerCase().equals("script") ||
				this.cleanLinks  && tag.getShortTag().toLowerCase().equals("link") ||
				this.cleanStyles && tag.getShortTag().toLowerCase().equals("style") ||
				this.cleanFormElements && tag.getShortTag().equals("form") ||
				this.cleanImages && tag.getShortTag().toLowerCase().equals("img") ||
				this.cleanAnchors && tag.getShortTag().toLowerCase().equals("a"))
			return true;
		return false;
	}
}
