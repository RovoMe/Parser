package at.rovo.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * The <em>SimpleTreeParser</em> creates a simple tree representation of the
 * parsed HTML tree by adding opening and closing tags as children to their
 * corresponding parent node. Moreover each {@link Tag} node contains a unique
 * ID as well as a reference to the ID of its parent and its position in the
 * siblings list of its parent.
 * </p>
 * <p>
 * Ancestor informations are collected through the utilization of a stack. A
 * simple heuristic is used to probe if a closing tag is suitable. The heuristic
 * checks the stack for an opening tag that matches the closing tag. If there is
 * a matching tag on the stack the opening tag is used that was added the
 * latest. This further implies that all tags that are added after the opening
 * tag are removed from the stack as well.
 * </p>
 * <p>
 * <em>SimpleTreeParser</em> allows adding tag names to the parser instance via
 * {@link #addTagToIgnoreIndentation(String)} which will ignore these tags from
 * taking an effect on the indentation. The subsequent tag will be on the same
 * level as the declared tags that ignore indentation.
 * </p>
 * 
 * @author Roman Vottner
 * @see Parser
 */
public class SimpleTreeParser extends Parser 
{
	/** A static logger instance **/
	protected static Logger logger = LogManager.getLogger(SimpleTreeParser.class.getName());
	
	/** Tags contained in this list will ignore parenting. This means that tags
	 * following one of these tags will be on the same tree-level like the tag
	 * contained in this list **/
	protected List<String> ignoreIndentationTags = new ArrayList<String>();
		
	/**
	 * <p>
	 * Initializes a new instance and sets default values for certain fields
	 * like the tags that are combined or tags that ignore parenting.
	 * </p>
	 */
	public SimpleTreeParser()
	{
		super();
				
		// single line tags
		this.ignoreIndentationTags.add("hr");
		this.ignoreIndentationTags.add("br");
		this.ignoreIndentationTags.add("meta");
		this.ignoreIndentationTags.add("link");
		this.ignoreIndentationTags.add("img");
		this.ignoreIndentationTags.add("!doctype");
		this.ignoreIndentationTags.add("input");
	}
	
	/**
	 * <p>
	 * Add a tag which ignores indentation. This means the proximating tag will
	 * be on the same tree level like this tag.
	 * </p>
	 * 
	 * @param tagName
	 *            The tag name which should ignore parenting
	 */
	public void addTagToIgnoreIndentation(String tagName)
	{
		if (!this.ignoreIndentationTags.contains(tagName))
			this.ignoreIndentationTags.add(tagName);
	}
	
	/**
	 * <p>
	 * Removes a tag from the list of tags that ignore indentation.
	 * </p>
	 * 
	 * @param tagName
	 *            The tag to remove from the list to ignore parenting
	 */
	public void removeTagToIgnoreIndentation(String tagName)
	{
		if (this.ignoreIndentationTags.contains(tagName))
			this.ignoreIndentationTags.remove(tagName);
	}
								
	/**
	 * <p>
	 * Creates a new HTML tag and keeps track of ancestor information as well as
	 * the indentation level of the current token.
	 * </p>
	 * 
	 * @param token
	 *            The HTML text of the tag to create
	 * @param tokenList
	 *            The list of already parsed HTML tokens
	 * @param stack
	 *            The stack that keeps track of the ancestors of a certain HTML
	 *            node
	 * @return The newly created HTML tag
	 * @throws InvalidAncestorException
	 *             If no matching opening tag can be found on the stack for a
	 *             closing tag
	 */
	@Override
	protected Tag createNewTag(String token, List<Token> tokenList, Stack<Tag> stack) throws InvalidAncestorException
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
	protected void addAncestorInformation(Tag tag, List<Token> tokenList, Stack<Tag> stack, int parent) throws InvalidAncestorException
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
		if (tokenList.size() > parent && !stack.isEmpty())
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
					tokenList.add(newTag);
					
					if (logger.isDebugEnabled())
					{
						StringBuilder builder = new StringBuilder();
						for (int i=0; i<newTag.getLevel(); i++)
							builder.append("\t");
						logger.debug("{} id: {} parent: {} html: \t\t{}", 
								builder.toString()+newTag.getName(), newTag.getNo(), 
								newTag.getParentNo(), newTag.getHTML());
					}
					
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
	
	/**
	 * <p>
	 * Adds either a new word to the list of parsed tokens or appends the word
	 * to the last added word if {@link #combineWords} is set to true.
	 * </p>
	 * 
	 * @param word
	 *            The word to add
	 * @param id
	 *            The id of the word to add
	 * @param stack
	 *            The ancestors of the word
	 * @param tokenList
	 *            The list of parsed tokens
	 * @return The number of words added; This is 0 if the word got appended to
	 *         a preceding word
	 */
	@Override
	protected int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList)
	{
		int ret = 0;
		// check if we should exclude words from the token list and instead
		// append the text to the text field of the last tag
		if (this.excludeWordTokens)
		{
			if (stack.peek().getText() != null && this.lastWord != null)
				this.lastWord.setText(this.lastWord.getText()+" "+word);
			else
			{
				this.lastWord = new Word(word);
				this.lastWord.setText(word);
			}
			
			stack.peek().setText(this.lastWord.getText());
			
			return 0;
		}
		// check if this word is the first word after a HTML tag and if we should
		// combine words to preceding words
		else if (!this.combineWords || (this.combineWords && 
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
					this.newWord(id, word, parent, stack.peek().getChildren().length, level);
				}
				else
				{
					// this is the first word after a HTML tag
					this.newWord(id, word, parent, 0, level);
				}
			}
			else
			{
				// this is the first word on the stack!
				parent = 0;
				this.newWord(id, word, 0, 0, 0);
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
	 * <p>
	 * Creates a new word or updates a word from a previous iteration.
	 * </p>
	 * 
	 * @param word
	 *            The word to add
	 * @param parent
	 *            The parent HTML element of this word
	 * @param sibling
	 *            The position of this word amongst its siblings
	 * @param level
	 *            The level of this word
	 */
	protected void newWord(int id, String word, int parent, int sibling, int level)
	{
		if (this.lastWord == null)
		{
			this.lastWord = new Word(id, word, parent, sibling, level);
			this.lastWord.setText(word);
		}
		else
		{
			this.lastWord.setNo(id);
			this.lastWord.setName(word);
			this.lastWord.setText(word);
			this.lastWord.setLevel(level);
			this.lastWord.setParentNo(parent);
			this.lastWord.setSibNo(sibling);
		}
	}
	
	/**
	 * <p>
	 * Checks if a HTML node has a corresponding parent on the stack. If so
	 * nodes are taken from the stack until the parent is reached. The parent is
	 * now the last entry on the stack.
	 * </p>
	 * <p>
	 * If no matching parent could be found, the algorithm assumes that the tag
	 * itself is a wild tag and should not be included in the final output,
	 * therefore the tag is removed from the tokenList and the reference of the
	 * parent pointing to this node is removed.
	 * </p>
	 * 
	 * @param node
	 *            The node to check if a corresponding parent is on the stack
	 * @param stack
	 *            The stack that includes all ancestors
	 * @param tokenList
	 *            The list containing all HTML nodes
	 * @param childEndTag
	 *            Defines if the end tag is on the same level as the start tag
	 *            (true) or the end tag is a child of the start tag (false)
	 * @return Returns true if the element is a wild node and has no ancestor on
	 *         the stack, false otherwise
	 */
	protected boolean checkElementsOnStack(Tag node, Stack<Tag> stack, List<Token> tokenList)
	{
		// first element on the stack is the root-element
		if (node.isComment())
			return false;
		
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
		logger.warn("Ignoring {} {}", node.getNo(), node.getName());
		return true;
	}
}
