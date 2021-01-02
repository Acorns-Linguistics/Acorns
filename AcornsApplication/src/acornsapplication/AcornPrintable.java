/** AcornPrintable.java
 *
 *   @author  harveyD
 *   @version 3.00 Beta
 *   
 *   Copyright 2007-2015, all rights reserved
 */
// Written Dan Harvey, 7/8/2005
package acornsapplication;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.text.*;
import org.w3c.dom.*;
import java.awt.image.*;
import java.awt.print.*;

import org.acorns.*;
import org.acorns.lesson.*;
import org.acorns.language.*;

/**
 * Class used by print and print preview commands to print or display output
 * lesson data
 */
public class AcornPrintable extends Component implements Printable
{
   private static final long serialVersionUID=1L;

   private final static String FONTFAMILY = "Courier"; //Monospaced";
   private final static int    FONTSIZE = 10;
   private final static int    FONTSTYLE = Font.PLAIN;
   private static Hashtable<String, String> translation;

   private DomNodeData dom;         // The data to be output.
   private Font        font;        // The default font to use.
   private String      reportName;  // File name.
   private Lesson      lesson;      // The current lesson object.
   private int         lastPage;    // The last page processed.
      
   /** Constructor for print and print preview output
    *
    * @param data The dom object holding data to output
    * @param title The title for this report
    */
   public AcornPrintable(Document data, String title)
   {  reportName = title;
      font = new Font(FONTFAMILY, FONTSTYLE, FONTSIZE);
      dom  = new DomNodeData(data);
      lastPage = -1;

      if (translation!=null) return;

      translation = new Hashtable<String,String>();

      String[] text =
         {"acorns", "version", "language", "author", "description",
          "lesson", "number", "type", "title","name",
          "layer", "value", "language", "align",
          "image", "picture", "scale", "angle", "src",
          "font", "name", "foreground", "background", "size",
          "param", "category", "point", "type",
          "link", "gloss", "spell", "description", "sound"  };

      String[] translate = LanguageText.getMessageList("acornsApplication", 49);
      for (int i=0; i<text.length; i++)
      {
          if (i>=translate.length) break;
          translation.put(text[i], translate[i]);
      }
   }
	
   /* Print is called multiple times by java. So we need to
    *    draw the in between objects and then repaint the last page */
   public int print(Graphics page, PageFormat pageFormat, int pageNumber)
   {
       Graphics newPage = page.create();
       if (pageNumber==lastPage)
       {
          dom.restoreIndex();
          return processPage(page, pageFormat, pageNumber);
       }

       for (int i=lastPage + 1; i<pageNumber; i++)
       {  processPage(newPage, pageFormat, i); }
       dom.saveIndex();
       lastPage = pageNumber;
       return processPage(page, pageFormat, pageNumber);
   }

   /* Print a page of output
    *
    * @param page Page to draw the output to
    * @param pageFormat page parameters such as page width, height, etc. 
    * @param pageNumber page number to output
    *
    * @return PAGE_EXISTS or NO_SUCH_PAGE
    */
	  public int processPage
           (Graphics page, PageFormat pageFormat, int pageNumber)
   {
      if (dom.peekAtElement() == null)  return NO_SUCH_PAGE;

      OutputData output =
              new OutputData(page, pageFormat, reportName, pageNumber);
      try
      {   String[] element;
          while ((element = dom.getNextElement()) != null)
          {   processTag(page, output, element, true);

              // See if next element will fit.
              element = dom.peekAtElement();
              if (!processTag(page, output, element, false))
                  return PAGE_EXISTS;
          }
          return PAGE_EXISTS;
      }
      catch (IOException io) { return NO_SUCH_PAGE; }
   }  // End of print.

   /** Reset object so can regenerate pages for printing */
   public void resetDom()  
   { dom.reset();
     lastPage = -1;
   }

