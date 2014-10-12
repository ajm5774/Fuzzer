package fuzzer.discover;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import fuzzer.test.TestHelper;

public class Main {
	

	public static void main(String[] args)
	{
		String url;
		String custom_auth = null;
		String common_words = null;
		
		//Used for fuzz test
		String vectors = null;
		String sensitive = null;
		boolean test = false;
		boolean random = false;
		int slow = 500;
		
		if(args.length == 0)
			return;



		if (args[0].equals("test"))
		{
			test = true;
		}
		
		url = args[1];
		for (int i = 2; i < args.length; i++)
		{
			String option = args[i];
			
			
			if (option.startsWith("--custom-auth="))
			{
				custom_auth = option.substring(14);
			}
			
			if (option.startsWith("--common-words="))
			{
				common_words = option.substring(15);
			}

			//for fuzz test
			if (test)
			{
				if (option.startsWith("--vectors="))
				{
					vectors = option.substring(10);
				}
				if (option.startsWith("--sensitive="))
				{
					sensitive = option.substring(12);
				}
				if (option.startsWith("--random="))
				{
					random = option.substring(9).equals("true")?true:false;
				}
				if (option.startsWith("--slow="))
				{
					slow = Integer.parseInt(option.substring(7));
				}
			}
			
		}	
		
		
		if (common_words == null)
		{
			System.out.println("--common-words is required.");
			return;
		}
		if (test && vectors == null)
		{
			System.out.println("--vectors is required");
			return;
		}
		if (test && sensitive == null)
		{
			System.out.println("--sensitive is required");
			return;
		}
		
		DiscoverHelper.Discover(url, common_words, custom_auth);
		if(test)
			TestHelper.Test(url, common_words,vectors , sensitive, random, slow);
	}	
}		
