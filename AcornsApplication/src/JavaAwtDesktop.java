


import java.awt.Desktop;
import java.io.File;
import java.util.List;

import org.acorns.audio.SoundDefaults;
import org.acorns.language.LanguageText;

import acornsapplication.AcornMenu;
import acornsapplication.AppEnv;
import acornsapplication.FileOpen;
import acornsapplication.HelpAbout;
import acornsapplication.ToolsOptions;

public class JavaAwtDesktop {
 
    public JavaAwtDesktop() 
    {
        Desktop desktop = Desktop.getDesktop();

        desktop.setAboutHandler(e -> {
    		HelpAbout.aboutMessage();
        });
        
        desktop.setPreferencesHandler(e -> {
	           if (!SoundDefaults.isSandboxed())
	           {
	           		ToolsOptions.preferences();
	        		return;
	           }
	        	
	  	      if (SoundDefaults.resetBookmarkFolder())
		      {
		  	      String data = SoundDefaults.getDataFolder();
		  	      AppEnv.resetPaths(data);
		  	      
		  	      AppEnv.setText(LanguageText.getMessage("commonHelpSet", 100));
		      }
        });
        
        desktop.setQuitHandler((e,r) -> {
        	AppEnv.shutDown();
        	System.exit(0);
        });
        
        desktop.setOpenFileHandler(e -> {
            // Method to open an Acorns file (Called from Mac OS application listener).
        	String path = "???";
        	String step = "";
        	try
            {  
            	List<File> files = e.getFiles();
            	for (File file: files)
            	{
            	   step  = "getting path";
            	   path = file.getCanonicalPath();
            	   step = "opening file " + file;
                   String status =  FileOpen.openFile(file); 
                   step = "setting status";
                   AppEnv.setText(status);         // Status of the open
                   step = "enabling";
                   AcornMenu.setEnable();          // Activate the icons 
            	}
            }
            catch (Exception exception) 
            { AppEnv.setText("Main: " + step + " " + path + ":" + exception.toString());  }
 
        });
    }
}

