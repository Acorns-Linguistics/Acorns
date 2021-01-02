package acornsapplication;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ZipWebPage
{
   /** Compress an ACORNS web page and its assets
    *  @param directory holding the acorns asset folder
    */
   public ZipWebPage(String directory) throws IOException
   {
      List<String> fileList = makeFileList(directory);

      String root = directory.substring(0, directory.lastIndexOf(File.separator));
      String zipFile = directory + ".acorns";
      File zip = new File(zipFile);
      if (zip.exists()) 
    	  zip.delete();
      
      String zipName;
      FileOutputStream fos = new FileOutputStream(zipFile);
      ZipOutputStream zipOut = new ZipOutputStream(fos);
      int len;
      ZipEntry zipEntry;
      byte[] buffer = new byte[1024];
         
      for(String file : fileList)
      {
         zipName = file.substring(root.length()+1);
         zipEntry = new ZipEntry(zipName);
         zipOut.putNextEntry(zipEntry);
            
         FileInputStream in = new FileInputStream(file);
         while ((len = in.read(buffer)) > 0) 
         {
             zipOut.write(buffer, 0, len);
         }
         in.close();
         zipOut.closeEntry();
      }
      zipOut.close();
   }
   
   /** Create list of files corresponding to an ACORN web page for a file of lessons
    *
    *  @param root directory of the acorns file assets
    *  @return list of files needed to process an ACORNS web page
    */
   public List<String> makeFileList(String directory) throws IOException
   {
      List<String> list = new ArrayList<String>();
      
      // Insure that root directory to lesson assets exists
      File dirFile = new File(directory);
      if (!dirFile.exists()) throw new IOException();
      if (!dirFile.isDirectory()) throw new IOException();
      
      // Insure that the ACORNS web page exists
      File webPage = new File(directory + ".html");
      if (!webPage.exists()) throw new IOException();
      if (!webPage.isFile()) throw new IOException();
      
      makeFileList(list, webPage);  // Add ACORNS the web page
      makeFileList(list, dirFile);  // Add directory of assets
      return list;      
   }
   
   /** Recursively add directory structure list of files to
    *     a list object
    *
    *  @param list is the list object of files and directories\
    *  @param node is either a file or directory to add
    */
   public void makeFileList(List<String> list, File node)
   {
      if(node.isFile())
      {
         list.add(node.toString());
      }

      if(node.isDirectory())
      {
         String[] subNode = node.list();
         for(String filename : subNode)
         {
            makeFileList(list, new File(node, filename));
         }
      }
   }
}
