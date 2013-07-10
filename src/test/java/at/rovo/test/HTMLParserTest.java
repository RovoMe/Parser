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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import at.rovo.parser.ParseResult;
import at.rovo.parser.Parser;
import at.rovo.parser.Token;

public class HTMLParserTest
{
	/** The logger of this class **/
	protected static Logger logger;
	
	private String html = "";
		
	@BeforeClass
	public static void initLogger() throws URISyntaxException
	{
		String path = HTMLParserTest.class.getResource("/log/log4j2-test.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		logger = LogManager.getLogger(HTMLParserTest.class);
	}
	
	@AfterClass
	public static void cleanLogger()
	{
		System.clearProperty("log4j.configurationFile");
	}
	
	@Before
	public void loadTestPage() throws IOException, URISyntaxException
	{
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		logger.info(config.getLoggers());
		LoggerConfig loggerConfig = config.getLoggerConfig("at.rovo.parser.Parser");
		loggerConfig.setLevel(Level.TRACE);
		ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
		
		StringBuffer sb = new StringBuffer();
		URL url = this.getClass().getResource("/webPage.html");
		Path path = Paths.get(url.toURI());
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
		{
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				// process each line in some way
				sb.append(" ");
				sb.append(line);
			}
		}
		
		this.html = sb.toString();
	}
	
//	@Test
	public void test()
	{
		Parser parser = new Parser();
		
		ParseResult parse = parser.tokenize(this.html, false);
		List<Token> tokens = parse.getParsedTokens();
		
		System.out.println(tokens);
		System.out.println("Byline: "+parse.getByline());
		System.out.println("Title: "+parse.getTitle());
		System.out.println("Date: "+parse.getPublishDate());
		System.out.println("Authors: "+parse.getAuthors());
		System.out.println("Number of Tags: "+parse.getNumTags());
		System.out.println("Number of Words: "+parse.getNumWords());
	}
	
	@Test
	public void test2()
	{
		Parser parser = new Parser();
		ParseResult parse = parser.tokenizeURL("http://latimesblogs.latimes.com/world_now/2012/08/norway-killer-could-have-been-stopped-sooner-report.html", false);
		List<Token> tokens = parse.getParsedTokens();
		
		System.out.println(tokens);
		System.out.println("Byline: "+parse.getByline());
		System.out.println("Title: "+parse.getTitle());
		System.out.println("Date: "+parse.getPublishDate());
		System.out.println("Authors: "+parse.getAuthors());
		System.out.println("Number of Tags: "+parse.getNumTags());
		System.out.println("Number of Words: "+parse.getNumWords());
	}
}
