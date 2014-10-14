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
	
	public static String RemoveTag(String url)
	{
		int tagIndex = url.indexOf("#");
		if(tagIndex >=0)
		{
			return url.substring(0, tagIndex);
		}
		else
			return url;
	}
	
	public static boolean EndsWithExtension(String url) throws MalformedURLException
	{
		String lastPiece = PathHelper.GetLastPiece(url);
		PathHelper.RemoveTag(lastPiece);
		int qIndex = lastPiece.indexOf("?");
		
		if(qIndex >= 0)
		{
			lastPiece = lastPiece.substring(0, qIndex);
		}
		
		if(lastPiece.contains("."))
		{
			return true;
		}
		return false;
	}
	
	public static String RemoveLastPiece(String url) throws MalformedURLException
	{
		int slashIndex = url.lastIndexOf("/");
		String lastPiece = "";
		if(slashIndex > 0)
			url = url.substring(0, slashIndex);

		return url;
	}
	
	public static String GetUrlNoParams(String url)
	{
		int index = url.indexOf("?");
		if(index >= 0)
			return url.substring(0, index);
		else
			return url;
	}
}
