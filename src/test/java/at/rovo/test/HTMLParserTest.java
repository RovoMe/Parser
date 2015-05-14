package at.rovo.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import at.rovo.parser.ParseResult;
import at.rovo.parser.Parser;
import at.rovo.parser.Token;

public class HTMLParserTest
{
	private final static Logger LOG = LogManager.getLogger(HTMLParserTest.class);
	
	private String html = "";
	
	@Before
	public void loadTestPage() throws IOException, URISyntaxException
	{
		StringBuilder sb = new StringBuilder();
		URL url = this.getClass().getResource("/testPage2.html");
		Path path = Paths.get(url.toURI());
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				// process each line in some way
				sb.append(" ");
				sb.append(line);
			}
		}
		
		this.html = sb.toString();
	}
	
	@Test
	public void test()
	{
		Parser parser = new Parser();
		
		ParseResult parse = parser.tokenize(this.html, false);
		List<Token> tokens = parse.getParsedTokens();
		
		LOG.info(tokens);
		LOG.info("Byline: " + parse.getByline());
		LOG.info("Title: " + parse.getTitle());
		LOG.info("Date: " + parse.getPublishDate());
		LOG.info("Authors: " + parse.getAuthors());
		LOG.info("Number of Tags: " + parse.getNumTags());
		LOG.info("Number of Words: " + parse.getNumWords());
	}
	
//	@Test
//	public void test2()
//	{
//		Parser parser = new Parser();
//		ParseResult parse = parser.tokenizeURL("http://latimesblogs.latimes.com/world_now/2012/08/norway-killer-could-have-been-stopped-sooner-report.html", false);
//		List<Token> tokens = parse.getParsedTokens();
//		
//		System.out.println(tokens);
//		System.out.println("Byline: "+parse.getByline());
//		System.out.println("Title: "+parse.getTitle());
//		System.out.println("Date: "+parse.getPublishDate());
//		System.out.println("Authors: "+parse.getAuthors());
//		System.out.println("Number of Tags: "+parse.getNumTags());
//		System.out.println("Number of Words: "+parse.getNumWords());
//	}
}