   /** Method to process a tag by counting required lines or
    *         writing to the output page
    * 
    * @param the graphics object for the output
    * @param The object controlling the output
    * @param element the element to process
    * @param write true if to write, false if to count lines
    * @return true if fits on page, false otherwise
    */
   private boolean processTag(Graphics page, OutputData output
                             , String[] element, boolean write)
                                 throws IOException
   {
      if (element == null) return false;

      String tag = element[0];
      String[] format;
      String data;
      int index;
      int lineCount = 0;

      if (tag.equals("acorns"))
      {   format = new String[]
                { "version", "language", "author"
                , "~\n", "description", "~\n" };
          data = formatOutput(element, format);
          lineCount = output.processString(data, write);
      }   // End if acorns tag

      if (tag.equals("lesson"))
      {  index = findAttribute(element, "number");
         int number = 1;
         try
         {
            if (index>=0)
                number = Integer.parseInt(element[index+1]);
         }
         catch (NumberFormatException e) {}

         FileObject active = Files.getActiveFile();
         lesson = active.readLesson(number - 1);
         lineCount = output.processString("\n", write);

         format = new String[]
                 {"number", "type", "~\n", "title"
                          , "name", "~\n", "description", "~\n"};
         data = formatOutput(element, format);
         lineCount += output.processString(data, write);
      }   // End if lesson tag

      if (tag.equals("layer"))
      {    lineCount = output.processString("\n", write);
           format = new String[]
                {"value", "name", "language", "align", "excluded", "~\n"};
           data = formatOutput(element, format);
           lineCount += output.processString(data, write);
      }   // End if layer tag

      if (tag.equals("image") || tag.contains("picture"))
      {  lineCount = output.processString("\n", write);
         format = new String[]{ "value", "scale", "angle"
                 , "~\n", "src", "~\n"};
         data = formatOutput(element, format);
         lineCount += output.processString(data, write);

         int number = 0;
         index = findAttribute(element, "value");
         try
         {  if (index>=0)
                number = Integer.parseInt(element[index+1]);
         }
         catch (NumberFormatException e) {}

         BufferedImage image = lesson.getPictureData(number).getImage
                           (null, new Rectangle(0,0,-1,-1));
         lineCount += output.processImage(this, image, write);
      }   // End if image tag

      if (tag.equals("font"))
      {   lineCount = output.processString("\n", write);
          format = new String[]
               {"name", "foreground", "background", "size", "align", "~\n"};
          data = formatOutput(element, format);
          lineCount += output.processString(data, write);
     }

      if (tag.equals("param"))
      {   format = new String[element.length/2];
          for (int i=2; i<element.length; i+=2)
          {  format[i/2 - 1] = element[i]; }
          format[element.length/2 - 1] = "~\n";
          data = formatOutput(element, format);
          lineCount = output.processString(data, write);
      }

      if (tag.equals("category"))
      {  lineCount += output.processString("\n", write);
         format = new String[]  {"value", "name"};
         data = formatOutput(element, format);
         lineCount += output.processString(data, write);
      }
      
      if (tag.equals("sound"))
      {
    	  lineCount += output.processString("\n", write);
    	  format = new String[] {"src", "rate", "value" };
          data = formatOutput(element, format);
          lineCount += output.processString(data, write);
      }

      if (tag.equals("point"))
      {  lineCount = output.processString("\n", write);
         format = new String[] {"~[ ", "x", "y", "~]  ", "type", "~\n"};
         data = formatOutput(element, format);
         lineCount += output.processString(data, write);

         Font languageFont;
         boolean more = true;
         do
         {   element = dom.peekAtElement();
             if (element==null) break;

             languageFont = null;
             if (element[0].equals("link"))
             {  format = new String[] {};
                data = formatOutput(element, format);
                lineCount += output.processString(data, write);
             }
             else if (element[0].equals("gloss"))
             {  format = new String[] {};
                data = formatOutput(element, format);
                lineCount += output.processString(data, write);
             }
             else if (element[0].equals("description"))
             {  format = new String[] {};
                data = formatOutput(element, format);
                lineCount += output.processString(data, write);
                lineCount += output.processString("\n", write);
              }
             else if (element[0].equals("spell"))
             {  format = new String[] {"language"};
                data = formatOutput(element, format);
                lineCount += output.processString(data, write);

                index = findAttribute(element, "language");

                if (index>=0)
                {  String language = element[index+1];
                   languageFont
                       = KeyboardFonts.getLanguageFonts().getFont(language);

                   if (languageFont!=null) page.setFont(languageFont);
                }
             }
             else if (element[0].equals("sound"))
             {  format = new String[] {"src", "rate"};
                data = formatOutput(element, format);
                lineCount += output.processString(data, write);
             }
             else more = false;

             // Handle text content with point sub-element
             if (more)
             {  if (element[1].length()>0)
                {  lineCount += output.processString(element[1], write);
                }
                if (languageFont!=null) page.setFont(font);
                lineCount += output.processString("\n", write);
                if (write) { dom.savePeekIndex(); }
             }
             else dom.restorePeekIndex();
         } while (more);
      }    // End of point tag

      if (write) { return true; }
      if (!output.isRoom(lineCount))
      {   dom.restorePeekIndex();
          return false;
      }
      return true;
   }  // End of processTag()

