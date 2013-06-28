package at.rovo.parser;

import java.util.ArrayList;
import java.util.List;

public class Util
{
	/**
	 * <p>Splits a text into a sequence of tokens where <em>replace</em> is a
	 * set of characters that will split the character sequence into tokens 
	 * without including the splitting characters in the final token.</p>
	 * <p><em>nonReplace</em> defines characters that will split the text and
	 * include the token in the token before the split, while 
	 * <em>splitAndInclude</em> splits tokens and includes the splitting
	 * character with the following token.</p>
	 * 
	 * @param text The text to split into tokens
	 * @param replace Splitting characters that should not get included in the 
	 *                resulting tokens
	 * @param nonReplace Splitting characters that should get appended to the
	 *                   leading token
	 * @param splitAndInclude Splitting character that is the fist character of
	 *                        the new token
	 * @return A sequence of tokens
	 */
	public static String[] split(String text, String replace, String nonReplace, String splitAndInclude)
	{
		List<String> tokens = new ArrayList<String>();
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
						tokens.add(sb.toString());
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
						tokens.add(sb.toString());
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
						tokens.add(sb.toString());
					sb = new StringBuilder();
					found = true;
					break;
				}
			}
			
			if (found)
				found = false;
		}
		
		return tokens.toArray(new String[0]);
	}
	
	/**
	 * <p>Returns the node and all of its child nodes in a tree-like structure 
	 * which can be used to inspect the correctness of the parser.</p>
	 * 
	 * @param node The tag to start traversing through child nodes
	 * @param endTagsIncluded Defines the list of parsed tokens contains end
	 *                        tags (true) or if they where omitted (false). In
	 *                        case they where omitted, this function does create
	 *                        closing tags automatically.
	 * @return A formated representation of the HTML tree based on the selected
	 *         root tag
	 */
	public static String niceHTMLFormat(Token node, boolean endTagsIncluded)
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < node.getLevel(); i++)
			builder.append("\t");

		if (node.getText() != null)
			builder.append(node.getText());
		else
			builder.append(node.getHTML());

		boolean hasPrintedLeaf = false;
		for (Token child : node.getChildren())
		{
			if (child.getText() == null)
			{
				builder.append("\n");
				builder.append(Util.niceHTMLFormat(child, endTagsIncluded));
				hasPrintedLeaf = false;
			}
			else
			{
				if (!hasPrintedLeaf)
				{
					builder.append("\n");
					for (int i = 0; i < child.getLevel(); i++)
						builder.append("\t");
				}
				builder.append(child.getText());
				builder.append(" ");
				hasPrintedLeaf = true;
			}
		}
		if (!endTagsIncluded && node.getText() == null
				&& !node.getHTML().endsWith("/>"))
		{
			if (!node.getHTML().toLowerCase().startsWith("<meta")
					&& !node.getHTML().toLowerCase().startsWith("<img")
					&& !node.getHTML().toLowerCase().startsWith("<link")
					&& !node.getHTML().toLowerCase().startsWith("<br"))
			{
				builder.append("\n");
				for (int i = 0; i < node.getLevel(); i++)
					builder.append("\t");
				builder.append("</");
				builder.append(node.getName().replace("<", ""));
			}
		}

		return builder.toString();
	}
}
