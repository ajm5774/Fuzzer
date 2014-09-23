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
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class DiscoverHelper {
	public static String[] _pageGuesses = {"admin", "edit"};
	public static String[] _commonExtensions = {"", ".php", ".jsp"};


	//==================================================Public Methods=============================================================================
	
	
	public static String Discover(String url, String fileName)
	{
		String result = "";
		
		String[] commonWords = GetCommonWords(fileName);
		
		try {
			Set<String> urls = DiscoverPages(url, commonWords);
			result += PrintHelper.PagesToString(urls);
			result += DiscoverInputs(urls);
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
		webClient.getOptions().setTimeout(2000);
		
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
			for(HtmlAnchor anchor: anchors)
			{
				try
				{
					href = anchor.getHrefAttribute();
					
					if(!href.startsWith("http"))
					{
						String lastPiece = PathHelper.GetLastPiece(url);
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
		catch(MalformedURLException ex){System.err.println("GetLinks: " + ex.getMessage());}
		catch(IOException ex){System.err.println("GetLinks: " + ex.getMessage());}
		catch(Exception ex){System.out.println(ex.getMessage());}
		
		return UniqueLinks;
	}
	
	private static Set<String> GuessPages(Set<String> links, String[] commonWords)
	{
		Set<String> successGuesses = new HashSet<String>();
		URI uri;
		String guess, fragmentWithextension;
		
		for(String link: links)
		{
			try
			{
				uri = new URI(link);
				for(String newFragment: commonWords)
				{
					
					for(String newExtension: _commonExtensions)
					{
						fragmentWithextension = "/"+newFragment + newExtension;
						try
						{
							String lastPiece = PathHelper.GetLastPiece(link);
							boolean endsWithExtension = false;
							for(String ce: _commonExtensions)
								if (ce.length() > 0 && link.endsWith(ce))
									endsWithExtension = true;
							if(!endsWithExtension)
							{
								guess = link+fragmentWithextension;
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
						}
						catch(MalformedURLException ex){System.out.println(ex.getMessage());}
						catch(IOException ex){System.out.println(ex.getMessage());}
					}
				}
			}
			catch(URISyntaxException ex){System.out.println(ex.getMessage());}
		}
		
		links.addAll(successGuesses);
		
		if(successGuesses.size() > 0)
		{
			links.addAll(GuessPages(successGuesses, commonWords));
			return links;
		}
		else
		{
			return links;
		}
	}
	
	public static void customAuth(String address, String usernamesFile, String passwordsFile, String url, boolean custom) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		//HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
		WebClient webClient = new WebClient();
		HtmlPage page;
		String successURL;

		String[] common_users = GetCommonWords(usernamesFile);
		String[] common_passwords = GetCommonWords(passwordsFile);
		if (custom)
		{
			if (address.compareTo("dvwa") == 0){
				page = webClient.getPage("http://127.0.0.1/dvwa/");
				successURL = ("http://127.0.0.1/dvwa/index.php");
			}
			else {
				page = webClient.getPage("http://127.0.0.1/bodgeit");
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
			page = webClient.getPage(url);
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
}
