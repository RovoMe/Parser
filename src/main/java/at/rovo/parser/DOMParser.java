package at.rovo.parser;

import java.util.List;
import java.util.Stack;

/**
 * <p>
 * Creates a Document Object Model (DOM) like structure of the parsed HTML
 * tokens from the provided HTML page.
 * </p>
 * <p>
 * The resulting output will not contain any closing tags, but as the parsed
 * Tokens are stored in a DOM tree structure it is relatively easy to add the
 * closing tags automatically.
 * </p>
 * 
 * @author Roman Vottner
 */
public class DOMParser extends SimpleTreeParser
{
	public DOMParser()
	{
		super();
		
		// do not clean any tags except the DOCTYPE by default
		this.cleanIFrame(false);
		this.cleanLinks(false);
		this.cleanScripts(false);
		this.cleanNoScripts(false);
		this.cleanStyles(false);
		this.cleanFormElements(false);
		this.cleanComments(true);
		this.cleanDoctypes(true);
	}
	
	/**
	 * <p>
	 * Builds a DOM tree from the HTML text provided as input.
	 * </p>
	 * 
	 * @param html
	 *            A {@link String} representing the full HTML code of a web site
	 * @param formatText
	 *            Indicates if the tokens should be formated or not
	 * @return A {@link List} containing the root element of the HTML page
	 */
	@Override
	public ParseResult tokenize(String html, boolean formatText)
	{
		ParseResult result = new ParseResult();
		if (html == null || html.equals(""))
			throw new IllegalArgumentException("Invalid html string passed.");
				
		this.reset();
		
		// split the html into a token-array
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
	 * <p>
	 * Adds ancestor information to <em>tag</em> and adds the tag as a child to
	 * it's parent.
	 * </p>
	 * 
	 * @param tag
	 *            The tag to build the ancestor information for
	 * @param tokenList
	 *            The list of already parsed HTML tokens
	 * @param stack
	 *            The stack that keeps track of the ancestors of a certain HTML
	 *            node
	 * @param parent
	 *            The ID of the parent of <em>tag</em>
	 * @throws InvalidAncestorException
	 *             If a closing tag has no corresponding opening tag on the
	 *             stack
	 */
	@Override
	protected void addAncestorInformation(Tag tag, List<Token> tokenList, Stack<Tag> stack, int parent) 
			throws InvalidAncestorException
	{
		// check end tags for a corresponding opening tag on the stack
		// if none could be found the tag will not be added to the tokenList
		if ((!tag.isOpeningTag() && !tag.isInlineCloseingTag()) && !stack.isEmpty())
		{
			stack.peek().setEndNo(this.id-1);
			if (!this.ignoreIndentationTags.contains(tag.getShortTag().toLowerCase()))
			{
				if (this.checkElementsOnStack(tag, stack, tokenList))
				{
					this.id--;
					throw new InvalidAncestorException("No matching opening tag for "+tag.getName()+" found!");
				}
			}
		}

		// add child to the parent
		if (tokenList.size() > parent && !stack.isEmpty() 
				// don't add closing tags to the children list of the parent tag
				&& (tag.isOpeningTag() || tag.isInlineCloseingTag() || tag.isComment()))
			tokenList.get(parent).addChild(tag);
		// opening tags will get added to the stack if they do not occur
		// in the ignoreParentingTags list
		if (tag.getHTML().startsWith("</") || tag.getHTML().endsWith("/>") 
			|| this.ignoreIndentationTags.contains(tag.getShortTag().toLowerCase()))
			tag.setEndNo(this.id-1);				
	}
	
	/**
	 * <p>
	 * Checks if a tag is allowed to be added to the list of parsed tokens.
	 * </p>
	 * 
	 * @param tag
	 *            The tag to check for its validity to be added to the list of
	 *            parsed tokens
	 * @param tokenList
	 *            The list of already parsed HTML tokens
	 * @param stack
	 *            The stack that keeps track of the ancestors of a certain HTML
	 *            node
	 */
	@Override
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
				try
				{
					// create a new tag object with ancestor and sibling informations
					Tag newTag = this.createNewTag(tag.getHTML(), tokenList, stack);
					// don't add closing tags to the token list
					if (newTag.isOpeningTag() || newTag.isInlineCloseingTag() || 
							newTag.isComment() && !this.cleanComments())
					{
						tokenList.add(newTag);
					
						if (logger.isDebugEnabled())
						{
							StringBuilder builder = new StringBuilder();
							for (int i=0; i<newTag.getLevel(); i++)
								builder.append("\t");
							logger.debug("{} id: {} parent: {} html: \t\t{}", builder.toString()+newTag.getName(), newTag.getNo(), newTag.getParentNo(), newTag.getHTML());
						}
					}
					else
						this.id--;
					
					// put the tag on the stack if it does not appear within the
					// ignoreParentingTags list and either the stack is empty or
					// the tag is an opening tag
					if (!this.ignoreIndentationTags.contains(newTag.getShortTag().toLowerCase()) 
							&& (stack.isEmpty() || newTag.isOpeningTag()) 
							&& !newTag.isInlineCloseingTag())
						stack.push(newTag);
					
					this.metaData.checkTag(newTag);
				}
				catch (InvalidAncestorException iaEx)
				{
					logger.catching(iaEx);
				}
			}
		}
	}
}
