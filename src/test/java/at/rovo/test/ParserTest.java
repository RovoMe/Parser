package at.rovo.test;

import at.rovo.parser.DOMParser;
import at.rovo.parser.ParseResult;
import at.rovo.parser.Parser;
import at.rovo.parser.ParserUtil;
import at.rovo.parser.SimpleTreeParser;
import at.rovo.parser.TSReCParser;
import at.rovo.parser.Tag;
import at.rovo.parser.Token;
import at.rovo.parser.Word;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String html = "";

    @Before
    public void loadTestPage() throws IOException, URISyntaxException
    {
        StringBuilder sb = new StringBuilder();
        URL url = this.getClass().getResource("/testPage.html");
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
    public void testParser()
    {
        Parser parser = new Parser();

        ParseResult parse = parser.tokenize(this.html, false);
        List<Token> tokens = parse.getParsedTokens();

        // [<html>, <head>, <meta>, <title>, Test, Page, </title>, </head>, <body>,
        //  <div>, <div>, <div>, <p>, Test, Page, Content, </p>, <br />, </div>,
        //  <div>, </div>, </div>, </div>, <div>, <div>, <div>, Join, the, Discussion,
        //  </div>, <span>, </span>, </div>, <div>, You, are, using, an, outdated,
        //  version, of, Internet, Explorer., Please, <a>, click, here, </a>, to,
        //  upgrade, your, browser, in, order, to, comment., </div>, <div>, </div>,
        //  </div>, <a>, blog, comments, powered, by, <span>, Disqus, </span>, </a>,
        //  </body>, </html>]

        Assert.assertEquals(71, tokens.size());
        Assert.assertEquals("<html>", tokens.get(0).getHTML());
        Assert.assertEquals("</html>", tokens.get(70).getHTML());

        Assert.assertEquals("<div>", tokens.get(10).getName());
        Assert.assertEquals("<div class=\"header header-logged-in true\">", tokens.get(10).getHTML());

        Assert.assertNull(tokens.get(12).getText());
        Assert.assertEquals("<p>", tokens.get(12).getHTML());
        Assert.assertEquals("<p>", tokens.get(12).getName());

        Assert.assertEquals("Join", tokens.get(26).getText());
        Assert.assertEquals("Join", tokens.get(26).getHTML());
        Assert.assertEquals("Join", tokens.get(26).getName());
        Assert.assertEquals(Word.class, tokens.get(26).getClass());

        Assert.assertNull(tokens.get(60).getText());
        Assert.assertEquals("<a href=\"http://disqus.com\" class=\"dsq-brlink\">", tokens.get(60).getHTML());
        Assert.assertEquals("<a>", tokens.get(60).getName());
        Assert.assertEquals(Tag.class, tokens.get(60).getClass());

        LOG.debug("\n{}", tokens);
    }

    @Test
    public void testSimpleTreeParser()
    {
        Parser parser = new SimpleTreeParser();

        // By default IFrame-, Script-, NoScript-, Link-, Style-, Form-,
        // Doctype-Tags and Comments are removed

        parser.cleanFormElements(false);

        ParseResult parse = parser.tokenize(html, false);
        List<Token> tokens = parse.getParsedTokens();

        //		<html>
        //			<head prefix="og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# githubog: http://ogp.me/ns/fb/githubog#">
        //				<meta charset='utf-8'>
        //				<title>
        //					Test Page
        //				</title>
        //			</head>
        //			<body class="logged_in  windows  env-production  ">
        //				<div class="wrapper">
        //					<div class="header header-logged-in true">
        //						<div class="container clearfix">
        //							<p>
        //								Test Page Content
        //							</p>
        //							<br />
        //						</div>
        //						<div>
        //							<form enctype="application/x-www-form-urlencoded" action="http://query.nytimes.com/search/sitesearch" method="get" name="searchForm" id="searchForm">
        //								<input type="hidden" value="full" name="date_select"/>
        //								<label for="searchQuery">
        //									Search All NYTimes.com
        //								</label>
        //								<input type="text" class="text" value="" size="" name="query" id="searchQuery"/>
        //								<input type="hidden" id="searchAll" name="type" value="nyt"/>
        //								<input id="searchSubmit" title="Search" width="22" height="19" alt="Search" type="image" src="http://graphics8.nytimes.com/images/global/buttons/go.gif">
        //							</form>
        //						</div>
        //					</div>
        //				</div>
        //				<div id="disqus">
        //					<div class="widget_head">
        //						<div class="title">
        //							Join the Discussion
        //						</div>
        //						<span>
        //						</span>
        //					</div>
        //					<div id="disqus_ie7" class="disqus_msg">
        //						You are using an outdated version of Internet Explorer. Please
        //						<a href="http://windows.microsoft.com/en-us/internet-explorer/download-ie" name="lpos=disqus[story_ie7]&lid=[upgrade]" target="_blank">
        //							click here
        //						</a>
        //						to upgrade your browser in order to comment.
        //					</div>
        //					<div id="disqus_thread">
        //					</div>
        //				</div>
        //				<a href="http://disqus.com" class="dsq-brlink">
        //					blog comments powered by
        //					<span class="logo-disqus">
        //						Disqus
        //					</span>
        //				</a>
        //			</body>
        //		</html>

        Assert.assertEquals(82, tokens.size());
        Assert.assertEquals(tokens.get(0).getHTML(), "<html>");
        Assert.assertEquals(tokens.get(81).getHTML(), "</html>");
        Assert.assertNull(tokens.get(3).getText());
        Assert.assertEquals("Test Page", tokens.get(4).getText() + " " + tokens.get(5).getText());


        LOG.debug("\n{}", ParserUtil.niceHTMLFormat(tokens.get(0), parser, true));

        // remove form elements

        parser = new SimpleTreeParser();
        parser.cleanFormElements(true);

        parse = parser.tokenize(html, false);
        tokens = parse.getParsedTokens();

        Assert.assertEquals(71, tokens.size());
        Assert.assertEquals(tokens.get(0).getHTML(), "<html>");
        Assert.assertEquals(tokens.get(70).getHTML(), "</html>");
        Assert.assertNull(tokens.get(3).getText());
        Assert.assertEquals("Test Page", tokens.get(4).getText() + " " + tokens.get(5).getText());

        LOG.debug("\n{}", ParserUtil.niceHTMLFormat(tokens.get(0), parser, true));

        // remove form elements and combine words

        parser = new SimpleTreeParser();
        parser.cleanFormElements(true);
        parser.combineWords(true);

        parse = parser.tokenize(html, false);
        tokens = parse.getParsedTokens();

        Assert.assertEquals(46, tokens.size());
        Assert.assertEquals(tokens.get(0).getHTML(), "<html>");
        Assert.assertEquals(tokens.get(45).getHTML(), "</html>");
        Assert.assertNull(tokens.get(3).getText());
        Assert.assertEquals("Test Page", tokens.get(4).getText());

        LOG.debug("\n{}", ParserUtil.niceHTMLFormat(tokens.get(0), parser, true));
    }

    @Test
    public void testTSReCParser()
    {
        Parser parser = new TSReCParser();

        ParseResult parse = parser.tokenize(this.html, false);
        List<Token> tokens = parse.getParsedTokens();

        //		<html> (0, 27, 0, 0) :
        //		<head> (1, 3, 0, 1) : <title>Test Page</title>
        //		<meta> (2, 2, 1, 2) :
        //		<body> (4, 26, 0, 1) : <a>blog comments powered by<span>Disqus</span></a>
        //		<div> (5, 15, 4, 2) :
        //		<div> (6, 14, 5, 3) :
        //		<div> (7, 11, 6, 4) :
        //		<p> (8, 9, 7, 5) : Test Page Content
        //		<br /> (10, 10, 7, 5) :
        //		<div> (12, 13, 6, 4) :
        //		<div> (16, 25, 4, 2) :
        //		<div> (17, 20, 16, 3) : <span></span>
        //		<div> (18, 19, 17, 4) : Join the Discussion
        //		<div> (21, 22, 16, 3) : You are using an outdated version of Internet Explorer. Please<a>click here</a>to upgrade your browser in order to comment.
        //		<div> (23, 24, 16, 3) :

        Assert.assertEquals(28, tokens.size());
        Assert.assertEquals("<html>", tokens.get(0).getHTML());
        Assert.assertEquals(27, tokens.get(0).getEndNo());

        // p-Tag assertions
        Assert.assertEquals("<p>", tokens.get(8).getName());
        Assert.assertEquals(9, tokens.get(8).getEndNo());
        Assert.assertEquals(7, tokens.get(8).getParentNo());
        Assert.assertEquals(5, tokens.get(8).getLevel());
        Tag pTag = (Tag) tokens.get(8);
        Assert.assertEquals("Test Page Content", pTag.getSubElements().get(0).getText());
        Assert.assertEquals("</p>", tokens.get(9).getName());

        // div-Tag assertion
        Assert.assertEquals(Tag.class, tokens.get(21).getClass());
        Assert.assertEquals("<div>", tokens.get(21).getName());
        Assert.assertEquals(22, tokens.get(21).getEndNo());
        Assert.assertEquals(16, tokens.get(21).getParentNo());
        Assert.assertEquals(3, tokens.get(21).getLevel());

        Tag divTag = (Tag) tokens.get(21);
        Assert.assertEquals(5, divTag.getSubElements().size());
        Assert.assertEquals("You are using an outdated version of Internet Explorer. Please",
                            divTag.getSubElements().get(0).getText());
        Assert.assertEquals("You are using an outdated version of Internet Explorer. Please",
                            divTag.getSubElements().get(0).getName());
        Assert.assertEquals(Word.class, divTag.getSubElements().get(0).getClass());

        Assert.assertNull(divTag.getSubElements().get(1).getText());
        Assert.assertEquals("<a>", divTag.getSubElements().get(1).getName());
        Assert.assertEquals(Tag.class, divTag.getSubElements().get(1).getClass());

        Assert.assertEquals("click here", divTag.getSubElements().get(2).getText());
        Assert.assertEquals("click here", divTag.getSubElements().get(2).getName());
        Assert.assertEquals(Word.class, divTag.getSubElements().get(2).getClass());

        Assert.assertNull(divTag.getSubElements().get(3).getText());
        Assert.assertEquals("</a>", divTag.getSubElements().get(3).getName());
        Assert.assertEquals(Tag.class, divTag.getSubElements().get(3).getClass());

        Assert.assertEquals("to upgrade your browser in order to comment.", divTag.getSubElements().get(4).getText());
        Assert.assertEquals("to upgrade your browser in order to comment.", divTag.getSubElements().get(4).getName());
        Assert.assertEquals(Word.class, divTag.getSubElements().get(4).getClass());

        LOG.debug("\n{}", ParserUtil.niceTSReCFormat(tokens));
    }

    @Test
    public void testDOMParser()
    {
        Parser parser = new DOMParser();
        parser.combineWords(true);

        ParseResult parse = parser.tokenize(html, false);
        List<Token> tokens = parse.getParsedTokens();

        // printed without tag-endings
        //		<html>
        //			<head prefix="og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# githubog: http://ogp.me/ns/fb/githubog#">
        //				<meta charset='utf-8'>
        //				<title>
        //					Test Page
        //				<link rel="icon" type="image/x-icon" href="/favicon.ico" />
        //				<script src="https://a248.e.akamai.net/assets.github.com/assets/frameworks-8a3b89300fb38cc706e3ed8b1c9d7853a40d9689.js" type="text/javascript">
        //			<body class="logged_in  windows  env-production  ">
        //				<div class="wrapper">
        //					<script>
        //						abcnws_fw_params = {siteSectionId: 'nws_blotter', siteSectionIdType: 0, siteSectionNetworkId: 168234, siteSectionFallbackId: 109523, customVisitor:'', keyValues:'pageType=story'}; pc.fwSeg(); pc.fwAppendKeyValues('show=gma'); pc.getSWID(); pc.subsectionOverride(); function fw_config(){return abcnws_fw_params;}if (tq.videoOverrideContext != null) { jsvideoViewEventProp16Value = tq.videoOverrideContext; } else { jsvideoViewEventProp16Value = "none"; } if (jsvideoViewEventProp16Value == "none") { jsvideoViewEventProp16Value = "; } jsvideoViewEventEvar20Value = jsvideoViewEventProp16Value; currentURL = window.location.href; closedCaptionActiveValue = true; hdPluginActive = (currentURL.search('hdplugin=true') != -1)?true:false; kdp_embed_default = { doKdpEmbed : function() { // Should only be changed if you are running Kaltura On Prem / Kaltura CE: var service_url = "http://cdnapi.kaltura.com/"; // logic cascade for deciding which entry to load var entry_id = this.getEntryIdFromUrl() || this.getEntryIdFromDataAttr() || this.fallback_entry; var embedSrc = "http%3A%2F%2Fcdnapi.kaltura.com%2Findex.php%2Fextwidget%2FembedIframe%2Fentry_id%2F" + entry_id + "%2Fwidget_id%2F_" + kdp_embed_default.partner_id + "%2Fuiconf_id%2F" + 5590821; flashembed(this.placeholder_id, {	// attributes and params: id :				"kaltura_player_default", src : service_url + "/index.php/kwidget/wid/_" + kdp_embed_default.partner_id + "/uiconf_id/" + kdp_embed_default.uiconf_id + "/entry_id/" + entry_id, height :			361, width :				640, bgcolor :			"#eeeeee", allowNetworking : "all", version :			[10,0], expressInstall :	"http://cdn.kaltura.org/apis/seo/expressinstall.swf", wmode: "transparent" }, {	// flashvars (double-quote the values) externalInterfaceDisabled : "false", jsInterfaceReadyFunc :		"jsInterfaceReady", contentType: "video", //"restrictUserAgent.restrictedUserAgents": "GoogleTV", referer : "http://abcnews.go.com/GMA/video/al-qaeda-releases-video-us-hostage-warren-weinstein-16293486", "omniture.videoViewEventEvar15Value" : "player|storypage", "omniture.videoViewEventProp18Value" : "player|storypage", "omniture.videoViewEventProp16Value" : jsvideoViewEventProp16Value, "omniture.videoViewEventEvar20Value" : jsvideoViewEventEvar20Value, "omniture.adStartEvar15Value" : "player|storypage", "omniture.adStartEvar20Value" : jsvideoViewEventEvar20Value, "closedCaptionActive" : closedCaptionActiveValue, noThumbnail: true, "abcnews.displayEndCard":false, "addThis.embedCodeLinks" : "%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Fus%2F%3Fcid%3D11_extvid1%22%3EUS%20News%3C%2Fa%3E%7C%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Ftopics%2Fnews%2Fgeorge-zimmerman.htm%3Fcid%3D11_extvid2%22%3EGeorge%20Zimmerman%20Trial%3C%2Fa%3E%7C%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Fvideo%2F%3Fcid%3D11_extvid3%22%3EMore%20ABC%20News%20Videos%3C%2Fa%3E", "addThis.embedFlashVars" : "referer=http://abcnews.go.com/GMA/video/al-qaeda-releases-video-us-hostage-warren-weinstein-16293486%26flashvars[autoPlay]=false%26addThis.playerSize=392x221%26freeWheel.siteSectionId=nws_offsite%26closedCaptionActive=false", "addThis.iframeTemplate" : "%3Ciframe%20id%3D%22%24playerId%24%22%20height%3D%22360%22%20width%3D%22640%22%20style%3D%22%24cssStyle%24%22%20src%3D%22"+embedSrc+"%22%3E%24noIFrameMessage%24%3C%2Fiframe%3E%20%3Cdiv%20style%3D%22text-align%3Aleft%3Bfont-size%3Ax-small%3Bmargin-top%3A0%3B%22%3E%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Fus%2F%3Fcid%3D11_extvid1%22%3EUS%20News%3C%2Fa%3E%7C%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Ftopics%2Fnews%2Fgeorge-zimmerman.htm%3Fcid%3D11_extvid2%22%3EGeorge%20Zimmerman%20Trial%3C%2Fa%3E%7C%3Ca%20href%3D%22http%3A%2F%2Fabcnews.go.com%2Fvideo%2F%3Fcid%3D11_extvid3%22%3EMore%20ABC%20News%20Videos%3C%2Fa%3E%3C%2Fdiv%3E", "shareBtnControllerScreen.enabled" : "true", //"video.stretchThumbnail":true, //"volumeBar.initialValue":0.75, //"volumeBar.forceInitialValue":true, debugMode: true } ) }, onFail : function() { alert("FLASH EMBEDDING FAILED"); }, getEntryIdFromUrl : function() { if(location.hash.indexOf(kdp_embed_default.url_param_name) != -1) { // get the entry id from the url document fragment (aka hash): return location.hash.split("#")[1].substring((kdp_embed_default.url_param_name.length+1)); } else if(location.search.indexOf(kdp_embed_default.url_param_name) != -1) { // get the entry id from the url parameters (aka querystring): return location.search.split("?")[1].substring((kdp_embed_default.url_param_name.length+1)); } else { // use the default video defined in "fallback_entry" below: //						return kdp_embed_default.fallback_entry; return false; } }, getEntryIdFromDataAttr : function() { var data_attr_val = document.getElementById(this.placeholder_id).getAttribute("data-entryid"); if(data_attr_val && !(data_attr_val < 1)) { return data_attr_val; } else return false; }, on_playerReady : function() { }, on_load : function() { }, on_play : function() { pc.setClipAtts({}); }, on_paused : function() { }, on_end : function() { playNextVideo(); }, on_adStart : function() { positionAd(); }, on_adEnd : function() { resetAd(); }, on_mediaReady : function() { //pc.checkVolume(); storyPlayer.ready(); }, on_volumeChanged : function(data) { //console.log(' ********** reset cookie = '+data.newVolume+' ************ '); //pc.setVolume(data.newVolume); }, // for supporting using of page as landing page with entry id passed as url parameter: url_param_name : "entryId", placeholder_id :	"mediaplayer", fallback_entry : "0_plqnbfvy", //fallback_entry : "1_45xhfpgk", partner_id : 483511, uiconf_id : 3775332, auto_play :			false, height :			"100%", width : "100%", playButton :	false, endCard : false } // embed the player: kdp_embed_default.doKdpEmbed(); function jsInterfaceReady() { return true; } function jsCallbackReady(player_id) { // create a (global) reference to the KDP so we don't have to repeat querying the dom. // we use the "window." prefix as a convention to point out that this var is global window.kdp = document.getElementById(player_id); // @todo: this will fail if kdp_embed has not yet been defined if(!kdp_embed_default.endCard) { kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.endScreenOverId", "); kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.endScreenId", "); } if(kdp_embed_default.playButton) { kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenOverId", "startScreen"); kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenId", "startScreen"); } else { kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenOverId", "); kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenId", "); } if(kdp_embed_default.auto_play) { kdp.setKDPAttribute("configProxy.flashvars", "autoPlay", "true"); kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenOverId", "); kdp.setKDPAttribute("configProxy.flashvars", "screensLayer.startScreenId", "); } else { kdp.setKDPAttribute("configProxy.flashvars", "autoPlay", "false"); } if(hdPluginActive) { kdp.setKDPAttribute("configProxy.flashvars", "streamerType", "hdnetworkmanifest"); kdp.setKDPAttribute("configProxy.flashvars", "akamaiHD.loadingPolicy", "preInitialize"); kdp.setKDPAttribute("configProxy.flashvars", "akamaiHD.asyncInit", "true"); kdp.setKDPAttribute("configProxy.flashvars", "twoPhaseManifest", "true"); } kdp.addJsListener("entryReady", "kdp_embed_default.on_load"); kdp.addJsListener("playerPaused", "kdp_embed_default.on_paused"); kdp.addJsListener("playerPlayed", "kdp_embed_default.on_play"); kdp.addJsListener("playerPlayEnd", "kdp_embed_default.on_end"); kdp.addJsListener("volumeChanged", "kdp_embed_default.on_volumeChanged"); kdp.addJsListener("adStart", "kdp_embed_default.on_adStart"); kdp.addJsListener("adEnd", "kdp_embed_default.on_adEnd"); kdp.addJsListener("mediaReady", "kdp_embed_default.on_mediaReady"); kdp_embed_default.on_playerReady(); }
        //					<style type='text/css' media='screen,print'>
        //						#interactiveFooter {border-top:1px solid #DDDDDD; margin-top:20px; padding-top:20px !important;} #interactiveFooter .module .noWrap{white-space:normal;} #nytg-aiGraphic{ position:relative; width:190px; height:320px; margin-top:15px; } #nytg-shell{ width:190px; }
        //					<div class="header header-logged-in true">
        //						<div class="container clearfix">
        //							<p>
        //								Test Page Content
        //							<br />
        //						<div>
        //							<form enctype="application/x-www-form-urlencoded" action="http://query.nytimes.com/search/sitesearch" method="get" name="searchForm" id="searchForm">
        //								<input type="hidden" value="full" name="date_select"/>
        //								<label for="searchQuery">
        //									Search All NYTimes.com
        //								<input type="text" class="text" value="" size="" name="query" id="searchQuery"/>
        //								<input type="hidden" id="searchAll" name="type" value="nyt"/>
        //								<input id="searchSubmit" title="Search" width="22" height="19" alt="Search" type="image" src="http://graphics8.nytimes.com/images/global/buttons/go.gif">
        //				<div id="disqus">
        //					<div class="widget_head">
        //						<div class="title">
        //							Join the Discussion
        //						<span>
        //					<div id="disqus_ie7" class="disqus_msg">
        //						You are using an outdated version of Internet Explorer. Please
        //						<a href="http://windows.microsoft.com/en-us/internet-explorer/download-ie" name="lpos=disqus[story_ie7]&lid=[upgrade]" target="_blank">
        //							click here
        //						to upgrade your browser in order to comment.
        //					<div id="disqus_thread">
        //				<script type="text/javascript">
        //					/* * * CONFIGURATION VARIABLES: EDIT BEFORE PASTING INTO YOUR WEBPAGE * * */ // required: replace example with your forum shortname var disqus_shortname = 'abcnewsdotcom'; var disqus_identifier = '17221075'; var data_disqus_identifier = '17221075'; var disqus_url = 'http://abcnews.go.com/Blotter/story?id=17221075'; var disqus_config = function(){ if(disqusCookie != null) { disqusCookie = disqusCookie.replace("+"," "); disqusCookie = disqusCookie.replace("+"," "); } this.page.remote_auth_s3 = disqusCookie; //alert("this.page.remote_auth_s3: "+this.page.remote_auth_s3); this.page.api_key = "on0ouUQvUM8scIts70yQ4WderrXYTAaV6OocbTDvkojzX6j3i3oEkjxbugvQZQnu"; // Get your API public key from the same Disqus API application page this.sso = { name: "ABCNews", button: "http://a.abcnews.com/assets/images/buttons/abc_discuss_button.png", icon: ", url: "https://register.go.com/global/login?rd=true&appRedirect=http://abcnews.go.com/disqus/disqusSignin", logout: "http://abcnews.go.com/disqus/disqusSignout?appRedirect="+disqus_url, width: "745", height: "500" }; }; /* * * DON'T EDIT BELOW THIS LINE * * */ (function() { var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true; dsq.src = 'http://' + disqus_shortname + '.disqus.com/embed.js'; (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq); })(); (function () { var s = document.createElement('script'); s.async = true; s.type = 'text/javascript'; s.src = 'http://' + disqus_shortname + '.disqus.com/count.js'; (document.getElementsByTagName('HEAD')[0] || document.getElementsByTagName('BODY')[0]).appendChild(s); if(ie7flag){ $('#disqus_ie7').show(); } }());
        //				<noscript>
        //					Please enable JavaScript to view the
        //					<a href="http://disqus.com/?ref_noscript">
        //						comments powered by Disqus.
        //				<a href="http://disqus.com" class="dsq-brlink">
        //					blog comments powered by
        //					<span class="logo-disqus">
        //						Disqus

        Assert.assertEquals("<html>", tokens.get(0).getHTML());
        Assert.assertEquals(2, tokens.get(0).getChildren().length);

        // body-tag
        Assert.assertEquals(Tag.class, tokens.get(0).getChildren()[1].getClass());
        Assert.assertEquals("<body>", tokens.get(0).getChildren()[1].getName());
        Tag body = (Tag) tokens.get(0).getChildren()[1];
        Assert.assertEquals(5, body.getChildren().length);

        // p-tag - xpath: /html/body/*[0]/*[2]/*[0]/*[0]
        Tag pTag = (Tag) body.getChildren()[0].getChildren()[2].getChildren()[0].getChildren()[0];
        Assert.assertEquals(Tag.class, pTag.getClass());
        Assert.assertEquals("<p>", pTag.getName());
        Assert.assertEquals(1, pTag.getChildren().length);
        Assert.assertEquals("Test Page Content", pTag.getChildren()[0].getText());

        LOG.debug("\n{}", ParserUtil.niceHTMLFormat(tokens.get(0), parser, false));
    }
}
