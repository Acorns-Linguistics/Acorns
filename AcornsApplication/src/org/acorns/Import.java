/**
 * Import.java
 * @author HarveyD
 * @version 5.00 Beta
 *
 * Copyright 2009-2015, all rights reserved
 */
package org.acorns;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.sound.sampled.*;

import org.acorns.lesson.*;
import org.acorns.language.*;
import org.acorns.data.*;

/** Class to import an ACORNS file of lessons */
public class Import 
{
    private Document   document;
    private FileObject fileObject;
    private String     directoryName;
    private URLClassLoader loader;

    /** The active lesson being processed. */
    Lesson lesson;
    /** The lesson layer being processed */
    private int layer;
    /** The coordinates for an acorn, link, picture, or sound annotation */
    private Point point;
    /** Flag for type of point */
    private boolean sound = false;
    /** Flags to determine which tags are below a point tag */
    private boolean soundExists=false, descExists=false, spellExists=false;

    /** Parameters for importing audio into lessons */
 	private String[] soundData;
    /** The URL for reading the sound file */

    public Import(File file, FileObject fileObject, String directoryName)
           throws ParserConfigurationException, IOException, SAXException
    {
       soundData = new String[SoundData.SIZE];
       for (int i=0; i<soundData.length; i++) soundData[i] = "";

       DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
       DocumentBuilder db = dbf.newDocumentBuilder();
       document = db.parse(file);
       importDocument(fileObject, directoryName);
    }
    /** Constructor to open and create a DOM from the XML document
     *
     *  @param url The URL cotaining the document to import.
     * @param fileObject The fileObject into which to import
     * @param directoryName The directory part of the import file
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public Import(URL url, FileObject fileObject, String directoryName)
           throws ParserConfigurationException, IOException, SAXException
    {
       soundData = new String[SoundData.SIZE];
       for (int i=0; i<soundData.length; i++) soundData[i] = "";

       String fileName = url.getPath();

       if (fileName.endsWith(".jar"))
       {
           fileName = fileName.substring(0,fileName.length() - 3);
           fileName += "xml";
           fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
           loader = new URLClassLoader(new URL[]{url});
           url = Import.class.getResource(fileName);
       }

       URLConnection urlConn = url.openConnection();

       // The following was to attempt to solve the problem of security
       //   exceptions in browsers. However, it is not a caching problem. I
       //   suspect it is because the number of persistent connections to
       //   the server exceeds the maximum. This would explain why things
       //   work better when all lessons are in the same directory instead of
       //   sub-directories.
       //       urlConn.setUseCaches(false);
       //       urlConn.setRequestProperty("Cache-Control","no-store,max-age=0,no-cache");
       //       urlConn.setRequestProperty("Expires", "0");
       //       urlConn.setRequestProperty("Pragma", "no-cache");

       DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
       DocumentBuilder builder = factory.newDocumentBuilder();

       InputStream stream = urlConn.getInputStream();
       document = builder.parse(stream);
       importDocument(fileObject, directoryName);
       stream.close();
    }

    /** Common method between importing from URL and FIle
     *
     * @param fileObject The file object containing ACORNS lessons
     * @param directoryName The path to the file containing the DOM
     */
    private void importDocument
            (FileObject fileObject, String directoryName)
    {
       this.fileObject = fileObject;
       this.directoryName = directoryName;

       Element root = document.getDocumentElement();
       root.normalize();
       String language = root.getAttribute("language");
       String author = root.getAttribute("author");
       String description = root.getAttribute("description");

        // Deprecated (for upward compatibility with version 5.00
        if (description.length()==0)
            description = root.getAttribute("desc1")
                                    + root.getAttribute("desc2");

        fileObject.setProperties(description, language, author);
    }

    /** Get list of lesson nodes
     *
     * @return A NodeList of all the <lesson> node elements
     */
    public NodeList getLessons()
    {  return document.getElementsByTagName("lesson"); }

