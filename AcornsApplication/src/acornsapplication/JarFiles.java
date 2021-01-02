/*
 * JarFiles.java
 *
 *   @author  HarveyD
 *   @version 4.1
 *
 *   Copyright 2007-2015, all rights reserved
 */

package acornsapplication;
 
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.net.*;
  
/** This class is used to merge a set of Jar/Zip Files in a Jar File
  * It is ignored the manifest.
  */
public class JarFiles
{
   private final static int BUFF_SIZE = 65000;
  
   /** Class to merge and jar files together */
   public JarFiles()  {}
 
   /** This method takes the content of all jar/zip files from the set
     * @param jarNames URL vector of the names of the jars to merge together
     * @param objects array of objects to write to the jar as a file
     * @param destination of the file to contain the merged jars
     * classes of jarFilesNames
     */
   public boolean merge(ArrayList<URL> jarNames, Object[] objects, String destination)
   {
       HashSet<String> directories = new HashSet<String>();
       byte[] buffer = new byte[BUFF_SIZE];
       boolean success = true;

       JarOutputStream jarOutput = null;
       JarFile         source    = null;
       String          fileName;
  
       // create manifest object
       Manifest manifest = new Manifest();
       manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

       // create the output jar file
       try
       {
          jarOutput = new JarOutputStream(new BufferedOutputStream
                            (new FileOutputStream(destination)), manifest);
       }
       catch (Exception e) { return false; }
  
       for (int i=0; i<jarNames.size(); i++)
       {
          try
          {
             fileName = new File(jarNames.get(i).toURI()).getCanonicalPath();
             source = new JarFile(fileName);
          }
          catch (Exception e) { success = false; break; }

          if (!addJar(jarOutput, source, directories, buffer)) 
          { success = false; break; }
          
          try {source.close();} catch (Exception e) {}
      }  //End for

      if (objects!=null && success)
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = null;
         try
         {
             // Create jar entry.
             ZipEntry zipName = new ZipEntry("applicationObjects");
             jarOutput.putNextEntry(zipName);
             byte[] byteArray = null;

             // Serialize the list of objects.
             oos = new ObjectOutputStream(baos);
             for (int i=0; i<objects.length; i++)
             {
                oos.writeObject(objects[i]);
                byteArray = baos.toByteArray();
                baos.close();
             }

            // Write the byte array to the jar entry
            if (byteArray !=null) jarOutput.write(byteArray);
         }  catch (Exception e)
         {   success=false;
             try {  baos.close();
                    if (oos!=null)oos.close();}
             catch(Exception oex) {}
         }
      }
      try
      {   jarOutput.close(); source.close(); }
      catch (Exception e) {}
      return success;
      
    } // End of merge()
  
  
    /** copy all entries from source to destination jar stream
      * @param out the jar that will merge all entries from source jar
      * @param jar source jar object
      * @param directories HashSet of directories added so far
      * @param buffer byte array for copying data to merged jar
      * @return false if fails, true otherwise
      */
   private boolean addJar
          (JarOutputStream out, JarFile jar, HashSet<String> directories, byte[] buffer)
   {
      try 
      {
        Enumeration<JarEntry> entries = jar.entries(); 
        JarEntry    currentEntry = null;
        InputStream in;
        int         bytes;
        
        while (entries.hasMoreElements()) 
        {
           currentEntry = (JarEntry)entries.nextElement();
 
           // if current entry is manifest then it is skipped
           if(currentEntry.getName().equalsIgnoreCase("META-INF/") ||
              currentEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))
           continue;
 
          // if current entry is a directory that was previously added to the
          // destination JAR then it is skipped
          if( currentEntry.isDirectory() 
                   && directories.contains(currentEntry.getName())) 
              continue;
 
         try // otherwise add the current entry
         {
            if (currentEntry.isDirectory())
            {
                directories.add(currentEntry.getName());
            }    
 
            // put the entry into the destination JAR
            out.putNextEntry(new JarEntry(currentEntry.getName()));
 
            // Now copy the data
            in = jar.getInputStream(currentEntry);
            bytes = 0;
            while((bytes = in.read(buffer,0,BUFF_SIZE))!=-1)
            {   out.write(buffer,0,bytes);  }
            in.close();
            out.flush();
            out.closeEntry();
         } 
         catch (Exception ex) 
         { 
        	 if (ex.getMessage().startsWith("duplicate")) continue;
        	 return false; 
         }
           
        }  // End while
        
      } catch (Exception e) { return false; }
      return true;
		
   }  // End addJar()
 
}  // End JarFiles class
