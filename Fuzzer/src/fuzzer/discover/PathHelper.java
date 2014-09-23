package fuzzer.discover;

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
}
