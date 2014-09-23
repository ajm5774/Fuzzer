package fuzzer.discover;

import java.net.MalformedURLException;
import java.net.URL;

public class PathHelper {
	public static String Combine(String[] segments)
	{
		String result = "";
		
		for(String segment: segments)
		{
			if(result.endsWith("/") && segment.startsWith("/"))
				result += segment.substring(1);
			else if(result.endsWith("/") || segment.startsWith("/"))
				result += segment;
			else if(result.isEmpty())
				result += segment;
			else
				result += "/" + segment;
		}
		return result;
	}
	
	public static String GetLastPiece(String url) throws MalformedURLException
	{
		String file = new URL(url).getFile();
		int slashIndex = file.lastIndexOf("/");
		String lastPiece = "";
		if(slashIndex > 0)
			lastPiece = file.substring(slashIndex);
		
		return lastPiece;
	}
}
