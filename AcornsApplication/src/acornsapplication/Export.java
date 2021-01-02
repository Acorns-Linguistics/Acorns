/**
 * Export.java
 * @author HarveyD
 * @version 4.10 Beta
 *
 * Copyright 2007-2015, all rights reserved
 */

package acornsapplication;

import java.io.*;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

public class Export 
{
    private Document document;
    private Element  rootNode;
    private FileObject currentFile;

    /** Constructor to build the framework for the xml document
     *
     * @param currentFile The currently active file
     * @param directory The path to the export directory (or null for none)
     * @param extensions Extensions for multimedia files (null for none)
     */
    public Export(AppObject file, File directory, String[] extensions)
            throws ParserConfigurationException, IOException
    {
       this.currentFile = file;

       // Create XML document header
       DocumentBuilderFactory factory
                                = DocumentBuilderFactory.newInstance();
       DocumentBuilder builder  = factory.newDocumentBuilder();
       document = builder.newDocument();

       // Create the root node
       String[] acornsAttributes
               = { "description", "language", "author", "version" };

       String[] properties = currentFile.getProperties();
       String[] acornsValues = new String[acornsAttributes.length];
       acornsValues[0] = properties[FileObject.DESC];
       acornsValues[1] = properties[FileObject.LANG];
       acornsValues[2] = properties[FileObject.AUTHOR];
       acornsValues[3] = FileObject.getVersion();

       rootNode
           = makeNode(document, "acorns", acornsAttributes, acornsValues);
       document.appendChild(rootNode);
       
       // Format lesson skeleton.
       Lesson lesson;
       Element node;

       int lessonNum = 0;
       String[] lessonData;
       String[] lessonAttributes
                = {"number", "type", "title", "name", "description"};
       String[] lessonValues = new String[lessonAttributes.length+1];

       while ((lesson = currentFile.readLesson(lessonNum)) != null)
       {
            lessonData = currentFile.getLessonHeader(lessonNum++);
            lessonValues[0] = "" + (lessonNum);
            System.arraycopy(lessonData, 0, lessonValues, 1, lessonAttributes.length);

            node = makeNode
                    (document, "lesson", lessonAttributes, lessonValues);
            rootNode.appendChild(node);

            String link = lesson.getLink();
            if (link.length()>0)
            {
                String[] paramAttributes = {"link"};
                String[] paramValues = { link };
                Element paramNode = makeNode
                    (document, "param", paramAttributes, paramValues);
                node.appendChild(paramNode);
            }

            if (!lesson.print(document, node, directory, extensions))
                throw new IOException
                        (LanguageText.getMessage("acornsApplication", 73));

            NodeList list = node.getElementsByTagName("layer");
            int count = list.getLength();

            Element layerNode;
            String layerValue, attribute;
            int index;
            for (int i=0; i<count; i++)
            {   
            	try
                {  layerNode = (Element)list.item(i);
                   attribute = layerNode.getAttribute("value");
                   index = Integer.parseInt(attribute);
                   if (index<=0) continue;
                   layerValue
                         = lessonData[AcornsProperties.LAYERNAMES+index-1];
                   if (layerValue.length()>0)
                   layerNode.setAttribute("name", layerValue);
                }
                catch (Exception e) {}
            }
        }   // End of processing all of the lessons
    }       // End of constructor

    /** Method to get the dom object for a file
     *
     * @return The dom object
     */
    public Document getDocument() { return document; }

    /** Method to copy the XML output to a file */
    public void outputFile(File file)
            throws TransformerConfigurationException, TransformerException
    {
      // Use a Transformer for output
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(file);
      transformer.transform(source, result);
    }    // End of outputFile.
    
    /** Method to copy the XML output to a string */
    public String outputString()
    	throws TransformerConfigurationException
    	                      , TransformerException, IOException
    {
        // Use a Transformer for output
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);
        
        transformer.transform(source, result);
        writer.close();
    	return writer.toString();
    }

    /** Method to create a node in the document
     *
     * @param doc  The XML document object
     * @param node The name of the node to create
     * @param attributes An array of attribute names
     * @param properties An array of attribute values
     * @return The element that was created
     */
    private Element makeNode(Document doc, String node
                           , String[] attributes, String[] properties)
    {
        Element element = doc.createElement(node);
        for (int i=0; i<attributes.length; i++)
        {  if (!attributes[i].equals("") && !properties[i].equals(""))
           {
               element.setAttribute(attributes[i], properties[i]);
           }
        }
        return element;
    }   // End of makeNode() method
}       // End of Export class