    /** Import a particular <lesson> tags
     *
     * @param element The <lesson> tag element in the DOM
     * @return
     * @throws org.xml.sax.SAXException
     * @throws org.acorns.InvalidFileTypeException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    public Lesson importLesson(Node element)
             throws SAXException, InvalidFileTypeException
                  , UnsupportedAudioFileException, IOException
    {
       importLessonHeader(element);
       traverseDOM(element);
       return null;
    } // End of importLesson()

    /** Method to import the lesson header and set the lesson properties
     *
     * @param element The DOM <lesson> tag element
     * @throws org.xml.sax.SAXException
     * @throws org.acorns.InvalidFileTypeException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    public void importLessonHeader(Node element)
             throws SAXException, InvalidFileTypeException
                  , UnsupportedAudioFileException, IOException
    {
        Element lessonNode = (Element)element;

        String[] info = new String[AcornsProperties.TYPES];
        for (int i=0; i<info.length; i++) info[i] = "";

        info[Properties.TYPE]  = lessonNode.getAttribute("type");
        info[Properties.TITLE] = lessonNode.getAttribute("title");
        info[Properties.NAME]  = lessonNode.getAttribute("name");
        info[Properties.DESC]  = lessonNode.getAttribute("description");

        // Verify needed information.
        if (info[AcornsProperties.TITLE].trim().length()==0)
         throw new SAXException(LanguageText.getMessage("acornsApplication",2));

      // Create panel to hold this lesson's data.
      boolean createdLesson = fileObject.newLesson(info, null);
      if (!createdLesson)
         throw new SAXException(LanguageText.getMessage("acornsApplication",3));
      lesson = fileObject.getActiveLesson();
      lesson.setDirty(true);
    }

    /** method to recursively process a node of the DOM tree
     *
     * @param node the node to process
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     */
    private void traverseDOM(Node node) 
            throws SAXException, IOException
                 , UnsupportedAudioFileException
    {   Element element;
        NodeList nodes = node.getChildNodes();
        int count = nodes.getLength();
        for (int i=0; i<count; i++)
        {   try
            {  element = (Element)nodes.item(i);  
            }
            catch (ClassCastException e) { continue; }
            processNode( element );
            traverseDOM( element );
        }
    }

    /** Process an element in the DOM tree
     *
     * @param element The element in the tree to process
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     */
    private void processNode(Element element) 
            throws SAXException, IOException, UnsupportedAudioFileException
    {
        String tag = element.getTagName();
        if (tag.equals("level"))       processLayer(element);
        if (tag.equals("layer"))       processLayer(element);
        if (tag.equals("param"))       processParam(element);
        if (tag.equals("category"))    processCategory(element);
        if (tag.equals("point"))       processPoint(element);
        if (tag.equals("gloss"))       processGloss(element);
        if (tag.equals("spell"))       processSpell(element);
        if (tag.equals("description")) processDescription(element);
        if (tag.equals("sound"))       processSound(element);
        if (tag.equals("link"))        processLink(element);
        if (tag.equals("image"))       processImage(element, 0);
        if (tag.equals("font"))        processFont(element);
        if (tag.equals("picture"))     processImage(element, -1);
    }   // End of processNode()

    /** Method to process a layer tag
     *
     * @param tag The <layer> tag elements
     * @throws org.xml.sax.SAXException
     */
    private void processLayer(Element tag) throws SAXException
    {
       String language;
       LessonPanel lessonPanel = fileObject.getLessonInfo();
       String[] info = lessonPanel.getPanelInfo();

       layer  = 1;
       String layerName = tag.getAttribute("name");
       String align = tag.getAttribute("align");
       String excluded = tag.getAttribute("excluded");
       language = tag.getAttribute("language");
       if (language.length()==0) language = "English";

       try
       {
          layer = Integer.parseInt(tag.getAttribute("value"));
          if (layer<0 || layer>AcornsProperties.MAX_LAYERS)
              throw new SAXException();
          if (layer>0)
              info[AcornsProperties.LAYERNAMES+layer-1] = layerName;
          lessonPanel.setPanelInfo(info);

          String[] values = {language, align, excluded};
          lesson.importData(layer, null, null, values
                                 , AcornsProperties.LAYER);
       }
       catch (Exception nfe)
       { throw new SAXException(LanguageText.getMessage("acornsApplication",4) + " " + nfe);
       }
    }  // End of processLayer()

