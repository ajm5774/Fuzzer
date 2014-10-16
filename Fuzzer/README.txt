Enter a line in the following formats, where fuzzer.jar is in the current folder.

To run the 'discover' portion:

java -jar fuzzer.jar discover *url* --common-words=*commonWordFile* **OPTIONAL** --custom-auth=*string*

--custom-auth authenticates to the given string's website

Example:
C:\Users\Andrew>java -jar fuzzer.jar discover http://127.0.0.1:8080/bodgeit/ --common-words=hello.txt

The output shows all links(including those guessed using the common words file), inputs, URL parameters,
and cookies for the given URL.

To run the 'test' portion:

java -jar fuzzer.jar discover *url* --common-words=*commonWordFile* --custom-auth=*string* --vectors=*vectorFile* 
--sensitive=*sensitiveFile* 

To run test you must provide a vector file and a sensitive word file.

Any data that you do not want leaked to the user, make sure to include in the sensitive word file. 

The output will show all instances in which an input form or URL query string, when a vector is inserted, 
reveals content from the sensitive file. 