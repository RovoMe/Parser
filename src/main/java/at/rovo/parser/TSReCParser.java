package at.rovo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The Tag Sequence with Region Code (TSReC) parser creates a sequential list of HTML tokens that break the text flow.
 * These tag sequences will contain tree based information like parent, children and siblings.
 * <p>
 * The basic idea behind <em>TSReC</em> is to extend a tag sequence with extra structural information as the utilization
 * of region code has proven to be an ideal way in XML processing to attach structural information in element based
 * storage.
 * <p>
 * <em>TSReC</em> is a sequence of elements, each of which is defined as: <code>TS = &lt;N, RCb, RCe, RCp, RCl,
 * C></code>
 * <p>
 * where <em>N</em> is the name of the tag sequence, <em>RC</em> is the region code for the begin, end, parent and level
 * the tag sequence is found. <em> C</em> refers to the content of the tag sequence.
 * <p>
 * The content of a tag sequence can either be a word or a further HTML tag if it does not break the text flow. Such
 * tags are f.e. &lt;a> or &lt;span> tags. A new tag sequence therefore is only created if a tag breaks the text flow
 * such as &lt;div> or &lt;p>
 *
 * @author Roman Vottner
 */
public class TSReCParser extends SimpleTreeParser
{
	protected static Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private List<String> textFlowBreakingTags = null;
	
	public TSReCParser()
	{
		super();
		
		this.combineWords(true);
		this.loadTextFlowBreakingTags();
	}
	
	private void loadTextFlowBreakingTags()
	{
		this.textFlowBreakingTags = new ArrayList<>();
		
		this.textFlowBreakingTags.add("html");
		this.textFlowBreakingTags.add("head");
//		this.textFlowBreakingTags.add("title");
		this.textFlowBreakingTags.add("meta");
		this.textFlowBreakingTags.add("body");
		this.textFlowBreakingTags.add("div");
		this.textFlowBreakingTags.add("ul");
		this.textFlowBreakingTags.add("ol");
		this.textFlowBreakingTags.add("dl");
		this.textFlowBreakingTags.add("li");
		this.textFlowBreakingTags.add("p");
		this.textFlowBreakingTags.add("center");
		this.textFlowBreakingTags.add("blockquote");
		this.textFlowBreakingTags.add("isindex");
		this.textFlowBreakingTags.add("hr");
		this.textFlowBreakingTags.add("table");
		this.textFlowBreakingTags.add("td");
		this.textFlowBreakingTags.add("form");
		this.textFlowBreakingTags.add("br");
		this.textFlowBreakingTags.add("h1");
		this.textFlowBreakingTags.add("h2");
		this.textFlowBreakingTags.add("h3");
		this.textFlowBreakingTags.add("h4");
		this.textFlowBreakingTags.add("h5");
		this.textFlowBreakingTags.add("h6");
	}

	/**
	 * Checks if a tag is allowed to be added to the list of parsed tokens.
	 *
	 * @param tag
	 * 		The tag to check for its validity to be added to the list of parsed tokens
	 * @param tokenList
	 * 		The list of already parsed HTML tokens
	 * @param stack
	 * 		The stack that keeps track of the ancestors of a certain HTML node
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
				// check if the tag to add breaks the text flow
				if (this.textFlowBreakingTags.contains(tag.getShortTag()))
				{
					try
					{
						// create a new tag object with ancestor and sibling informations
						Tag newTag = this.createNewTag(tag.getHTML(), tokenList, stack);
						tokenList.add(newTag);
						
						if (LOG.isDebugEnabled())
						{
							StringBuilder builder = new StringBuilder();
							for (int i=0; i<newTag.getLevel(); i++)
							{
								builder.append("\t");
							}
							LOG.debug("{} id: {} parent: {} html: \t\t{}",
									builder.toString()+newTag.getName(), newTag.getNo(), 
									newTag.getParentNo(), newTag.getHTML());
						}
						
						// put the tag on the stack if it does not appear within the
						// ignoreParentingTags list and either the stack is empty or
						// the tag is an opening tag
						if (!this.ignoreIndentationTags.contains(newTag.getShortTag().toLowerCase()) 
								&& (stack.isEmpty() || newTag.isOpeningTag()) 
								&& !newTag.isInlineClosingTag())
						{
							stack.push(newTag);
						}
						
						this.metaData.checkTag(newTag);
					}
					catch (InvalidAncestorException iaEx)
					{
							LOG.catching(iaEx);
					}
				}
				else
				{
					// non text flow breaking tag found - append it to the last
					// tag on the stack
					if (!stack.isEmpty())
					{
						tag.setName(this.getTagName(tag.getHTML()));
						stack.peek().append(tag);
						this.metaData.checkTag(tag);
					}
				}
			}
		}
	}

	@Override
	protected int addWord(String word, int id, Stack<Tag> stack, List<Token> tokenList)
	{
		int ret = 0;
		// check if this word is the first word after a HTML tag and if we should
		// combine words to preceding words
		if (this.lastWord == null || this.lastWord.getText()==null)
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
				this.newWord(id, word, 0, 0, 0);
			}
			
			stack.peek().append(this.lastWord);
							
			ret = 0;
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
}
