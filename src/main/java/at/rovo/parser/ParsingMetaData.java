package at.rovo.parser;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ParsingMetaData
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean isTitle = false;
    private String title = "";
    private boolean isAuthorName = false;
    private List<String> authorName = new ArrayList<>();
    private boolean isAuthor = false;
    private List<String> authors = new ArrayList<>();
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
                "(?:(\\d{1,2}/\\d{2}/\\d{2,4})|" +
                // 2012-08-14
                "(\\d{2,4}-\\d{1,2}-\\d{1,2})|" +
                // Sept. 12, 2012 or September 12, 2012 or Sept. 5th, 2004
                "((?:Jan|Feb|Mar|Apr|May|Jul|Jun|Aug|Sept|Oct|Nov|Dec|January|February|" +
                "March|April|June|July|August|September|October|November|December)[\\.|,]? " +
                "\\d{1,2}(?:st|nd|rd|th)?[,]? \\d{2,4}))");
        Matcher matcher = pattern.matcher(this.date);
        List<String> possibleDates = new ArrayList<>();
        while (matcher.find())
        {
            possibleDates.add(matcher.group());
        }
        if (possibleDates.isEmpty())
        {
            return this.date;
        }
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
            LOG.trace("Found title tag");
            isTitle = true;
            if (tag.getLevel() > 0)
            {
                foundLevel = tag.getLevel();
            }
        }
        else if (tag.getHTML().equals("</title>"))
        {
            LOG.trace("Found title end tag");
            isTitle = false;
        }

        if (tag.getHTML().contains("byline"))
        {
            LOG.trace("Found byline");
            if (tag.getLevel() > 0)
            {
                foundLevel = tag.getLevel();
            }
            isByline = true;
            byline = tag.getHTML();
            bylineTag = tag.getShortTag();
            // TODO: create a stack that holds all tags added since the byline start
        }

        if ((tag.isOpeningTag() && tag.getHTML().contains("\"date")))
        {
            LOG.trace("Found tag containing date");
            if (tag.getLevel() > 0)
            {
                foundLevel = tag.getLevel();
            }
            isDate = true;
        }
        else if (isDate)
        {
            LOG.trace("Found end of date block");
            isDate = false;
        }

        if ((tag.isOpeningTag() && tag.getHTML().contains("\"authorName\"")))
        {
            LOG.trace("Found tag containing authorName");
            if (tag.getLevel() > 0)
            {
                foundLevel = tag.getLevel();
            }
            authorName.add("");
            isAuthorName = true;
        }
        else if (tag.isOpeningTag() && tag.getHTML().contains("\"author\""))
        {
            LOG.trace("Found tag containing author");
            if (tag.getLevel() > 0)
            {
                foundLevel = tag.getLevel();
            }
            authors.add("");
            isAuthor = true;
        }
        else if (isAuthorName && !tag.isOpeningTag())
        {
            LOG.trace("Found end of block containing authorName");
            isAuthorName = false;
        }
        else if (isAuthor && !tag.isOpeningTag())
        {
            LOG.trace("Found end of block containing author");
            isAuthor = false;
        }
    }


    public void checkToken(Word word, boolean combineWords)
    {
        if (isTitle)
        {
            LOG.trace("Adding title: '{}'", word);
            setSimpleWord(word, title, combineWords, foundLevel).ifPresent((String value) -> title = value);
        }
        if (isDate)
        {
            LOG.trace("Adding date: '{}'", word);
            setSimpleWord(word, date, combineWords, foundLevel).ifPresent((String value) -> date = value);
        }
        if (isAuthorName)
        {
            LOG.trace("Adding authorName: '{}'", word);
            setWord(word, authorName, combineWords, foundLevel);
        }
        if (isAuthor)
        {
            LOG.trace("Adding author: '{}'", word);
            setWord(word, authors, combineWords, foundLevel);
        }
        if (isByline)
        {
            LOG.trace("Adding byline: '{}'", word);
            if (foundLevel > 0 && foundLevel + 1 == word.getLevel() || foundLevel < 0)
            {
                byline += " " + word.getText();
            }
            else if (foundLevel > 0)
            {
                this.clear();
            }
        }
    }

    private Optional<String> setSimpleWord(Word word, String currentValue, boolean combineWords, int foundLevel)
    {
        Optional<String> ret = Optional.empty();
        if (foundLevel > 0 && foundLevel + 1 == word.getLevel() || foundLevel < 0)
        {
            if (!combineWords)
            {
                currentValue += " " + word.getText();
            }
            else
            {
                currentValue = word.getText();
            }
            ret = Optional.of(currentValue);
        }
        else if (foundLevel > 0)
        {
            this.clear();
            ret = Optional.empty();
        }

        return ret;
    }

    private void setWord(Word word, List<String> words, boolean combineWords, int foundLevel)
    {
        if (foundLevel > 0 && foundLevel + 1 == word.getLevel() || foundLevel < 0)
        {
            if (!combineWords)
            {
                words.set(words.size() - 1, (words.get(words.size() - 1) + " " + word.getText()).trim());
            }
            else
            {
                words.set(words.size() - 1, word.getText());
            }
        }
        else if (foundLevel > 0)
        {
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
