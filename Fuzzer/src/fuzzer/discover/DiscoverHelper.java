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

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class DiscoverHelper {
	public static String[] _pageGuesses = {"admin", "edit"};
	public static String[] _commonExtensions = {"", ".php", ".jsp"};

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Discover("http://127.0.0.1:8080/bodgeit/contact.jsp", "CommonWordsTest.txt");
	}
	
	//==================================================Public Methods=============================================================================
	
	
	public static String Discover(String url, String fileName)
	{
		String result = "";
		
		String[] commonWords = GetCommonWords(fileName);
		
		try {
			System.out.println("Page Discovery (may take ~5-10 seconds)");
			Set<String> urls = DiscoverPages(url, commonWords);
			String pages = PrintHelper.PagesToString(urls);
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
	
	public static Set<String> DiscoverPages(String url, String[] commonWords)
	{
		Set<String> linkStrings = new HashSet<String>();

		UniqueLinks = new HashSet<String>();
		linkStrings = GetLinks(url, false);
		linkStrings = GuessPages(linkStrings, commonWords);
		
		return linkStrings;
		
	}
	
	public static String DiscoverInputs(Set<String> urls) throws MalformedURLException, IOException
	{
		String result = "";
		
		Map<String, List<String>> urlParams = GetUrlParams(urls);
		result += PrintHelper.UrlParamsToString(urlParams);
		
		Set<String> inputEles = GetInputElements(urls);
		result += PrintHelper.InputElementsToString(inputEles);
		
		Set<Cookie> cookies = GetCookies(urls);
		result += PrintHelper.CookiesToString(cookies);
		
		return result;
	}
	
	//==================================================Private Methods=============================================================================
	
	private static String[] GetCommonWords(String fileName)
	{
		if(fileName == null || fileName.isEmpty())
			return new String[0];

		File file = new File(fileName);
		if(!file.exists())
		{
			System.out.println("File does not exist: " + fileName);
			return new String[0];
		}
		
		String commonWordsString = "";
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(fileName));
		    try {
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();
	
		        while (line != null) {
		            sb.append(line);
		            sb.append(System.lineSeparator());
		            line = br.readLine();
		        }
		        commonWordsString = sb.toString();
		        
		    }
		    catch (FileNotFoundException e){}
		    finally {
		        br.close();
		    }
		}
		catch (IOException e){}
		
	    return commonWordsString.split("[ |\r\n|,|\n|\t]+");
	    
	}
	
	
	/*
	 * This isn't working right now for some reason. The only other way to get cookies i saw
	 * was through servlets.
	 */
	private static Set<Cookie> GetCookies(Set<String> urls) throws MalformedURLException
	{
		WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(true);
		
		Set<Cookie> allCookies = new HashSet<Cookie>();
		Set<Cookie> cookies = new HashSet<Cookie>();
		for(String url: urls)
		{
			cookies = webClient.getCookies(new URL(url));
			allCookies.addAll(cookies);
		}
		
		return allCookies;
	}
	
	private static Set<String> GetInputElements(Set<String> urls) throws MalformedURLException, IOException
	{
		WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setTimeout(5000);
		
		Set<String> inputs = new HashSet<String>();
		for(String url: urls)
		{
			try
			{
				HtmlPage page = webClient.getPage(url);
				
				List<DomElement> inputElements = page.getElementsByTagName("input");
				for(DomElement ele: inputElements)
					inputs.add(ele.asXml());
			}
			catch(FailingHttpStatusCodeException ex){System.err.println("Problem getting page: " + url);}
		}
		return inputs;
	}
	
	/**
	 * Get the parameters for a url.
	 * ex somesite.com/home?var=value would have the parameter var=value
	 * @param urls
	 * @return
	 */
	private static Map<String, List<String>> GetUrlParams(Set<String> urls)
	{
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		String rootUrl;
		int qIndex;
		for(String urlString: urls)
		{
			qIndex = urlString.indexOf("?");
			if(qIndex >= 0)
			{
				String[] params = urlString.substring(qIndex + 1).split("&");
				rootUrl = urlString.substring(0, qIndex);
				
				if(params.length > 0 && !result.containsKey(rootUrl))
					result.put(rootUrl, new ArrayList<String>());
				
				for(String param: params)
					result.get(rootUrl).add(param);
			}
		}
		
		return result;
	}
	
	private static Set<String> UniqueLinks;
	private static Set<String> GetLinks(String url, boolean recursive)
	{
		WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(true);
		HtmlPage page;
		try
		{
			page = webClient.getPage(url);
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

					if(new URL(href).getHost().equals(new URL(url).getHost())
							&& !UniqueLinks.contains(href))
					{
						UniqueLinks.add(href);
						if(recursive)
							GetLinks(href, recursive);
					}	
				}
				catch(MalformedURLException ex){}
			}
		}
		catch(MalformedURLException ex){
			System.err.println("GetLinks: " + ex.getMessage());
			}
		catch(IOException ex){System.err.println("GetLinks: " + ex.getMessage());}
		
		return UniqueLinks;
	}
	
	/**
	 * Tries replacing the end of links with common words with different extensions.
	 * @param links
	 * @param commonWords
	 * @param recursive
	 * @return
	 */
	private static Set<String> GuessPages(Set<String> links, String[] commonWords)
	{
		Set<String> successGuesses = new HashSet<String>();
		URI uri;
		String guess, fragmentWithExtension;
		
		Set<String> linksNoLastPiece = new HashSet<String>();
		
		for(String link: links)
		{
			try {
				linksNoLastPiece.add(PathHelper.RemoveLastPiece(link));
			} catch (MalformedURLException e){}
		}
		
		for(String link: linksNoLastPiece)
		{
			try
			{
				uri = new URI(link);
				for(String newFragment: commonWords)
				{
					for(String newExtension: _commonExtensions)
					{
						fragmentWithExtension = "/"+newFragment + newExtension;
						try
						{
							guess = PathHelper.Combine(new String[]{link, fragmentWithExtension});
							if (!links.contains(guess))
							{
								HttpURLConnection huc = (HttpURLConnection) new URL(guess).openConnection();
								huc.setRequestMethod("HEAD");
								int responseCode = huc.getResponseCode();
								
								if(responseCode == 200)
								{
									successGuesses.add(guess);
								}
							}
						}
						catch(MalformedURLException ex){System.err.println(ex.getMessage());}
						catch(IOException ex){System.err.println(ex.getMessage());}
						catch(IllegalArgumentException ex){System.err.println(ex.getMessage());}
					}
				}
			}
			catch(URISyntaxException ex){System.err.println(ex.getMessage());}
		}
		
		links.addAll(successGuesses);
		

		links.addAll(successGuesses);
		return links;
	}
}
