package at.rovo.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import at.rovo.parser.DOMParser;
import at.rovo.parser.ParseResult;
import at.rovo.parser.Parser;
import at.rovo.parser.ParserUtil;
import at.rovo.parser.Tag;
import at.rovo.parser.Token;
import at.rovo.parser.Word;

public class DOMText
{
	private static Logger logger;
	
	@BeforeClass
	public static void initLogger() throws URISyntaxException
	{
		String path = DOMText.class.getResource("/log/log4j2-test.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		logger = LogManager.getLogger(DOMText.class);
	}
	
	@AfterClass
	public static void cleanLogger()
	{
		System.clearProperty("log4j.configurationFile");
	}
	
	@Test
	public void testDOMGeneration()
	{
		String s ="<html>"
				+ "    <head>"
				+ "        <title>"
				+ "            Dies ist ein Test"
				+ "        </title>"
				+ "    </head>"
				+ "    <body>"
				+ "        <p class=\"text\">"
				+ "            Erster Test."
				+ "        </p>"
				+ "        <a href=\"www.test.at\">"
				+ "            <img src=\"test.jpg\"/>"
				+ "            Anchor Text"
				+ "        </a>"
				+ "        <hr class=\"test\"/>"
				+ "        <p>"
				+ "            Und noch ein Test. Dieser Test wird durch einen weiteren Satz erweitert, der sogar noch einen Nebensatz beinhaltet."
				+ "        </p>"
				+ "    </body>"
				+ "</html>";
		
		Parser parser = new DOMParser();		
		parser.combineWords(true);
		ParseResult res = parser.tokenize(s, false);
		List<Token> nodes = res.getParsedTokens();
		Token html = nodes.get(0);
		
		// Debug-Output
		for (Token node : html.getChildren())
		{
			if (node.getChildren() != null && node.getChildren().length > 0)
			{
				logger.debug("No: {}", node.getNo());
				logger.debug("Name: {}", node.getName());
				logger.debug("Parent: {}", node.getParentNo());
			
				for (Token n : node.getChildren())
				{
					logger.debug("child of {}: {} {}", node.getName(), n.getNo(), n.getName());
				}
				logger.debug("");
			}
		}
		
		String text0 = "Dies ist ein Test Erster Test. Anchor Text Und noch ein Test. Dieser Test wird durch einen weiteren Satz erweitert, der sogar noch einen Nebensatz beinhaltet.";
		String text1 = "Dies ist ein Test";
		String text4 = "Erster Test. Anchor Text Und noch ein Test. Dieser Test wird durch einen weiteren Satz erweitert, der sogar noch einen Nebensatz beinhaltet.";
		String text11 = "Und noch ein Test. Dieser Test wird durch einen weiteren Satz erweitert, der sogar noch einen Nebensatz beinhaltet.";

		String anchorText = "Anchor Text";
		
		Assert.assertEquals(text0, html.getSubtreeText());
		
		// first child of root == <head>
		Token head = html.getChildren()[0];
		Assert.assertEquals(1, head.getNo());
		Assert.assertEquals("<head>", head.getName());
		Assert.assertTrue(head instanceof Tag);
		Assert.assertFalse(head instanceof Word);
		Assert.assertEquals(text1, head.getSubtreeText());
		
		// first child of first child == <title>
		Token title = head.getChildren()[0];
		Assert.assertEquals(2, title.getNo());
		Assert.assertEquals("<title>", title.getName());
		Assert.assertTrue(title instanceof Tag);
		Assert.assertFalse(title instanceof Word);
		Assert.assertEquals(text1, title.getSubtreeText());
		
		// second child of root == <body>
		Token body = html.getChildren()[1];
		Assert.assertEquals(4, body.getNo());
		Assert.assertEquals("<body>", body.getName());
		Assert.assertTrue(body instanceof Tag);
		Assert.assertFalse(body instanceof Word);
		Assert.assertEquals(text4, body.getSubtreeText());
		
		// 4th child of body == <p>
		Token p = body.getChildren()[3];
		Assert.assertEquals(11, p.getNo());
		Assert.assertEquals("<p>", p.getName());
		Assert.assertTrue(p instanceof Tag);
		Assert.assertFalse(p instanceof Word);
		Assert.assertEquals(text11, p.getSubtreeText());
		
		Assert.assertEquals(anchorText, html.getSubtreeAnchorText());
		
		Assert.assertEquals(((double)anchorText.length() / text0.length()), html.getAnchorTextRatio(), 0.);
		// no anchor text inside of node 1
		Assert.assertEquals(0.0, head.getAnchorTextRatio(), 0.);
		Assert.assertEquals(((double)anchorText.length() / text4.length()), body.getAnchorTextRatio(), 0.);
		// no anchor text inside of node 10
		Assert.assertEquals(0.0, p.getAnchorTextRatio(), 0.);
		
		Assert.assertEquals(text0.split(" ").length, html.getSegNum());
		Assert.assertEquals(text1.split(" ").length, head.getSegNum());
		Assert.assertEquals(text4.split(" ").length, body.getSegNum());
		Assert.assertEquals(text11.split(" ").length, p.getSegNum());
		
		Assert.assertEquals((text0.length() - text0.replaceAll("[,|;|.]", "").length()), html.getPunctNum());
		Assert.assertEquals((text1.length() - text1.replaceAll("[,|;|.]", "").length()), head.getPunctNum());
		Assert.assertEquals((text4.length() - text4.replaceAll("[,|;|.]", "").length()), body.getPunctNum());
		Assert.assertEquals((text11.length() - text11.replaceAll("[,|;|.]", "").length()), p.getPunctNum());
	}
	
	@Test
	public void testSimplePageDOMCreation() throws IOException
	{
		String url = "<html><head><title>Testpage 1</title></head><body><p>This is an example text.</p></html>";
		String expected = 
				"<html>\n"+
						"\t<head>\n"+
						"\t\t<title>\n"+
						"\t\t\tTestpage 1 \n"+
						"\t\t</title>\n"+
						"\t</head>\n"+
						"\t<body>\n"+
						"\t\t<p>\n"+
						"\t\t\tThis is an example text. \n"+
						"\t\t</p>\n"+
						"\t</body>\n"+
						"</html>";

		Parser parser = new DOMParser();		
		ParseResult res = parser.tokenize(url, false);
		List<Token> nodes = res.getParsedTokens();
			
		String output = ParserUtil.niceHTMLFormat(nodes.get(0), parser, false);
		
//		writeFile("output.html", output);
		Assert.assertEquals(expected, output);
	}
		
	static void writeFile(String file, String content) throws IOException
	{
		logger.debug("Writing file '{}'", file);
	    PrintWriter out = new PrintWriter(new FileWriter(file));
	         
	    // Write text to file
	    out.println(content);
	    out.close();
	    try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	    logger.debug(" ... DONE");
	}
}
