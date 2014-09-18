package fuzzer.discover;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.util.URI.MalformedURIException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class DiscoverHelper {
	
	public static String[] _pageGuesses = {"admin", "edit"};
	public static String[] _commonExtensions = {".php", ".jsp"};

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		discoverLinks(webClient);
		doFormPost(webClient);
		webClient.closeAllWindows();
	}

	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public static void discoverLinks(WebClient webClient) throws IOException, MalformedURLException {
		HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit");
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
		}
	}
	
	public static List<String> DiscoverPages(String url)
	{
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		
		HtmlPage page;
		List<String> linkStrings = new ArrayList<String>();
		try
		{
			page = webClient.getPage(url);
			List<HtmlAnchor> anchors = page.getAnchors();
			
			for(HtmlAnchor anchor: anchors)
				linkStrings.add(anchor.getHrefAttribute());
			
			linkStrings = GuessPages(linkStrings);
			linkStrings = GuessExtensions(linkStrings);
		
		}
		catch(MalformedURLException ex){}
		catch(IOException ex){}
		
		return linkStrings;
		
	}
	
	private static List<String> GuessPages(List<String> links)
	{
		List<String> successGuesses = new ArrayList<String>();
		URI uri;
		String guess;
		
		for(String link: links)
		{
			try
			{
				uri = new URI(link);
				for(String newFragment: _pageGuesses)
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
	
	private static List<String> GuessExtensions(List<String> links)
	{
		List<String> successGuesses = new ArrayList<String>();
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
}
