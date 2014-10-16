package fuzzer.discover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import fuzzer.util.Utility;

public class DiscoverHelper {
	private static String[] _commonExtensions = {"", ".php", ".jsp"};
	private static HashSet<String> _allLinks;
	private static HashMap<String, HtmlPage> UniqueLinks;
	private static WebClient client;

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Discover("http://127.0.0.1:8080/bodgeit/contact.jsp", "CommonWordsTest.txt", "dvwa");
	}

	
	//==================================================Public Methods=============================================================================
	
	
	public static String Discover(String url, String fileName, String customAuth)
	{
		String result = "";
		client = new WebClient();
		String[] commonWords = Utility.GetDelimStrings(fileName);
		
		try {
			
			System.out.println("Page Discovery (may take ~5-10 seconds)");
			HashMap<String, HtmlPage> urls = DiscoverPages(url, commonWords, customAuth, customAuth!=null);
			String pages = PrintHelper.PagesToString(urls.keySet());
			result += pages;
			System.out.println(pages);
			
			System.out.println("Input Discovery (may take ~5-10 seconds)");
			String inputs = DiscoverInputs(urls);
			result += inputs;
			System.out.println(inputs);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
		
		return result;
	}
	
	public static HashMap<String, HtmlPage> DiscoverPages(String url, String[] commonWords, String customAuth, boolean isCustom) throws FailingHttpStatusCodeException, MalformedURLException, IOException
	{
		customAuth(customAuth,"./common_usernames.txt","common_passwords.txt",url,isCustom);
		HashMap<String, HtmlPage> pages = new HashMap<String, HtmlPage>();
		UniqueLinks = new HashMap<String, HtmlPage>();
		_allLinks = new HashSet<String>();
		pages = GetLinks(url, false);
		pages = GuessPages(pages, commonWords);
		
		return pages;
		
	}
	public static Page getPageWithTimeout(String url, HtmlElement submitButton) throws FailingHttpStatusCodeException, MalformedURLException, IOException
	{
		Page responsePage = null;
		customAuth("dvwa","./common_usernames.txt","common_passwords.txt",url,true);
		try {
			if(submitButton == null)
				responsePage = client.getPage(url);
			else
				responsePage = submitButton.click();
		}
		catch (FailingHttpStatusCodeException e) {
			System.out.println("--" + url + "--");
			System.out.println("Status code not ok: "  + e.getStatusCode() + "-" + e.getStatusMessage());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return responsePage;
	}
	public static String DiscoverInputs(HashMap<String, HtmlPage> pages) throws MalformedURLException, IOException
	{
		String result = "";
		for (String al : _allLinks)
			System.out.println(al);
		Map<String, Set<String>> urlParams = GetUrlParams(_allLinks);
		result += PrintHelper.UrlParamsToString(urlParams);
		
		Set<String> inputEles = GetElements(pages, "input");
		result += PrintHelper.InputElementsToString(inputEles);
		
		Set<Cookie> cookies = GetCookies(pages);
		result += PrintHelper.CookiesToString(cookies);
		
		return result;
	}
	
	//==================================================Private Methods=============================================================================
	
	
	
	
	/*
	 * This isn't working right now for some reason. The only other way to get cookies i saw
	 * was through servlets.
	 */
	private static Set<Cookie> GetCookies(HashMap<String, HtmlPage> pages) throws MalformedURLException
	{
		Set<Cookie> allCookies = new HashSet<Cookie>();
		Set<Cookie> cookies = new HashSet<Cookie>();
		for(String url: pages.keySet())
		{
			cookies = client.getCookies(new URL(url));
			allCookies.addAll(cookies);
		}
		
		return allCookies;
	}
	
	private static Set<String> GetElements(HashMap<String, HtmlPage> pages, String name) throws MalformedURLException, IOException
	{
		Set<String> allElements = new HashSet<String>();
		HtmlPage page;
		for(String url: pages.keySet())
		{
			page = pages.get(url);
			if(page != null)
			{
				List<DomElement> elements = (List<DomElement>) page.getElementsByTagName(name);
				
				for(DomElement ele: elements)
				{
					allElements.add(ele.asXml());
				}
			}
		}
		return allElements;
	}
	
	/**
	 * Get the parameters for a url.
	 * ex somesite.com/home?var=value would have the parameter var=value
	 * @param urls
	 * @return
	 */
	public static Map<String, Set<String>> GetUrlParams(Set<String> urls)
	{
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		String rootUrl;
		int qIndex, qLoc;
		for(String urlString: urls)
		{
			qIndex = urlString.indexOf("?");
			if(qIndex >= 0)
			{
				String[] params = urlString.substring(qIndex + 1).split("&");
				rootUrl = urlString.substring(0, qIndex);
				
				if(params.length > 0 && !result.containsKey(rootUrl))
					result.put(rootUrl, new HashSet<String>());
				
				for(String param: params)
				{
					qLoc = param.indexOf('=');
					if(qLoc >= 0)
						result.get(rootUrl).add(param.substring(0, qLoc));
				}
			}
		}
		
		return result;
	}
	
	private static HashMap<String, HtmlPage> GetLinks(String url, boolean recursive) throws FailingHttpStatusCodeException, MalformedURLException, IOException
	{
		HtmlPage page;
		String urlNoParams = "";
		urlNoParams = PathHelper.GetUrlNoParams(url);
		if(UniqueLinks.containsKey(urlNoParams))
			page = UniqueLinks.get(urlNoParams);
		else
		{
			try {
				page = client.getPage(url);
			} catch(Exception ex)
			{
				return null;
			}
		}
		List<HtmlAnchor> anchors = page.getAnchors();
		String href;
		String lastPiece;
		
		for(HtmlAnchor anchor: anchors)
		{
			try
			{
				href = anchor.getHrefAttribute();
				
				href = PathHelper.RemoveTag(href);
				if(!href.startsWith("http"))
				{
					lastPiece = PathHelper.GetLastPiece(url);
					if(!lastPiece.isEmpty() && !lastPiece.equals("/"))
						url = url.replace(lastPiece, "");
					
					href = PathHelper.Combine(new String[]{url, href});
				}
				
				_allLinks.add(href);
				urlNoParams = PathHelper.GetUrlNoParams(href);
				if(new URL(href).getHost().equals(new URL(url).getHost())
						&& !UniqueLinks.containsKey(urlNoParams))
				{
					page = client.getPage(href);
					
					UniqueLinks.put(urlNoParams, page);
					if(recursive)
						GetLinks(urlNoParams, recursive);
				}	
			}
			catch(MalformedURLException ex){
				System.err.println("GetLinks: " + ex.getMessage());
				}
			catch(IOException ex){System.err.println("GetLinks: " + ex.getMessage());}
		}
		UniqueLinks.put("http://127.0.0.1/dvwa/vulnerabilities/sqli/?id=admin&Submit=Submit#", (HtmlPage)client.getPage("http://127.0.0.1/dvwa/vulnerabilities/sqli/?id=admin&Submit=Submit#")); 
		return UniqueLinks;
	}
	
	/**
	 * Tries replacing the end of links with common words with different extensions.
	 * @param links
	 * @param commonWords
	 * @param recursive
	 * @return
	 */
	private static HashMap<String, HtmlPage> GuessPages(HashMap<String, HtmlPage> pages, String[] commonWords)
	{
		HashMap<String, HtmlPage> successGuesses = new HashMap<String, HtmlPage>();
		String guess, fragmentWithExtension;
		HtmlPage page;
		
		Set<String> linksNoLastPiece = new HashSet<String>();
		
		for(String link: pages.keySet())
		{
			try {
				linksNoLastPiece.add(PathHelper.RemoveLastPiece(link));
			} catch (MalformedURLException e){}
		}
		
		for(String link: linksNoLastPiece)
		{
			for(String newFragment: commonWords)
			{
				for(String newExtension: _commonExtensions)
				{
					fragmentWithExtension = "/"+newFragment + newExtension;
					try
					{
						guess = PathHelper.Combine(new String[]{link, fragmentWithExtension});
						if (!pages.containsKey(guess))
						{
							HttpURLConnection huc = (HttpURLConnection) new URL(guess).openConnection();
							huc.setRequestMethod("HEAD");
							int responseCode = huc.getResponseCode();
							
							if(responseCode == 200)
							{
								page = client.getPage(guess);
								successGuesses.put(guess, page);
							}
						}
					}
					catch(MalformedURLException ex){System.err.println(ex.getMessage());}
					catch(IOException ex){System.err.println(ex.getMessage());}
					catch(IllegalArgumentException ex){System.err.println(ex.getMessage());}
				}
			}
		}
		
		pages.putAll(successGuesses);
		
		return pages;
	}
	
	
	public static void customAuth(String address, String usernamesFile, String passwordsFile, String url, boolean custom) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page;
		String successURL;

		String[] common_users = Utility.GetDelimStrings(usernamesFile);
		String[] common_passwords = Utility.GetDelimStrings(passwordsFile);
		if (custom)
		{
			if (address.compareTo("dvwa") == 0){
				page = client.getPage("http://127.0.0.1/dvwa/");
				successURL = ("http://127.0.0.1/dvwa/index.php");
			}
			else {
				page = client.getPage("http://127.0.0.1/bodgeit");
				successURL = ("http://127.0.0.1/bodgeit/logout");
			}
			List<HtmlForm> forms = page.getForms();
			for (HtmlForm form : forms) 
			{
				for (String username: common_users)
				{
					for (String password: common_passwords)
					{
						try
						{
							form.getInputByName("username").setValueAttribute(username);
							form.getInputByName("password").setValueAttribute(password);
							page = (HtmlPage) form.getInputByName("Login").click();
							
							if (page.getUrl().toString().compareTo(successURL) == 0)
							{
								System.out.println("SUCCESS\n");
								System.out.print("USERNAME: " + username + "\n");
								System.out.print("PASSWORD: " + password + "\n");
								return;
							}
							
						}
						catch(IOException ex){}
					}
				}
			}
		}
		else
		{
			page = client.getPage(url);
			String pageString = page.getWebResponse().getContentAsString();
			List<String> allMatches = new ArrayList<String>();
			Matcher m = Pattern.compile("(.*[u|U]sername|[p|P]assword)[=|:|-|;|.|,| ]*[a-zA-Z0-9!@$#%^&*()]*.*")
					.matcher(pageString);
			while(m.find())
			{
				allMatches.add(m.group());
			}
			System.out.println(allMatches);
			return;
		}
	}

	public static WebClient getClient() {
		return client;
	}


	public static Set<String> getAllLinks() {
		return _allLinks;
	}
}
