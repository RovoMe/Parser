package at.rovo.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParsingMetaData
{
	private static Logger logger = LogManager.getLogger(ParsingMetaData.class);
	private boolean isTitle = false;
	private String title = "";
	private boolean isAuthorName = false;
	private List<String> authorName = new ArrayList<String>();
	private boolean isAuthor = false;
	private List<String> authors = new ArrayList<String>();
	private boolean isDate = false;
	private String date = "";
	private boolean isByline = false;
	private String bylineTag = null;
	private String byline = "";
	
	private int foundLevel = -1;
	
	public String getTitle()
	{
		return this.title;
	}
	
	public List<String> getAuthorNames()
	{
		return this.authorName;
	}
	
	public List<String> getAuthor()
	{
		return this.authors;
	}
	
	public String getDate()
	{
		// time formats:
		// Thu May 30 19:26:48 EDT 2013
		Pattern pattern = Pattern.compile(
		// DD/MM/YYYY
		// MM/DD/YYYY
		// DD/MM/YY
		// MM/DD/YY
		"(?:(\\d{1,2}/\\d{2}/\\d{2,4})|"+
		// 2012-08-14
		"(\\d{2,4}-\\d{1,2}-\\d{1,2})|"+
		// Sept. 12, 2012 or September 12, 2012 or Sept. 5th, 2004
		"((?:Jan|Feb|Mar|Apr|May|Jul|Jun|Aug|Sept|Oct|Nov|Dec|January|February|"+
		"March|April|June|July|August|September|October|November|December)[\\.|,]? "+
		"\\d{1,2}(?:st|nd|rd|th)?[,]? \\d{2,4}))");
		Matcher matcher = pattern.matcher(this.date);
		List<String> possibleDates = new ArrayList<String>();
		while (matcher.find())
			possibleDates.add(matcher.group());
		if (possibleDates.isEmpty())
			return this.date;
		return possibleDates.get(0);
	}
	
	public String getBylineTag()
	{
		return this.bylineTag;
	}
	
	public String getByline()
	{
		return this.byline;
	}
	
	public void checkTag(Tag tag)
	{
		if (tag.getHTML().equals("<title>"))
		{
			logger.trace("Found title tag");
			isTitle = true;
			if (tag.getLevel() > 0)
				foundLevel = tag.getLevel();
		}
		else if (tag.getHTML().equals("</title>"))
		{
			logger.trace("Found title end tag");
			isTitle = false;
		}
		
		if (tag.getHTML().contains("byline"))
		{
			logger.trace("Found byline");
			if (tag.getLevel() > 0)
				foundLevel = tag.getLevel();
			isByline = true;
			byline = tag.getHTML();
			bylineTag = tag.getShortTag();
			// TODO: create a stack that holds all tags added since the byline start
		}
		
		if ((tag.isOpeningTag() && tag.getHTML().contains("\"date")))
		{
			logger.trace("Found tag containing date");
			if (tag.getLevel() > 0)
				foundLevel = tag.getLevel();
			isDate = true;
		}
		else if (isDate)
		{
			logger.trace("Found end of date block");
			isDate = false;
		}
		
		if ((tag.isOpeningTag() && tag.getHTML().contains("\"authorName\"")))
		{
			logger.trace("Found tag containing authorName");
			if (tag.getLevel() > 0)
				foundLevel = tag.getLevel();
			authorName.add("");
			isAuthorName = true;
		}
		else if (tag.isOpeningTag() && tag.getHTML().contains("\"author\""))
		{
			logger.trace("Found tag containing author");
			if (tag.getLevel() > 0)
				foundLevel = tag.getLevel();
			authors.add("");
			isAuthor = true;
		}
		else if (isAuthorName && !tag.isOpeningTag())
		{
			logger.trace("Found end of block containing authorName");
			isAuthorName = false;
		}
		else if (isAuthor && !tag.isOpeningTag())
		{
			logger.trace("Found end of block containing author");
			isAuthor = false;
		}
	}
	
	
	public void checkToken(Word word, boolean combineWords)
	{
		if (isTitle)
		{
			logger.trace("Adding title: '{}'", word);
			if (foundLevel > 0 && foundLevel+1 == word.getLevel() || foundLevel < 0)
			{
				if (!combineWords)
					title += " "+word.getText();
				else
					title = word.getText();
			}
			else if (foundLevel > 0)
				this.clear();
		}
		if (isDate)
		{
			logger.trace("Adding date: '{}'", word);
			if (foundLevel > 0 && foundLevel+1 == word.getLevel() || foundLevel < 0)
			{
				if (!combineWords)
					date += " "+word.getText();
				else
					date = word.getText();
			}
			else if (foundLevel > 0)
				this.clear();
		}
		if (isAuthorName)
		{
			logger.trace("Adding authorName: '{}'", word);
			if (foundLevel > 0  && foundLevel+1 == word.getLevel() || foundLevel < 0)
			{
				if (!combineWords)
					authorName.set(authorName.size()-1, (authorName.get(authorName.size()-1)+" "+word.getText()).trim());
				else
					authorName.set(authors.size()-1, word.getText());
			}
			else if (foundLevel > 0)
				this.clear();
		}
		if (isAuthor)
		{
			logger.trace("Adding author: '{}'", word);
			if (foundLevel > 0 && foundLevel+1 == word.getLevel() || foundLevel < 0)
				if (!combineWords)
					authors.set(authors.size()-1, (authors.get(authors.size()-1)+" "+word.getText()).trim());
				else
					authors.set(authors.size()-1, word.getText());
			else if (foundLevel > 0)
				this.clear();
		}
		if (isByline)
		{
			logger.trace("Adding byline: '{}'", word);
			if (foundLevel > 0 && foundLevel+1 == word.getLevel() || foundLevel < 0)
				byline += " "+word.getText();
			else if (foundLevel > 0)
				this.clear();
		}
	}
	
	private void clear()
	{
		this.isAuthor = false;
		this.isAuthorName = false;
		this.isByline = false;
		this.isDate = false;
		this.isTitle = false;
	}
}
