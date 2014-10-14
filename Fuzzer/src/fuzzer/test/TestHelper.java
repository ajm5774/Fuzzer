package fuzzer.test;

import java.io.IOException;
import java.net.MalformedURLException;
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
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import fuzzer.discover.DiscoverHelper;
import fuzzer.util.Utility;

public class TestHelper {
	private static WebClient client = new WebClient();
	private static String[] sensitives;
	
	public static void Test(String url, String commonFileName, String vectorFileName, String sensitiveFileName, boolean random, int timeout)
	{
		String[] vectors = Utility.GetDelimStrings(vectorFileName, "\r\n");
		sensitives = Utility.GetDelimStrings(sensitiveFileName, "\r\n");
		String[] commonWords = Utility.GetDelimStrings(commonFileName);
		
		try
		{
			HashMap<String, HtmlPage> pages = DiscoverHelper.DiscoverPages(url, commonWords);
			
			for(String vector : vectors)
			{
				SendToForm(vector, pages);
				SendToUrl(vector, DiscoverHelper.GetUrlParams(pages.keySet()));
			}
		}
		//TODO: we need to catch timeout exceptions in a different block
		catch(Exception ex){}
		
	}
	
	private static void SendToForm(String vector, HashMap<String, HtmlPage> pages)
	{
		List<HtmlElement> inputs;
		List<HtmlForm> forms;
		HtmlElement submitButton = null;
		Page responsePage;
		String formAction = "";
		
		ArrayList<String> inputNames = new ArrayList<String>();
		inputNames.add("input");
		inputNames.add("textArea");
		
		for(HtmlPage page : pages.values())
		{
			forms = page.getForms();
			for(HtmlForm formEle : forms)
			{
				
				inputs = formEle.getHtmlElementsByTagNames(inputNames);
				for(HtmlElement inputEle : inputs)
				{
					if(inputEle.hasAttribute("type") && inputEle.getAttribute("type").equals("submit"))
						submitButton = inputEle;
					else
						inputEle.setAttribute("value", vector);
				}
				
				if(submitButton != null)
				{
					try {
						responsePage = submitButton.click();
						
						if(SensitiveDataleaked(responsePage, sensitives))
						{
							System.out.println("--" + page.getUrl() + "--");
							formAction = formEle.getAttribute("action");
							if(formAction.isEmpty())
								formAction = page.getUrl().toString();
							System.out.println("The form submitting to '" + formAction +
									"' may have a potential vulnerability from vector " + vector + ".");
						}
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
	private static void SendToUrl(String vector, Map<String, Set<String>> urlParams)
	{
		String vectoredUrl = "";
		String vectoredParams = "";
		Set<String> vectoredParamSet;
		String[] vectoredParamList;
		String[] vectoredParamArray;
		Page responsePage;
		
		for(String rootUrl : urlParams.keySet())
		{
			vectoredParamSet = urlParams.get(rootUrl);
			vectoredParamArray = vectoredParamSet.toArray(new String[vectoredParamSet.size()]);
			for(int i = 0; i < vectoredParamArray.length; i++)
				vectoredParamArray[i] += "=" + vector;
			
			vectoredParams = Utility.Implode("&", vectoredParamArray);
			
			vectoredUrl = rootUrl + "?" + vectoredParams;
			
			try {
				responsePage = client.getPage(vectoredUrl);
				
				if(SensitiveDataleaked(responsePage, sensitives))
					System.out.println("The parameters for " + rootUrl +
							" may have a potential vulnerability from vector (" + vector + ").");
				
			} catch (FailingHttpStatusCodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				System.err.println(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println(e.getMessage());
			}
		}
	}
	
	private static boolean SensitiveDataleaked(Page page, String[] keywords)
	{
		WebResponse response = page.getWebResponse();
	    String content = response.getContentAsString();
		for(String keyword: keywords)
		{
			if(content.contains(keyword))
				return true;
		}
		
		return false;
	}
}