   /** Method to format a tag for output
    *
    * @param element The data for this element
    * @param format The formatted string array
    * @return The formatted output
    */
   private String formatOutput(String[] element, String[] format)
   {
       StringBuilder buffer = new StringBuilder(element[0] + " ");
       int index;

       for (int i=0; i<format.length; i++)
       {   if (format[i].startsWith("~"))
           {   buffer.append(getTranslation(format[i].substring(1)));
               continue;
           }

           index = findAttribute(element, format[i]);
           
           if (index>=0)
           {
               buffer.append(getTranslation(element[index]));
               buffer.append(" ");
               buffer.append(element[index+1]);
               buffer.append(" ");
           }
       }
       return buffer.toString();
   }

   /** Get the translation of a particular tag or attribute
    *
    * @param data The tag or attribute
    * @return
    */
   public String getTranslation(String data)
   {
       String value = translation.get(data);
       if (value==null) value = data;
       return value;
   }

   /** Method to find an attribute in a tag
    *
    * @param element The element data to search
    * @param format The name of the attribute to find
    * @return index to element or -1 if it doesn't exist
    */
   private int findAttribute(String[] element, String format)
   {  for (int i=2; i<element.length; i+=2)
        if (element[i].equals(format))
        {   if (element[i].equals("")) return -1;
            return i;
        }
      return -1;
   }


   /** Nested class to handle output to the printable page */
   class OutputData
   {   private final static int    POINTS_PER_INCH = 72;
       private final static int    HEADERSIZE = 3;
       private final static int    TOPMARGIN  = POINTS_PER_INCH/4;
       private final static int    LEFTMARGIN = 0;

       /** Parameters for writing to the ouput page */
       private Graphics2D  graphics;
       private int         lineSpacing, linesPerPage, line, offset;
       private double      pageWidth, pageHeight;

       /** Constructor to establish page parameters for output
        *
        * @param page The graphics object onto which to draw
        * @param pageFormat The parameters for margins, etc.
        * @param title The title for the heading of the report
        * @param pageNumber the report page number
        */
       public OutputData(Graphics page, PageFormat pageFormat
                                      , String title, int pageNumber)
       {            // Get imagable bounds.
          pageWidth  = pageFormat.getImageableWidth();
          pageHeight = pageFormat.getImageableHeight();

          graphics = (Graphics2D)page;
          double x      = pageFormat.getImageableX();
          double y      = pageFormat.getImageableY();
          graphics.translate(x, y);
          graphics.setPaint(Color.black);

          // Set the font to be used.
          Font font = new Font(FONTFAMILY, FONTSTYLE, FONTSIZE);
          graphics.setFont(font);

          FontMetrics metrics = graphics.getFontMetrics();
          lineSpacing = metrics.getMaxDescent()
               + metrics.getLeading() + metrics.getMaxAscent();
          linesPerPage
                 = (int)Math.floor((pageHeight-HEADERSIZE*lineSpacing)/lineSpacing);
          line   = 0;
          offset = 0;

          printHeader(title, pageNumber);
       }

