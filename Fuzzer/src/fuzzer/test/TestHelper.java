package fuzzer.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import fuzzer.discover.DiscoverHelper;
import fuzzer.util.Utility;

public class TestHelper {
	private static Pattern keywordPattern = Pattern.compile("[sql|SQL|exception|malformed]");
	public void Test(String url, String commonFileName, String vectorFileName, String sensitiveFileName, boolean random)
	{
		String[] vectors = Utility.GetDelimStrings(vectorFileName);
		String[] sinsitives = Utility.GetDelimStrings(sensitiveFileName);
		String[] commonWords = Utility.GetDelimStrings(commonFileName);
		
		try
		{
			HashMap<String, HtmlPage> pages = DiscoverHelper.DiscoverPages(url, commonWords);
			
			SendVectors(vectors, pages);
		}
		//TODO: we need to catch timeout exceptions in a different block
		catch(Exception ex){}
		
	}
	
	private void SendVectors(String[] vectors, HashMap<String, HtmlPage> pages)
	{
		
	}
	
	private void SendToForm(String vector, HashMap<String, HtmlPage> pages)
	{
		List<HtmlElement> inputs;
		List<DomElement> forms;
		HtmlElement submitButton = null;
		Page responsePage;
		
		for(HtmlPage page : pages.values())
		{
			forms = page.getElementsByName("form");
			for(DomElement formEle : forms)
			{
				inputs = formEle.getElementsByTagName("input");
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
						
						if(keywordPattern.matcher(responsePage.toString()).find())
							System.out.println("The form submitting to " + formEle.getAttribute("action") +
									" may have a potential vulnerability from vector " + vector + ".");
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
	private void SendToUrl(String vector, HashMap<String, HtmlPage> pages)
	{
		
	}
}