   /** Process the <param> tags
    *
    * @param tag The <param> tag element
    * @throws org.xml.sax.SAXException
    * @throws java.io.IOException
    * @throws javax.sound.sampled.UnsupportedAudioFileException
    */
   private void processParam(Element tag) 
           throws SAXException, IOException, UnsupportedAudioFileException
   {
      String name, value;
      ArrayList<String>  listOfAttributes = new ArrayList<String>();

      NamedNodeMap attributes = tag.getAttributes();
      int count = attributes.getLength();
      for (int i=0; i<count; i++)
      {
          name = attributes.item(i).getNodeName();
          value = attributes.item(i).getNodeValue();

          if (name.equals("link"))
          { lesson.setLink(value); }
          else
          {  listOfAttributes.add(name);
             listOfAttributes.add(value);
          }
      }

      if (listOfAttributes.size()>0)
      {  String[] params = new String[listOfAttributes.size()];
         params = listOfAttributes.toArray(params);
         lesson.importData
                (layer, point, null, params, AcornsProperties.PARAM);
      }
   }

   /** Method to process a <category> tag
    *
    * @param tag tag object
    * @throws SAXException
    * @throws IOException
    * @throws UnsupportedAudioFileException
    */
   private void processCategory(Element tag)
           throws SAXException, IOException, UnsupportedAudioFileException
   {
       String[] params = new String[] {"", ""};
       params[0] = tag.getAttribute("value");
       params[1] = tag.getAttribute("name");
       lesson.importData(layer, point, null, params, AcornsProperties.CATEGORY);
       sound = false;
   }

   /** Method to process a <point> tag
    * 
    * @param tag The <point> DOM element
    * @throws org.xml.sax.SAXException
    */
   private void processPoint(Element tag) throws SAXException
   {  int x=-1, y=-1;
		sound = soundExists = descExists = spellExists = false;
      for (int i=0; i<soundData.length; i++) soundData[i] = "";

      if (tag.hasAttribute("x"))
         x = Integer.parseInt(tag.getAttribute("x"));
      if (tag.hasAttribute("y"))
         y = Integer.parseInt(tag.getAttribute("y"));

      if (tag.getAttribute("type").toLowerCase().equals("sound"))
          sound = true;

      if (x<0) 
         throw new SAXException(LanguageText.getMessage("acornsApplication",5));
      point = new Point(x, y);

      NodeList nodes = tag.getChildNodes();
      int count = nodes.getLength();
      for (int i=0; i<count; i++)
      {   try
          {  tag = (Element)nodes.item(i);  }
          catch (ClassCastException e) { continue; }

          if (tag.getNodeName().equals("sound")) soundExists = true;
          if (tag.getNodeName().equals("description")) descExists = true;
          if (tag.getNodeName().equals("spell")) spellExists = true;
      }
   }

   /** Method to process the <gloss> element
    *
    * @param tag The <gloss> element in the DOM
    * @throws org.xml.sax.SAXException
    */
   private void processGloss(Element tag)
           throws SAXException, IOException, UnsupportedAudioFileException
   {
	   if (!sound) throw new SAXException
                         (LanguageText.getMessage("acornsApplication",6));
      soundData[SoundData.GLOSS] = tag.getTextContent();

      if (!(soundExists || descExists || spellExists))
      { lesson.importData(layer,point,null,soundData,AcornsProperties.SOUND); }
   }

   /** Method to process <spell> tags
    *
    * @param tag The <spell> element in the DOM
    * @throws org.xml.sax.SAXException
    */
   private void processSpell(Element tag) 
           throws SAXException, IOException, UnsupportedAudioFileException
   {
		if (!sound) throw new SAXException
                 (LanguageText.getMessage("acornsApplication",7));

      soundData[SoundData.LANGUAGE] = tag.getAttribute("language");
      soundData[SoundData.NATIVE] = tag.getTextContent();

      if (!(soundExists || descExists))
      { lesson.importData(layer,point,null,soundData,AcornsProperties.SOUND); }
   }

   private void processDescription(Element tag)
           throws SAXException, IOException, UnsupportedAudioFileException
   {
      if (!sound) throw new SAXException
                             (LanguageText.getMessage("acornsApplication",152));

      soundData[SoundData.DESC] = tag.getTextContent();

      if (!soundExists)
      { lesson.importData(layer,point,null,soundData,AcornsProperties.SOUND); }
   }

