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

import org.apache.xerces.util.URI.MalformedURIException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class DiscoverHelper {
	
	public static String[] _pageGuesses = {"admin", "edit"};
	public static String[] _commonExtensions = {".php", ".jsp"};
	public static File common_usernames = new File("./common_usernames.txt");
	public static File common_passwords = new File("./common_passwords.txt");

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		//System.out.println(Discover("http://torrentz.eu/", ""));
		WebClient webClient =  new WebClient();
		customAuth(webClient, "dwa", common_usernames, common_passwords);
	}
	
	//==================================================Public Methods=============================================================================
	
	public static String Discover(String url, String fileName)
	{
		String result = "";
		
		String[] commonWords = GetCommonWords(fileName);
		
		try {
			Set<String> urls = DiscoverPages(url, commonWords);
			result += PrintHelper.PagesToString(urls);
			result += DiscoverInputs(urls);
		} catch (MalformedURLException e) {
			e.getMessage();
		} catch (IOException e) {
			e.getMessage();
		}
		
		return result;
	}
	
	public static Set<String> DiscoverPages(String url, String[] commonWords)
	{
		Set<String> linkStrings = new HashSet<String>();

		linkStrings = GetLinks(url, true);
		linkStrings = GuessPages(linkStrings, commonWords);
		linkStrings = GuessExtensions(linkStrings);
		
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
		
		if(!new File(fileName).exists())
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
		
	    return commonWordsString.split("[ |\r\n|,|\n|\t]*");
	    
	}
	
	private static Set<Cookie> GetCookies(Set<String> urls)
	{
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		
		Set<Cookie> cookies = new HashSet<Cookie>();
		for(String url: urls)
		{
			cookies.addAll(webClient.getCookieManager().getCookies());
		}
		
		return cookies;
	}
	
	private static Set<String> GetInputElements(Set<String> urls) throws MalformedURLException, IOException
	{
		Set<String> inputs = new HashSet<String>();
		for(String url: urls)
		{
			WebClient webClient = new WebClient();
			webClient.setJavaScriptEnabled(true);
			
			HtmlPage page = webClient.getPage(url);
			
			List<HtmlElement> inputElements = page.getElementsByName("input");
			for(HtmlElement ele: inputElements)
				inputs.add(ele.toString());
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
			String[] params = urlString.substring(qIndex).split("&");
			rootUrl = urlString.substring(0, qIndex - 1);
			
			if(params.length > 0 && result.containsKey(rootUrl))
				result.put(rootUrl, new ArrayList<String>());
			
			for(String param: params)
				result.get(rootUrl).add(param);
		}
		
		return result;
	}
	
	private static Set<String> GetLinks(String url, boolean recursive)
	{
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		
		HtmlPage page;
		Set<String> linkStrings = new HashSet<String>();
		try
		{
			page = webClient.getPage(url);
			System.out.println(url);
			List<HtmlAnchor> anchors = page.getAnchors();
			String href;
			for(HtmlAnchor anchor: anchors)
			{
				href = anchor.getHrefAttribute();
				System.out.println(href);
				if(new URL(href).getHost() == new URL(url).getHost())
				{
					linkStrings.add(href);
					if(recursive)
						linkStrings.addAll(GetLinks(href, recursive));
				}
			}
		}
		catch(MalformedURLException ex){System.out.println(ex.getMessage());}
		catch(IOException ex){System.out.println(ex.getMessage());}
		
		return linkStrings;
	}
	
	private static Set<String> GuessPages(Set<String> links, String[] commonWords)
	{
		Set<String> successGuesses = new HashSet<String>();
		URI uri;
		String guess;
		
		for(String link: links)
		{
			try
			{
				uri = new URI(link);
				for(String newFragment: commonWords)
				{
					try
					{
						guess = uri.toString().replace(uri.getFragment(), newFragment);
						HttpURLConnection huc = (HttpURLConnection) new URL(guess).openConnection();
						huc.setRequestMethod("HEAD");
						int responseCode = huc.getResponseCode();
						
						if(responseCode == 200)
							successGuesses.add(guess);
					}
					catch(MalformedURLException ex){}
					catch(IOException ex){}
				}
			}
			catch(URISyntaxException ex){}
		}
		
		links.addAll(successGuesses);
		
		return links;
	}
	
	private static Set<String> GuessExtensions(Set<String> links)
	{
		Set<String> successGuesses = new HashSet<String>();
		URI uri;
		String guess;
		
		for(String link: links)
		{
			try
			{
				uri = new URI(link);
				for(String newExtension: _commonExtensions)
				{
					try
					{
						if(uri.getPath().contains("."))
						{
							String extension = uri.getPath().substring(uri.getPath().indexOf("."));
							guess = uri.getPath().replace(extension, newExtension);
						}
						else
							guess = uri.getPath() + newExtension;
						HttpURLConnection huc = (HttpURLConnection) new URL(guess).openConnection();
						huc.setRequestMethod("HEAD");
						int responseCode = huc.getResponseCode();
						
						if(responseCode == 200)
							successGuesses.add(guess);
					}
					catch(MalformedURLException ex){}
					catch(IOException ex){}
				}
			}
			catch(URISyntaxException ex){}
		}
		
		links.addAll(successGuesses);
		
		return links;
		
	}

	/**
	 * This code is for demonstrating techniques for submitting an HTML form. Fuzzer code would need to be
	 * more generalized
	 * @param webClient
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private static void doFormPost(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			HtmlInput input = form.getInputByName("quantity");
			input.setValueAttribute("2");
			HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath("//input[@id='submit']");
			System.out.println(submit.<HtmlPage> click().getWebResponse().getContentAsString());
		}
	} 
	private static void customAuth(WebClient webClient, String address, File usernamesFile, File passwordsFile) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		//HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
		HtmlPage page;
	
		String[] common_users = GetCommonWords(usernamesFile.toString());
		String[] common_passwords = GetCommonWords(passwordsFile.toString());
		
		if (address.compareTo("dwa") == 0){
			page = webClient.getPage("http://127.0.0.1/dvwa/");
		}
		else {
			page = webClient.getPage("http://127.0.0.1/bodgeit");
		}
		
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			for (String username: common_users){
				for (String password: common_passwords){
					try{
						form.getInputByName("username").setValueAttribute(username);
						form.getInputByName("password").setValueAttribute(password);
						page = (HtmlPage) form.getInputByName("Login").click();
						
					}
					catch(MalformedURLException ex){}
					catch(IOException ex){}
				}
			}
		}
	}
}
