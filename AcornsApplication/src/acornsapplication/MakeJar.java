/*
 * MakeJar.java
 *
 *   @author  HarveyD
 *   @version 4.1 
 *
 *   Copyright 2007-2015, all rights reserved
 */
package acornsapplication;

import java.io.*;
import java.util.jar.*;

public class MakeJar
{
    byte[] data;

    public MakeJar() 
    {
        data = new byte[2048];
    }
	
	/**
 	 * direct a source directory to a JarOutputStream
	  *
	  * @param sourceDir the directory to be jarred
	  * @param out the jar output stream
	  * @param parent the parent of the current entry
   * @return true if successful, false otherwise
	  */
	private boolean jar(JarOutputStream out, String sourceDir, String dir)
	{
			    // get a list of files from the current directory
       String dirName = sourceDir + "/" + dir;       
			    File file = new File(dirName);
			    String files[] = file.list();
			
			    String tempFile;
			    for (int i=0; i<files.length; i++) 
			    {
          tempFile = dir + "/" + files[i];
          if (!makeJarEntry(out, sourceDir, tempFile)) return false;
			    }   // end of the for loop
			
    return true;
	}
	
	/** jar a source directory
	 *
	 * @param prefix prefix for xml and jar file
  * @param name of xml file (without extension)
  * @return true if success, false otherwise
	 */
	public boolean jar(String sourceDir, String prefix, String xmlPrefix)
	{
		    Manifest manifest = new Manifest();
			   manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

      String xmlName = xmlPrefix + ".xml";
      String jarDest = sourceDir + "/" + prefix + ".jar";

      JarOutputStream out = null;
      try
      {
			      out = new JarOutputStream(new BufferedOutputStream
                                  (new FileOutputStream(jarDest)), manifest);
      }
      catch(Exception e) { return false; }
      
      boolean success = makeJarEntry(out, sourceDir, xmlName);
      if (success) success = makeJarEntry(out, sourceDir, xmlPrefix);
			   if (success) success = jar(out, sourceDir, xmlPrefix);
			   try { out.close(); } catch(Exception e) {}
      return success;
	}
 
 /** Create a jar entry
  * 
  * @param jar the JarOutpuStream to write to
  * @param dir directory containing file
  * @param fileName name of file or add to insert into jar
  * @return true if success, false otherwise
  */
   private boolean makeJarEntry(JarOutputStream jar, String dir, String name)
   {
      String fileName = dir + "/" + name;
      File file = new File(fileName);
      
      JarEntry entry = new JarEntry(name);
      if (file.isDirectory()) entry = new JarEntry(name + "/");
      
      boolean  status = true;
      
      try
      {
         jar.putNextEntry(entry);
    
         BufferedInputStream origin = null;
         if(file.isFile())
		 {
	 	    origin 
               = new BufferedInputStream(new FileInputStream(fileName), 2048);
					
            int count;
            while((count = origin.read(data, 0, 2048)) != -1) 
            {
               jar.write(data, 0, count);
            }
         }
         jar.flush();
         origin.close();
      }
      catch (Exception e) { status = false;  }
      
      try { jar.closeEntry(); } catch(Exception e) {}
      return status;

   }   // End of makeJarEntry
   
} // end of class MakeJar