   /** Method to process <sound> tags
    *
    * @param tag The <sound> element in the DOM
    * @throws org.xml.sax.SAXException
    * @throws java.net.MalformedURLException
    * @throws java.io.IOException
    * @throws javax.sound.sampled.UnsupportedAudioFileException
    */
   public void processSound(Element tag)
           throws SAXException, MalformedURLException
                , IOException, UnsupportedAudioFileException
   {
	  if (!sound)  
	  {
		  try
		  {
			  String value = tag.getAttribute("value");
			  point = new Point(Integer.parseInt(value), -1);
		  }
		  catch (Exception e)
		  {
	    		throw new SAXException
	    	                 (LanguageText.getMessage("acornsApplication",8));
		  }
	  }

      URL soundURL = null;
      if (tag.hasAttribute("src"))
      {   String fullName = directoryName + "/" + tag.getAttribute("src");
          soundURL = makeURL(fullName);
          soundData[SoundData.FRAMERATE] = tag.getAttribute("rate");
      }
      lesson.importData(layer, point, soundURL
                             , soundData, AcornsProperties.SOUND);
      return;
   }

   /** Method to process the <link> tag
    *
    * @param tag The <link> element in the DOM
    * @throws org.xml.sax.SAXException
    * @throws java.io.IOException
    * @throws javax.sound.sampled.UnsupportedAudioFileException
    */
   public void processLink(Element tag)
       throws SAXException, IOException, UnsupportedAudioFileException
   {
		    if (sound) 
         throw new SAXException(LanguageText.getMessage("acornsApplication",9));

      String[] linkData  = new String[]{tag.getTextContent()};
      lesson.importData(layer, point, null
                                      , linkData, AcornsProperties.LINK);
   }

   /** Process the <image> (within a point) or <picture> tags
    *
    * @param tag The <image> or <picture> tag element
    * @param tag
    * @throws org.xml.sax.SAXException
    * @throws java.io.IOException
    * @throws javax.sound.sampled.UnsupportedAudioFileException
    */
   private void processImage(Element tag, int number)
           throws SAXException, IOException, UnsupportedAudioFileException
   {
      String[] params = new String[3];
      params[0] = tag.getAttribute("value");
      params[1] = tag.getAttribute("scale");
      params[2] = tag.getAttribute("angle");

      if (tag.hasAttribute("value"))  number = Integer.parseInt(params[0]);
      else params[0]="0";

      URL url = null;
      if (tag.hasAttribute("src"))
      {   String fullName = directoryName + "/" + tag.getAttribute("src");
          url = makeURL(fullName);
      }

      int scaleFactor=100;
      if (tag.hasAttribute("scale"))  scaleFactor = Integer.parseInt(params[1]);
      else params[1] = "" + 100;
  	   if (scaleFactor>500 || scaleFactor<0)
        throw new SAXException(LanguageText.getMessage("acornsApplication",10));

      int angle=0;
      if (tag.hasAttribute("angle"))   angle = Integer.parseInt(params[2]);
      else params[2] = "" + 0;

      if (angle>=360 || angle<0)
        throw new SAXException(LanguageText.getMessage("acornsApplication",11));

       // Decide if to import normally or though the lesson's import method
       if (url != null)
       {  if (number<0 || number>=1000)
          {
              lesson.importData(layer,point,url,params,AcornsProperties.PICTURE);
          }
          else
          {
              lesson.insertPicture(url, scaleFactor, angle);
          }
       }
   }  // End of processImage().

   /** Method to process <font> tags
    *
    * @param tag The <font> element in the DOM
    * @throws org.xml.sax.SAXException
    * @throws java.io.IOException
    * @throws javax.sound.sampled.UnsupportedAudioFileException
    */
   private void processFont(Element tag)
           throws SAXException, IOException, UnsupportedAudioFileException
   {
      String[] values = {"","",""};
      values[0] = tag.getAttribute("background");
      values[1] = tag.getAttribute("foreground");
      values[2] = tag.getAttribute("size");
      lesson.importData(layer, null, null, values, AcornsProperties.FONT);
   }
  
   /** Method to create a URL from a string (either local, rmote, or jar)
    *
    * @param file The name of the file
    * @return The URL object
    * @throws java.net.MalformedURLException
    */
    private URL makeURL(String file) throws MalformedURLException
    {  URL url = null;
       if (loader!=null)
       {
           file = file.substring(file.lastIndexOf("/")+1);
           return loader.getResource(file);
       }
       else  {  url = getClass().getResource
                      (file.substring(directoryName.length()));
             }

       if (url==null)
       {   try { url = new URL(file); }
           catch (Exception e) { url =  new File(file).toURI().toURL(); }
       }
       return url;
    }   // End of makeURL()

}   // End of Import class