       /** Method to process or draw strings on output page
        *
        * @param data The string to draw
        * @param write Whether to draw or just count lines
        * @return
        */
       public int processString(String data, boolean write)
       {   // Break the string into lines of output.
           int    newLine, lineCount = 0;
           String output = data, back;

           while ((newLine = output.indexOf('\n'))>=0)
           {
               if (output.length()>newLine)
                    back = output.substring(newLine+1);
               else back = "";

               output = output.substring(0, newLine);
               lineCount += processSubstring(output, write);
               lineCount++;
               if (write) line++;
               offset = 0;
               output = back;
           }
           if (output.length()>0)
               lineCount += processSubstring(output, write);

           return lineCount;
       }   // End of processString

       //------------------------------------------------------------
       // Method to draw the image if there is enough room.
       //------------------------------------------------------------
       public int processImage
               (Component comp, BufferedImage image, boolean write)
       {
          if (lesson==null) return 0;

          double scaleFactor;

          int imageWidth = image.getWidth();
          int imageHeight = image.getHeight();
          int maxWidth    = (int)((pageWidth - 2 * LEFTMARGIN)/2);
          int maxHeight   
                = (int)((pageHeight - 2*TOPMARGIN )/2 - HEADERSIZE*lineSpacing);
          if (imageWidth > maxWidth)
          {   scaleFactor = 100*maxWidth/imageWidth;
              imageHeight = (int)(imageHeight * scaleFactor / 100);
              imageWidth  = (int)(imageWidth  * scaleFactor / 100);
          }

          if (imageHeight > maxHeight)
          {   scaleFactor = 100*maxHeight/imageHeight;
              imageHeight = (int)(imageHeight * scaleFactor / 100);
              imageWidth  = (int)(imageWidth  * scaleFactor / 100);
          }

          double percentageHeight = imageHeight / pageHeight;
          int neededLines = (int)(percentageHeight * linesPerPage) + 3;
          if (!write) return neededLines;

          int top  = TOPMARGIN + (line+HEADERSIZE) * lineSpacing;
          int left = (int)((pageWidth - imageWidth)/2);

          FontMetrics metrics = graphics.getFontMetrics();
          graphics.drawImage(image, left, top - metrics.getAscent()/2
                                  , imageWidth, imageHeight, comp);
          if (write) line += neededLines;

          return neededLines;
       }

       /** Method to determine if the next output can fit
        *
        * @param lines The number of lines required
        * @return true if fits, false otherwise
        */
       public boolean isRoom(int lines)
       {  if (line + lines >= linesPerPage) return false;
          return true;
       }

       /** Method to draw or process a string without newlines
        *
        * @param data The string to process
        * @param write true if to write, false otherwise
        * @return the number of required lines
        */
       private int processSubstring(String data, boolean write)
       {
           if (data.length()==0) return 0;

           double lineWidth;
           String output = data, front;
           int top, left, stringWidth, len, count = 0;
           FontMetrics metrics = graphics.getFontMetrics();

           do
           {   // Find part of string that can fit.
               lineWidth = pageWidth - 2*LEFTMARGIN - offset;
               if (lineWidth<=0)
               {  count++;
                  offset = 0;
                  lineWidth = pageWidth - 2*LEFTMARGIN;
               }

               len = output.length();
               stringWidth = metrics.stringWidth(output);
               while (stringWidth>lineWidth)
               {   stringWidth
                        = metrics.stringWidth(output.substring(0,len));
                   if (stringWidth>lineWidth) len--;
               }
               front = output.substring(0,len);
               stringWidth = metrics.stringWidth(front);

               // Output the portion that can fit; or count lines
               left = LEFTMARGIN + offset;
               top  = TOPMARGIN + (line+count+HEADERSIZE)*lineSpacing
                                - metrics.getAscent();
               if (write)
                   graphics.drawString(front, left, top);

               // Get the remaining part of the output
               output = output.substring(len);
               if (output.length()>0)
               {   count++;
                   offset = 0;
               }
               else offset += stringWidth;

           } while (output.length()>0);

           if (write) line += count;

           return count;
       }

