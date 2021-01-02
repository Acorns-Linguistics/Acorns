/*
 * JarLoader.java
 *
 * Created on December 15, 2008, 4:04 PM
 *
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;

import java.net.*;
import java.util.*;

import org.acorns.*;
import org.acorns.language.*;

public class JarLoader
{
   /** Method to add jar files to the system loaderl
    * 
    * @param codebase The root folder containing the jar files
    */
   public JarLoader(URL codebase)  
   {
	  // Find the jars in the base directory
      ArrayList<String> types = new ArrayList<String>();
      ArrayList<URL> jars = findJars(codebase, types);
      if (jars==null)
      {
          throw new NoSuchElementException("Couldn't find any jars");
      }
      
      String[] newArgs = new String[types.size()];
      newArgs = types.toArray(newArgs);
      setAvailableLessonTypes(newArgs);
   }	   // End of JarLoader.
   
/** Do a directory search for all the jar files 
  * 
  *  @param urlBase the URL to the folder containing the code base
  *  @param types list of lesson types found (if null, return all jars)
  *  @return A vector of the jars found in the code base folder (or null if none)
  */
   public static ArrayList<URL>findJars(URL urlBase, ArrayList<String> types)
   {
      ArrayList<URL> urls = new ArrayList<URL>();
      
      try
      {
         URL url = new URL(urlBase + "jars");
         URLConnection connection = url.openConnection();
         InputStream stream = connection.getInputStream();
         BufferedReader in = new BufferedReader(new InputStreamReader(stream));
         String[] fileName;
         String line;
         URL jarURL;
         Boolean addJar = false;
      
         while ( (line = in.readLine()) != null)
         {
            addJar = true;
            fileName = line.split(";");
            if (fileName[0].endsWith(".jar"))
            {
                // If class already loaded, don't attach the jar.
                if (fileName.length>1)                   
                {  
                   if (fileName[1].equals(".")) 
                   {
                       if (types!=null) addJar = false;
                   }
                   else
                   {
                       try
                       { types.add(fileName[1]);  }
                       catch (Throwable t)  {}
                   }
                }
                else if (types==null) addJar = false;
                if (addJar)
                {    
                   jarURL = new URL(urlBase + fileName[0]);
                   urls.add(jarURL);
                }
            }
         }
         in.close();
         return urls;
      }
      catch (Exception e)  {  return null; }
   }	
  
   /** Determine the list of available lesson types 
     *  @return false if none available.
     */
     private static boolean setAvailableLessonTypes(String[] args)
     {
         new LanguageText(Environment.getHelpSet(), 
         		new String[]{"dictionary"}, false );

        Class <?>  className;
        
        int     tablePointer = 0;
        Class<?>[] lessonTable  = new Class[args.length];
        for (int a=0; a<args.length; a++)
        {
            try
            {   // Try to locate the class name.
                className = Class.forName(args[a]);
                lessonTable[tablePointer] = className;
                tablePointer++;
            }
            catch (Throwable t)  {}
        }
   
        if (tablePointer==0) return false;
        
        Class<?>[] lessonTypes = new Class[tablePointer];
        System.arraycopy(lessonTable, 0, lessonTypes, 0, tablePointer);
        LessonTypes.setAvailableLessonTypes(lessonTypes);
        return true;
     }    // End of setAvailableLessonTypes.
	
}  // End of JarLoader