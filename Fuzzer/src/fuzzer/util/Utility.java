package fuzzer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

public class Utility {

	public static String[] GetDelimStrings(String fileName)
	{
		if(fileName == null || fileName.isEmpty())
			return new String[0];

		File file = new File(fileName);
		if(!file.exists())
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
		
	    return commonWordsString.split("[ |\r\n|,|\n|\t]+");
	    
	}
	
	public static String Implode(String glue, String[] strArray)
	{
	    String ret = "";
	    for(int i=0;i<strArray.length;i++)
	    {
	        ret += (i == strArray.length - 1) ? strArray[i] : strArray[i] + glue;
	    }
	    return ret;
	}

}