       /** Method to print the header on the page
        *
        * @param title The title for this report
        * @param pageNumber The page number (counting from zero)
        */
       private void printHeader(String title, int pageNumber)
       {
          Date today         = new Date();
          DateFormat df      = DateFormat.getInstance();
          String dateAndPage
               = df.format(today) + " " + 
                 LanguageText.getMessage("acornsApplication", 48) + "    " +
                 (pageNumber+1) + " ";

          int top  = TOPMARGIN;
          int left = LEFTMARGIN;

          FontMetrics metrics = graphics.getFontMetrics();
          int stringWidth = metrics.stringWidth(dateAndPage);

          graphics.drawString
                  (dateAndPage, (int)(pageWidth-stringWidth), top);
          graphics.drawString(title, left, top + lineSpacing);
       }  // End of printHeader()
   }      // End of OutputData class

   /** Nested class to help process the DOM object */
   class DomNodeData
   {
       ArrayList<String[]> elementList;  // The list of elements
       int index;                       // Current element index
       int peekIndex;                   // Index to peek at
       int saveIndex;                   // Index of last page processed

       /** Constructor method
        *
        * @param document The DOM document to be processed
        */
       public DomNodeData(Document document)
       {
           elementList = getPreorderList(document);
           index = peekIndex = saveIndex = 0;
       }

       /** Method to start accumulating a preorder list of elements
        *
        * @param document The DOM object
        * @return An ArrayList of elements
        */
       private ArrayList<String[]> getPreorderList(Document document)
       {
           ArrayList<String[]> list = new ArrayList<String[]>();
           elementList = getPreorderList
                           (list, document.getDocumentElement());
           return list;
       }

       /** Recursive method to gather a preorder list of elements
        *
        * @param list The ArrayList to which to add elements
        * @param element The current element
        * @return the updated ArrayList of elements
        */
       private ArrayList<String[]> getPreorderList
               (ArrayList<String[]> list, Element element)
       {
           if (element==null) return list;

           list.add(getNodeInfo(element));

           Element current;
           NodeList children = element.getChildNodes();
           int numChildren = children.getLength();
           for (int i=0; i<numChildren; i++)
           {
               if (children.item(i) instanceof Element)
               {   current = (Element)children.item(i);
                   getPreorderList(list, current);
               }
           }
           return list;
      }    // End of getPreorderList()

       /** Method to get an element from the list
        *
        * @return String array defined by the followint offsets
        *         [0] = tag name
        *         [1] = text content of tag
        *         [2*i+2],[2*i+3] = attribute name and value pairs
        */
       public String[] getNextElement()
       {
           if (index>=elementList.size()) return null;

           peekIndex = index + 1;
           return elementList.get(index++);
       }
       
       /** Method to peek at next element
        * 
        * @return Next element or null
        */
       public String[] peekAtElement()
       {
           if (peekIndex>=elementList.size()) return null;
           return elementList.get(peekIndex++);
       }
       
       /** Method to update the index to the point past the peeks. */
       public void savePeekIndex() { index = peekIndex; }

       /** Method to restore the peek index to the last tag */
       public void restorePeekIndex() { peekIndex = index; }

       /** Reset the object so we can reprocess for printing */
       public void reset() { index = peekIndex = 0; }

       public void saveIndex()  { saveIndex = index; }

       public void restoreIndex() { index = peekIndex = saveIndex; }

       /** Method to get the information for this node
        *
        * @param name The name of the element
        * @return String array with information for this node
        *                or null if no element to process
        *   [0] = tag name, [1] = text content
        *   [2*i+2], [2*i+3] = attribute name and value
        */
       private String[] getNodeInfo(Element element)
       {  if (element == null) return null;

          Node node;

          NamedNodeMap atts = element.getAttributes();
          int size = atts.getLength();
          String[] data = new String[2 + size*2];

          data[0] = element.getTagName();
          data[1] = "";

          Object child = element.getFirstChild();
          if (child!=null && !(child instanceof Element))
          {  data[1] = element.getTextContent();  }

          for (int i=0; i<size; i++)
          {
             node = atts.item(i);
             data[2*i+2] = node.getNodeName();
             data[2*i+3] = node.getNodeValue();
          }
          return data;
       }  // End of getNodeInfo()
   }      // End of DomNodeData class
}         // End of AcornPrintable