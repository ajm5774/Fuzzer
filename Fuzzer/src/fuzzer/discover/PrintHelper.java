package fuzzer.discover;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gargoylesoftware.htmlunit.util.Cookie;

public class PrintHelper {
	
	private static String GetPrintLabel(String label)
	{

		return "------------------------------" + label + "------------------------------\r\n";
	}
	
	public static String UrlParamsToString(Map<String, Set<String>> urlParams)
	{
		String result = "";
		result += GetPrintLabel("Url Parameter Inputs");
		
		for(String rootUrl: urlParams.keySet())
		{
			result += rootUrl + "\r\n";
			for(String param: urlParams.get(rootUrl))
				result += "\t" + param + "\r\n";
		}
		
		return result;
	}
	
	public static String InputElementsToString(Set<String> inputEles)
	{
		String result = "";
		result += GetPrintLabel("Input Elements");
		
		for(String inputEle: inputEles)
		{
			result += inputEle;
		}
		
		return result;
	}
	
	public static String CookiesToString(Set<Cookie> cookies)
	{
		String result = "";
		result += GetPrintLabel("Cookies");
		
		for(Cookie cookie: cookies)
		{
			result += cookies.toString() + "\r\n";
		}
		
		return result;
	}
	
	public static String PagesToString(Set<String> urls)
	{
		String result = "";
		result += GetPrintLabel("Pages");
		
		for(String url: urls)
			result += url + "\r\n";
		
		return result;
	}
}
