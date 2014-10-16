package fuzzer.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
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
	private static int _timeout = 1;
	
	public static void Test(String url, String commonFileName, String vectorFileName, String sensitiveFileName, boolean random, int timeout, String customAuth)
	{
		client.getOptions().setTimeout(_timeout);
		String[] vectors = Utility.GetDelimStrings(vectorFileName, "\r\n");
		sensitives = Utility.GetDelimStrings(sensitiveFileName, "\r\n");
		String[] commonWords = Utility.GetDelimStrings(commonFileName);
		
		try
		{
			HashMap<String, HtmlPage> pages = DiscoverHelper.DiscoverPages(url, commonWords, customAuth, customAuth!=null);
			
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
					formAction = formEle.getAttribute("action");
					if(formAction.isEmpty())
						formAction = page.getUrl().toString();
					
					responsePage = getPageWithTimeout(client, formAction, submitButton);
					
					if(responsePage == null || !IsOutcomeOrdinary(responsePage))
					{
						System.out.println("The form submitting to '" + formAction +
								"' may have a potential vulnerability from vector " + vector + ".\n");
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
			
			responsePage = getPageWithTimeout(client, vectoredUrl, null);
			
			if(responsePage == null || !IsOutcomeOrdinary(responsePage))
				System.out.println("The parameters for " + rootUrl +
						" may have a potential vulnerability from vector (" + vector + ").");
		}
	}
	
	private static Page getPageWithTimeout(WebClient client, String url, HtmlElement submitButton)
	{
		Page responsePage = null;
		
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
	
	private static boolean IsOutcomeOrdinary(Page page)
	{
		WebResponse response = page.getWebResponse();
		boolean ret = true;
		ArrayList<String> list = new ArrayList<String>();
		
	    if(SensitiveDataleaked(response.getContentAsString(), sensitives))
	    {
	    	list.add("Sensitive data leaked");
	    	ret = false;
	    }
	    if(!IsResponseCodeOK(response))
	    {
	    	list.add("Status code not ok: "  + response.getStatusCode() + "-" + response.getStatusMessage());
	    	ret = false;
	    }
	    
	    if(ret == false)
	    {
	    	System.out.println("--" + page.getUrl() + "--");
	    	System.out.println(Utility.Implode("\n", list.toArray(new String[list.size()])));
	    }
	    
	    return ret;
	}
	
	private static boolean IsResponseCodeOK(WebResponse response)
	{
		int code = response.getStatusCode();
		if(code != 200)
		{
			return false;
		}
			
		return true;
	}
	
	private static boolean SensitiveDataleaked(String responseString, String[] keywords)
	{
		for(String keyword: keywords)
		{
			if(responseString.contains(keyword))
			{
				return true;
			}
		}
		
		return false;
	}
}
