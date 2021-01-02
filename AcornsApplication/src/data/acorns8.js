/* JavaScript to process ACORNS lesson types in a browser window, using parsed XML data */
		
var acorns = null;
var timer = null;
var resizable = false;
var userInteraction = false;

var GAP=0, SCALE = 1, DIFFICULTY = 2, FONT = 3, OPTIONS = 4, RESET = 5, TOGGLE = 6, CONTINUOUS = 7, SELECT = 8, CATEGORY = 9;
var INCORRECT =0 , CLOSE = 1, CORRECT = 2, HELP = 3, SHOW = 4;

// Reload the window when it is resized
var reloadWindow = function()
{
	// Don't allow reloading while components are being drawn
	if (!resizable) return;
	if (acorns.isDrawing()) return;

	// Stop multiple reloading calls and any lesson using the global timer.
	clearTimeout(timer); 
	acorns.stopAudio();

	// Restart the timer.
	timer = setTimeout(parseXML,500);
	if (window.audioTools)
		window.audioTools.outLog("resizing");

}

// Process error messages that could occur when the lesson fails due to an exception
var errorMessage = function(name, line1, line2)
{
	var h1 = document.createElement("h1");
	h1.style.textAlign = "center";
	h1.style.fontSize = "24px";
	var text = "Sorry: Application " + name + " is not compatible with this browser<br>Try Chrome, Firefox, Safari, or Edge. /></h1>";
	h1.innerHTML = text;
	document.body.appendChild(h1);
	
	if (line1)
	{
		var h3 = document.createElement("h3");
		h3.style.fontSize = "20px";
		text = line1;
		if (line2) text += "<br /><br />" + line2;
		h3.innerHTML = text;
		document.body.appendChild(h3);
	}
}

this.parse = function()
{
	setTimeout(parseXML, 500);
	if (window.audioTools)
		window.audioTools.outLog("parsing");
}
	

// Parse the lesson XML and start it playing
function parseXML()
{
   var name = "";
   try
   {
		clearTimeout(timer); 
		resizable = false;
		acorns = null;
		
		var parser = new ParseACORNSFile();
		name = location.href.substring(location.href.lastIndexOf("/") + 1, location.href.lastIndexOf(".htm"));
		acorns = parser.parseXMLString(name, xml);
		if (acorns)
		{
			parser.parseJSON(acorns, fonts, feedback);
			acorns.start(recordEnabled);
			resizable = true; 
		}
		else errorMessage(name);
		
		parser = null;  // Prevent memory leak of XML DOM data.
		if (window.onorientationchange)
			 window.onorientationchange = reloadWindow;
		else window.onresize = reloadWindow;
	}
	catch (e) 
	{
		errorMessage(name, e.message, e.stack);
	}
}

/***** The following section pertains common code for lesson categories *****/
function CategoryMultiplePictures(data)
{
    var lessonData = data;

	var pictureList = undefined; 
	var audioList = [];

	var freePictures = undefined;
	var freeAudio = undefined;

	/* Return the list of recorded audio data for the indicated layer */
	this.getAudioList = function(layer)
	{
		var layerList, MIN_PICTURES = 4;
		
		if (pictureList == undefined) pictureList = lessonData.objectList;
	    if (pictureList.length < MIN_PICTURES) return false;
		
		var audioList = [];
		for (var p=0; p<pictureList.length; p++)
		{
			layerList = pictureList[p].objectList;
			if (layerList==undefined) return false;
			if (layerList.length==0) return false;
			if (layerList[layer - 1] == undefined) return false;
			
			audioList.push(layerList[layer - 1].objectList);
		}
		return audioList;
	}

	this.isPlayable = function(layer) 
    {
		var audioList = this.getAudioList(layer);
		if (!audioList) return false;

		var audioObject;
		for (var p=0; p<audioList.length; p++)
		{
			audioObject = audioList[p];
			for (var a=0; a<audioObject.length; a++)
			{
				if (!audioObject[a].isComplete()) return false;
			}
		}
		return true;	   
    };
	
	this.getPictureList = function() 
	{ 
		if (pictureList == undefined) pictureList = lessonData.objectList;
		return pictureList; 
	}
	
	/* Reset free list for another round of selection */
	this.resetLesson = function()
	{
		audioList = this.getAudioList(lessonData.getLayer());

		freePictures = [];
		freeAudio = [];
				
		var pictureList = this.getPictureList();
		for (var p=0; p<pictureList.length; p++)
		{
			freePictures[p] = p;
			freeAudio[p] = [];
			for (var a=0; a<audioList[p].length; a++)
			{
				freeAudio[p][a] = a;
			}
		}
	}
	
	/* Select the audio to use for a particular user interaction */
	this.selectAudio = function(selectedPicture)
	{
		if (!freePictures || freePictures.length == 0) { this.resetLesson(); }

		var freeIndex = Math.floor(Math.random() * freeAudio[selectedPicture].length);
		var audioIndex = freeAudio[selectedPicture][freeIndex];
		
		freeAudio[selectedPicture][freeIndex] = freeAudio[selectedPicture][freeAudio[selectedPicture].length - 1];
		freeAudio[selectedPicture].pop();
		return 	audioList[selectedPicture][audioIndex];
	}
	
	// Reload the audio list for the selected picture and optionally remove picture from back of free list
	this.updateAudioList = function(selectedPicture, randomPictures)
	{
		if (freeAudio[selectedPicture].length == 0)
		{
			for (var a=0; a<audioList[selectedPicture].length; a++)
			{
				freeAudio[selectedPicture][a] = a;
			}
			if (randomPictures) freePictures.pop();
		}
	} 
	
	var selections = [];
	var freeIndices = [];

	/* Replace one of the pictures with one of the unused ones */
	this.replacePicture = function(pictureNo)
	{ 
		if (freeIndices.length==0) return;
		if (selections.length<pictureNo) return;
		
		var index = Math.floor(Math.random()*freeIndices.length);
		var temp = freeIndices[index];
		freeIndices[index] = selections[pictureNo];
		selections[pictureNo] = temp;
		return selections;
	}
	
	/* Select a picture to use with  a particular user interaction */
	this.selectPictures = function(numPictures)
	{
		selections = [];
		if (!freePictures || freePictures.length == 0) { this.resetLesson(); }
		
		// Randomly pick the picture, which will contain the audio
		var freeIndex = Math.floor(Math.random() * freePictures.length);
		var pictureIndex = freePictures[freeIndex];
		
		// Swap with last entry to set the freeIndex position
		// It will be the end of the frePicture list and first among the selection array
		freePictures[freeIndex] = freePictures[freePictures.length - 1];
		freePictures[freePictures.length - 1] = pictureIndex;
		selections.push(pictureIndex);	

		// Pick randomly from all of the other pictures to complete set of selections
		freeIndices = [];
		var pictureList = this.getPictureList();
		for (var i=0; i<pictureList.length; i++) {  freeIndices[i] = i; }
		freeIndex = pictureIndex;
		
		for (var i=1; i<numPictures; i++)
		{
			freeIndices[freeIndex] = freeIndices[freeIndices.length - 1];
			freeIndices.pop();
			
			freeIndex = Math.floor(Math.random() * freeIndices.length);
			selections.push(freeIndices[freeIndex]);
		}
		freeIndices[freeIndex] = freeIndices[freeIndices.length - 1];
		freeIndices.pop();
		return selections;
	}	// End of selectPictures()
	
	// Display picture data when a pcture is clicked
	this.displayMultiplePictureData = function(parent, options)
	{
		var buttonNo = parent.id.substring(7);
		var audio = buttonNo;
		if (buttonNo<selections.length) audio = selections[buttonNo];
		var point  = this.selectAudio(audio);
		this.updateAudioList(audio, false);
		
		acorns.widgets.makePointDisplay(parent, point, lessonData, undefined, undefined, undefined, options);
	}	// End of displayMultiplePictureData()

}	// End of CategoryMultiplePictures class

/***** The following section creates classes for each lesson type which is supported *****/

function MultipleChoice(acornsObject, data)
{
	var NUM_PICTURES = 4;
	
	var acorns = acornsObject;
	var lessonData = data;
   	var options = new Options(acorns, lessonData, [OPTIONS, GAP]);

	var category = new CategoryMultiplePictures(data);
	var correct = undefined;

    this.parse = function(parseObject, lessonDom)  
    { 
    	parseObject.parseMultiplePicturesCategory(acorns, lessonDom, lessonData); 
    };
   
    this.play = function(reset, changeOfLayer) 
    {
		if (reset) 
		{
			category.resetLesson();
		}
		
		var element;
		var selections = category.selectPictures(NUM_PICTURES);
		var audio  = category.selectAudio(selections[0]);
		category.updateAudioList(selections[0], true);
		
		var pictureList = category.getPictureList();
		var picture = pictureList[selections[0]];
				
		var correctButton = Math.floor(Math.random() * NUM_PICTURES);
		correct = { picture: picture, audio: audio, id: "button " + correctButton };

		for (var buttonNo=0; buttonNo<NUM_PICTURES; buttonNo++)
		{
			if (buttonNo == correctButton) 
			{
				picture = pictureList[selections[0]];
				
			}
			else if (buttonNo < correctButton)
			{
				picture = pictureList[selections[buttonNo + 1]];
			}
			else picture = pictureList[selections[buttonNo]];
			
			element = document.getElementById("button " + buttonNo);
			picture.centerAndScalePicture(element);
		}
		var point = correct.audio;
		var userOptions = options.getOptions();
		point.display(document.getElementById("scorepanel"), [userOptions.gloss, userOptions.spell]);
		if (userOptions.sound=="y")
		point.playAudio();
    };
	
	/* Handle interaction with the user when one of the pictures are clicked */
	var pictureHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";

		if (correct.id == element.id)
		{
			acorns.feedbackAudio("correct");
			acorns.widgets.updateScore(true);
			lessonData.play(false);
		}
		else
		{
			acorns.feedbackAudio("incorrect");
			acorns.widgets.updateScore(false);
			setTimeout(function() { correct.audio.playAudio(); }, 10);
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	};	  

    this.isPlayable = function(layer) 
    {
		return category.isPlayable(layer);
    };
   
    this.configurePlayPanel = function(panel) 
    {
		var GAP = 5;
		var BORDER = acorns.widgets.getButtonBorderSize();
		var MIN_BUTTON = 30;
		
	    var top = acorns.widgets.configureScorePanel(lessonData, panel);
		var width = panel.clientWidth, height = panel.clientHeight - top;
		
		var buttonWidth = (width - GAP - 2*BORDER)/2;
		var buttonHeight = (height - top - GAP - 2*BORDER)/2;
	    var buttonSize = Math.min( buttonWidth, Math.floor(buttonHeight) );
		if (buttonSize < MIN_BUTTON) buttonSize = MIN_BUTTON;
		
	    var block = document.createElement("div");
		block.style.position = "absolute";
		block.style.width  = (2*(buttonSize + BORDER) + GAP) + "px";
		block.style.height = (buttonSize + 2*BORDER) + "px";
		block.style.overflow = "hidden";
		block.style.top = top + (height - 2*(buttonSize + BORDER) - GAP)/2 + "px";
		block.style.left = "" + (width - 2*(buttonSize + BORDER) - GAP)/2 + "px";
		
	    var div;
	    panel.appendChild(block);

	    for (var buttonNo=0; buttonNo<NUM_PICTURES; buttonNo++)
	    {
			div = acorns.widgets.makePictureButton
			      (buttonNo, buttonSize, (buttonNo%2==1)?buttonSize + BORDER + GAP:0, pictureHandler);
			block.appendChild(div);
	        if (buttonNo % 2 == 1 && buttonNo<NUM_PICTURES -1) 
			{
				block = document.createElement("div");
				block.style.position = "absolute";
				block.style.width  = (2*(buttonSize + BORDER) + GAP) + "px"
				block.style.top = top + (height/2 + BORDER + GAP/2) + "px";
				block.style.left = "" + (width - 2*(buttonSize + BORDER) - GAP)/2 + "px";
				panel.appendChild(block);
			}
	    }
    };
	
	this.getHelpMessage = function()
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Multiple Choice</h3>");
		buffer.append("<p>After hearing an audio message, click on the correct picture. ");
		buffer.append("You will hear a confirmation sound if your selection is correct ");
		buffer.append("and then four more pictures will show and another audio message will play. ");
		buffer.append("</p>");
		buffer.append("<p>On the top of the window is a score panel that keeps track of the total number ");
		buffer.append("of audio messages and the number of correct answers. The score panel also shows the ");
		buffer.append("gloss and indigenous text that corresponds to each audio message. ");
		buffer.append("</p>");
		buffer.append("<p>You can click on the lesson options (icon with the 'i' in it) if you prefer that the gloss ");
		buffer.append("or indegenous text not display.");
		buffer.append("</p>"); 
		return buffer.toString();
	};
	
	/* Read cookie and set options */
	this.getOptions = function() { return options; }

}	// End of MultipleChoice lesson class

function Pictionary(acornsObject, data)
{
	var acorns = acornsObject;
    var lessonData = data;
	var options = new Options(acorns, lessonData, [] );

	var category = new CategoryMultiplePictures(data);
    
	this.parse = function(parseObject, lessonDom)  
	{ parseObject.parseMultiplePicturesCategory(acorns, lessonDom, lessonData); }
	
    this.play = function(reset) 
	{
		if (reset) 	{ category.resetLesson() }
	}  
	
    this.isPlayable = function(layer) {return  category.isPlayable(layer); }
	

	/* Handle interaction with the user when one of the pictures are clicked */
	var pictureHandler = function(event, category)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";
		
		category.displayMultiplePictureData(element);
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	};	  
	
	this.configurePlayPanel = function(panel) 
	{
		var GAP = 3, BORDER = acorns.widgets.getButtonBorderSize(), SCROLL = 34, BUTTON_SIZE = 90;
		
		if (acorns.system.isMobilePhone()) button_size = 45;
		else button_size = BUTTON_SIZE;
		
		var pictureList = category.getPictureList();
		var width = panel.clientWidth;
		var columns = Math.floor((width - SCROLL - GAP)/(2*BORDER + button_size + GAP));
		if (columns<2) columns = 2;
		
		var buttonSize = Math.floor((width - SCROLL - (columns-1)*GAP - 2*columns*BORDER)/columns);
		var rowWidth = columns*(buttonSize + 2*BORDER) + (columns-1)*GAP;
		if (columns > pictureList.length) 
			rowWidth = pictureList.length*(buttonSize + 2*BORDER) + (pictureList.length - 1)*GAP;
	
		var top = 0;
		var left = (width - rowWidth)/2;
		panel.style.overflow = "auto";
		acorns.system.scrollableDiv(panel);
	
	    var block = document.createElement("div");
		block.style.position = "absolute";
		block.style.width  = rowWidth + "px";
		block.style.height = (buttonSize + 2*BORDER) + "px";
		block.style.overflow = "hidden";
		block.style.top = top + "px";
		block.style.left = left + "px";
	
	    var div;
	    panel.appendChild(block);

		var table = document.createElement("table");
		table.style.marginLeft = table.style.marginRight = "auto"; 
		panel.appendChild(table);
		var tr = document.createElement("tr");
		table.appendChild(tr);
		for (var buttonNo=0; buttonNo<pictureList.length; buttonNo++)
	    {
			div = acorns.widgets.makePictureButton(buttonNo, buttonSize
			        , (buttonNo % columns) * (buttonSize + 2 * BORDER + GAP)
					, function(e) { pictureHandler(e, category) } );

			block.appendChild(div);
			pictureList[buttonNo].centerAndScalePicture(div);

	        if ( ((buttonNo + 1) % columns == 0)  && buttonNo<pictureList.length -1) 
			{
				block = document.createElement("div");
				block.style.position = "absolute";
				block.style.width  = rowWidth + "px"
				top += buttonSize + 2*BORDER + GAP;
				block.style.top = top + "px";
				block.style.left =  left + "px";
				panel.appendChild(block);
			}
	    }
	}
		
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Pictionary</h3>");
		buffer.append("<p>Pictionary lessons display a series of pictures shown on the frame.");
		buffer.append("If there are more than can fit, a scroll bar enables you to view those not shown.");
		buffer.append("When you click on one of the pictures, you will see the gloss (first language) translation,"); 
		buffer.append("indigenous spelling, and additional descriptive information attached to the picture.");
		buffer.append("An attached audio also plays, which you can replay, if you wish, by clicking the play button.");
		buffer.append("</p>");
		buffer.append("<p>There are no special lesson options for this lesson type. Clicking the icon ");
		buffer.append("with the 'i' in it will have no effect.");
		buffer.append("</p>"); 
		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }
}	// End of Pictionary lesson class
	
function MovingPictures(acornsObject, data)
{
	var acorns = acornsObject;
    var lessonData = data;
   	var options = new Options(acorns, lessonData, [OPTIONS, GAP]);

 	var category = new CategoryMultiplePictures(data);

	var NUM_PICTURES = 4, MIN_BUTTON_SIZE = 50, SCROLL = 20;
	
	this.parse = function(parseObject, lessonDom)  
	{ parseObject.parseMultiplePicturesCategory(acorns, lessonDom, lessonData); }
	
	var DELTA_TIME = 60;
	var DIRECTIONS = 16,  DISTANCE   = 1;
    var MIN_KILL   = 160, MAX_KILL   = 320;  // Every 20 - 30 seconds
    var MIN_CHANGE = 16,  MAX_CHANGE = 32;  // Average 2 to 4 seconds per picture

	var deltas = [ [ 0, 4], [ 1, 3], [ 2, 2], [ 3, 1]
				 , [ 4, 0], [ 3,-1], [ 2,-2], [ 1,-3]
				 , [ 0,-4], [-1,-3], [-2,-2], [-3,-1]
				 , [-4, 0], [-3, 1], [-2, 2], [-1, 3] ];
				 
	// current time, current direction, time to change each picture's direction, time to kill a picture, 
	var time = 0, direction = [], kill = 0;
	
	timer = null;  // setTimeout for moving pictures (global so it will reset on a resize)
	
	// Function to randomly pick another direction
	var newDirection = function()
	{
        var direction = Math.floor((Math.random() * DIRECTIONS));
		var change = time +	Math.floor(Math.random()*(MAX_CHANGE-MIN_CHANGE+1)+MIN_CHANGE);
		return { change: change, direction: direction };
	}
	
	// Function to set a new location and possibly when to change direction next
	var newLocation = function(element, buttonNo)
    {
		var left = parseInt(element.style.left,10);
		var top = parseInt(element.style.top,10);
		var height = parseInt(element.style.height,10);
		var width = parseInt(element.style.width,10);

	    var newX = left + deltas[direction[buttonNo].direction][0] * DISTANCE;
        var newY = top  + deltas[direction[buttonNo].direction][1] * DISTANCE;
		
		var main = document.getElementById("main");
		var panelSize = { width: main.clientWidth, height: main.clientHeight };
		if (newY < 0 || newX < 0 
				|| newY + height > panelSize.height - SCROLL 
				|| newX + width> panelSize.width - SCROLL)
		{   
			direction[buttonNo].direction = Math.floor((Math.random() * DIRECTIONS));  
		}
		else
		{
			element.style.left = newX + "px";
			element.style.top = newY + "px";

		}
    } 	// End of newLocation()
	
	var pictureThread = function()
	{
		var buttonNo;
		var div;
		if (time>=kill)
		{
			buttonNo = Math.floor(Math.random() * NUM_PICTURES);
			div = document.getElementById("button " + buttonNo );
			var selections = category.replacePicture(buttonNo);
			var picture = category.getPictureList()[selections[buttonNo]];
			
			picture.centerAndScalePicture(div);
			kill = time + Math.floor(Math.random()*(MAX_KILL-MIN_KILL+1)+MIN_KILL);
		}

		// Move the pictures and possibly change directions
		for (buttonNo=0; buttonNo < NUM_PICTURES; buttonNo++)
		{
			div = document.getElementById("button " + buttonNo );
			if (time>=direction[buttonNo].change)
			{
				direction[buttonNo] = newDirection();
			}
			newLocation(div, buttonNo);
		}
		time++;
		timer = setTimeout(pictureThread, DELTA_TIME);
	}
	
	// Start the pictures moving about the display and change them every 20 to 30 seconds
    this.play = function(reset) 
	{
		var buttonNo;
		if (reset)  
		{ 	time = 0; 
			direction = [];
			
			category.resetLesson()
			var selections = category.selectPictures(NUM_PICTURES);
			var div;
			for (buttonNo = 0; buttonNo < NUM_PICTURES; buttonNo++)
			{
				div = document.getElementById("button " + buttonNo);
				var pictureList = category.getPictureList();
				picture = pictureList[selections[buttonNo]];
				picture.centerAndScalePicture(div);

				direction.push(newDirection());
				kill = time + Math.floor(Math.random()*(MAX_KILL-MIN_KILL+1)+MIN_KILL);
			}
		}
		
		if (timer == null) timer = setTimeout(pictureThread, DELTA_TIME);
	}
	
    this.isPlayable = function(layer) {return  category.isPlayable(layer); }

	/* Handle interaction with the user when one of the pictures are clicked */
	var pictureHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";
		var userOptions = options.getOptions();
		category.displayMultiplePictureData(element, [userOptions.gloss, userOptions.spell, userOptions.sound, 'n', 'n']);
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	};	  
	
	this.configurePlayPanel = function(panel) 
	{
		var COLUMNS = 10;
		
		var div;
		var windowSize = acorns.system.getWindowSize();
		var buttonSize = Math.min(windowSize.width / COLUMNS - SCROLL, windowSize.height / COLUMNS);
		buttonSize = Math.max(buttonSize, MIN_BUTTON_SIZE);
		
		var maxX = windowSize.width - buttonSize;
		var buttonNo; 
		direction = [];

		for (buttonNo=0; buttonNo<4; buttonNo++)
		{
			div = acorns.widgets.makePictureButton(buttonNo, buttonSize
			        , Math.floor(Math.random() * maxX)
					, function(e) { pictureHandler(e, category) } );

			panel.appendChild(div);
		}
	};
   	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Moving Pictures</h3>");
		buffer.append("When you execute a Moving Pictures lesson, four pictures move about the frame. ");
		buffer.append("Every twenty or thirty seconds, one of the pictures change. ");
		buffer.append("At any time, you can click one of the pictures to hear an audio recording ");
		buffer.append("relating to that picture. If there is more than one recording attached to the picture, ");
		buffer.append("the program will randomly choose among them."); 
		buffer.append("<p>When you click, you will also see the gloss (first language) translation ");
		buffer.append("and the indigenous spelling in a dialog box that appears</p>");

		buffer.append("<p>You can click on the lesson options (icon with the 'i' in it) ");
		buffer.append("if you prefer that the gloss or indegenous text not display.");
		buffer.append("</p>"); 
		return buffer.toString();	
	};
	
	/* Read cookie and set options */
	this.getOptions = function() { return options; }
	
	this.stop = function() 
	{ 
		clearTimeout(timer);
		timer = null; 
	}

}	// End of MovingPictures lesson class


function FlashCards(acornsObject, data)
{
	var acorns = acornsObject;
    var lessonData = data;
   	var options = new Options(acorns, lessonData, [RESET, GAP, TOGGLE, GAP]);
	
 	var category = new CategoryMultiplePictures(data);
	
	var audioList = undefined;
	var pictureList = undefined;
	var piles = [ [], [], [] ];
	var display = [];
	
	this.lastPileClicked = undefined;
	
    this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseMultiplePicturesCategory(acorns, lessonDom, lessonData); 
	}
	
	var displayCard = function(pile)
	{
		var element = document.getElementById("button " + pile);
		
		var picture;
		if (piles[pile].length == 0)
			 picture = new Picture(acorns, "");
		else picture = pictureList[piles[pile][0].picture];
		picture.centerAndScalePicture(element);
	}
	
	var cardHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";
		
		var correct = [ 3, 5, 0 ];
		var incorrect = [ 0, -2, -1 ];
		var pile = this.lastPileClicked;
		
		if (pile == undefined || piles[pile].length == 0) acorns.beep();
		else
		{
			var cardNo = parseInt(element.name,10);
			var answer = display[cardNo];
			var correctAnswer = piles[pile][0];

			if (correctAnswer == answer) 
			{
				acorns.feedbackAudio("correct");
				answer.responses = Math.max(answer.responses + 1, 1);
				
				// Move to next pile if enough correct answers
				piles[pile].splice(0, 1);
				if (correct[pile] != 0 && correct[pile] <= answer.responses)
				{
					piles[pile + 1].push(answer);
					correctAnswer.responses = 0;
					displayCard(pile + 1);
				}
				else
				{		
					piles[pile].push(answer);
				}
			}
			else 
			{
				acorns.feedbackAudio("incorrect");		
				correctAnswer.responses = Math.min(correctAnswer.responses - 1, -1);
				
				// Move to previous pile if enough incorrect answers
				if (incorrect[pile]!=0 && incorrect[pile] >= correctAnswer.responses)
				{
					piles[pile].splice(0, 1);
					correctAnswer.responses = 0;
					piles[pile - 1].push(correctAnswer);
					displayCard(pile - 1);
				}
				else
				{
					var picture = piles[pile][0].picture;
					var audio = piles[pile][0].audio;
					var point = audioList[picture][audio];
					setTimeout( function() {point.playAudio(); }, 1000);
				}
			}
			displayCard(pile);
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}
	
	// Method to randomize the order of array elements
	var shuffle = function(array)
	{
		var dest = [], source = [];
		var i, rand, length = array.length;
		for (i=0; i<length; i++)
		{
			source.push(array[i]);
		}
		for (i = 0; i <length; i++)
		{
			rand = (Math.floor(Math.random()*source.length));
			dest.push( source[rand] );
			source[rand] = source[source.length - 1];
			source.pop();
		}
		return dest;
	}
	
 	var resetLesson = function()
	{
		category.resetLesson();
		
		pictureList = category.getPictureList();
		audioList = category.getAudioList(lessonData.getLayer());

		// Initialize the pile of cards.
		piles = [ [], [], [] ];
		var card, audio;
		for (card = 0; card < audioList.length; card++)
		{
			for (audio=0; audio < audioList[card].length; audio++)
				piles[0].push( {picture: card, audio: audio, responses: 0 } );
		}
		
		// Randomize the display order
		display = shuffle(piles[0]);

		// Reandomize the pile of phrases
		piles[0] = shuffle(piles[0]);
				
		// Empty the previous list of phrases
		var list = document.getElementById("phraselist");
		if ( list.hasChildNodes() )
		{
			while (list.childNodes.length > 0)
			{
				list.removeChild( list.firstChild );       
			} 
		}
		
		// Initialize the list of phrases to display.
		var item, span;
		for (card=0; card< piles[0].length; card++)
		{
			item = document.createElement("p");
			list.appendChild(item);

			lessonData.environment.setColors(item);
			item.style.borderStyle = "outset";
			item.style.marginTop = "0px";
			item.style.marginBottom = "-5px";
			acorns.system.addListener(item, "click", function(e) { cardHandler(e) });
			item.name = "" + card;
			item.setAttribute("name", "" + card);

			span = document.createElement("span");
			item.appendChild(span);
			audio = display[card].audio;
			picture = display[card].picture;
			point = audioList[picture][audio];
			userOptions = options.getOptions();
			if (userOptions.toggle == "y")
			{  	
				span.setAttribute("name", "gloss");
				point.display(item, ['y', 'n', 'n', 'n', 'n']);
			}
			else
			{
				span.setAttribute("name", "spell");
				point.display(item, ['n', 'y', 'n', 'n', 'n']);
			}
		}
		list.appendChild(document.createElement("br"));
		
		// Display the first card of the first pile.
		displayCard(0);
		displayCard(1);
		displayCard(2);
		
		// No picture was clicked yet
		this.lastPileClicked = undefined;
	}
	
    this.play = function(reset) { if (reset) { resetLesson(); }  }
    this.isPlayable = function(layer) {return  category.isPlayable(layer); }
	
	var pictureHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";
		
		var buttonNo = parseInt(element.id.substring(7),10);
		this.lastPileClicked = buttonNo;
		if (piles[buttonNo].length == 0) { acorns.beep(); }
		else
		{		
			var picture = piles[buttonNo][0].picture;
			var audio = piles[buttonNo][0].audio;
			var point = audioList[picture][audio];
			point.playAudio();
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}
	
	var controlHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement;
		element.style.borderStyle = "inset";
		
		var card = parseInt(element.getAttribute("name"),10);
		if (piles[card].length == 0)
		{
			acorns.beep();
			return;
		}
		
		var temp;
		var alt = element.alt;
		switch (alt)
		{
			case "prev":
				temp = piles[card].pop();
				piles[card].splice(0,0, temp);
				break;
			case "random":
				var index = Math.floor(Math.random() * piles[card].length);
				temp = piles[card][index];
				piles[card].splice(index, 1);
				piles[card].splice(0, 0, temp);
				break;
			case "next":
				temp = piles[card][0];
				piles[card].splice(0, 1);
				piles[card].push(temp);
				break;
			case "moveright":
				temp = piles[card][0];
				temp.responses = 0;
				piles[card].splice(0,1);
				piles[card+1].splice(0,0, temp);
				displayCard(card+1);
				break;
			case "moveleft":
				temp = piles[card][0];
				temp.responses = 0;
				piles[card].splice(0,1);
				piles[card-1].splice(0,0, temp);
				displayCard(card-1);
		}
		displayCard(card);
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}
	
	// Make a single flash card with its controls
	var makeCard = function(div, buttonNo, numButtons)
	{
		var card = document.createElement("div");
		card.style.height = (parseInt(div.style.height,10) + 30) + "px";
		card.style.width = (parseInt(div.style.width,10) + 30) + "px";
		card.appendChild(div);
	
		var text = [ "moveleft", "prev", "random", "next", "moveright" ];
		var tips = [ "move card to the pile on the left", "Select previous card", "Select random card"
						, "Select next card", "move card to the pile on the right"];
		if (acorns.system.isMobilePhone())
		{
			var text = [ "moveleft", "random", "moveright" ];
			var tips = [ "move card to the pile on the left", "Select random card", "move card to the pile on the right"];
		}
		if (buttonNo == 0) 
		{
			text.splice(0,1);
			tips.splice(0,1);
		}
		if (buttonNo >= numButtons - 1)
		{
			text.pop();
			tips.pop();
		}
		var controls = acorns.widgets.makeButtonPanel
						(text, tips, parseInt(div.style.left,10), parseInt(div.style.width,10), buttonNo, controlHandler);
						
		lessonData.environment.setColors(controls);
		card.appendChild(controls);
		return card;
	}
	
    this.configurePlayPanel = function(panel) 
	{
		var GAP = 3, BORDER = acorns.widgets.getButtonBorderSize(), SCROLL = 34;
		var COLUMNS = 3, NUM_PICTURES = 3, MAX_PCT = 0.4;
		
		var width = panel.clientWidth, height = parseInt(panel.style.height);
		var buttonSize = Math.floor((width - SCROLL - (COLUMNS-1)*GAP - 2*COLUMNS*BORDER)/COLUMNS);
		if (buttonSize > height * MAX_PCT) buttonSize = Math.floor(height * MAX_PCT);
		var rowWidth = COLUMNS*(buttonSize + 2*BORDER) + (COLUMNS-1)*GAP;
	
		var left = (width - rowWidth)/2;
		panel.style.overflow = "hidden";
		acorns.system.touchScroll(panel);
		
	    var block = document.createElement("div");
		block.style.position = "relative";
		block.style.width  = rowWidth + "px";
		block.style.height = (buttonSize + 2*BORDER) + "px";
		block.style.overflow = "hidden";
		block.style.top = "0px";
		block.style.left = left + "px";
	
	    var div = undefined, buttonNo;
	    panel.appendChild(block);

		var table = document.createElement("table");
		table.style.marginLeft = table.style.marginRight = "auto"; 
		panel.appendChild(table);
		var tr = document.createElement("tr");
		table.appendChild(tr);
		for (buttonNo=0; buttonNo<NUM_PICTURES; buttonNo++)
	    {
			
			div = acorns.widgets.makePictureButton(buttonNo, buttonSize
			        , (buttonNo % COLUMNS) * (buttonSize + 2 * BORDER + GAP)
					, function(e) { pictureHandler(e) } );
			
		    div = makeCard(div, buttonNo, NUM_PICTURES);
			block.appendChild(div);
	    }
		
		var list = document.createElement("div");
		var height =  block.clientHeight;

		list.style.position = "relative";
		list.style.width = "90%";
		list.style.left = "5%";
		list.style.height = (panel.clientHeight - height) + "px";
		list.style.minHeight = (panel.clientHeight - height) + "px"; 
		list.style.maxHeight = (panel.clientHeight - height) + "px"; 
		list.style.fontSize = Math.floor(14 * acorns.system.getFontRatio()) + "px";
		list.style.backgroundColor = "#c0c0c0";
		list.style.borderStyle = "outset";
		list.style.overflow = "auto";
		acorns.system.scrollableDiv(list);
		list.id = "phraselist";
		panel.appendChild(list);
	}
	
 	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Flash Cards</h3>");
		
		buffer.append("<p>When you execute a Flash Card lesson, you will see piles of cards ");
		buffer.append("at the top of the frame.");
		buffer.append(" Each card has a picture on it, and the top card of each pile shows.");
		buffer.append(" If a pile has no cards, ");
		buffer.append("you will just see a background color on which the cards would rest.");
		buffer.append(" After clicking on a card, you will hear the audio recorded phrase ");
        buffer.append("which is attached to that card.");
		buffer.append(" After hearing the audio, click the correct phrase from the list of phrases ");
		buffer.append("shown at the bottom of the frame.</p>");
		buffer.append(" If you are correct, you will hear feedback to acknowledge your success;");
		buffer.append(" otherwise the audio feeback indicates that you should try again.</p>");
		
		buffer.append("<p>The object of the game is to get all the cards to the right most pile.");
		buffer.append(" By answering correctly a number of times, a card moves to the next pile to the right.");
		buffer.append(" Answer more times correctly and the card will eventually get to the rightmost pile.");
		buffer.append(" However, incorrect answers might cause a card to move back to the left.</p>");
		
		buffer.append("<p>You can manually move the cards from one pile to another.");
		buffer.append(" To do this, simply click on one of the buttons with a page and arrow under a card pile.");
		buffer.append(" this action will move the card either to the adjacent left or right pile.");
		buffer.append(" The card will then appear on the top of the destination pile.</p>");
		
		buffer.append("<p>There are additional buttons that can appear under the card piles.");
		buffer.append(" These enable you to select the previous card, the next card, or a random card");
		buffer.append(" from the card pile.");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" click on this button to see a pop up menu with two Flash Card lesson options.");
		buffer.append(" The first of these resets the lesson, which causes ACORNS to ");
		buffer.append("shuffle the cards and move them back to the left pile.");
		buffer.append(" In effect, the game starts over.");
		buffer.append(" The second option toggles the the audio phrase display between ");
		buffer.append("the indigenous or gloss (first) language.");
		buffer.append(" The initial state is to show the phrases in the gloss language.</p>");
		buffer.append("");
		return buffer.toString();	
	}
	
	/* Read cookie and set options */
	this.getOptions = function() { return options; }
}   // End of FlashCards lesson class

function PicturesandSounds(acornsObject, data)
{
	var MIN_ICON_SIZE = 20;
	
	var acorns = acornsObject;
	var lessonData = data;
   	var options = new Options(acorns, lessonData, [SCALE, GAP]);

	var picture = undefined, imgTag = undefined;
	var audioList = undefined, audioIndex = undefined;
	var points = undefined;

	this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseMultipleAudioCategory(acorns, lessonDom, lessonData);
		if (lessonData.paramList["points"]==undefined) points = 50;
		else points = parseInt(lessonData.paramList["points"],10);
	}
	
	/* Create a button showing an icon */
    var makeIconButton = function(iconName, tooltip, handler, p, left, top, size)
    {
		var img = document.createElement("img");
		img.style.position = "absolute";
		img.style.top = top + "px";
		img.style.left = left + "px";
		img.style.width = img.style.height = size + "px";
		img.style.borderStyle = "outset";
		img.style.borderWidth = "2px";
		img.style.margin = "0px -3px";
		img.id = "button " + p;
		
		img.alt = iconName;
		img.name = "pictureAndSounds";
		
		img.src = acorns.iconLink + iconName + ".png";
		img.title = tooltip;
				
		acorns.system.addListener(img, "click", function(e) { handler(img); });
		return img;
    }	// End of makeIconButton()

	// Compute where icon button should display
    var displaySpot = function(view, point, iconSize)
    {
		// layer already removed from the point.   
		var x = Math.floor(point.x * view.width  / points);
		var y = Math.floor(point.y * view.height / points);
		
		if (x + iconSize > view.width )  x = view.width - iconSize;
		if (y + iconSize > view.height ) y = view.height - iconSize;
		x += view.x;
		y += view.y;

		return { x: x, y: y }; 
	}
	
	// Get the bounds of the img tag so we can correctly position the acorns
	// Note: For rotated objects, the positions in the img tag reflects values before the rotation
	//			This means we need to adjust the values to compute the locations after the rotation completes.
	var getView = function()
	{
		var	angle = picture.getAngle();
		var parent = imgTag.parentNode;
		var left = parseInt(parent.style.left,10);
		var top  = parseInt(parent.style.top,10);

	    var view = { x:left + parseInt(imgTag.style.left,10), y: top + parseInt(imgTag.style.top,10)
				   , height: imgTag.clientHeight, width: imgTag.clientWidth };

		if (angle == "90" || angle == "270")
		{
			view.height = imgTag.clientWidth;
			view.width = imgTag.clientHeight;
			
			if (!acorns.system.isOldIE())
			{
				view.x += (view.height - view.width)/2;
				view.y += (view.width - view.height)/2;
			}
		}	
		return view;
	}
	
	var waitHandler = function()
	{
		return imgTag.clientWidth!=0 && imgTag.clientHeight != 0
				&& (parseInt(imgTag.style.left,10)>0 || parseInt(imgTag.style.top,10)>0);
	}

	var controlHandler = function(tag)
	{
		tag.style.borderStyle = "inset";

		var index = parseInt(tag.id.substring(7),10);
		
		var alt = tag.getAttribute("alt");
		if (alt == "acorn")
		{
			var numAudios = audioIndex[index+1] - audioIndex[index];
			var selection = Math.floor(Math.random()*numAudios) + audioIndex[index];
			acorns.widgets.makePointDisplay(tag, audioList[selection], lessonData);
		}
		else // Its a link to another lesson
		{
			if (!acorns.setLinkedLesson(audioList[audioIndex[index]])) acorns.beep();
			else acorns.getActiveLesson().play(true);				
		}
		setTimeout(function() { tag.style.borderStyle = "outset" }, 250);
	}
	
	this.play = function(reset) 
	{ 
		var loaded = picture.isLoaded(imgTag) && waitHandler();

		if (!loaded) 
		{
			setTimeout( function() {lessonData.play(false); }, 500);
			return;
		}
		
		var div = imgTag.parentNode;
		var panel = div.parentNode;
		displayFormat(div, panel);

		var imgView = getView();
		var angle = picture.getAngle();
		var elements = document.getElementsByName("pictureAndSounds");
		var main = document.getElementById("main");
		var e;
		
		for (e = elements.length - 1; e>=0; e--)
		{
			elements[e].parentNode.removeChild( elements[e] );
		}
	
		var coordinates,  spot, point;

		if (imgView.height < imgView.width) iconSize = Math.ceil(imgView.height / points);
		var iconSize = Math.ceil((imgView.width + imgView.height) / points);

		if (iconSize<MIN_ICON_SIZE) 
		{
			iconSize = MIN_ICON_SIZE;
		}
		var size = audioIndex.length;
		
		for (var p=0; p<size - 1; p++)
		{
			point = audioList[audioIndex[p]];
			coordinates = point.getCoordinates();
			switch (angle)
			{   
				case "90":  coordinates = {x: points - 1 - coordinates.y, y: coordinates.x };
							break;
				case "180": coordinates = {x: points -1 - coordinates.x, y: points -1 - coordinates.y };
							break;
				case "270": coordinates = {x: coordinates.y, y: points -1 - coordinates.x };
							break;
			}  
			
			spot = displaySpot( imgView, coordinates, iconSize );

			if (point.isAudio())
				button = makeIconButton
					 ("acorn", "Hear audio", controlHandler, p, spot.x, spot.y, iconSize);
			else
				button = makeIconButton
					 ("anchor", "Link to another lesson", controlHandler, p, spot.x, spot.y, iconSize);
			main.appendChild(button);
		}		
	}
	
	this.isPlayable = function(layer) 
	{
		if (lessonData.objectList.length==0) return;
		picture = lessonData.objectList[0];
		
		// First layer does not need audio recordings.
		try
		{
			var layerList = picture.objectList;
			if (layer>0)
			{
				if (layerList==undefined) return false;
				if (layerList.length==0) return false;
				if (layerList[layer - 1] == undefined) return false;
			}
			audioList = layerList[layer - 1].objectList;
			audioIndex = [];
			for (var a=0; a<audioList.length; a++)
			{
				if (a==0 || !audioList[a-1].isSameCoordinates(audioList[a]))
				{
					audioIndex.push(a);
				}
			}
			audioIndex.push(audioList.length);
			return true;
		}
		catch (e) 
		{ return false; }
	}

	this.configurePlayPanel = function(panel) 
	{
		panel.style.overflow = "auto";
		
		var width = panel.clientWidth, height = panel.clientHeight;
		picture.setScaleFactor(options.getScaleFactor());
		var ratio = picture.getScaleFactor()/100;
		
		var iconSize = Math.ceil((width + height) / points);
		if (iconSize<MIN_ICON_SIZE) 
		{
			ratio *= MIN_ICON_SIZE / iconSize;
		}		

		var div = document.createElement("div");
		div.style.margin = "auto";
		div.id = "picturesoundpicture"
		panel.appendChild(div);

		divWidth = Math.ceil(width * ratio);
		divHeight = Math.ceil(height * ratio);
		div.style.width = divWidth + "px";
		div.style.height = divHeight + "px";

		imgTag = document.createElement("img");
		div.appendChild(imgTag);
		picture.centerAndScalePicture(div);
		lessonData.environment.setColors(div);
	}
	
	function displayFormat(div, panel)
	{
		var panelWidth = parseInt(panel.clientWidth);
		var panelHeight = parseInt(panel.clientHeight);
		
		divWidth = Math.ceil(parseInt(div.style.width,10));
		divHeight = Math.ceil(parseInt(div.style.height,10));
		divLeft = Math.floor((panelWidth - divWidth)/2);
		divTop = Math.floor((panelHeight - divHeight)/2);
		div.name = "goobly";
		
		imgWidth = parseInt(imgTag.style.width, 10);
		imgHeight = parseInt(imgTag.style.height, 10);
		imgTop = parseInt(imgTag.style.top, 10);
		imgLeft = parseInt(imgTag.style.left, 10);
		
		div.style.left = divLeft + "px";
		div.style.top = divTop + "px";
		
		if (imgWidth <= panelWidth && imgHeight <= panelHeight)
			return;
		
		if (imgWidth > panelWidth) 
		{
			div.style.left = -imgLeft + "px";
			div.style.width = (imgWidth + imgLeft) + "px";
		}

		if (imgHeight > panelHeight) 
		{
			div.style.top = -imgTop + "px";
		}
		div.style.height = (imgHeight + imgTop) + "px";
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Picture and Sounds</h3>");
		buffer.append("<p>If you wish to hear the recorded sounds attached to places on the picture,");
		buffer.append(" simply click on any of the acorns.");
		buffer.append(" ACORNS will randomly play the audio for one of the sounds attached to the acorn." );
		buffer.append(" You will also see a pop-up window containing the gloss (first language) translation,");
		buffer.append(" the native spelling associated with the audio, and possibly additional information.");
		buffer.append(" Note the arrow to the right of the dialog.");
		buffer.append(" You can click this should you want to rehear the audio.</p>");

		buffer.append("<p>If you see an icon with a chain link picture, they link to other lessons;");
		buffer.append(" click on these to switch. This feature works like hyperlinks do on the Internet.");
		buffer.append(" For example, the linked lesson can be a blown up picture that provides more detail.");
		buffer.append(" ACORNS can support sophisticated lessons using this feature.</p>");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" click on this button to see a pop up menu with two Picture and Sound lesson options.");
		buffer.append(" Selecting these options changes the size of the displayed picture from the size indicated."); 
		buffer.append(" By adjusting the scale factor, you can shrink the picture so the whole thing fits");
		buffer.append(" in the display area. Each use of this option alters the size by a fixed percentage.");
		buffer.append(" If the picture is larger than the display area,");
		buffer.append(" you can scroll to see the areas that don't show.");
		buffer.append("");
		return buffer.toString();	
	}

	this.getOptions = function() { return options; }

}   // End of PictureandSounds lesson class

function Phrase(word, format, p, listOfPhrases, x, y)
{
	var controlString = word;
	var text = format;
	var point = p;
	var list = listOfPhrases;
	var first = x;
	var last = y;
	
	this.isPlus = function()
	{
		var plus = controlString.charAt(0);
		return plus == "+";
	}
	
	this.getControlString 	= function() 	 { return controlString; }
	this.setControlString 	= function(category, c, finish) 	 
	{ 
		controlString = c; 
		text = category.formatPhrasesForDisplay(c, false, false, finish);
	}
	
	this.getEmbeddedPhrases = function() 	 { return list; }
	this.getRange           = function() 	 { return {x:first, y:last}; }
	this.setRange 			= function(x, y) { first = x; last = y; }
	this.getText 			= function() 	 { return text; }
	this.getPoint			= function()	 { return point; }
}

/***** The following section pertains common code for lesson categories *****/
function CategoryRelatedPhrases(acornsObject, data, opt)
{
	var acorns = acornsObject
	var lessonData = data;
	var options = opt;
	var currentCategory = this;

	var categoryList = undefined;
	var phrases = [];
	var sentenceNo = -1;
	var free		 = [];
	var correct = 0, incorrect = 0;
	
	var BORDER = 20, WIDTH = 500, IMAGE = 60, HEIGHT = 250, SPACE = 5, ICON = 30;
	
	var SPECIALS =
	[
			[ ['a', 0xE0, 0xE5, '\u00E0-\u00E5' ] ], [ ['e',0xE8, 0xEB, '\u00E8-\u00EB'] ], [ ['i',0xEC, 0xEF, '\u00EC-\u00EF' ] ],
			[ ['o', 0xF2, 0xF6, '\u00F2-\u00F6' ] ], [ ['u',0xF9, 0xFC, '\u00F9-\u00FC'] ],

			[ ['c',0xE7, 0xE7, '\u00E7' ], ['c',0x107, 0x107, '\u0107' ], ['c',0x109, 0x109, '\u0109' ], ['c',0x10B, 0x10B, '\u0108' ],
			  ['c',0x10D, 0x10D, '\u010D' ] ],
			  
			[ ['n',0xF1, 0xF1, '\u00F1' ], ['n',0x144, 0x144, '\u0144'], ['n',0x146, 0x146, '\u0146' ], ['n',0x148, 0x148, '\u0148' ] ],
				
			[ ['r',0x155, 0x155, '\u0155'], ['r',0x157,  0x157, '\u0157'], ['r',0x159, 0x159, '\u0159'] ],
			
			[ ['s',0x15B, 0x15B, '\u015B'], ['s',0x15D, 0x15D, '\u015D'],  ['s',0x15F, 0x15F, '\u015F'], ['s',0x161, 0x161, '\u0161'] ],
			
			[ ['w',0x175, 0x175, '\u0175'], ['y',0xFD, 0xFD, '\u00FD'], ['y',0xFF, 0xFF, '\u00FF'], ['y',0x177, 0x177, '\u0177'], 
			  [ 0, 0xBF, 0xBF, '\u00BF']	],
			
			[ ['z',0x17A, 0x17A, '\u017A'], ['z',0x17C, 0x17C, '\u017C'], ['z',0x17E, 0x17E, '\u017E'] ]
	   ];
	   
	if (typeof Custom !== 'undefined')
	{
		var custom = new Custom();
		if (custom.specialCharacters)
			SPECIALS = custom.specialCharacters();
	}
	   
	this.getCategoryList = function() { return categoryList; }
	
	this.reset = function(create)
	{
		sentenceNo = -1;
		free = [];
	}
	
	this.getSpecials = function() { return SPECIALS; }
	
	this.feedback = function(parent, object, type, title, handler, all, category)
	{
		// Get data pertaining to this category
		if (category == undefined)
			category = object.getCurrentCategory();
		var audio = category.getAudio(); 
        var picture = category.getPicture();
		
        // Create the wrapping panel
		var popup = document.createElement("div");
		popup.style.position = "absolute";
	    popup.style.border = "thick ridge";
		var messageBackground = acorns.system.getColor(255, 255, 204);
		popup.style.backgroundColor = messageBackground;
		popup.style.color = "black";
		popup.id = "answer";
		popup.style.overflowY = "auto";;
		acorns.system.scrollableDiv(popup);

		var width = (WIDTH < main.clientWidth - BORDER) ? WIDTH : main.clientWidth - BORDER;
		popup.setAttribute("width", width + "px");
 
		// Add the title
		if (title == undefined) 
			title =  category.getSentence();
		
		acorns.widgets.makeCategoryHTML(popup, title);
		
		// Configure the scorePanel with audio, possible picture, and the score
		var scorePanel = document.createElement("div");
		scorePanel.style.borderStyle = "ridge";
		scorePanel.style.fontSize = "large";
		scorePanel.style.marginTop = "3px";
		popup.appendChild(scorePanel);

		var audio = acorns.widgets.makeAudioHTML(scorePanel, audio, ICON);
		if (audio) 	
			audio.style.cssFloat = "left";				
		acorns.widgets.makePictureHTML(scorePanel, picture, ICON + 10);
		
		var score = object.getScore();
		var answers = (score[0]<=1) ? "" : "s";
		var results = score[0] + " correct answer" + answers
				+ " out of " + score[2] + " for " + score[3] + "%";
		var resultText = document.createElement("p");
		resultText.innerHTML = results;
		resultText.style.height = (ICON + 5) + "px";
		resultText.style.margin = "4px";
		scorePanel.appendChild(resultText);
		
		var pointsPanel = document.createElement("div");
		pointsPanel.style.clear = "none";
		pointsPanel.style.verticalAlign = "top";
		popup.appendChild(pointsPanel);
			
		var point, p, indigenous, formatted, pointPanel;
		if (type != CORRECT)
		{
	       var first = true;
		   var sentences = category.getPoints();
	 	   for (p = 0; p<sentences.length; p++)
	 	   {
				pointPanel = document.createElement("div");
				pointPanel.style.width = "100%";
			    if (!all) point = object.getCurrentSentence();
				else point = sentences[p];
				
				audio = acorns.widgets.makeAudioHTML(pointPanel, point.getAudio(), ICON);
				if (audio) 	
					audio.style.cssFloat = "left";				
				indigenous = point.getSpell();
				formatted = object.formatPhrasesForDisplay(indigenous, true, true, true);
				point = point.clone();
				point.setSpell(formatted);
				acorns.widgets.makePointHTML(pointPanel, point);
		        
				hr = document.createElement("hr"); 
				hr.style.margin = "0px";
				hr.style.clear = "left";
				hr.style.height = "3px";
				pointPanel.appendChild(hr);

		        first = false;
				pointsPanel.appendChild(pointPanel);
				if (!all) break;
		    }	// End of loop through sentences
	    }		// End if type

		var buttonPanel = document.createElement("div");
		buttonPanel.style.textAlign = "center";
		buttonPanel.style.width = "100%";
		buttonPanel.style.height = ICON + "px";
		popup.appendChild(buttonPanel);
		
		var buttonText = ["OK"];
		switch (type)
		{
			case CORRECT:
				buttonText = [ "Continue to next category", "Repeat category" ];
				break;
			case INCORRECT:
				buttonText = [ "Continue normally", "Skip this question" ];
				break;
		}

		acorns.widgets.makeInterfaceButtonHTML(buttonPanel, popup.id, buttonText[0], handler);
		if (buttonText.length > 1) 
			acorns.widgets.makeInterfaceButtonHTML(buttonPanel, popup.id, buttonText[1], handler);
		
		acorns.widgets.positionHTML(parent, popup);
		popup.style.height = (popup.clientHeight + 1.5*ICON) + "px";  // Explorer strikes again!
 	}  // End of Feedback()
	   

	this.isPlayable = function(layer, categoryAudio, sentence, all) 
	{
		var layerData = lessonData.objectList[layer-1];
		if (layerData == undefined) return false;
		
		categoryList = layerData.objectList;
		if (categoryList==undefined || categoryList.length==0) return false;
		
		category = this.getCurrentCategory();
		var sentences = category.getPoints();

		var points;
		var audioArray = [];
		for (var i=0; i<categoryList.length; i++)
		{  
			points = categoryList[i].objectList;
			if (categoryAudio)
			{
				var audio = category.getAudio();
				if (!audio) return false;
				if (audio.length == 0) return false;
			}

			if (points.length==0) return false;
			
			var j = 0;
			for (j=0; j<points.length; j++)
			{
				if (!points[j].isComplete()) 
				{
					if (all) return false;
					else continue;
				}
				if (sentence && !points[j].isSentence()) 
				{
					if (all) return false;
					else continue;
				}
				audioArray.push(points[j]);
			}
		}
		
		if (audioArray.length == 0) return false;
		return audioArray;  // List of all the possible sentences (for Magnet Game)
	};
	
	this.getScore = function()
	{
		var total = correct + incorrect;
		var percent = (total == 0) ? 0 : correct * 100 / total;
		return [correct, incorrect, total, Math.round(percent)];
	}
	
	this.updateScore = function(result)
	{
		if (result == INCORRECT)
			incorrect++;
		else
			correct++;
	}
	


	this.formatControlString = function(controlString, left)
	{
		var LEFT1 = /(^[+]?[-]?)\([^\[\]\( ]*\)(.*)/g;
		var LEFT2 = /(^[+]?[-]?)\[([^\(\)\[ ]*)\]\([^\[\]\( ]*\)(.*)/g;
		var LEFT3 = /^([+]?[-]?)\[([^\(\)\[ ]*)\](.*)/g;
		
		var RIGHT1 = /(.*)\([^\[\]\( ]*\)([-]?$)/g;
		var RIGHT2 = /(.*)\([^\[\]\( ]*\)\[([^\(\)\[ ]*)\]([-]?$)/g;
		var RIGHT3 = /(.*)\[([^\(\)\[ ]*)\]([-]?$)/g;
		
		if (left)
		{
			// replace (stuff between) at front of control string by nothing
			controlString = controlString.replace(LEFT1, "$1$2");

			// replace [stuff between](stuff between) at front of control string by stuff between
			controlString = controlString.replace(LEFT2, "$1$2$3");

			// replace [stuff between] at front of control string by stuff between
			controlString = controlString.replace(LEFT3, "$1$2$3");
		}
		else
		{
			// replace (stuff between) at back of control string by nothing
			controlString = controlString.replace(RIGHT1, "$1$2");

			// replace (stuff between)[stuff between] at back of control string by stuff between
			controlString = controlString.replace(RIGHT2, "$1$2$3");

			// replace [stuff between] at back of control string by stuff between
			controlString = controlString.replace(RIGHT3, "$1$2$3");
		}
		return controlString;
    }

	this.formatPhrasesForDisplay = function(sentence, finish, first, last)
	{
		if (finish)
		{
			sentence = sentence.replace(/\+\S*/g, "");
			sentence = sentence.replace(/\([^\(\)\] ]*\)/g, "");
			sentence = sentence.replace(/[-]\s+[-]/g, "");
			sentence = sentence.replace(/\s+[-]/g, "");
			sentence = sentence.replace(/[-]\s+/g, "");
			sentence = sentence.replace(/(\S)\.{3}(\S)/g, "$1$2");
			sentence = sentence.replace(/  /g, " ");
		}
		else
		{
			sentence = sentence.replace(/\[[^\(\)\[ ]*\]/g, "");
			sentence = sentence.replace(/^\+/g, "");
		}
		
			if (first) 
			{
				sentence = sentence.replace(/^[-]/g, "");
				sentence.replace(/^\.{3}/g, "");
			}
			if (last) {
				sentence = sentence.replace(/[-]$/g, "");
				sentence = sentence.replace(/\.{3}$/g,"");
			}
			
			sentence = sentence.replace(/[\(\)\[\]]*/g, "");
			sentence = sentence.replace(/\.{3}\s+/g, "");
			sentence = sentence.replace(/\s+\.{3}/g, "");
			sentence = sentence.replace(/\s+/g, " ");
			return acorns.system.trim(sentence);
	}
	
	this.formatTextField = function(indigenous, point)
	{
		if (!point)
			point = this.getCurrentSentence();
		
		var sentence;
		if (indigenous === "y")
		{
			sentence = point.getSpell();
			sentence = this.formatPhrasesForDisplay(sentence, true, true, true);
		}
		else
		{
			sentence = point.getGloss();
		}
		return sentence;
	}
	
	this.checkAnswer = function(text, indigenous, point, silent)
	{
		if (indigenous === "y")
			text = this.formatPhrasesForDisplay(text, true, true, true);
		
		sentence = this.formatTextField(indigenous,  point);
		if (typeof Custom !== 'undefined')
		{
			var custom = new Custom();
			if (custom.convertSpecialChars)
			{
				sentence = custom.convertSpecialChars(sentence);
				text = custom.convertSpecialChars(text);
			}
		}


		
		var distance = acorns.system.stringDistance(text, sentence);
		var noControls = this.removeSpecialCharacters(text);
		var sNoControls = this.removeSpecialCharacters(sentence);
		var sLower = sNoControls.toLowerCase();
		var cLower = noControls.toLowerCase();
		var controlDistance = acorns.system.stringDistance(cLower, sLower);
		
		distance = distance/sentence.length;
		controlDistance = controlDistance/sentence.length;
		if (controlDistance>0.1)
		{
			if (!silent) acorns.feedbackAudio("incorrect");
			if (!silent)incorrect++;
			return [INCORRECT, controlDistance];
		} 
		else if (distance>0.0)
		{
			if (!silent) acorns.feedbackAudio("spell");
			if (!silent) correct++;
			return [CLOSE, distance];
		} 
		else
		{
			if (!silent) acorns.feedbackAudio("correct");
			if (!silent) correct++;
			return [CORRECT, distance];
		}
	}
	
   	this.removeSpecialCharacters = function(source)
   	{
   		var specials = this.getSpecials();
		var r, row, regex, replacement, hex1, hex2, c, column;
    	for (r=0; r<specials.length; r++)
    	{
			row = specials[r];
    		for (c=0; c<row.length; c++)
    		{
				column = row[c];
    			replacement = "" + ((column[0]===0)? "" : column[0]);
    			regex = new RegExp("[" + column[3] + "]", "g");
    			source = source.replace(regex, replacement);
    		}
   		}   // end of row loop
   		return source;
   	}
	
	this.createPhraseList = function(point, indigenous)
	{
		var sentence = point.getGloss();
		if (indigenous === "y")
			sentence = point.getSpell();

        var plusPhrases = [];
        var allPhrases  = [];
		
		var words = sentence.split(" ");
		var count = words.length;
      
		// Remove hanging dashes
		var k;
		for (k=0; k<count; k++)
		{
		    if (words[k].charAt(0) == '+')
		    {
	   		    if (plusPhrases.length == 0)
	   		    {
	   			    words[k] = words[k].replace(/^[+][-]/g,"+");
	   	      	    words[k] = this.formatControlString(words[k], true);
	   		    }
	    	     
	   		    words[k] = words[k].replace(/\.{3}/g, "");
	 		    plusPhrases.push(words[k]);
	   	    }
	   	    else  
	   	    {
	   		    if (allPhrases.length == 0)
	   		    {
					words[k] = words[k].replace(/^[-]/g,"");
					words[k] = this.formatControlString(words[k], true);
				}
				allPhrases.push(words[k]);
			}
		}
      
		var size = plusPhrases.length;
		var word;
		if (size>0)
		{
			word = plusPhrases[size-1];
			word = word.replace(/[-]$/g, "");
			word = this.formatControlString(word, false);
			plusPhrases[size-1] = word;
		}

		size = allPhrases.length;
		if (size>0)
		{
			word = allPhrases[size-1];
			word = word.replace(/[-]$/g, "");
			word = this.formatControlString(word, false);
			allPhrases[size-1] = word;
		}

		allPhrases = allPhrases.concat(plusPhrases);
     
		var phrase; 
		phrases = [];
		for (k=0; k<count; k++)
		{  
			word = allPhrases[k];
    				
			if (word.replace(/[-]/g,"").length==0) // Don't add phrases that are empty
				continue;
				
    	    if (word.replace(/[\+\-\.\[\]\(\) ]*/g, "").length==0)
				continue;

			var text = this.formatPhrasesForDisplay(word, false, k==0, k==count-1);
			phrase = new Phrase(word, text, point, [], k, k);
			phrases.push(phrase);
		}
	 
		var prefix, suffix, center, len;
		var p = phrases.length-1;
		while (p>1)
		{
			prefix = phrases[p-2].getControlString();
			center = phrases[p-1].getControlString();
			suffix = phrases[p].getControlString();
  
			if (suffix.charAt(0) == '+')
			{
				p--;
				continue;
			}

			if (suffix.indexOf("...") === 0 &&
    			 prefix.substring(prefix.length-3, prefix.length) == "..." && !(center.indexOf("...")>0))
			{
				if (prefix.indexOf("...")===0) prefix = "-" + prefix.substring(3);
    		 
				len = suffix.length-3;
				if  (suffix.substring(len, len+3) === "...") suffix = suffix.substring(0,len) + "-";
    		 
				prefix = prefix.replace(/\.{3}/g, "");
				center = center.replace(/\.{3}/g, "");
				suffix = suffix.replace(/\.{3}/g, "");
				if (suffix.replace(/[-]/g, "").length==0)
				{   // only a dash left
					phrases.splice(p,1);
					p--;
					continue;
				}
    		 		
				phrases[p-2].setControlString(this, prefix + "..." + suffix, false);
    		 
				phrases.splice(p,1);
				p -= 2;
			}
			else 
			{
				suffix = suffix.replace(/\.{3}/g, "");
				phrases[p].setControlString(this, suffix, false);
			}
			p--;
		}

		// Set the ranges for each magnet for easy locating without a search
		var controlString, phrases;
		for (var p = 0; p<phrases.length; p++)
		{
			phrase = phrases[p];
			controlString = phrase.getControlString();
			phrase.setRange(p, p);
		}
		return phrases;
	}
	
	this.makeMagnet = function(text, point, draggable, indigenous, handler)
	{
		
		var magnet = document.createElement("span");
		if (!point)
		{
			point = text.getPoint();
			magnet._source = text;
			text = text.getText();
		}

		div.innerHTML = text;
		textNode = document.createTextNode(text);
		magnet.appendChild(textNode);
		magnet.title = text;
		if (draggable) magnet.setAttribute("draggable", draggable);
		magnet.name = "magnet";
		if (handler) acorns.system.addListener(magnet, "click", handler);
		
		magnet.style.display = "inline-block";
		magnet.style.marginLeft = "20px";
		magnet.style.marginRight = "5px";
		magnet.style.marginTop = "10px";
		
		magnet.style.borderStyle = "outset";
		lessonData.environment.setColors(magnet);
		
		if (indigenous === "y")
		{
			point.applyFont(magnet);
		}
		
		magnet.style.fontSize = options.getFontSize() + "pt";
		return magnet;
	}
	
	// Create a list of magnets and add them to a panel
	this.makeMagnets = function(panel, point, draggable, indigenous, randomPosition, scrambleOrder, handler)
	{
		var scrambledList = [], i, p
		var size = { width: panel.clientWidth - BORDER, height: panel.clientHeight - BORDER };
		var left, top, wLen = phrases.length;
		var magnetList = [];
		var scrambledList = [];
		var indices = []; 
		
		if (scrambleOrder)
		{
			for (p=0; p<phrases.length; p++) { indices.push(p); }
			
			while (indices.length>0)
			{
				index = Math.floor(Math.random() * indices.length);
				scrambledList.push(phrases[indices[index]]);
				indices.splice(index,1);
			}
		}
		
		for (i=0; i<wLen; i++)
		{
			var magnet = this.makeMagnet(scrambledList[i], null, false, indigenous, handler);
			
			panel.appendChild(magnet);
			magnetList.push(magnet);
			
			if (randomPosition)
			{
				magnet.style.position = "absolute";
				left = Math.floor(Math.random() * size.width);
				top = Math.floor(Math.random() * size.height); 
				magnet.style.left = left + "px";
				magnet.style.top = top + "px";
				
				if (magnet.clientWidth + left >= size.width) 
			       magnet.style.left = (size.width - magnet.clientWidth) + "px";
				if (magnet.clientHeight + top >= size.height)
				   magnet.style.top = (size.height - magnet.clientHeight) + "px";
			}
		}
		return magnetList;
	}
	
	this.displayMagnets = function(panel, randomPosition, scrambledOrder, handler, point)
	{
		var indigenous = "y";
		if (point)
		{
			phrases = this.createPhraseList(point, indigenous);
			if (point)  point.setPhrases(phrases);
		}
		else
		{
		    point = this.getCurrentSentence();
			indigenous = options.getOptions()["select"];
		}
		
		this.makeMagnets(panel, point, false, indigenous, randomPosition, scrambledOrder, handler);
	}
	
   this.makePhrase = function(phrase, controlString)
   {
		var nestedPhrases = [];
		var phraseList = phrase.getEmbeddedPhrases();
		nestedPhrases = nestedPhrases.concat(phraseList);
		if (phraseList.lentgh == 0)  nestedPhrases.push(phrase);
	  
		var range = phrase.getRange();
		var point = phrase.getPoint();
	  
		var text = this.formatPhrasesForDisplay(controlString, false, false, false);
		var newPhrase = new Phrase(controlString, text, point, nestedPhrases, range.x, range.y);
		return newPhrase;
   }
 
   this.join = function(first, second, separator)
   {
		var firstRange = first.getRange();
		var secondRange = second.getRange();
		var point = second.getPoint();
		
		if (!separator) separator = "";
		
		var joinPhrases = point.getPhrases();
		if (joinPhrases == undefined)
			joinPhrases = phrases;
	  
		if (first.isPlus() != second.isPlus())
		  return null;
      
		if (point != first.getPoint())
			return null;

		// Verify that the ranges are consecutive
		var phrase, temp;
		var alternatePosition = secondRange.x - 1; // Position of phrase before second
		if (alternatePosition>=0)
		{
			if (point.getPhrases)
				
			phrase = joinPhrases[alternatePosition];
			if (phrase.getText() === first.getText())
			{
				firstRange = phrase.getRange();
			}
		}
		
		if (firstRange.y + 1 != secondRange.x)
		{ return null; }
      
		var firstText = first.getControlString();
		var secondText = second.getControlString();
		var newText = "", previous, splitText;

		if (firstRange.x > 0 && !(firstText.indexOf("...")>=0))
		{
			previous = joinPhrases[firstRange.x - 1];
			if (previous.getControlString().indexOf("...")>=0)
				return null;
		}
      
		if (firstText.indexOf("...")>=0)
		{
			splitText = firstText.split("...");
			if (splitText.length < 2) return null;
    	  
			splitText[0] = this.formatControlString(splitText[0], false);
			splitText[1] = this.formatControlString(splitText[1], true);
    	  
			secondText = secondText.replace(/^[-]*/g, "");
			secondText = secondText.replace(/[-]*$/g, "");
			newText = splitText[0] + secondText + splitText[1];
			newText = this.formatControlString(newText, true);
		}
		else
		{
			var firstEnd = firstText.charAt(firstText.length - 1) === "-";
			secondText = secondText.replace(/^\+/,"");
			var secondStart = secondText.charAt(0) == "-"; 
			if ( !(firstEnd || secondStart || (separator.length > 0))) { return null; }
    	  
			firstText = this.formatControlString(firstText, false);
			firstText = firstText.replace(/[-]$/g, "");
			secondText = this.formatControlString(secondText, true);
			secondText = secondText.replace(/^[-]/g, "");
			
			var sep = (firstEnd || secondStart) ? "" : separator;
			newText = firstText + sep + secondText;
			newText = newText.replace(/[-]{2}/g,"");
			newText = newText.replace(/\([^\) ]*\)/g, "");
			newText = newText.replace(/[\[\]]/g, "");
		}
      
		var nestedPhrases = [];
		var firstPhrases = first.getEmbeddedPhrases();
		var secondPhrases = second.getEmbeddedPhrases();
		nestedPhrases = firstPhrases.concat(secondPhrases);
		if (firstPhrases.length == 0)  nestedPhrases.push(first);
		if (secondPhrases.length == 0) nestedPhrases.push(second);
	  
		var text = this.formatPhrasesForDisplay(newText, false, false, false);
		var newPhrase = new Phrase(newText, text, point, nestedPhrases, firstRange.x, secondRange.y );
		return newPhrase;
   }	// End of join method
   
   this.verify = function()
   {
		var sentencePanel = document.getElementById("sentencePanel");
		var childNodes = sentencePanel.childNodes;
		
		var n, len = childNodes.length, sentence = "", point;
		var buffer = new acorns.system.StringBuffer();
		var plusCount = 0, controlString;
		for (n=0; n<len; n++)
		{
			if (childNodes[n]._source)
				controlString = childNodes[n]._source.getControlString();
			else
				controlString = childNodes[n].value;
			
			if (controlString.charAt(0) == "+") plusCount++;
			buffer.append(controlString);
			if (n < len-1) buffer.append(" ");
		}
		
		var text = buffer.toString();
		var indigenous = options.getOptions()["select"];
		var answer = INCORRECT;
		var object = this;

		if (plusCount==0)
				answer = this.checkAnswer(text, indigenous)[0];

		if (answer === INCORRECT)
		{
			this.feedback(parent, this, INCORRECT, "Incorrect answer, do you want to try again?",
				function(text) 
				{
					if (text.indexOf("Continue") === 0)
					{
						object.retryCurrentSentence();
						object.getNextSentence();
						acorns.play(false);

					}
					else
					{
						object.processNextSentence(parent, this);
					}
				}, false);
		} 
		else if (answer == CLOSE)
		{
			this.feedback(parent, this, CLOSE, "Not quite correct, check your answer", 
				function(e)
				{
					object.processNextSentence(parent, this);
				}, false);
		} 
		else
		{
			this.processNextSentence(parent, this);
		}		
   }
   
   var controlHandler = function(e, object, parent)
   {
	   	if (!e) e = window.event;
		
		var source = (e.currentTarget) ? e.currentTarget : e.srcElement;
		source.style.borderStyle = "inset";

		var category = object;
		var currentCategory = category.getCurrentCategory();
		var categoryText = currentCategory.getSentence();

		switch (source.id)
		{
			case "hear":
				var point = phrases[0].getPoint(); // All phrases point to same sentence
				point.playAudio();
				break;
			case "speed":
				var point = phrases[0].getPoint();
				point.playAudioSlow();
				break;
			case "answers":
				category.feedback(source, category, HELP, categoryText, null, false);
				break;
			case "check":
			    object.verify();
				break;	// End of check
		}				// End of switch
		setTimeout(function() { source.style.borderStyle = "outset" }, 250);

   }	// End of control handler
   
   this.createAnswerPanel = function()
   {
	   var answerPanel = document.createElement("div");
	   answerPanel.style.marginBottom = "-20px";
	   answerPanel.style.border = "thick groove";
	   answerPanel.style.width = "100%";
	   answerPanel.style.height = ICON + SPACE + "px";
	   answerPanel.style.marginBottom = SPACE + "px";
	   lessonData.environment.setColors(answerPanel);
	   
	   var playButton = acorns.widgets.makeIconButton("play", "Hear the question", 
			function(e) 
			{ 
				controlHandler(e, currentCategory, playButton);
			}, ICON);
		answerPanel.appendChild(playButton);
		playButton.style.marginRight = SPACE + "px";
		playButton.id = "hear";

		if (!acorns.system.isOldIE())
		{
			var speedButton = acorns.widgets.makeIconButton("slowdown", "Change the speed", 
				function(e) 
				{ 
					controlHandler(e, currentCategory, speedButton);
				}, ICON);
			answerPanel.appendChild(speedButton);
			speedButton.style.marginRight = SPACE + "px";
			speedButton.id = "speed";
		}
			
		var answerButton = acorns.widgets.makeIconButton("answers", "See an answer", 
			function(e) 
			{ 
				controlHandler(e, currentCategory, answerButton);
			}, ICON);
		answerPanel.appendChild(answerButton);
		answerButton.style.marginRight = SPACE + "px";
		answerButton.id = "answers"
		
		var checkButton = acorns.widgets.makeIconButton("check", "Check your sentence",
			function(e) 
			{ 
				controlHandler(e, currentCategory, checkButton);
			}, ICON);
		answerPanel.appendChild(checkButton);
		checkButton.style.marginRight = SPACE + "px";
		checkButton.id = "check";
		
		var span = document.createElement('div');
		span.style.position = "absolute";
		span.style.display = "inline-block";
		span.style.whiteSpace = "nowrap";
		span.style.overflow = "auto";
        span.style.whitSpace = "nowrap";

		
		var answerWidth = acorns.system.getWindowSize().width;
		span.style.width = (answerWidth - 165) + "px"; // 4 * (button width + button border + space between)
		span.style.top = SPACE + "px";

		span.style.fontSize = ICON + "px";
		span.id = "textDisplay";
		answerPanel.appendChild(span);
		return answerPanel;
   }
    
   /* Get the next sentence to process */
   this.getNextSentence = function()
   {
	   var index, choice;
	   var sentences =this.getCurrentCategory().getPoints();
	   var size = sentences.length;
	   
       // Get number of sentences available for this category
	   var ordered = [];
	   sentenceNo = -1;
	   
	   // If there are no sentences, create a scramble the list with all of them
	   if (free.length==0)
	   {
		   for (choice = 0; choice<size; choice++)
			  ordered.push(choice);
		   
		   while (ordered.length>0)
		   {
			   index = Math.floor((Math.random() * ordered.length));
			   sentenceNo = ordered[index];
			   ordered.splice(index, 1);
			   free.push(sentenceNo);
		   }
	   }

	   sentenceNo = free[0];
	   free.splice(0,1);
	   var sentence = sentences[sentenceNo];
	   
 	   var indigenous = options.getOptions()["select"];
	   phrases = this.createPhraseList(sentence, indigenous);
	   return sentence
   }

   /** Get the current sentence being used in the lesson */
   this.getCurrentSentence = function()
   {
	   if (sentenceNo == -1)
		    return this.getNextSentence();

		var sentences = this.getCurrentCategory().getPoints();
		return sentences[sentenceNo];
   }
   
   
   // Return true if there are more lessons in the category, zero otherwise
   this.processNextSentence = function(parent, object)
   {
	   	if (free.length)
		{
			this.getNextSentence();
			acorns.play(false);
		}
		else
		{
			var categoryText = category.getSentence();
			var title = "You've completed the " + categoryText + " category";
			this.feedback(parent, object, CORRECT, title, 
				function(text) 
				{
					if (text.indexOf("Continue")===0)
					{
						object.getNextCategory();
						acorns.play(true);
					}
					else
					{
						acorns.play(true);
					}
				}, false);
 		}
   }

   // Place current sentence at the end of the free list
   this.retryCurrentSentence = function()
   {
	   if (sentenceNo >= 0) free.push(sentenceNo);
	   sentenceNo = -1;
   } 
 

   this.resetCategory = function(num) 
   {
	   sentence = null;
	   free = [];
	   var categoryNo = 0;
	   if (num)
		   categoryNo = num;
	   
	   options.setCategoryNo(num);
   }
 
   this.getCurrentCategory = function()
   {
	   var categoryNo = options.getCategoryNo();
		if (isNaN(categoryNo) || categoryNo<0 || categoryNo >= categoryList.length) 
	   {
		   this.resetCategory(0);
	   }
	   return categoryList[categoryNo];
   }
 
   this.getNextCategory = function()
   {
	   var categoryNo = options.getCategoryNo();
	   categoryNo++;
	   this.resetCategory(categoryNo);
	   return this.getCurrentCategory();
   }
}

function MagnetGame(acornsObject, data)
{
	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [RESET, GAP, DIFFICULTY, GAP, FONT, GAP]);
	var category = new CategoryRelatedPhrases(acorns,data,options);

	var BORDER = 20;
	
    this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseRelatedCategory(acorns, lessonDom, lessonData); 
		options.setFontSize(lessonData.environment.getFontSize());
	}

	// Determine if a magnet contains a fully reconstructed sentence
	var getCompleteSentence = function(magnet)
	{
		if (!magnet) return null;
		
		var text = magnet.innerHTML;
		if (!magnet._source || !text || text.length==0) return null;
		
		var sentence = magnet._source.getPoint().getSpell();
		var formatted = category.formatPhrasesForDisplay(sentence, true, true, true);
		if (text == formatted) return text;
		return null;
	}
	
	// Determine if div intersects another
	var findIntersectingElement = function(element)
	{
		var main = document.getElementById("main");
		if (!main.hasChildNodes()) return false;
		
		var elementRect = {x: parseInt(element.style.left,10), y: parseInt(element.style.top,10)
									, width: element.clientWidth, height: element.clientHeight };
		
		var child, childRect, leftRect, rightRect, topRect, bottomRect;
		for (var c=0; c<main.childNodes.length; c++)
		{
			child = main.childNodes[c];
			childRect = {x: parseInt(child.style.left,10), y: parseInt(child.style.top,10)
								, width: child.clientWidth, height: child.clientHeight};
			
			// Don't count intersection with itself
			if (child == element) continue;
			
			// Ignore display fields
			if (child.id.indexOf("data") == 0) continue;
			
			// Check if child overlaps horizontally
			if (elementRect.x < childRect.x) 
				 { leftRect = elementRect; rightRect = childRect; }
			else { leftRect = childRect; rightRect = elementRect; }
			
			if ( leftRect.x + leftRect.width < rightRect.x) continue;
			
			// Check if child overlaps vertically
			if (elementRect.y < childRect.y)
				 { topRect = elementRect; bottomRect = childRect; }
			else { topRect = childRect; bottomRect = elementRect; }
			
			if ( topRect.y + topRect.height < bottomRect.y ) continue;
			
			return child;
		}
		return false;
	}
	
	var intersectionSide = function(lastTouch, dropMagnet)
	{
		var centerDropMagnet = parseInt(dropMagnet.style.marginLeft, 10) + parseInt(dropMagnet.style.marginRight, 10) 
		                          + parseInt(dropMagnet.style.left,10) + parseInt(dropMagnet.clientWidth,10) / 2;
		
		return (lastTouch.x < centerDropMagnet);
	}

	var firstTouch = undefined;
	var displayHandler = function(element)
	{
		try
		{	
			var sentence = getCompleteSentence(element);
			if (sentence === null) return;

			var point = element._source.getPoint();
			if (!point) return;
			
			var categoryNo = point.getCategory();
			var categoryList = category.getCategoryList();
			var currentCategory = categoryList[categoryNo];
			var picture = currentCategory.getPicture();
			var audio = currentCategory.getAudio();

			var saveSpell = point.getSpell();
			var formatted = category.formatPhrasesForDisplay(saveSpell, true, true, true);
			point.setSpell(formatted);
			acorns.widgets.makePointDisplay(element, point, lessonData, picture, audio);
			point.setSpell(saveSpell);
		} catch (err) {console.log(err.message); }	
	}
	
	// Grab magnet for dragging
	var grabMagnet = function(element, lastTouch) //event)
	{
		if (!element || element.name != "magnet") return;
			
		var offsetPos = {x: parseInt(element.style.left,10), y: parseInt(element.style.top,10) };
		var left = offsetPos.x + lastTouch.x - firstTouch.x;
		var top  = offsetPos.y + lastTouch.y - firstTouch.y;
		setPosition(element, left, top);
	}
	
	// Process mouse down or touch start
	var mouseDownHandler = function(event)
	{
		document.body.style.cursor = "move";
		if (event==null) event = window.event;
		
		var target = (event.target) ? event.target : event.srcElement;
		if (target.id == "") target.id = "moving"
		if (event.dataTransfer && event.dataTransfer.setData) event.dataTransfer.setData("text", target.title);
		firstTouch = acorns.system.getCoords(event);
	}
	
	// Set position of element and adjust if goes past the right or bottom
	var setPosition = function(element, left, top)
	{
		var main = document.getElementById("main");
		var size = { width: main.clientWidth - BORDER, height: main.clientHeight - BORDER };

		if (element.clientWidth + left >= size.width)  left = size.width - element.clientWidth;
		if (left < 0) left = 0;
	    if (element.clientHeight + top >= size.height) top = size.height - element.clientHeight;
		if (top < 0) top = 0;
		
		element.style.position = "absolute";
		element.style.left = left + 'px';
		element.style.top = top + 'px';
	}
	
	// Drag magnets about the display
	var mouseMoveHandler = function(event)
	{
		if (event==null) event = window.event;
		return false;
	}
	
	// Release magnet and join if appropriate
	var mouseUpHandler = function (event)
	{
		document.body.style.cursor = "default";
		
		if (event==null) event = window.event;
		var target = (event.target) ? event.target : event.srcElement;

		var dragMagnet = document.getElementById("moving");
		if (!dragMagnet || dragMagnet.name != "magnet") return;
		dragMagnet.id = "";

		// Drop magnet at new position
		var lastTouch = acorns.system.getCoords(event, 3);
		grabMagnet(dragMagnet, lastTouch);

		var sentence = getCompleteSentence(dragMagnet);
		if (sentence !== null) 
		{
			displayHandler(dragMagnet);
		    return;
        }
		
		var top, left, newMagnet, point;
		var dropMagnet = findIntersectingElement(dragMagnet);
		if (dropMagnet)
		{
			var left = intersectionSide(lastTouch, dropMagnet);
			var joinPhrase;
			if (left)
				joinPhrase = category.join(dragMagnet._source, dropMagnet._source, " ");
			else
				joinPhrase = category.join(dropMagnet._source, dragMagnet._source, " ");

			if (joinPhrase)
			{
				var main = document.getElementById("main");
				point = joinPhrase.getPoint();
				main.removeChild(dragMagnet);

				newMagnet = category.makeMagnet(joinPhrase, null, false, "y");
				left = parseInt(dropMagnet.style.left,10);
				top = parseInt(dropMagnet.style.top,10);
				main.removeChild(dropMagnet);

				main.appendChild(newMagnet);
				setPosition(newMagnet, left, top);
				
				sentence = getCompleteSentence(newMagnet);
				if (sentence !== null)
				{
					acorns.feedbackAudio("correct");
				}
			}
			else acorns.feedbackAudio("incorrect");
		}
	}
	
    this.play = function(reset) 
	{
		var main = document.getElementById("main");

		if (reset)
		{    
			var categoryList = category.getCategoryList();
			var points, choices = [], index, choice, sentence, which;
			for (var choice=0; choice<categoryList.length; choice++) 
			{
			   points = categoryList[choice].objectList;
			   for (var index=0; index<points.length; index++)
			   {
					choices.push(choice * 1000 + index );
			   }
			}
			
			var level = options.getDifficultyLevel();
		    for (var i=0; i<level.difficulty && choices.length>0; i++)
			{
				index = Math.floor(Math.random() * choices.length);
				choice = Math.floor(choices[index] / 1000);
				which = Math.floor(choices[index] % 1000);
				
				if (index == choices.length - 1) choices.pop();
				else choices[index] = choices.pop();

				points = categoryList[choice].objectList;
				point = points[ which ];
				point.setCategory(choice);
				category.displayMagnets(main, true, true, null, point);
			}
		}
	}
	
	// For lesson to be playable, there must be at least one sentence with at least two words.
    this.isPlayable = function(layer) 
	{
		var layerData = lessonData.objectList[layer-1];
		if (layerData == undefined) return false;
		
		return category.isPlayable(layer, false, true); 
	}
	
	// Just add the mouse handlers
    this.configurePlayPanel = function(panel) 
	{
		acorns.system.addListener(document.body, "mousedown", mouseDownHandler);
		acorns.system.addListener(document.body, "mousemove", mouseMoveHandler);
		acorns.system.addListener(document.body, "mouseup", mouseUpHandler);
		document.onselectstart = function(e)
		{
			e.preventDefault();
			return false;
		}
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Magnet Games</h3>");
		buffer.append("<p>When you execute a Magnet Game, you will see a series of magnets displayed");
		buffer.append(" randomly about the display.");
		buffer.append(" These magnets contain words of a series of sentences in the indigenous language.");
		buffer.append(" Some of the magnets will be on top of others, but don't be concerned, this is normal.</p>");
		
		buffer.append("<p>Your job is to drag magnets and then drop them onto another magnet");
		buffer.append(" containing an adjacent word of the sentence.");
		buffer.append(" You drag magnets by pressing the mouse on them and while keeping the mouse depressed");
		buffer.append(" moving the mouse.");
		buffer.append(" You drop by releasing the mouse when you get to the desired place on the display.");
		buffer.append(" On touch screens, touch the magnet, move your finger to the drop location, and then release.</p>");
		
		buffer.append("<p>If you release a magnet over one that does not have an adjacent word of the sentence,");
		buffer.append(" you will hear a negative feedback audio (a sqwalk sound is the default).");
		buffer.append(" Otherwise, you will see a larger magnet form that combines the one you dragged with");
		buffer.append(" the one that you dropped onto.");
		buffer.append(" Continue this process until you completely reconstruct the sentences.");
		buffer.append(" When a sentence is fully reconstructed, you will hear a positive feedback");
		buffer.append(" (a bird tweet is the default).");
		buffer.append(" You can then click on the reconstructed magnet to hear prerecorded audio,");
		buffer.append(" see a picture (if one exists), and view descriptive text.");
		buffer.append(" Clicking on the display box that contains the text plays back");
		buffer.append(" additional audio (if recorded).");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" If you click on this button, you'll see a pop up menu with Magnet Game lesson options.");
		buffer.append(" The reset option restarts the game and causes");
		buffer.append(" a new set of magnets to appear.");
		buffer.append(" Other options enable you to alter the difficulty of the game or change the font size");
		buffer.append(" of the text appearing within the magnets.");
		buffer.append(" Increasing the difficulty level means that more magnets will display;");
		buffer.append(" Similarly, a lower difficulty level means that less magnets will display.");
        buffer.append("</p>");		   
    
		return buffer.toString();	
	}
	
	this.stop = function() 
	{
		acorns.system.removeListener(document.body, "mousedown", mouseDownHandler);
		acorns.system.removeListener(document.body, "mousemove", mouseMoveHandler);
		acorns.system.removeListener(document.body, "mouseup", mouseUpHandler);
		try
		{
			document.onselectstart = undefined;
		}
		catch (e) {}
	}
	
	this.getOptions = function() { return options; }

}   // End of MagnetGame lesson class

function QuestionsandAnswers(acornsObject, data)
{
	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [RESET, GAP, DIFFICULTY, GAP]);
	var category = new CategoryRelatedPhrases(acorns, data, options);
	
	var CELL_WIDTH = 400, CELL_HEIGHT = 360, PICTURE_HEIGHT = 250, ICON_WIDTH = 20;
	
	var cells = undefined; 		   // Number of grid cells
	var categoryList = undefined;  // Category objects
	var cellContents = undefined;  // Mapping from cells to category contents.
	var bestPoint = undefined;     // The closest answer to the student's (if was close)
	var audioArray = [];           // List of points with audio files
	
	// Perform spell checks to see determine accuracy of the user's answer
	var verifyAnswer = function(points, target)
	{
		var point, ratio;
		var json, result, bestResult = INCORRECT, bestRatio = 1.0;
		
		bestPoint = undefined;
		for (var pointNo=0; pointNo<points.length; pointNo++)
		{
			point = points[pointNo];
			
			if (window.audioTools)
			{
				json = "[\"" + point.getAudio() + "\"]"; 
				result = window.audioTools.compare(json);				
				if (result==CORRECT) 
				{
					bestPoint = point;
					category.updateScore(CORRECT);
					return CORRECT;
				}
				
				if (result == CLOSE) 
				{
					bestPoint = point;
					return CLOSE;
				}
			}
			
			var result = category.checkAnswer(target, "y", point, true, pointNo);
			if (pointNo==0 || result[1] < bestRatio) 
			{
				bestPoint = point;
				bestResult = result[0];
				bestRatio  = result[1];
				
				if (result[0] == CORRECT)
				{
					category.updateScore(CORRECT);
					return CORRECT;
				}
			}
		}

		if (bestResult<0)
		{
			acorns.widgets.showMessage("Unexpected comparison: value = " + bestResult);
			bestResult = INCORRECT;
		}
		
		category.updateScore(bestResult);
		return bestResult;
	}
	
	var controlHandler = function(event, element)
	{
		var audioHandler = window.audioTools;

		element.style.borderStyle = "inset";
		// Parent Node is the icon button; grandparent is the cell 
		var button = element.parentNode.parentNode;
		var categoryContents = cellContents[button.id];
		var points = categoryContents.getPoints();
	    var alt = element.getAttribute("alt");
		var point = points[0];
		if (bestPoint != undefined) point = bestPoint;
		var question = categoryContents.getSentence();

		switch (alt)
		{
			case "playanswer":
				point.playAudio();
				break;
			case "slowdown":
				point.playAudioSlow();
				break;
			case "answers":
				category.feedback(button, category, HELP, "Questions and Answers", null, true, categoryContents);
				//acorns.widgets.makePointDisplay(button, point, lessonData, undefined, undefined, question);
				break;
			case "check":
				var id = "text" + (button.id).substring(6);
				var inputField = document.getElementById(id);
				var data = inputField.value;
				switch (verifyAnswer(points, data))
				{
					case INCORRECT:
						acorns.feedbackAudio("incorrect");
						break;
					case CLOSE:
						acorns.feedbackAudio("spell");
						break;
					case CORRECT:
						acorns.feedbackAudio("correct");
						break;
				}
				break;
				
			case "play":
				var title = element.getAttribute("title");
				if (title.substring(0,4)=="Hear")
				{
					var audio = categoryContents.getAudio();
					acorns.playAudio(audio); 
					break;
				}
				
			default:
				acorns.beep();
		}
		
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}

	// Create the panel with a particular question to insert into the grid
	var makeCellPanel = function(cell, categoryChoice, cellChoice)
	{
		var picture = categoryChoice.getPicture();
		var picture_height = PICTURE_HEIGHT, cell_height = CELL_HEIGHT;
		if (acorns.system.isMobilePhone()) 
		{
			picture_height -= 35;
			cell_height = cell_height * 3 / 4;
		}

		cell.style.height = picture_height + "px";
		if (picture) picture.centerAndScalePicture(cell);
		cell.style.height = cell_height + "px";
		cell.firstChild.style.marginTop = "35px";
		
		
		var div = document.createElement("div");
		div.style.position = "absolute";
		div.style.width = "100%";
		div.style.left = "0px";
		div.style.height = (ICON_WIDTH + 5) + "px";
		div.style.overflow = "hidden";
		div.style.fontSize = Math.floor(12 * acorns.system.getFontRatio()) + "px";
		div.style.top = "0px";
		div.style.textAlign = "left";
		lessonData.environment.setColors(div);
		cell.appendChild(div);

		var button = acorns.widgets.makeIconButton
			("play", "Hear the question", controlHandler , 20);
		button.style.position = "absolute";
		button.style.top = "0px";
		button.style.right = "0px";
		div.appendChild(button);
		
		var span = document.createElement("span");
		var sentence = categoryChoice.getSentence();
		span.style.width = "90px";
		span.innerHTML = sentence;
		span.title = span.innerHTML;
		div.appendChild(span);

		div = document.createElement("div");
		div.style.position = "absolute";
		div.style.textAlign = "center";
		div.style.bottom = "0px";
		div.style.left = "0px";
		cell.appendChild(div);
		
		var input = document.createElement("input");
		input.setAttribute("type", "input");
		input.id = "text " + cellChoice;
		input.style.width = "95%";
		var point = categoryChoice.getPoints()[0];
		point.applyFont(input);
		var language = point.getLanguage();
		var handler = new KeyboardHandler(input, language);
		div.appendChild(input);
		
		var button = acorns.widgets.makeIconButton("play", "Play an answer", controlHandler);
		button.alt = "playanswer";
		button.style.marginRight = "5px";
		div.appendChild(button);

		if (!acorns.system.isOldIE())
		{
			button = acorns.widgets.makeIconButton("slowdown", "hear an answer more slowly", controlHandler);
			button.style.marginRight = "5px";
			div.appendChild(button);
		}
		
		button = acorns.widgets.makeIconButton("answers", "See an answer", controlHandler);
		button.style.marginRight = "5px";
		div.appendChild(button);
		
		button = acorns.widgets.makeIconButton("check", "Verify (check) your answer", controlHandler);
		div.appendChild(button);
		
	}
	
    this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseRelatedCategory(acorns, lessonDom, lessonData); 
	}
	
    this.play = function(reset) 
	{
		if (reset)
		{
			var audioList = acorns.system.makeJSONAudio(audioArray);
			if (window.audioTools)
				window.audioTools.loadAudioFiles(audioList);
			
			var level = options.getDifficultyLevel();
			var numChoices = categoryList.length / (level.max - level.difficulty + 1);
			if (categoryChoices = 0) numChoices = 1;
			if (numChoices > cells) numChoices = cells;
			if (numChoices <1) numChoices = 1;
			
			var cellArray = [], choiceArray = [], index, choice, cellChoice;
			for (var choiceNo = 0; choiceNo < categoryList.length; choiceNo++) choiceArray.push(choiceNo);
			for (var cellNo = 0; cellNo < cells; cellNo++) cellArray.push(cellNo);
			
			cellContents = [];
			var min = Math.min(numChoices, categoryList.length);
			var id;
			for (var choiceNo = 0; choiceNo<min; choiceNo++)
			{
				index = Math.floor(Math.random()*choiceArray.length);
				choice = choiceArray[index];
				if (index < choiceArray.length - 1) choiceArray[index] = choiceArray[choiceArray.length - 1];
				choiceArray.pop();
				
				index = Math.floor(Math.random()*cellArray.length);
				cellChoice = cellArray[index];
				if (index < cellArray.length - 1) cellArray[index] = cellArray[cellArray.length - 1];
				cellArray.pop();
								
				id = "button " + cellChoice;			
				cell = document.getElementById(id);
				cellContents[id] = categoryList[choice];
				makeCellPanel(cell, categoryList[choice], cellChoice );
			}
		}
	}
	
    this.isPlayable = function(layer) 
	{
		var layerData = lessonData.objectList[layer-1];
		if (layerData == undefined) return false;
		
		categoryList = layerData.objectList;
		if (categoryList==undefined || categoryList.length==0) return false;

		audioArray = category.isPlayable(layer, true, false); 
		var playable = audioArray && audioArray.length > 0;
		return playable;
	}
	
    this.configurePlayPanel = function(panel) 
	{
	    var cell_width = CELL_WIDTH, cell_height = CELL_HEIGHT;
		if (acorns.system.isMobilePhone()) 
		{
			cell_width /= 2;
			cell_height = cell_height * 3 / 4;
		}

		cells = acorns.widgets.makeGrid(panel, cell_width, cell_height);
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Questions and Answers</h3>");
		buffer.append("<p>The purpose of Question and Answer lessons is for a fluent speaker to ask a question,");
		buffer.append(" which requires a student response; hopefully an appropriate answer.</p>");
		
		buffer.append("<p>The questions are grouped into panels representing, which display about the window.");
		buffer.append(" To hear a question, click the play button on the top right of one of the display panels.");
		buffer.append(" To the left of the play button is a label describing the question in the first language.");
		buffer.append(" If the question text is cut off, you can see it in full by moving your mouse (or finger)");
		buffer.append(" over the label.</p>");
		
		buffer.append("<p>Students attempt to properly answer the question,");
		buffer.append(" either verbally, or by typing the answer into the panel's text field.");
		
		buffer.append(" A verbal response is possible using the record, play, and playback icons");
		buffer.append(" (if audio recording is allowed on your computer, phone, or tablet device).</p>");

		buffer.append("<p>There is an icon with a green check mark on the bottom right.");
		buffer.append(" Click this icon to verify the answer;");
		buffer.append(" the program will give appropriate feedback indicating");
		buffer.append(" if the response is correct, close, or way off.</p>");
		
		buffer.append("<p>Students can discover an appropriate answer by clicking on the panel's blue icon");
		buffer.append(" containing a white question mark.");
		buffer.append(" Another panel will popup containing gloss and indigenous text,");
		buffer.append(" along with a play button to hear one of the possible correct answers.</p>");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" If you click on this button,");
		buffer.append(" you'll see a pop up that provides a Question and Answer lesson reset option.");
		buffer.append(" This option restarts the game and causes");
		buffer.append(" a new set of question and answers to appear at random positions on the display.");
		buffer.append(" There are also options to adjust the difficulty level.");
		buffer.append(" A higher difficulty level means that more questions will show on the frame");
		buffer.append(" if the lesson is configured to contain many question possibilities.");
		buffer.append(" Similarly, a lower difficulty level means that less questions will display.</p>");
		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }

}   // End of QuestionsandAnswers lesson class

function MissingWord(acornsObject, data)
{
	var CELL_WIDTH = 190, CELL_HEIGHT = 220, PICTURE_HEIGHT = 110, ICON_WIDTH = 20;

	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [RESET, CATEGORY, GAP, TOGGLE, SELECT, GAP, FONT, GAP]);
	var category = new CategoryRelatedPhrases(acorns, data, options);

	var BUTTON_FONT = Math.ceil(20 * acorns.system.getFontRatio());
	var SPECIALS = category.getSpecials();
	
	var categoryList = undefined;  // Category objects
	var cellContents = undefined;  // Mapping from cells to category contents.
	var audioArray = [];           // List of points with audio files
	
    // Determine if the entered text is correct one.
	var textHandler = function(event)
	{
		if (!event) event = window.event;
		if (!event || event.keyCode!=13) return;
		category.verify();
	}
	
	var makeButtonPanel = function()
	{
		var buttonPanel = document.createElement("div");
		buttonPanel.style.position = "relative";
		buttonPanel.style.overflow = "auto";
		buttonPanel.style.fontFamily = "Verdana";
		buttonPanel.style.marginTop = "15px";
		buttonPanel.style.lineHeight = "0.7";
		buttonPanel.id = "buttonPanel";
    	
    	var rowPanel, button, r, c, index, row, column;
		var rows = SPECIALS.length;
    	for (r=0; r < rows; r++)  
    	{
			row = SPECIALS[r];
    		rowPanel = document.createElement("div");
			rowPanel.style.display = "inline";
			rowPanel.style.margin = "5px";
			
    		for (c=0; c<row.length; c++)
    		{
				column = row[c];
    			for (index = column[1]; index <= column[2]; index++)
    			{
					button = document.createElement("input");
					button.type = "button";
					button.style.fontSize = BUTTON_FONT + "px";
				    button.style.borderStyle = "outset"
					
					var printable = String.fromCharCode(index);
					if (printable == NaN) continue;
					
					button.value = printable;
					acorns.system.addListener(button, "click", 
						function(e) 
						{
							if (e==null) event = window.event;
							if (e.preventDefault) e.preventDefault();
							
							var target = (event.target) ? event.target : e.srcElement;
							target.style.borderStyle = "inset";
							
							var replaceText = target.value;
							var input = document.getElementById('MissingWordInput');
							var oldText = input.value;
							
							var startSelect = input.selectionStart;
							var endSelect = input.selectionEnd;
							
							if (startSelect == undefined)
							{
								newText = oldText + replaceText;
							}
							else if (endSelect == undefined)
							{
								newText = oldText.substring(0,startSelect) + replaceText 
											+ oldText.substring(startSelect);
							}
							else
							{
								newText = oldText.substring(0,startSelect) + replaceText 
											+ oldText.substring(endSelect);
							}
											
							input.value = newText;
							input.focus(); 
							setTimeout(function() { target.style.borderStyle = "outset" }, 250);

						}
					);
    	    		rowPanel.appendChild(button);
    			}   // End of column loop
    		}   // end of row loop
    		buttonPanel.appendChild(rowPanel);
    	}   // special table processing
    	return buttonPanel;
    }   // end of makeButtonPanel()
	
	this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseRelatedCategory(acorns, lessonDom, lessonData); 
		options.setFontSize(lessonData.environment.getFontSize());
	}
	
    this.play = function(reset, changeLesson) 
	{
		if (changeLesson)
		{
			category.resetCategory(0);
		}

		var main = document.getElementById("main");
		
		if (reset)
		{ 
			var categoryNo = options.getCategoryNo();
			category.resetCategory(categoryNo);
			category.getNextSentence();
		}
		
		var textDisplay = document.getElementById("textDisplay");
		var display = options.getOptions()["toggle"];
		var select = options.getOptions()["select"];
		if (select === display)
		{
			var indigenous = (display === "y") ? "n" : "y";
			var text = category.formatTextField(indigenous);
			textDisplay.innerHTML = text;
		}
		
		var sentence = category.getCurrentSentence();
		var text = sentence.getGloss();
		if (select == "y")
		{
			text = sentence.getSpell();
			text = category.formatPhrasesForDisplay(text, true, true, true);
		}
		
		var words = text.split(" ");
		var index = Math.floor( Math.random() * words.length );
		var i, prefix ="", middle = "", suffix = "";
		for (i=0; i<index; i++)
			prefix += words[i] + " ";
		for (i=index+1; i<words.length; i++)
			suffix +=" " + words[i];
		for (i=0; i<words[index].length; i++)
			middle += "\u00A0"
		
		var displayPanel = document.getElementById("MissingWordDisplay");
		var prefixElement, suffixElement, inputElement;
		while (displayPanel.firstChild) { displayPanel.removeChild(displayPanel.firstChild); }
				
		prefixElement = document.getElementById("prefix");
		prefixElement.value = acorns.system.trim(prefix);
		inputElement = document.getElementById("MissingWordInput");
		inputElement.value = "";
		if (select === "y") 
		{ 
			sentence.applyFont(inputElement); 
			var language = sentence.getLanguage();
		    var handler = new KeyboardHandler(inputElement, language);
		}

		suffixElement = document.getElementById("suffix");
		suffixElement.value = acorns.system.trim(suffix);

		if (prefix.length > 0) 
		{
			displayPanel.appendChild( category.makeMagnet(prefix, sentence, false, select));
		}
		displayPanel.appendChild( category.makeMagnet(middle, sentence, false, select));
		if (suffix.length > 0) 
		{
			displayPanel.appendChild( category.makeMagnet(suffix, sentence, false, select));
		}
		
		mainHeight = main.clientHeight;
			
		var buttonPanel = document.getElementById("buttonPanel");
		var elemRect = buttonPanel.getBoundingClientRect();
		buttonPanel.style.height = (main.clientHeight - elemRect.top) + "px";
		
		setTimeout(function() { sentence.playAudio() }, 250);
	}

    this.isPlayable = function(layer) 
	{
		var layerData = lessonData.objectList[layer-1];
		if (layerData == undefined) return false;
		
		categoryList = layerData.objectList;
		if (categoryList==undefined || categoryList.length==0) return false;

		return category.isPlayable(layer, false, false); 
	}
	
	this.configurePlayPanel = function(panel) 
	{
		// Create panel of user controls
		var answerPanel = category.createAnswerPanel();
		panel.appendChild(answerPanel);
		
		// Create panel to display sentence with missing word
		var displayPanel = document.createElement("div");
		displayPanel.style.fontSize = "large";
		displayPanel.style.textAlign = "center";
		displayPanel.id = "MissingWordDisplay";
		panel.appendChild(displayPanel);
		displayPanel.innerHTML = "first part" + "xxxxxxx" + "last part";
		
		// entry panel for entering the missing word
		var entryPanel = document.createElement("div");
		panel.appendChild(entryPanel);
		
		var label = document.createElement("span");
		label.innerHTML = "Enter missing word here: ";
		label.style.fontSize = "xx-large";
		panel.appendChild(label);
		
		var sentencePanel = document.createElement("span");
		panel.appendChild(sentencePanel);
		sentencePanel.id = "sentencePanel";
		
		var sentencePrefix = document.createElement("input");
		sentencePrefix.id = "prefix";
		sentencePrefix.style.display = "none";
		sentencePanel.appendChild(sentencePrefix);
		
		var inputField = document.createElement("input");
		inputField.id = "MissingWordInput";
		inputField.style.fontSize = "x-large";
		inputField.onkeyup = textHandler;
		sentencePanel.appendChild(inputField);
		
		var sentenceSuffix = document.createElement("input");
		sentenceSuffix.id = "suffix";
		sentenceSuffix.style.display = "none";
		sentencePanel.appendChild(sentenceSuffix);
		
		var buttonPanel = makeButtonPanel(panel.clientHeight);
		panel.appendChild(buttonPanel);
		
		var elemRect = buttonPanel.getBoundingClientRect();
		buttonPanel.style.height = (panel.clientHeight - elemRect.top) + "px";
	}


	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Missing Word Lesson</h3>");
		buffer.append("<p>Missing Word lessons playback indigenous audios and then require the student to enter a missing word from the");
		buffer.append(" sentence that they hear. These lessons group various sentences into particular categories (like food, travel, interests, ");
		buffer.append(" etc.). The student responds to each sentence, one-by-one. When finished they will have the option to repeat the category ");
		buffer.append(" or to move onto the next. A running score provides the student with feedback to show how they are doing.");
		buffer.append("</p>");

		buffer.append("<p>");
		buffer.append("For each sentence of a category, the student hears an audio and sees the display of the sentence with a randomly");
		buffer.append(" chosen word missing. Their job is to type in the missing word. Buttons on the bottom of the display can be clicked ");
		buffer.append(" (or touched) to insert language accents in case the indigenous keyboard is not set up.");
		buffer.append("</p>");

		buffer.append("<p>There are a set of buttons at the top left of the display. These respectively are used to repeat the playback,");
		buffer.append(" to slow down the audio for eaier comprehension, to see the answer, and to check the answer. When the answer is checked,");
		buffer.append(" the student will be provided with feedback as to whether it is correct. If incorrect, one can");
		buffer.append(" proceed normaly, or skip the question. When the answer is nearly correct, or only different by accent characters, the ");
		buffer.append(" answer is shown, but it will be counted as correct. After all questions in a category are handled, one can repeat working with the");
		buffer.append(" current category, or they can move onto the next. A running score of correct versus incorrect answers also");
		buffer.append(" displays.");
		buffer.append("</p>");

		buffer.append("<p>You can click on the lesson options (icon with the 'i' in it) to configure how this lesson executes (as described below)");
		buffer.append("<ul>");
		buffer.append("	<li>Resetting the game and starting over at the first category.</li>");
		buffer.append("	<li>Configure the way the student interacts with the lesson. There are four possibilities.");
		buffer.append("		<ul>");
		buffer.append("			<li>Playback the audio. The student enters the missing word for the sentence that was heard, </li>");
		buffer.append("			<li>Playback the audio. The student enters the gloss language translation of the ");
		buffer.append("				missing word from the sentence that was heard.</li>");
		buffer.append("			<li>Playback the audio and see the indigenous sentence. The student enters the gloss"); 
		buffer.append("				translation of missing word from the sentence that was heard.</li>");
		buffer.append("			<li>Playback the audio and see the gloss sentence. The student enters the indigenous word from the");
		buffer.append("				sentence that was heard.</li>");
		buffer.append("		</ul>");
		buffer.append("	</li>");
		buffer.append("	<li>Increase or decrease the font size used in the lesson.</li>");
		buffer.append("</ul>");
		
		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }
}	// End of Missing Word lesson

function Translate(acornsObject, data)
{
	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [RESET, CATEGORY, GAP, TOGGLE, SELECT, GAP, FONT, GAP]);
	var category = new CategoryRelatedPhrases(acorns, data, options);
	var pastePhrase = undefined;

	this.parse = function(parseObject, lessonDom)  
	{ 
		parseObject.parseRelatedCategory(acorns, lessonDom, lessonData); 
		options.setFontSize(lessonData.environment.getFontSize());
	}

	var processMagnet = function(e)
	{
		// return if no phrase to reposition
		if (!pastePhrase)  return;

		var sentencePanel = document.getElementById("sentencePanel");
		var children = sentencePanel.childNodes;
		var len = children.length, spot = -1;
		var current;
		
		var mousePoint = acorns.system.getCoords(e);
	
		// Find the place to insert the deleted phrase, 
		while (++spot<len)
		{
			current = acorns.system.getPosition(children[spot]);
			current = acorns.system.getPosition(children[spot]);
			if (mousePoint.y > current.top+current.height) continue;  // Find correct row
			if (mousePoint.x > current.left) continue; // Find correct column
			break;
		}

		var phrase, newPhrase, newMagnet, component;
		var indigenous = options.getOptions()["select"];
		if (spot>0)
		{
			try // Attempt to join with previous phrase
			{
				component = children[spot-1];
				phrase = component._source;
				newPhrase = category.join(phrase,  pastePhrase);

				if (newPhrase != null)
				{
					sentencePanel.removeChild(component);
					pastePhrase = newPhrase;
					spot--;
					len--;
				}
			}
			catch(err) {}
		}

		if (spot < len)
		{
			try   // Attempt to join with next phrase
			{
				component = children[spot];
				phrase = component._source;
				newPhrase = category.join(pastePhrase, phrase);
				if (newPhrase != null)
				{
					sentencePanel.removeChild(component);
					pastePhrase = newPhrase;
				}
			}
			catch(e) {}
		}
		
		newMagnet = category.makeMagnet(pastePhrase, null, false, indigenous, magnetHandler);
		len = children.length;
		if (len==0 || spot>=len) sentencePanel.appendChild(newMagnet);
		else sentencePanel.insertBefore(newMagnet, children[spot]);

		var phrasePanel = document.getElementById("phrasePanel");
		var p;
		var source = pastePhrase;
		var subPhrases = source.getEmbeddedPhrases();
		if (subPhrases.length == 0) subPhrases = [source];
		len = subPhrases.length;

		children = phrasePanel.childNodes; // Remove all of the matching phrases
		var cLen = children.length;
		for (p=0; p<len; p++)
		{
			for (c=0; c<cLen; c++)
			{
				if (children[c]._source == subPhrases[p])
				{
					phrasePanel.removeChild(children[c]);
					cLen--;
					break;
				}
			}
		}
		pastePhrase = undefined;
	}
	
	var magnetHandler = function(e)
	{
		if (!e) e = window.event;
		if (e.stopPropagation) { e.stopPropagation(); }
		else {  e.cancelBubble = true; } // IE8 and lower
		
		var sentencePanel = document.getElementById("sentencePanel");
		var phrasePanel = document.getElementById("phrasePanel");
		var indigenous = options.getOptions()["select"];
		var count;
		
		var source = (e.target) ? e.target : e.srcElement;
		
		var coords = acorns.system.getCoords(e);
		var elemRect = source.getBoundingClientRect();
		
		if ((coords.x < elemRect.left) ||( coords.x > elemRect.right))
		{
			processMagnet(e);
			return;
		}
		
		var parent = source.parentNode;
		var lastChild, component, phrase;
		if (parent == phrasePanel)
		{
			phrasePanel.removeChild(source);
			lastChild = sentencePanel.lastChild;
			if (lastChild)
			{
				try
				{
				newPhrase = category.join(lastChild._source, source._source);
					if (newPhrase)
					{
						sentencePanel.removeChild(lastChild);
						source = category.makeMagnet(newPhrase, null, false, indigenous, magnetHandler);
					}
				} catch(e) {}
			}
			sentencePanel.appendChild(source);
		}
		else // Unjoin the elements and redisplay in the phrasePanel
		{
			pastePhrase = source._source;
			sentencePanel.removeChild(source);
			var phrase = source._source;
			var embedded = phrase.getEmbeddedPhrases();
			count = embedded.length;
			var p;
			if (count==0)
			{
				phrasePanel.appendChild(source);
			}
			else
			{
				var indigenous = options.getOptions()["select"];
				for (p=0; p<count; p++)
				{
					var magnet = category.makeMagnet(embedded[p], null, false, indigenous, magnetHandler)
					phrasePanel.appendChild(magnet);
				}
			}	// end if/else
		}		// end append back to phrase panel
	}			// end of handler
	
	var phraseHandler = function(e)
	{
		if (!e) e = window.event;
		if (e.stopPropagation) { e.stopPropagation(); }
		else {  e.cancelBubble = true; } // IE8 and lower
		
		processMagnet(e);

	} 	// End of handler

    this.play = function(reset, changeLesson) 
	{
		if (changeLesson)
		{
			category.resetCategory(0);
		}
		
		if (reset)
		{ 
			var categoryNo = options.getCategoryNo();
			category.resetCategory(categoryNo);
			category.getNextSentence();
		}
				
		var phrasePanel = document.getElementById("phrasePanel");
		while (phrasePanel.firstChild) { phrasePanel.removeChild(phrasePanel.firstChild); }
		
		var sentencePanel = document.getElementById("sentencePanel");
		while (sentencePanel.firstChild) {sentencePanel.removeChild(sentencePanel.firstChild); }
	
		category.displayMagnets(phrasePanel, false, true, magnetHandler); 
		
		var textDisplay = document.getElementById("textDisplay");
		var display = options.getOptions()["toggle"];
		var select = options.getOptions()["select"];
		if (select === display)
		{
			var indigenous = (display === "y") ? "n" : "y";
			var text = category.formatTextField(indigenous);
			textDisplay.innerHTML = text;
		}

		var sentence = category.getCurrentSentence();
		setTimeout(function() { sentence.playAudio() }, 250);
	}		

	this.configurePlayPanel = function(panel)
	{
		category.getCurrentCategory();
		var answerPanel = category.createAnswerPanel();
		panel.appendChild(answerPanel);
			
		var sentencePanel = document.createElement("div");
		sentencePanel.style.borderStyle = "ridge";
		lessonData.environment.setColors(sentencePanel);
		sentencePanel.id = "sentencePanel";
		sentencePanel.style.width="100%";
		sentencePanel.style.height="35%";
		acorns.system.addListener(sentencePanel, "click", phraseHandler);
		panel.appendChild(sentencePanel);

		var phrasePanel = document.createElement("div");
		phrasePanel.id = "phrasePanel";
		phrasePanel.style.width="100%";
		phrasePanel.style.height="55%";
		panel.appendChild(phrasePanel);
	}

    this.isPlayable = function(layer) 
	{
		// Require recorded question, but not at least two word answer
		return category.isPlayable(layer, false, false); 
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Translate Lesson</h3>");
		
		buffer.append("<p>");
		buffer.append("Translate lessons playback indigenous audios and then require the student to contruct sentences that they");
		buffer.append(" hear. These lessons combine a series of sentences into a particular category (like food, travel, interests, etc.). The");
		buffer.append(" student responds to each sentence, one-by-one. When finished they will have the option to repeat the category or to");
		buffer.append(" move onto the next. A running score provides the student with feedback to show how they are doing.");
		buffer.append("</p>");

		buffer.append("<p>");
		buffer.append("For each sentence of a category, each word (or sub-word) is initially displayed separately (and randomly scrambled) on ");
		buffer.append(" the lower part of the display. Note though, that extra words that are not part of the sentence may appear at the bottom, ");
		buffer.append(" which the student should ignore. ");
		buffer.append("</p>");

		buffer.append("<p>The sentence is constructed by selecting (clicking or touching) each word. This causes the selected ");
		buffer.append(" word to append to the sentence being constructed at the top of the display. If the selection is incorrect, simply");
		buffer.append(" click where it appears at the top of the display and it will move back to the bottom part of the display. If it is in the ");
		buffer.append(" wrong place in the constructed sentence, first click to move it to the bottom and then click again between the words where ");
		buffer.append(" it should be, This will cause it will reappear in the correct position.");
		buffer.append("</p>");

		buffer.append("<p>There are a set of buttons at the top left of the display. These respectively are used to repeat the playback,");
		buffer.append(" to slow down the audio for eaier comprehension, to see the answer, and to check the answer. When the answer is checked,");
		buffer.append(" the student will be provided with feedback as to whether the constructed sentence is correct. If incorrect, one can");
		buffer.append(" proceed normaly, or skip the question. When the constructed sentence is nearly correct, the answer is shown, but it will ");
		buffer.append(" be counted as correct. After all questions in a category are handled, one can repeat working with the");
		buffer.append(" current category, or they can move onto the next. A running score of correct versus incorrect answers also ");
		buffer.append(" displays.");
		buffer.append("</p>");

		buffer.append("<p>You can click on the lesson options (icon with the 'i' in it) to configure how this lesson executes (as described below)");
		buffer.append("<ul>");
		buffer.append("	<li>Resetting the game and starting over at the first category.</li>");
		buffer.append("	<li>Configure the way the student interacts with the lesson. There are four possibilities.");
		buffer.append("		<ul>");
		buffer.append("			<li>Playback the audio. The student constructs the sentence that was heard, </li>");
		buffer.append("			<li>Playback the audio. The student constructs the gloss language translation of the"); 
		buffer.append("				sentence that was heard.</li>");
		buffer.append("			<li>Playback the audio and see the indigenous sentence. The student constructs the gloss ");
		buffer.append("				translation of what was heard.</li>");
		buffer.append("			<li>Playback the audio and see the gloss sentence. The student constructs the indigenous sentence ");
		buffer.append("				that was heard.</li>");
		buffer.append("		</ul>");
		buffer.append("	</li>");
		buffer.append("	<li>Increase or decrease the font size used in the lesson.</li>");
		buffer.append("</ul>");

		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }
}	// End of Translate lesson

function StoryBook(acornsObject, data)
{
	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [FONT, GAP]);
	
	// Data needed to process the lesson
	var picture = undefined;		// The story picture page
	var scale = undefined;			// The picture's scale factor
	var audio = undefined;			// The story's audio recording
	var text = undefined;			// The text for the story
	var points = undefined;			// Array of words with frame offsets
	var textDisplay = undefined;	// Component to display text
	var align = undefined; 			// "left" or "center"
	var language = undefined;		// "English" or an indigenous font
	var audioFormat = undefined;    // Format of the audio signal.
	var startTime = undefined; 		// Time stamp of last pause.
	var frames = undefined;         // Number of frames in the audio file.
	
	this.parse = function(parseObject, lessonDom)  
	{ parseObject.parseAudioLessonCategory(acorns, lessonDom, lessonData); }
	
    this.play = function(reset) 
	{
		if (reset)
		{
			acorns.setTimeUpdateListener( timeListener );
			textDisplay.style.fontSize 
				= Math.floor(options.getFontSize() * acorns.system.getFontRatio()) + "px";
		}
	};
    
	this.isPlayable = function(layer) 
	{
		picture = lessonData.paramList["image"];
		if (picture == undefined) return false;
		scale = picture.getScaleFactor();
		
		// Get layer 0, which has the audio object.
		var audioLayer = lessonData.objectList[0];
		if (audioLayer==undefined) return false;
		
		// Get audio filename (contained in the first point object)
		var audioPoint = audioLayer.objectList;
		if (audioPoint == undefined || audioPoint.length < 1) return false;
		
		audio = audioPoint[0].getAudio();
		if (audio == undefined || audio.length == 0) return false;
		audioFormat = audioPoint[0].getAudioFormat();
		frames = audioPoint[0].getFrames();
		
		// Get the points for the layer in question and set the alignment
		var layerData = lessonData.objectList[layer];
		if (layerData == undefined) return false;

		align = layerData.getAlign();
		language = layerData.getLanguage();
		points = layerData.objectList;
		if (points == undefined || points.length == 0) return false;
		
		text = layerData.paramList["text"];
		if (text == undefined || text.length == 0) return false;		
		return true;
	}
	
	var buttonHandler = function(event)
	{
		if (!event) event = window.event;

	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement;
		element.style.borderStyle = "inset";
		
		value = element.getAttribute("alt");
		switch (value)
		{
			case "play":
				if (startTime != undefined)
				{
					acorns.rewindAudio(new Date().getTime() - startTime);
				}
				acorns.playAudio(audio); 
				startTime = undefined;
				break;
			case "replay":
				if (!acorns.isAudioPaused()) acorns.beep();
				else if (startTime != undefined) acorns.beep();
				else startTime = new Date().getTime();
				break;
			case "pause":
				acorns.stopAudio(true); 
				break;
			case "stop":
				acorns.stopAudio();
				acorns.setTime(0);
				startTime = undefined;
				break;
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}
	
	var lastElement = undefined;
	var timeListener = function(audio)
	{
		var timeDuration = acorns.getTimeAndDuration(audio);
		if (!timeDuration) return;
		
		if (isNaN(timeDuration.duration)) 
		{
			if (frames)
				timeDuration.duration = frames;
			else
			{
				frames = points[points.length-1].getXY().y / audioFormat.rate;
				timeDuration.duration = frames;
			}
		}
		
		var rate = frames/ timeDuration.duration;
		var time = timeDuration.time;
		var pointNo = acorns.system.binarySearchPoints(points, time * rate);
		var element = document.getElementById("" + pointNo);

		if (time*rate >= points[points.length-1].getXY().y) 
		{
			if (lastElement) { lastElement.style.backgroundColor = "";	}
			lastElement = undefined;
			return;
		}
		
		if (element==lastElement) return;
		
		if (lastElement) { lastElement.style.backgroundColor = "";	}
		
		element.style.backgroundColor = "#FF9966";
		element.scrollIntoView(true);
		lastElement = element;
	}

    this.configurePlayPanel = function(panel) 
	{
		var SCROLL = 0, TOP = 10, HEIGHT_PCT = 70, WIDTH_PCT = 95;
		var width = panel.clientWidth, height = panel.clientHeight - SCROLL;

		var divWidth = Math.floor(width * WIDTH_PCT/100);
		var divHeight = Math.floor(height * HEIGHT_PCT/100);
		if (scale && scale<100) 
			divHeight = Math.floor(height * scale/100);
		var pictureSize = Math.min(divWidth, divHeight);
		var divLeft = (width - pictureSize)/2;
		var pictureComponent = acorns.widgets.makePictureButton (0, pictureSize, divLeft, null);
		var words;
		panel.appendChild(pictureComponent);
		picture.centerAndScalePicture(pictureComponent);
		pictureComponent.style.top = TOP + "px";
		
		var tips = ["replay the last part of the recording", "pause", "stop to start over", "play audio" ];
		var controls = [ "replay", "pause", "stop", "play" ];
		var players = document.getElementsByTagName("bgsound");
		if (players && players.length>0) 
		{
			controls.splice(0,2);
			tips.splice(0,2);
		}
		var block = acorns.widgets.makeButtonPanel(controls, tips, 0, pictureSize, 0, buttonHandler);
		pictureComponent.appendChild(block);
		
		textDisplay = document.createElement("div");
		textDisplay.style.position = "relative";
		textDisplay.style.top=(pictureComponent.offsetTop + pictureComponent.offsetHeight) + "px";
		textDisplay.style.left = ((width - width * 0.95)/2) + "px";
		textDisplay.style.width = (width * 0.95) + "px";
		if (align=="center") textDisplay.style.textAlign = "center";
		textDisplay.style.borderStyle = "outset";
		textDisplay.style.overflow = "auto";
		acorns.system.scrollableDiv(textDisplay);
		lessonData.environment.setColors(textDisplay);
		textDisplay.style.display = "inline-block";
        textDisplay.style.whitSpace = "nowrap";

		
		var thisText, pointCount = 0;;
		for (var pointNo=0; pointNo<points.length; pointNo++)
		{
			span = document.createElement("span");
			span.id = "" + pointCount++;
			acorns.system.applyFont(span, language);
			span.style.fontSize = options.getFontSize() + "pt";
			
			thisText = points[pointNo].getSpell();
			thisText = thisText.replace(/\n/g, "<br />");
			
			span.innerHTML = thisText;
			textDisplay.appendChild(span);
			if (pointNo< points.length - 1) textDisplay.appendChild(document.createTextNode(" "));
		}
		panel.appendChild(textDisplay);
		var top = textDisplay.offsetTop + textDisplay.clientTop;
		panel.appendChild(textDisplay);
		var top = textDisplay.offsetTop + textDisplay.clientTop;
		textDisplay.style.height = (panel.clientHeight - top - 5) + "px";
		textDisplay.style.maxHeight = (panel.clientHeight - top - 5) + "px";
		textDisplay.style.minHeight = (panel.clientHeight - top - 5) + "px";
	}

	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Story Book Lessons</h3>");
		
		buffer.append("<p>When you execute a Story Book, you will see a picture display, which that relates");
		buffer.append(" to the audio for one page of the story contained in the lesson.");
		buffer.append(" You will also see a series of control buttons near the bottom of the picture.");
		buffer.append(" These allow you to stop and replay the audio, pause and continue playing, or"); 
		buffer.append(" rewind the audio recording at the same speed at which it played. ");
		buffer.append(" The rewind control is useful when you want to back up the recording to rehear a word or");
		buffer.append(" phrase. To rewind, first click (or touch) pause, next click rewind, then click play.</p>");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" If you click on this button,");
		buffer.append(" you'll see a pop up that contains options to allow you to increase or decrease");
		buffer.append(" the font size of the story text.</p>");
 
		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }

}   // End of StoryBook lesson class

function HearandClick(acornsObject, data)
{
	var acorns = acornsObject;
	var lessonData = data;
   	var options = new Options(acorns, lessonData, [RESET, GAP, CONTINUOUS, GAP, DIFFICULTY, GAP]);

	var CELL_WIDTH = 75, CELL_HEIGHT = 75, TOP = 70;
	
	var audio = undefined;  	 // The story to display
	var points = undefined; 	 // The array of possible picture pause points
	var cells = undefined;  	 // The number of cells for pictures
    var audioFormat = undefined; // Audio format parameters
	
	var pausePoints = undefined;  // The current picture pause points
	var cellPoints = undefined;   // The current cells holding the pictures for pause points
	var pointCellMap = undefined; // The index into the cell objects, without duplicates
	
	var thisPoint = undefined;		// The current pause point
	var startTime = 0;              // Time starting the beginning of the current playback
	var frames = undefined;         // Number of frames in the audio file.

	
	// Set or remove a highlight from a grid element
	var setHighlight = function(cellNo, highlight)
	{
		// Remove the last highlighted cell
		if (cellNo==undefined) return;
		
		var cell = document.getElementById("button " + cellPoints[pointCellMap[cellNo]]);
		if (cell==undefined) return;
		if (highlight)
		{
			cell.style.borderColor = "red";
			cell.style.borderStyle = "inset";
		}
		else
		{
			cell.style.borderColor = "grey";
			cell.style.borderStyle = "outset";
		}
	}

	// Respond to clicks of the control buttons
	var buttonHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement;
		element.style.borderStyle = "inset";		
		var continuous = options.getOptions().continuous == "y";
		
		var value = element.getAttribute("alt");
		switch (value)
		{
			case "play":
				if (!acorns.isAudioPaused()) acorns.stopAudio(true);
				setHighlight(lastCell, false);
				if (thisPoint>=cellPoints.length)
				{
					thisPoint = 0;
					startTime = 0;
					if (continuous) acorns.playAudio(audio); 
					else setTimeout(function() { acorns.playAudio(audio); }, 1000);
					break;
				}
				
				if (!continuous) { thisPoint++; }
				startTime = acorns.getTime();
				acorns.playAudio(audio); 
				break;
				
			case "replay":
				if (!acorns.isAudioPaused()) acorns.stopAudio(true); 
					
				setHighlight(lastCell, false);
				acorns.setTime(startTime);
				acorns.playAudio(audio);
				break;
				
			case "pause":
				acorns.stopAudio(true); 
				break;
				
			case "answers":
				if (continuous) return; 
				if (!acorns.isAudioPaused())  acorns.stopAudio(true); 
				setHighlight(lastCell, false);
				setHighlight(thisPoint, true);
				lastCell = thisPoint;
				break;
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}

	this.parse = function(parseObject, lessonDom)  
	{ parseObject.parseAudioLessonCategory(acorns, lessonDom, lessonData); }

	// Determine if the clicked picture is the correct one.
	var pictureHandler = function(event)
	{
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement.parentNode;
		element.style.borderStyle = "inset";

		var continuous = options.getOptions().continuous == "y";
		if (continuous || thisPoint>=cellPoints.length) 
		{
			setTimeout(function() { element.style.borderStyle = "outset" }, 250);
			return;
		}

		var img = element.firstChild;
		if (img==undefined) return;
		
		var src = img.getAttribute("src");
		if (src.length==0)  return;

		acorns.stopAudio(true);
		var cell = document.getElementById("button " + cellPoints[pointCellMap[thisPoint]]);
		if (cell != undefined && cell.id == element.id)
		{
			acorns.widgets.updateScore(true);
			thisPoint++; 
			startTime = acorns.getTime();
			setTimeout( function() { acorns.playAudio(audio); }, 1500);
		}
		else 
		{
			acorns.feedbackAudio("incorrect", true);  // Use feedback player
			acorns.widgets.updateScore(false);
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}
	
	// Monitor the audio player
	var lastCell = undefined;
	var timeListener = function(audio)
	{
		var continuous = options.getOptions().continuous == "y";
		var timeDuration = acorns.getTimeAndDuration(audio);
		if (!timeDuration) return;
		
		if (isNaN(timeDuration.duration)) 
		{
			if (frames)
				timeDuration.duration = frames;
			else
			{
				frames = points[points.length-1].getXY().y / audioFormat.rate;
				timeDuration.duration = frames;
			}
		}
		
		// Get the current frame
		var rate = frames/ timeDuration.duration;
		var frame = timeDuration.time * rate;

		// If we are past the last point, no more pauses
		if (thisPoint >= pausePoints.length && !continuous)	
		{ 
			setHighlight(lastCell, false);
			return; 
		}
		// Get the frame to pause
		var point = points[pausePoints[thisPoint]];
		var endSpot = point.getXY().y;

		// If we are at or after the next play spot, pause the audio.
		if (frame < endSpot) return;
		if (frame >= endSpot)
		{
			acorns.stopAudio(true);
		}

		// Remove the last highlighted cell
		setHighlight(lastCell, false);
		
		// In Continouus mode, highlight the picture, pause, and then continue
		if (continuous)
		{
			setHighlight(thisPoint, true);
			lastCell =  thisPoint++;
			startTime = acorns.getTime();
			setTimeout(function() { acorns.playAudio(audio); }, 1000);
		}
	}
	
	var compareTo = function(a,b) { return a - b; }
	
    this.play = function(reset, changeOfLayer) 
	{
		if (reset)
		{
			var level = options.getDifficultyLevel();
			var numberChoices 
					= Math.floor(Math.min(points.length, cells) * level.difficulty  / (level.max-level.min+1));
			if (numberChoices==0) choices = 1;
			
			pausePoints = [], cellPoints = [], pointCellMap = [];
			var cellChoices = [], pointChoices = [], pictures = [];
			var point, picture, index, choice;
			
			for (var cellNo=0; cellNo<cells; cellNo++) cellChoices.push(cellNo);
			for (var pointNo = 1; pointNo<points.length; pointNo++)  pointChoices.push(pointNo);
			
			// Create list of sorted pause points
			for (var choiceNo=0; choiceNo<numberChoices; choiceNo++)
			{
				choice = Math.floor(Math.random() * pointChoices.length);
				pausePoints.push(pointChoices[choice]);
				if (choice!=(pointChoices.length-1)) pointChoices[choice] = pointChoices[pointChoices.length-1];
				pointChoices.pop();
			}
			pausePoints = pausePoints.sort(compareTo);

			// Index the pause points to a list of picture objects without duplicates
			for (var pauseNo = 0; pauseNo<pausePoints.length; pauseNo++)
			{
				point = points[pausePoints[pauseNo]];
				picture = point.getPicture();

				index = -1;
				for (var i=0; i<pictures.length; i++)
				{
					if (pictures[i].getSrc() == picture.getSrc())  
					{
						index = i;
						break;
					}
				}
		
				if (index == -1)
				{
					index = pictures.length;
					pictures.push(picture);
				}
				pointCellMap.push(index);
			}
			
			// Allocate the cells to display the pictures
			for (var choiceNo=0; choiceNo<pictures.length; choiceNo++)
			{
				choice = Math.floor(Math.random() * cellChoices.length);
				cellPoints.push(cellChoices[choice]);
				if (choice!=(cellChoices.length-1)) cellChoices[choice] = cellChoices[cellChoices.length-1];
				cellChoices.pop();
			}
			
			var cell, id;
			for (var pictureNo=0; pictureNo<pictures.length; pictureNo++)
			{
				id = "button " + cellPoints[pictureNo];			
				cell = document.getElementById(id);
				picture = pictures[pictureNo];
				picture.centerAndScalePicture(cell);
			}
			
			acorns.setTimeUpdateListener( timeListener );
			setHighlight(lastCell, false);
			// Start the story playing
			thisPoint = 0;
			startTime = 0;
			acorns.setTime(0);
		}		// End if reset
		
		acorns.playAudio(audio);
	}
	
    this.isPlayable = function(layer) 
	{
		// Get layer data
		var audioLayer = lessonData.objectList[layer - 1];
		if (audioLayer==undefined) return false;
		
		// Get the points for the layer containing the pause points and picture data
		points = audioLayer.objectList;
		if (points == undefined || points.length < 1) return false;

		// Get the audio recording for the current layer
		audio = points[0].getAudio();
		if (audio == undefined || audio.length == 0) return false;
		
		audioFormat = points[0].getAudioFormat();
		frames = points[0].getFrames();

		return true;
	}
	
    this.configurePlayPanel = function(panel) 
	{
		var cell_width = CELL_WIDTH;
		var cell_height = CELL_HEIGHT;
		if (acorns.system.isMobilePhone())  
		{
			cell_width = CELL_WIDTH/2;
			cell_height = CELL_HEIGHT/2;
		}
		cells = acorns.widgets.makeGrid(panel, cell_width, cell_height, TOP, pictureHandler);
		var players = document.getElementsByTagName("bgsound");
		if (players && players.length>0)
		{
			var h3 = document.createElement("h3");
			h3.setAttribute("align", "center");
			h3.innerHTML = "Sorry, Your browser does not support this lesson type. Try Firefox or Chrome.";
			panel.appendChild(h3);
		}
		else
		{
			var size = acorns.widgets.configureScorePanel(lessonData, panel, true);

			var controls = [ "replay", "answers", "pause", "play" ];
			var tips = ["replay the last part of the recording", "see the answer", "pause", "continue playing"];
			
			
			var left = (panel.clientWidth - size - 230)/2;
			var block = acorns.widgets.makeButtonPanel(controls, tips, left, 230, 0, buttonHandler);
			block.style.top = "10px";
			panel.appendChild(block);
		}
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Hear and Click Lessons</h3>");
		
		buffer.append("<p>When you execute a hear and click lesson, a series of pictures display,");
		buffer.append(" and an audio story plays, which relates to the pictures.");
		buffer.append(" Your job is to click (or touch) the correct picture when the playback pauses.");
		buffer.append(" The program acknowledges, so you know if you are correct.</p>");
		
		buffer.append("<p>At the top of the display, there are icon buttons that can help.");
		buffer.append(" If you want to rehear the last part of the recording again simply");
		buffer.append(" click (or touch) the replay button.");
		buffer.append(" Similarly, the button with the blue circle is for showing the answer (the correct picture highlights).");
		buffer.append(" There is also a button to pause the audio. ");
		buffer.append(" Finally, the play button continues the playback to the next pause point.</p>");
		
		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" If you click on this button,");
		buffer.append(" you'll see a pop up that contains options to allow you to reset the game,");
		buffer.append(" set the lesson to condinuous play mode, or to");
		buffer.append(" adjust the difficulty level.");
		buffer.append(" Reset causes a new set of pictures to display at random places");
		buffer.append(" and restarts the audio playback.");
		buffer.append(" Continuous play mode will cause the entire audio to play straight through,");
		buffer.append(" with a slight pause at the points that relate to each picture,");
		buffer.append(" when the appropriate picture highlights. ");
		buffer.append(" The difficulty level controls how many pictures display at a time.</p>");
		
		return buffer.toString();	

	}
	
	this.getOptions = function() { return options; }
}   // End of HearandClick lesson class

function HearandRespond(acornsObject, data)
{
	var MIN_POINTS = 7, MARGIN = 7, BUTTON_SIZE = 35
	var TOP = 2*BUTTON_SIZE + 2*MARGIN,  DIFF_PCT = .15, DELTA = 0.1;
	
	var acorns = acornsObject;
	var lessonData = data;
	var options = new Options(acorns, lessonData, [RESET, GAP, CONTINUOUS, GAP, DIFFICULTY, GAP, FONT, GAP]);

	var audio = undefined;  	   // The story to display
	var points = undefined; 	   // The array of possible picture pause points
	var textDisplay = undefined;   // Component to display text
    var audioFormat = undefined;   // Audio format parameters
	var align = undefined; 		   // "left" or "center"
	var language = undefined;	   // "English" or an indigenous font
	
	var pointChoices = undefined;  // The blank words
	var thisPoint = undefined;     // The current pause point
	var textComponent = undefined; // The text field into which the student enters answers
	var startTime = 0;
	var frames = undefined;         // Number of frames in the audio file.

	var pausePoints = undefined;  // The lesson's pause points
	
	var replay = function()
	{
		if (!acorns.isAudioPaused()) acorns.stopAudio(true); 
		if (thisPoint>0)
		{
			if (lastElement) setHighlight(lastElement, false);
		}
		
		var point =  points[pausePoints[thisPoint]];
		
		if (!acorns.system.isAndroid())
			startTime = point.getXY().x / audioFormat.rate;
			
		acorns.setTime(startTime);
		acorns.playAudio(audio);
	}
	
	var playNext = function()
	{
		if (!acorns.isAudioPaused()) acorns.stopAudio(true);
		
		var point = pausePoints[thisPoint];
		textComponent.value = "";

		if (thisPoint>=pausePoints.length)
		{
			thisPoint = 0;
			startTime = 0;
			return;
		}

		var element = document.getElementById("" + point);
		element.style.visibility = "visible";
		element.parentNode.style.backgroundColor = "";
		thisPoint++; 
		startTime = acorns.getTime();		
		if (thisPoint<pausePoints.length) 
			points[pausePoints[thisPoint]].applyFont(textComponent);
		setTimeout( function() {acorns.playAudio(audio); }, 1500);
	}
	
	// Set or remove a highlight from a grid element
	var setHighlight = function(cellNo, highlight)
	{
		// Remove the last highlighted cell
		if (cellNo==undefined) return;
		
		var cell = document.getElementById("" + cellNo);
		if (cell==null) return;
		
		if (highlight)	{ cell.style.backgroundColor = "#FF9966";}
		else { cell.style.backgroundColor = ""; }
	}

	var checkAnswer = function()
	{
		if (options.getOptions().continuous == "y") return;
		if (thisPoint>=pausePoints.length) return;

		var text = textComponent.value;
		if (typeof Custom !== 'undefined')
		{
			var custom = new Custom();
			if (custom.convertSpecialChars)
				text = custom.convertSpecialChars(text);
		}

		var CORRECT = 2, CLOSE = 1, NO_DATA = -2;
		var result = NO_DATA, distance = 99;
		var point = points[pausePoints[thisPoint]];
		var result = NO_DATA;
			

		var closeDistance = 1;
		if (length > 5) closeDistance = 2;
		if (length > 20) closeDistance = 3;
	
		if (text !=null)
		{
			var index = text.indexOf('(');
			if (index > 0)	text = text.substring(0, index);
			text = acorns.system.trim(text).toLowerCase();
			
			var answer = point.getSpell();
			if (typeof Custom !== 'undefined')
			{
				var custom = new Custom();
				if (custom.convertSpecialChars)
					answer = custom.convertSpecialChars(answer);
			}

			var index = answer.indexOf('(');
			if (index > 0)	answer = answer.substring(0, index);
			answer = acorns.system.trim(answer).toLowerCase();
			distance = acorns.system.stringDistance(text, answer);
		}

		if (window.audioTools)
		{
		   var startFrame = point.getXY().x;
		   var endFrame = point.getXY().y;

		   var json = "[\"" + audio + "\"," + startFrame + "," + endFrame + "," + audioFormat.rate + "]";
		   result = window.audioTools.compare(json);
		}
			
		if (result == CORRECT || distance==0)
		{
			acorns.widgets.updateScore(true);
			playNext();
		}
		else if (result == CLOSE || distance <= closeDistance)
		{
			acorns.feedbackAudio("spell", true);  // Use feedback player
			acorns.widgets.updateScore(false);  
		}
		else
		{
			acorns.feedbackAudio("incorrect", true); // Use feedback player	
			acorns.widgets.updateScore(false);
		}
	}
	
	// Respond to clicks of the control buttons
	var buttonHandler = function(event)
	{
		var audioHandler = window.audioTools;
	    var element = (event.currentTarget) ? event.currentTarget : event.srcElement;
		element.style.borderStyle = "inset";
		var continuous = options.getOptions().continuous == "y";
		
		var value = element.getAttribute("alt");
		switch (value)
		{
			case "next":
				if (!acorns.isAudioPaused()) acorns.stopAudio(true);
				if (thisPoint>=pausePoints.length)
				{
					thisPoint = 0;
					startTime = 0;
					if (continuous) acorns.playAudio(audio); 
					else setTimeout(function() { acorns.play(); }, 1000);
				}
				else if (!continuous) 
				{ 
					playNext();
				}
				else acorns.playAudio(audio); 
				break;
				
			case "replay":
				replay();
				break;
				
			case "pause":
				acorns.stopAudio(true); 
				break;
				
			case "answers":
				if (continuous) return; 
				if (!acorns.isAudioPaused())  acorns.stopAudio(true); 
				
				var point = points[pausePoints[thisPoint]];
				var text = point.getSpell();
				textComponent.value = text;
				break;
				
			case "check":
				checkAnswer();
				break;
				
			default:
				acorns.beep();
		}
		setTimeout(function() { element.style.borderStyle = "outset" }, 250);
	}

	// Monitor the audio player
	var lastElement = undefined;
	var timeListener = function(audio)
	{
		var timeDuration = acorns.getTimeAndDuration(audio);
		if (!timeDuration) return;
		
		if (isNaN(timeDuration.duration)) 
		{
			if (frames)
				timeDuration.duration = frames;
			else
			{
				frames = points[points.length-1].getXY().y / audioFormat.rate;
				timeDuration.duration = frames;
			}
		}
		
		var rate = frames/ timeDuration.duration;
		var time = timeDuration.time;
		var frame = time * rate;
		var pointNo = acorns.system.binarySearchPoints(points, frame);
		if (pointNo<0) return; // Don't try to highlight at beggining of audio

		var element = document.getElementById("" + pointNo);

		if (time*rate >= points[points.length-1].getXY().y) 
		{
			if (lastElement) { setHighlight(lastElement, false);	}
			lastElement = undefined;
			return;
		}
		
		// possibly pause if not in continuous play mode
		if (!(options.getOptions().continuous == "y") && thisPoint<pausePoints.length)
		{
			var point = points[pausePoints[thisPoint]];
			var spot = Math.max(point.getXY().x, point.getXY().y - rate*DELTA);
			// If we are playing the current word or phrase, pause the audio.
			// There is latency from the stop command to the actual stopping of the audio.
			if (frame >= spot)
			{
				acorns.stopAudio(true);
				return;
			}
		}
		
		if (element==lastElement) return;

		setHighlight(lastElement, false);
		setHighlight(pointNo, true);
		element.scrollIntoView(true);
		lastElement = pointNo;
	}

	// Determine if the entered text is correct one.
	var textHandler = function(event)
	{
		if (!event) event = window.event;
		if (!event || event.keyCode!=13) return;
		checkAnswer();
	}

	this.parse = function(parseObject, lessonDom)  
	{ parseObject.parseAudioLessonCategory(acorns, lessonDom, lessonData); }

	var compareTo = function(a,b) { return a - b; }
	
    this.play = function(reset, changeOfLayer) 
	{
		if (reset)
		{
			if (!textDisplay) return;
			
			// Load audio file for speechrecognition
			var audioList = acorns.system.makeJSONAudio(audio);
			if (window.audioTools)
			    window.audioTools.loadAudioFiles(audioList);

			startTime = 0;			
			acorns.setTimeUpdateListener( timeListener );
			textDisplay.style.fontSize 
				= Math.floor(options.getFontSize() * acorns.system.getFontRatio()) + "px";		

			var level = options.getDifficultyLevel();
			var numberChoices = Math.floor(points.length * level.difficulty * DIFF_PCT);
			if (numberChoices==0) choices = 1;
			
			pausePoints = [], pointChoices = [];
			var choice;
			
			for (var pointNo = 1; pointNo<points.length; pointNo++)  pointChoices.push(pointNo);
			
			// Create list of sorted pause points
			for (var choiceNo=0; choiceNo<numberChoices; choiceNo++)
			{
				choice = Math.floor(Math.random() * pointChoices.length);
				pausePoints.push(pointChoices[choice]);
				if (choice!=(pointChoices.length-1)) pointChoices[choice] = pointChoices[pointChoices.length-1];
				pointChoices.pop();
			}
			pausePoints = pausePoints.sort(compareTo);
		
			var id, cell;
			
			// Make all the points visible
			for (var pointNo=0; pointNo<points.length; pointNo++)
			{
				id = "" + pointNo;
				cell = document.getElementById(id);
				cell.style.visibility = "visible";
				cell.parentNode.style.backgroundColor = "";
			}
			
			// If not continuous mode, hide the words at the pause points
			if (options.getOptions().continuous!="y")
			{
				for (var pauseNo=0; pauseNo<pausePoints.length; pauseNo++)
				{
					id = "" + pausePoints[pauseNo];			
					cell = document.getElementById(id);
					cell.style.visibility = "hidden";
					cell.parentNode.style.backgroundColor = "#FF9966";
				}
			}
			thisPoint = 0;
			startTime = 0;
		}
		points[0].applyFont(textComponent);
		acorns.setTime(startTime);
		acorns.playAudio(audio);
	}

    this.isPlayable = function(layer) 
	{
		// Get layer 0, which has the audio object.
		var audioLayer = lessonData.objectList[0];
		if (audioLayer==undefined) return false;
		
		// Get audio filename (contained in the first point object)
		var audioPoint = audioLayer.objectList;
		if (audioPoint == undefined || audioPoint.length < 1) return false;
		
		// Make sure that there is an audio file
		audio = audioPoint[0].getAudio();
		if (audio == undefined || audio.length == 0) return false;
		audioFormat = audioPoint[0].getAudioFormat();
		frames = audioPoint[0].getFrames();

		// Get the points for the layer in question and set the alignment
		var layerData = lessonData.objectList[layer];
		if (layerData == undefined) return false;

		align = layerData.getAlign();
		language = layerData.getLanguage();
		points = layerData.objectList;
		if (points == undefined || points.length < MIN_POINTS) return false;
		return true;
	}
	
    this.configurePlayPanel = function(panel) 
	{
		var players = document.getElementsByTagName("bgsound");

		if (players && players.length>0)
		{
			var h3 = document.createElement("h3");
			h3.setAttribute("align", "center");
			h3.innerHTML = "Sorry, Your browser does not support this lesson type. Try Firefox or Chrome.";
			panel.appendChild(h3);
			return;
		}
		
		var size = acorns.widgets.configureScorePanel(lessonData, panel, true);
		var left = (panel.clientWidth - size - 230)/2;

		var controls = [ "replay", "answers", "pause", "next", "check"];
		
		var tips = ["replay the last part of the recording", "see the answer"
					, "pause playback", "continue playing", "verify your  answer"];

		var block = document.createElement("div");
		block.style.position = "relative";
		block.style.width = "230px";
		block.style.lineHeight = BUTTON_SIZE + "px";
		block.style.left = left + "px";
		block.style.textAlign = "center";
		block.style.borderStyle="solid";
		block.style.marginBottom = "5px";
		panel.appendChild(block);
		
		textComponent = document.createElement("input");
		textComponent.style.position = "relative";
		textComponent.style.top = "0px";
		textComponent.style.verticalAlign = "top";
		textComponent.setAttribute("type", "input");
		textComponent.style.width = "210px";
		textComponent.style.borderStyle = "solid";
		textComponent.onkeyup = textHandler;
		block.appendChild(textComponent);
		
		var handler = new KeyboardHandler(textComponent, language);
		
		var button;
		for (var i=0; i<controls.length; i++)
		{
			button = acorns.widgets.makeIconButton(controls[i], tips[i], buttonHandler , BUTTON_SIZE);
			if (i<controls.length - 1) button.style.marginRight = MARGIN + "px";
			block.appendChild(button);
		}
		
		var width = panel.clientWidth;
		textDisplay = document.createElement("div");
		textDisplay.style.position = "relative";
		textDisplay.style.left = ((width - width * 0.95)/2) + "px";
		textDisplay.style.width = (width * 0.95) + "px";
		if (align=="center") textDisplay.style.textAlign = "center";
		textDisplay.style.borderStyle = "outset";
		textDisplay.style.overflow = "auto";
		acorns.system.scrollableDiv(textDisplay);
		lessonData.environment.setColors(textDisplay);
		
		var span, wordCount = 0, wrap, spell;
		for (var wordNo=0; wordNo<points.length; wordNo++)
		{
			wrap = document.createElement("span");
			span = document.createElement("span");
			span.id = "" + wordCount++;
			acorns.system.applyFont(span, language);
			span.style.fontSize = options.getFontSize() + "pt";

			spell = points[wordNo].getSpell();
			spell = spell.replace(/\n/g, "<br />");
			span.innerHTML = spell;
			wrap.appendChild(span);
			textDisplay.appendChild(wrap);
			if (wordNo< points.length - 1) textDisplay.appendChild(document.createTextNode(" "));
		}
		panel.appendChild(textDisplay);
		var top = textDisplay.offsetTop + textDisplay.clientTop;
		textDisplay.style.height = (panel.clientHeight - top - 5) + "px";
		textDisplay.style.maxHeight = (panel.clientHeight - top - 5) + "px";
		textDisplay.style.minHeight = (panel.clientHeight - top - 5) + "px";
	}
	
	this.getHelpMessage = function() 
	{
		var buffer = new acorns.system.StringBuffer();
		buffer.append("<h3 style='text-align:center'>Hear and Respond Lessons</h3>");
		
		buffer.append("<p>When you execute a hear and respond lesson, you will see the text of a");
		buffer.append(" particular recording but with some words blank.");
		buffer.append(" The program will play back the recording, highlighting the annotated words as it goes,");
		buffer.append(" and stopping after reaching the first blank word.");
		buffer.append(" Your job is then to type in the correct word (or phrase).</p>");
		
		buffer.append("<p>At the top of the display, there are icon buttons that can help.");
		buffer.append(" If you want to rehear the last part of the recording again simply");
		buffer.append(" click (or touch) the replay button.");
		buffer.append(" Similarly, the button with the blue circle is for showing the correct answer.");
		buffer.append(" There is also a button to pause the audio and another button, play, to ");
		buffer.append(" continue the playback to the next pause point. Finally, click the check button to");
		buffer.append(" verify your answer. You can also verify your answer by pressing the enter key");
		buffer.append(" on your keyboard.</p>");
	
		
		buffer.append("<p>When you hit the enter key, there are three possibilites that we now list.");
		buffer.append(" If correct,  the program will continue the playback to the next blank word.");
		buffer.append(" If you are close, perhaps because of a spelling error, or if you are way off,");
		buffer.append(" you will hear an appropriate feedback sound.");
		buffer.append(" Click the replay button (at the top of the display) to repeat the last part of the audio.</p>");
		
		buffer.append("<p>You continue filling in the blanks until the program gets to the end of the recording.");
		buffer.append(" For long recordings, you will see the display change during the playback.");
		buffer.append(" After the lesson completes, the process will start all over again,");
		buffer.append(" this time with a different set of blanks.</p>");

		buffer.append("<p>On the bottom of the display, you will see an icon with an 'i' in a green circle.");
		buffer.append(" If you click on this button,");
		buffer.append(" you'll see a pop up menu that contains options to allow you to reset the game,");
		buffer.append(" set the lesson to continuous play mode, adjust the difficulty level, or to");
		buffer.append(" alter the font size of the displayed text.");
		buffer.append(" Reset randomly picks new blank points and restarts the audio playback.");
		buffer.append(" Continuous play mode will cause the entire audio to play straight through.");
		buffer.append(" The difficulty level controls the number of blank words.</p>");

		return buffer.toString();	
	}
	
	this.getOptions = function() { return options; }
}   // End of HearandRespond lesson class

function KeyboardHandler(component, language)
{
   var SHIFT_MASK 		 =	  1; 
   var CTRL_MASK  		 =	  2;	// ctrl on mac
   var META_MASK  		 =	  4;	// command on mac
   var ALT_MASK   		 =	  8;	// option on mac
   var CAP_MASK   		 =	 16;  
   var LEFT_SHIFT_MASK  =	 32;
   var RIGHT_SHIFT_MASK = 	 64;
   var LEFT_CTRL_MASK   =  128;
   var RIGHT_CTRL_MASK  =  256;
   var LEFT_ALT_MASK    =  512;  
   var RIGHT_ALT_MASK 	 = 1024;
   
   /* Indices int modifier table */
   var MUST_HAVE = 0;
   var CANT_HAVE = 1;
   
   /* Indices for dead key map data */
   var ACTION  = 0;
   var OUTPUT = 1;
   var MAP_LEN = 2;
   
   /** Map from characters to MAC keycodes */
   var lowerKeyCodeMapping =
      "asdfhgzxcv" + "\0bqweryt12" + "3465=97-80" + "]ou[ip\0lj'"
         + "k;\\,/nm.\0\0" + "`";

   var upperKeyCodeMapping =
      "ASDFHGZXCV" + "\0BQWERYT!@" + "#$^%+(&_*)" + "}OU{IP\0LJ\""
         + "K:|<?NM>\0\0" +"~";

    


   language = language.replace(/-/g,'');
   try
   {
		var json = eval(language);
   }
   catch(e) { return; }
   if (!json) return;
 
   var state = "none";

   var charCodes = json["charCodes"];
   var terminators = json["terminators"];
   var sequences = json["deadSequences"];
   var modifiers = json["modifiers"];
   var modifierData = json["modifierData"];
   var charKeyMaps = new Array();
   var deadKeyMaps = new Array();
   
   
   var index = null;
   for (index=0; index<modifierData.length; index++)
   {
	   charKeyMaps.push(modifierData[index].charMap);
	   deadKeyMaps.push(modifierData[index].deadKeyMap);
   }
   
   var getState = function() { return state; }
   var setState = function(s) { state = s; }

   // Search json array
   var searchJSON = function(tags, key)
   {
		var i = null;
		for (i = 0; tags.length > i; i += 1) 
		{
			if (tags[i].key === key) return tags[i];
        }
		return null;
    }
	
	var findDeadSequence = function(data, key)
	{
		var sequence = searchJSON(data, key);
		if (sequence)
		{
			return [sequence.action, sequence.output];
		}
	}
	
	var findModifierIndex = function(modifier)
	{
	  var index = null;
	  for (index=0; index < modifiers.length; index++)
	  {
		  values = [modifiers[index].must, modifiers[index].cant];
		  if ((values[MUST_HAVE] & modifier) != values[MUST_HAVE]) 
			 continue;
		  if ((values[CANT_HAVE] & modifier) != 0)
			  continue;
		  
		  return index;
	  }
	  return -1;
	}
	
	var getModifierKeyMap = function(modifier)
	{
		var index = findModifierIndex(modifier);
		if (index<0) return null;
		
		var data = new Array();
		var charMap = charKeyMaps[index];
		for (index=0; index<charMap.length; index++)
		{
			data[charMap[index].key] = charMap[index].output;
		}
		return data;
	}
	
	var getDeadKeyMap = function(modifier)
	{
		var index = findModifierIndex(modifier);
		if (index<0) return null;
		
		return deadKeyMaps[index];
	}
	
	var getCharacterCode = function(e)
	{
		var code = event.which || event.keyCode;

		var lowerSpecialsA = [';', '=', ',', '-', '.', '/', '`']; // 186-192
		var lowerSpecialsB = ['[', '\\', ']', '\'', '\'']; //219-222
		
		// Always return unshifted character 
		if (code>=48 && code <=57)
			return code;
		
		if (code>=65 && code<=90)
			return code;
		
		if (code>=186 && code <= 192)
		{
			code = lowerSpecialsA[code-186];
			return code.charCodeAt(0);
		}
		
		if (code>=219 && code<=222)
		{
			code = lowerSpecialsB[code-219];
			return code.charCodeAt(0);
		}
		return -1;
	}
	
   component.onkeydown = function(e) 
   {
	   	if (!e) e = window.event;
		
		character = getCharacterCode(e);
		if (character<0) return;
		
	   	if (processEvent(e, character)) 
		{
			if (event.preventDefault) event.preventDefault();
			e.stopPropagation();
		}
   }
 
   var processEvent = function(e, character)
   { 
		var modifier = getModifiers(e);

	    var sequence = computeOutput(modifier, character);
	    if (sequence==null) return true;
	    if (sequence.length==0) { return true; }

		sequence = sequence.replace(/undefined/g,"");
	    var field = e.currentTarget;
	    var start = field.selectionStart;
	    var end = field.selectionEnd;
		var text = field.value;
	    text = text.substring(0,start) + sequence + text.substring(end);
	    field.value = text;
	    var newPosition = start + sequence.length;
	    field.setSelectionRange(newPosition, newPosition);
	    return true;
   }
   
   var getModifiers = function(e)
   {
	   var modifiers = 0;
	   
	   if (e.shiftKey) modifiers += SHIFT_MASK;
	   if (e.altKey)   modifiers += ALT_MASK;
	   if (e.metaKey)  modifiers += META_MASK;
	   
	   if (event.getModifierState("CapsLock"))
		   modifiers += CAPS_LOCK;

	   if ((modifiers & SHIFT_MASK) != 0)
	   {
		   if (event.location != undefined)
		   { 
				if (event.location === KeyboardEvent.DOM_KEY_LOCATION_LEFT)
					modifiers |= LEFT_SHIFT_MASK;
				else if (event.location === KeyboardEvent.DOM_KEY_LOCATION_RIGHT)
					modifiers |= RIGHT_SHIFT_MASK;
		   }
	   }
		
	   if ((modifiers & ALT_MASK) != 0)
	   {
		   if (event.location != undefined)
		   { 
				if (event.location === KeyboardEvent.DOM_KEY_LOCATION_LEFT)
					modifiers |= LEFT_ALT_MASK;
				else if (event.location === KeyboardEvent.DOM_KEY_LOCATION_RIGHT)
					modifiers |= RIGHT_ALT_MASK;
		   }
	   }
   
	   return modifiers;
   }
  
   var computeOutput = function(modifier, character)
   {  
      var key, terminator, sequenceData;

	  deadKeyMap = getDeadKeyMap(modifier);
	  if (deadKeyMap==null)
	  {
		  terminator = searchJSON(terminators, getState()).output;
		  setState("none");
		  return terminator;
	  }
	
	  var mapData = findDeadSequence(deadKeyMap, character);
	  if (!mapData)
	  {
		  terminator = "";
		  if (!(getState() == "none"))
		  {
			  terminator = searchJSON(terminators, getState()).output;
		  }
    	  var keyMap = getModifierKeyMap(modifier);
    	  var xlate = translateChar
                  (keyMap, character);
    	  setState("none");
    	  return terminator + xlate;
	  }

	  if (mapData[ACTION].length==0)
	  {
		  setState("none");
		  return mapData[OUTPUT]; // No next state
	  }

	  if (getState() == "none")
      {
		  key = getState() + "~~" + mapData[ACTION];
		  sequenceData = findDeadSequence(sequences, key);
		  if (sequenceData == null)
		  {
			  terminator = searchJSON(terminators, mapData[ACTION]).output;
   		      setState("none");
			  return terminator;
		  }

		  if (sequenceData[ACTION].length>0)
		  {
			  setState(sequenceData[ACTION]);
			  return "";
		  }
		  else
		  {
			  setState("none");
			  return sequenceData[OUTPUT];
		  }
      }
	  
      key = getState() + "~~" + mapData[ACTION];
      sequenceData = findDeadSequence(sequences, key);
      if (sequenceData == null)
      {
		  terminator = searchJSON(terminators, getState()).output;
    	  var keyMap = getModifierKeyMap(modifier);
    	  var xlate = translateChar
                  (keyMap, character);
		  setState("none");
    	  return terminator + xlate;
      }
      
      if (sequenceData[ACTION].length==0)
      {
		  setState("none");
    	  return sequenceData[OUTPUT];
      }
    			  
      setState(sequenceData[ACTION]);
      return  "";
   }

   var translateChar = function(mapping, character) // expects integer, not character
   {  
   		var character = String.fromCharCode(character);
		if (character==' ')  return character;

		var lower = lowerKeyCodeMapping.indexOf(character);
		var upper = upperKeyCodeMapping.indexOf(character);

		var mapValue = lower;
		if (lower<0) mapValue = upper;
		if (mapValue<0) return '\0';
		return mapping[mapValue];
   }
   
}	// End of KeyboardHandler class	

// Prototype for an ACORNS group of lessons object
function Acorns(n, root)
{
	acorns = this;
	
    var name = n;
    var activeLesson = 0;
	var fonts = undefined;
	var EXP_DAYS = 30;	// Cookie expiration days.
	var drawing = false; // True if we are redrawing components
	
	var timeListener = undefined; // Lesson monitors progress of audio
	var player = undefined; // The audio player for non-feedback audio
	
    this.assets = name + "/Assets/";
    this.audioLink = this.assets + "Audio/";
    this.iconLink = this.assets + "Icons/";
    this.fontLink = this.assets + "Fonts/";
   
	this.system = new System();
	this.widgets = new Widgets(this);

	var timeout; // Timer object to initiate an audio playback
	var playing = false; // True if audio playing, false otherwise
	var queuedAudioFile = undefined; // Object to queue if previous audio is still playing
	var intervalId = undefined; // Id of interval listener so can call clearInterval

	var preloadAudio = function(type, sound)
	{
		var player;
		var audio = acorns.system.audioSupport();
		if (!audio["audio"] && audio["bgsound"])  return undefined;
		
		player = document.getElementById(type);
		player.src = sound;

		if (player.load) 
		{
			player.load();
		}
		if (player.pause) player.pause();
		return player;
	}
	
	var playNext = function(player)
	{
		playing = false;
		if (!queuedAudioFile) 
		{
			// preload in case we should play it again
			player.load();
			return;
		}
		var soundFile = queuedAudioFile;
		queuedAudioFile = undefined;
		acorns.playAudio(soundFile);
	}
		
	// Audio timeupdate handler
	var audioHandler = function(e)
	{	
		// Can only queue audio after a feedback effects
		player = document.getElementById("player");
		if (queuedAudioFile && (player.currentTime >= player.duration)) 
		{
			playNext(player);
			return;
		}
			
		if (timeListener) 
		{  
			if (playing) {
				if (!intervalId)
					intervalId = setInterval(audioHandler, 100);
			
				timeListener(player);
			}
			else {
				clearInterval(intervalId);
				intervalId = undefined;
			}
		}  
	}
	
	/** Establish the primary audio player */
	var getAudioPlayer = function()
	{
		if (player!= undefined) return player;
		
		var players = document.getElementsByTagName("bgsound");
		if (players.length == 0)
		{
			player = document.getElementById("player");
			player.setAttribute("autoplay", "false");
			acorns.system.addListener(player, "timeupdate", audioHandler );
			acorns.system.addListener(player, "ended", function(e) { playNext(this); } );
		}
		else player = players[0];
		player.playbackRate = 1.0;
		return player;
	}

	var beepEffect = this.audioLink + "beep.mp3"; 
	var beepPlayer = preloadAudio("beep", beepEffect);
	var player = undefined;  // Active audio player
	var feedback = undefined; // Audio feedback file names
	var feedbackPlayers = undefined; // Audio feedback players
 	
	this.BACKGROUND_COLOR = this.system.getColor(204, 204, 204);

    this.lessonList = new Array();
    this.paramList = new Array(); // Unused for now
	
	var feedbackHandler = function()
	{
		if (this.currentTime >= this.duration)
		{
			playing = false;
			queuedAudioFile = undefined; // Object to queue if previous audio is still playing
		}
	}
	
	this.setFeedback = function(fb) 
	{ 
		var feedbackPlayer;
		feedback = fb; 
		feedbackPlayers = [];
		feedbackTypes = [ "correct", "incorrect", "spell", "slow"];
		if (feedback)
		{
			var feedbackNo, effectNo, index;
			for (feedbackNo=0; feedbackNo<feedbackTypes.length; feedbackNo++)
			{
				index = feedbackTypes[feedbackNo];
				feedbackPlayers[index] = [];
				for (effectNo=0; effectNo<feedback[index].length; effectNo++)
				{
					if (feedback[index].length !== 0)
					{
						feedbackPlayer = preloadAudio(index, this.audioLink + feedback[index][effectNo]);
						feedbackPlayers[index].push(feedbackPlayer);
					}
					
					acorns.system.addListener(feedbackPlayer, "timeupdate", feedbackHandler );
					acorns.system.addListener(feedbackPlayer, "ended", feedbackHandler );
				}
			}
		}
	}
	this.setFonts = function(f) { fonts = f; }
	this.getFonts = function() { return fonts; }
    	
    this.getActiveLesson = function() { return this.lessonList[activeLesson]; }
	this.getLesson = function(lessonNo) { return this.lessonList[lessonNo]; }
	
    this.nextLesson = function() 
	{ 
		if (activeLesson >= this.lessonList.length - 1) 
		{
			return false;
		}
		
		activeLesson++; 
		if (this.getActiveLesson().setPlayableLayer())
		{
			var previousLesson = this.getLesson(activeLesson - 1);
			if (previousLesson.lessonObject.stop) previousLesson.lessonObject.stop();
			return true;
		}
		
		activeLesson--;
		return false;
	}
	
    this.previousLesson = function() 
	{ 
		if (activeLesson <= 0) 	{ return false; }
		activeLesson--; 
		if (this.getActiveLesson().setPlayableLayer())
		{
			var previousLesson = this.getLesson(activeLesson+1);
			if (previousLesson.lessonObject.stop) previousLesson.lessonObject.stop();
			return true;
		}

		activeLesson++;
		return false;
	}
	
    this.firstLesson = function() 
	{
		var previousActiveLesson = activeLesson;
		activeLesson = 0; 
		
		if (this.getActiveLesson().setPlayableLayer())
		{
			var previousLesson = this.getLesson(previousActiveLesson);
			if (previousLesson.lessonObject.stop) previousLesson.lessonObject.stop();
			return true;
		}
		activeLesson = previousActiveLesson;
		return false;
	}
    this.lastLesson = function() 
	{ 
		var previousActiveLesson = activeLesson;
		activeLesson = this.lessonList.length - 1; 
		
		if (this.getActiveLesson().setPlayableLayer()) 
		{
			var previousLesson = this.getLesson(previousActiveLesson);
			if (previousLesson.lessonObject.stop) previousLesson.lessonObject.stop();
			return true;
		}

		activeLesson = previousActiveLesson;
		return false;
	}   
	
	this.setLinkedLesson = function(point)
	{
		return this.switchLesson(point.getLink(), true);
	}
	
	this.getLinkedLesson = function(set)
	{
		var params = this.getActiveLesson().paramList;
		if (params == undefined) return  false;
		
		var link = params["link"];
		return this.switchLesson(link, set);
	}
	
	this.switchLesson = function(link, set)
	{
		if (link==undefined) return false;
		
		var lessonNo;
		for (lessonNo = 0; lessonNo < this.lessonList.length; lessonNo++)
		{
			if (this.lessonList[lessonNo].getName() == link)
			{
				if (this.lessonList[lessonNo].setPlayableLayer())
				{
					var previousLesson = this.getActiveLesson();
					if (previousLesson.lessonObject.stop) previousLesson.lessonObject.stop();
					
					if (set) 
					   activeLesson = lessonNo;
					return true;
				}
			}
		}
		return false;
	}
	
	/** Function to wait for can play through event required by some browsers */
	var endedHandler = function(event)
	{
		var sound = this;
		acorns.system.removeListener( sound, "loadeddata", canPlayThroughHandler );
		sound.pause();
	}
	
	var canPlayThroughHandler = function(event)
	{	
		var sound = this;
		var promise = undefined;
		acorns.system.removeListener( sound, "loadeddata", canPlayThroughHandler );
		
		if (sound.play) promise = sound.play();
		else if (sound.Play) sound.Play();
		else if (sound.DoPlay) sound.DoPlay();
	}
	
	/** Play audio files 
	 * audioFile: the name of the file
	 * audioPlayer: The player to use for non-queued audio
	 */
    this.playAudio = function(audioFile, audioPlayer)  
    {
		var audio = acorns.system.audioSupport();
		var sound = getAudioPlayer();
		var decodedSource;

		if (audioPlayer) 
		{
			sound = audioPlayer;
			queuedAudioFile = undefined;
		}
		else if (!player)
		{
			sound = getAudioPlayer();
			if (audio["audio"]) player = sound;
		}			
		else 
		{		
			if (audio["audio"] && !queuedAudioFile && playing) 
			{
				queuedAudioFile = audioFile; 
				return;
			}
			else 
			{
				queuedAudioFile = undefined;
				sound = player; 
			}
		}
		if (!sound) return;
		
		playing = true;
		if (acorns.system.isOldIE())
		{
			sound.src = audioFile;
			if (sound.play) sound.play();
			else if (sound.Play) sound.Play();
			else if (sound.DoPlay) sound.DoPlay();
			return;
		}
		
		acorns.system.addListener( sound, "loadeddata", canPlayThroughHandler );
		acorns.system.addListener( sound, "ended", endedHandler );
		
		decodedSource = decodeURI(sound.src);
		if ( decodedSource.length<audioFile.length 
		    || decodedSource.substring(decodedSource.length-audioFile.length) != audioFile)
		{
			try { sound.setAttribute("src", audioFile); }
			catch (e) {}
			if (sound.load) 
				sound.load();
		}
		else
		{
			if (sound.currentTime >= sound.duration || sound.currentTime == 0)
			{
				try
				{
					sound.currentTime = 0;
				}
				catch (err)
				{
					sound.setAttribute("src", audioFile);
					if (sound.load) sound.load();
					return;
				}
			}
	
			acorns.system.removeListener( sound, "loadeddata", canPlayThroughHandler );
			if (sound.play) sound.play();
			else if (sound.Play) sound.Play();
			else if (sound.DoPlay) sound.DoPlay();
		}
	}
	
	this.stopAudio = function(pause)	
	{
		// Reset the queueing of audio
		queuedAudioFile = undefined;
		playing = false;
		
		if (player==undefined) 
		{
			var players = document.getElementsByTagName("bgsound");
			if (players.length > 0)	
			{   
				players[0].src = ""; 
				return; 
			}
			player = getAudioPlayer();
		}
		
		clearTimeout(timeout);
		if (player.pause) 
		{ 
			player.pause(); 
			return;
		}
		else player.src = this.audioLink + "silence.mp3";  
	}
	
	this.rewindAudio = function(deltaTime)
	{
		if (player==undefined || deltaTime<0)  this.beep();
		{
			var current = player.currentTime;
			current -= deltaTime/1000;
			if (current<0) current = 0;
			player.currentTime = current;
		}
	}
	
	this.getTime = function()
	{
		return player.currentTime;
	}
	
	this.setTime = function(time) 
	{  
		if (player==undefined || time<0)  this.beep();
		{
			if (!player.duration) return;
			try { player.currentTime = time; } 
			catch(err) 
			{ console.log(err.message); }
		}
	}
	
	this.isAudioPaused = function()
	{
		if (player==undefined) return false;
		return (player.paused);
	}
	
	this.setTimeUpdateListener = function( listener )
	{
		timeListener = listener;
	}
	
	this.getTimeAndDuration = function(audio)
	{
		return { time: audio.currentTime, duration: audio.duration, rate: audio.playbackRate };
	}
	
    this.beep = function() 
	{ 
		this.playAudio(beepEffect, 0, true); 
		beepPlayer = preloadAudio("beep", beepEffect);
	}
		
	/** Echo the feedback effect 
	 *  useFeedbackPlayer = true if we should not use the standard AudioPlayer object.
	 *  		this is necessary when we are tracking audio progress
	 */
	this.feedbackAudio = function(type, useFeedbackPlayer)
	{
		// The following inhibits feedback effects for android phones (to avoid long  delays)
		// if (acorns.system.isMobilePhone() && acorns.system.isAndroid())  return;
		
	    var soundFile = this.audioLink + type + ".mp3"; 
		if (acorns.system.isOldIE())
		{
			this.playAudio(soundFile);
		}
		else 
		if ( (feedback != undefined) && (feedback[type].length>0) )
		{
			var feedbackPlayer = feedbackPlayers[type];
			var which = Math.floor(Math.random() * feedback[type].length);
			var oldFile = feedbackPlayer[which].src;
			
			if (useFeedbackPlayer) 
			      feedbackPlayer = feedbackPlayer[which];
			else  feedbackPlayer = getAudioPlayer();
			
			this.playAudio(oldFile, feedbackPlayer);
		}
	}
	
	this.playSlow = function(source)
	{
		var player = document.getElementById("slow");
		preloadAudio("slow", source);
		player.playbackRate = 0.5;
		this.playAudio(source, player);
	}
	
	this.play = function(reset)
	{
		if (reset == undefined) reset = true;
		var lesson = this.getActiveLesson();
		if (!lesson.isPlayable())  { this.beep(); }
		else  
		{
			if (userInteraction) lesson.play(reset);
			else  
			{
				var controlPanel = document.getElementById("controls");
				controlPanel.style.pointerEvents = "none";
				
				var messagePanel = acorns.widgets.showMessage("Press to start application", true);
				var messageInputPanel = messagePanel.getElementsByTagName("input");
				acorns.system.addListener(messageInputPanel[0], "mouseup", 
					function  playListener(e) 
					{ 
						acorns.widgets.removeMessageElement();
						var source = (e.target) ? e.target : e.srcElement;			
						acorns.system.removeListener(source, "mouseup", playListener );
						userInteraction = true;
						controlPanel.style.pointerEvents = "auto";
						setTimeout(function() { lesson.play(reset) }, 250);
					});
			}
		}
	}
	
	/* Read cookie for setting lesson options; returns undefined if no cookie found */
	this.getCookie = function()
	{
		// Perform any acorns initialization needed before running this lesson
		var cookieName = name + activeLesson;
		var cookies = document.cookie;
		var cookieList = cookies.split(";");
		var cookieNo;
		
		for (cookieNo=0; cookieNo<cookieList.length; cookieNo++)
		{
			cookie = cookieList[cookieNo].split("=");
			cookie[0] = cookie[0].replace(/^\s+|\s+$/g, '');
			if (cookie[0] == cookieName) return cookie[1];
		}
		return undefined;
	}
	
	/* Write a cookie that will expires in EXP_DAYS from now */
	this.setCookie = function(cookieValue)
	{
		var cookie = name + activeLesson + "=" + cookieValue + ";expires=";
		
		var expDate = new Date();
        expDate.setDate(expDate.getDate() + EXP_DAYS);
        cookie += expDate.toGMTString();
		document.cookie = cookie;		
    }
	
    /* Handle control panel button clicks */
    var controlHandler = function(event, tag, lesson)
    {
		var audioHandler = window.audioTools;
		if (audioHandler===undefined)
			audioHandler = document.getElementById("AudioHandler");
		   
		tag.style.borderStyle = "inset";
		acorns.stopAudio();
	
		var result;
		var alt = tag.getAttribute("alt");
		var audioNotSupported = "Access to the system microphone is disabled or unavailable";
		switch(alt)
		{
			case "up":
				if (!lesson.nextLayer()) acorns.beep();
				break;
				
			case "down":
				if (!lesson.previousLayer()) acorns.beep();
				break;
				
			case "anchor":
				if (!acorns.getLinkedLesson(true)) acorns.beep();
				else acorns.getActiveLesson().play(true);				
				break;
				
			case "info":
				var popupMenu = document.getElementById("popup");
				if (popupMenu.style.display == "block") 
				{ 
					popupMenu.style.display = "none"; 
				}
				else 
				{
					acorns.widgets.makePopupMenuVisible();
				}
				break;
				
			case "begin":
				if (!acorns.firstLesson()) acorns.beep();
				else acorns.getActiveLesson().play(true, true);
				break;
				
			case "prev":
				if (!acorns.previousLesson()) acorns.beep();
				else acorns.getActiveLesson().play(true, true);
				break;
				
			case "next":
				if (!acorns.nextLesson()) acorns.beep();
				else acorns.getActiveLesson().play(true, true);
				break;

			case "end":
				if (!acorns.lastLesson()) acorns.beep();
				else acorns.getActiveLesson().play(true, true);
				break;

			case "record":
				try
				{
 				    audioHandler.recordAudio();
				}
				catch (e) { acorns.widgets.showMessage(audioNotSupported); }
				break;

			case "play":
				try
				{
					audioHandler.playAudio();
				}
				catch (e) { acorns.widgets.showMessage(audioNotSupported); }
				break;
			case "stop":
				try
				{
					audioHandler.stopAudio();
				}
				catch (e) { acorns.widgets.showMessage(audioNotSupported); }
				break;
				
			case "help":
				var  helpPanel = document.getElementById("message");
				if (helpPanel) 
				{
					if (helpPanel.title == "help")
						 document.body.removeChild(helpPanel);
					else acorns.beep();
					break;
				}
				
				var buffer = new acorns.system.StringBuffer();
				buffer.append(acorns.getActiveLesson().getHelpMessage());
				buffer.append('<h4 style="text-align:center;">Lesson Controls</h4>');
				buffer.append("<p>There are control buttons at the bottom of the page. This panel is standard across ");
				buffer.append("all ACORNS lesson types. By clicking on the buttons, you can change lesson layers ");
				buffer.append("(for different dialects and difficulty levels) ");
				buffer.append("switch to another lesson (if linked), navigate between lessons (if they exist), ");
				buffer.append("record/playback audio (if supported by the device), and display ");
				buffer.append("this help message. Curser over each button for a short description of what it does.");
				buffer.append("To make the popup menu go away, simply click (or touch) again on the control button");
				buffer.append(" The help message has an ok button at the bottom; click it to make the message disappear.");
				buffer.append("</p>");
				acorns.widgets.showMessage(buffer.toString());
				
				var  helpPanel = document.getElementById("message");
				helpPanel.title = "help";
				firstChild = helpPanel.firstChild;
				firstChild.scrollIntoView(true);

				break;
		}
		setTimeout(function() { tag.style.borderStyle = "outset" }, 250);
	}	// End of controlHandler()

	var setDrawing = function(set) { drawing = set; }
	this.isDrawing = function()  {	return drawing; }
	
	this.start = function(record)
	{
		setDrawing(true);
		this.widgets.makePanels(controlHandler, record);
		var lesson = this.getActiveLesson();
		if (!lesson.setPlayableLayer())
			this.widgets.showMessage
			   ("The first lesson in this file has no playable layers", true);
		else
		{
			this.play(true);
		}
		
		var delay = 500;
		setTimeout( function() {setDrawing(false) }, delay);
	};
	
	
}  // end of Acorns prototype class

// Prototype for setting the ACORNS environment paramaeters
function Environment(acornsObject, fg, bg, fontSize)
{
	var acorns = acornsObject;
	var rgb, foreground, background;
	var size = Math.floor(((fontSize==undefined) ? 12  : parseInt(fontSize, 10)) * acorns.system.getFontRatio());
	
	if (fg==undefined) foreground = acorns.system.getColor(0,0,0);
	else
	{
		rgb = fg.split(",");
		foreground = acorns.system.getColor(rgb[0], rgb[1], rgb[2]);
	}
	
	if (bg==undefined) background = acorns.BACKGROUND_COLOR;
	else
	{
		rgb = bg.split(",");
		background = acorns.system.getColor(rgb[0], rgb[1], rgb[2]);
	}

	this.setColors = function(block)
	{
		block.style.backgroundColor = background;
		block.style.color = foreground;
	}

	this.getFontSize = function() {return size/acorns.system.getFontRatio(); }

	// Configure environment and set background picture if necessary
	this.configureEnvironment = function(block, picture)
	{
		this.setColors(block);
		block.style.fontSize = Math.floor(size * acorns.system.getFontRatio()) + "px";
		
		if (picture != undefined)
		{
			block.style.backgroundImage = "url(" + picture.getSrc() + ")";
			var window = acorns.system.getWindowSize();
			if (acorns.system.isOldIE())
				block.style.backgroundPosition = "center center";
			block.style.backgroundRepeat = "no-repeat";
			if ("MozBackgroundSize" in document.documentElement.style) 	
				 block.style.MozBackgroundSize = window.width + "px " + window.height + "px";
			else block.style.backgroundSize = window.width + "px " + window.height + "px";
	
		}
		else block.style.backgroundImage = "";
	};
	
}	// End of Environment class

function System()
{
	var getPixelsPerInch = function()
	{
		var element = document.createElement('div');
		element.style.width = '1in';
		element.style.padding = '0';
		document.body.appendChild(element);
		var ppi = element.offsetWidth;
		element.parentNode.removeChild(element);
		return ppi;
	}
	
	// Find pixels per default window font size.
	var getDefaultFontSize = function()
	{
		var element = document.createElement('div');
		element.appendChild(document.createTextNode('M'));
		document.body.style.fontSize = null;
		document.body.appendChild(element);
		var fs= [element.offsetWidth, element.offsetHeight];
		element.parentNode.removeChild(element);
		return fs[1];
	}
	
	var resolutionRatio = getPixelsPerInch() / 96;
	var fontRatio = getDefaultFontSize() / 12;  
	var previousOrientation = undefined;
	
	var testComponent = undefined;
	var audio = undefined;
	
	// Determine which audio codecs are supported
	this.audioSupport = function()
	{
		if (audio!=undefined) return audio;
		
		audio = [];
		audio["mp3"] = false;
		try
		{
			audio["bgsound"] = this.isOldIE();
			
			var myAudio = document.createElement('audio');

			// Currently canPlayType(type) returns: "", "maybe" or "probably" 
			audio["mp3"] = myAudio.canPlayType && "" != myAudio.canPlayType('audio/mpeg');
		}
		catch(e) {   }
		audio["audio"] = audio["mp3"];
		return audio;
	}	// End audioSupport()
	
	this.widthOfString = function(text, size)
	{
		if (testComponent==undefined)
		{
			testComponent = document.createElement("span");
			testComponent.style.position = "absolute";
			testComponent.style.left = "-1000px";
			testComponent.style.top = "-1000px";
			testComponent.style.visibility = "hidden";
		}
		
		if (testComponent.parentNode==null) document.body.appendChild(testComponent);

		if (typeof size === 'string' || size instanceof String)
			testComponent.style.fontSize = size;
		else
			testComponent.style.fontSize = size + "px";
		
		testComponent.innerHTML = text;
		var width = testComponent.clientWidth;
		//testComponent.parentNode.removeChild(testComponent);
		return width;
	}
	
	var isAndroid = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("android")>=0;
	}
	
	this.getResolutionRatio = function() 
	{ 
		if (this.getWindowSize().width <= 500) return resolutionRatio * 0.75;		
		return resolutionRatio;
	}
	
	this.getFontRatio = function() 
	{
		if (this.getWindowSize().width <= 500) return fontRatio * 0.70;
		return fontRatio; 
	}
	
	// Determine if we are running under an Internet Explorer Browser
	this.isOldIE = function()
	{
		var browser = navigator.appName;
		return browser.toLowerCase().indexOf("microsoft")>=0;
	}
	
	this.isAndroid = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("android")>=0;
	}
	
	/** Determine if device is either an iPad, iPod, or  IPhone */
	this.isMobileApple = function()
	{
		return this.isiPad() || this.isiPod() || this.isiPhone();
	}
	
	this.isMobilePhone = function()
	{
		if (this.isiPad() || this.isiPod()) return true;
		if (!this.isAndroid()) return false;
		
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("mobi")>=0;
	}
	
	this.isiPad = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("ipad")>=0;
	}
	
	this.isiPod = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("ipod")>=0;
	}
	
	this.isiPhone = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf("iphone")>=0;
	}
	
	this.isOpera = function()
	{
		var browser = navigator.userAgent;
		return browser.toLowerCase().indexOf('opera') >= 0;
	}

	/** Method to create a JSON array of audio file names */
	this.makeJSONAudio = function(argument)
	{
	   var json = [];
	   if (typeof argument === "string")
	   {
			json[0] = '\"' + argument + '\"'; 
	   }
	   else
	   {
		   	var len = argument.length;
			for (var i=0; i<len; i++)
		    {
			  json[i] = '\"' + argument[i].getAudio() + '\"';
		    }
	   }
	   return "[" + json.join() + "]";
	}
	
	/** Method to add a listener to an object */
	this.addListener = function(tag, type, handler)
	{
		if (tag==undefined) return;
		
		if (this.isTouchDevice())
		{
			if (type == "mouseup" || type == "click")
			{
				tag.ontouchend = function(event)
				{
					if (event==null) event = window.event;
					if (event.preventDefault) event.preventDefault();
					handler(event);
				}
			}
			else if (type == "mousedown")
			{
				tag.ontouchstart = function(event)
				{
					if (event==null) event = window.event;
					handler(event);
				}
			}
			else if (type == "mousemove")
			{
				tag.ontouchmove = function(event)
				{
					if (event==null) event = window.event;
					handler(event);
				}
			}
		}
		
		try
		{
		    tag.addEventListener(type, handler, false);
		}
		catch (err)
		{
			tag.attachEvent("on" + type, handler);
		}
	}
	
	/** Method to remove a listener from an object */
	this.removeListener = function(tag, type, handler)
	{
		if (!tag) return;
		if (this.isTouchDevice())
		{
			if (type == "mouseup" || type == "click")
			{
				tag.ontouchend = null;
			}
			else if (type == "mousedown")
			{
				tag.ontouchstart = null;
			}
			else if (type == "mousemove")
			{
				tag.ontouchmove = null;
			}
		}
		try
		{
		    tag.removeEventListener(type, handler, false);
		}
		catch (err)
		{
			tag.detachEvent(type, handler);
		}
	}

	/* This function makes a div scrollable with android and iphone */
	this.scrollableDiv = function(id)
	{
		var element = id;
		if (typeof is == 'string') element = document.getElementOf(id);

		var touchStartPos = 0;
		if (acorns.system.isTouchDevice() && (acorns.system.isAndroid() || acorns.system.isMobilePhone()))
		{
			acorns.system.addListener(element, "touchstart", function(event) 
			{  
				if (event.touches.length > 1) return;
				scrollStartPos = this.scrollTop + event.touches[0].pageY;
				if (event.preventDefault) event.preventDefault();  
			});
			acorns.system.addListener(element, "touchmove", function(event) 
			{
				if (event.touches.length > 1) return;
				this.scrollTop = scrollStartPos - event.touches[0].pageY;
				if (event.preventDefault) event.preventDefault();  
			});
		}
	}
	
	/* This function decides if this device is a touch device */
	this.isTouchDevice = function()
	{
		try
		{
			document.createEvent("TouchEvent");
			return true;
		}
		catch(e)  
		{	
			return false;	
		}
	}

	/** Create a touch scroll event */
	this.touchScroll = function(element)
	{
		if(this.isTouchDevice())
		{  
			var scrollStartPosY=0;

			element.ontouchstart = function(event) 
			{
				scrollStartPosY=this.scrollTop+event.touches[0].pageY;
			};

			element.ontouchmove = function(event) 
			{
				this.scrollTop=scrollStartPosY-event.touches[0].pageY;
			};
		}
	}
	
	/** Get coordinates of event for both touch screens and for mouse clicks */
    this.getCoords = function(event, z)
	{
		var x = event.clientX;
		var y = event.clientY;
		if (x==undefined)
		{
			var changes = (event.changedTouches) ? event.changedTouches : event.touches;
			var touch = changes[0];
			x = touch.pageX;
			y = touch.pageY;
		}
		return {x: x, y: y};
	}
	

	/** Get the size of the display area */
	this.getWindowSize = function()
	{
		var width = 0, height = 0;
		if( typeof( window.innerWidth ) == 'number' ) 
		{
			//Non-IE
			width = window.innerWidth;
			height = window.innerHeight;
		} 
		else if ( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) 
		{
			//IE 6+ in 'standards compliant mode'
			width = document.documentElement.clientWidth;
			height = document.documentElement.clientHeight;
		} 
		else if ( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) 
		{
			//IE 4 compatible
			width = document.body.clientWidth;
			height = document.body.clientHeight;
		}
		
		// Explorer sometimes has not set the width/height of the window and reload is required
		if (width==0 && height==0) 
		{
			location.reload(true); 
			return;
		}
		
		return {width:width, height: height };
	};  // End of getWindowSize()
	
	

	// Enables fast string concatenation
	this.StringBuffer  = function()
	{
	   var buffer = []; 
	   this.append = function(string) { buffer.push(string); };
	   this.toString = function() { return buffer.join(""); };
	}

	// Find coordinate position of elements (id can be a tag or the id of a tag)
	this.getPosition = function(id)
	{
	    var idTag = id;
		if (typeof id == "string")  idTag = document.getElementById(id);
		   
		var tag = idTag;
		var x = 0, y = 0;
		while( tag && !isNaN( tag.offsetLeft ) && !isNaN( tag.offsetTop ) ) 
		{
			x += tag.offsetLeft - tag.scrollLeft;
			y += tag.offsetTop - tag.scrollTop;
			tag = tag.offsetParent;
		}
		return { top: y, left: x, height: idTag.offsetHeight, width: idTag.offsetWidth};
	}

	// Prototype for creating RGB color strings
	this.getColor = function(red, green, blue)
	{
	   return "rgb(" + red + "," + green + "," + blue + ")";
	}	
	
	// Search from this element to see if any child element match the name
	this.findElementByName = function(node, name)
	{
		if (node.name == name) return node;
		if (node.getAttribute && node.getAttribute("name") == name) return node;
		if (!node.hasChildNodes()) return false;
		
		var element;
		for (var c=0; c<node.childNodes.length; c++)
		{
			element = this.findElementByName(node.childNodes[c], name);
			if (element) return element;
		}
		return false;
	}
	
	/** Trim leading and trailing whitespzed */
	var ltrim = function(str) { return str.replace(new RegExp("^[ + \\s + ]+", "g"), ""); }
	var rtrim = function(str)  { return str.replace(new RegExp("[ + \\s + ]+$", "g"), "");	}
	this.trim = function(str) 	{ return ltrim(rtrim(str));	}
	
	// Dynamic programming spell check algorithm (compute the distance between two strings)
    this.stringDistance = function(source, target)
	{ 
		var sLen = source.length, tLen = target.length;
		if (sLen==0) return tLen;
		if (tLen==0) return sLen;
		 
		// distance is a table with lenStr1+1 rows and lenStr2+1 columns
		var distance = new Array(sLen+1);
		for (var d=0; d<distance.length; d++)	distance[d] = new Array(tLen+1);
		  
		// Initialize the arrays
		for (var i=0; i<=sLen; i++) distance[i][0] = i;
		for (var j=1; j<=tLen; j++) distance[0][j] = j;
		 
		// Perform the algorithm
		var cost, costDelete, costInsert, costSubstitute, costTranspose;
		for (var i=1; i<=sLen; i++)
		{  for (var j=1; j<=tLen; j++)
			{  if (source.charAt(i-1)==target.charAt(j-1)) cost = 0;
				else cost = 1;
			   
				costDelete     = distance[i-1][j] + 1;
				costInsert     = distance[i][j-1] + 1;
				costSubstitute = distance[i-1][j-1] + cost;   
			   
				distance[i][j] = costDelete;
				if (distance[i][j] > costInsert)     distance[i][j] = costInsert;
				if (distance[i][j] > costSubstitute) distance[i][j] = costSubstitute;
				if(i > 1 && j > 1 && source.charAt(i-1) == target.charAt(j-2)
								  && source.charAt(i-2) == target.charAt(j-1))
				{               
					costTranspose = distance[i-2][j-2] + cost;
					if (distance[i][j] > costTranspose) distance[i][j] = costTranspose;
				}
			}     // End of inner j loop
		}        // End of outer i loop
		return distance[sLen][tLen];
	}  	// End of stringDistance()
	
	// Search array of points for the point with a matching value
	this.binarySearchPoints = function(points, value)
	{
		var top = -1, bottom = points.length, middle, pointValue;
		
		while (top+1 < bottom)
		{
			middle = (Math.floor( (top + bottom)/2 ));
			pointValue =  points[middle].getXY();
			if (value>=pointValue.x && value<pointValue.y) return middle;
			if (value<pointValue.x) bottom = middle;
			else top = middle;
		}
		return top;
	}
	
	// Apply an indigenous font to a particular tag		
	this.applyFont = function(tag, lang)
	{
		if (lang == undefined) return;
		var fonts = acorns.getFonts();
		if (fonts == undefined) return;
		
		var font = fonts[lang];
		if (font==undefined) 
		{
			tag.style.fontFamily = "serif";
			tag.style.fontSize = Math.floor(12 * acorns.system.getFontRatio()) + "px";
		}
		else
		{
			tag.style.fontFamily = font["Family"];
			tag.style.fontSize = Math.floor(font["Size"] * acorns.system.getFontRatio()) + "px";
		}
	}

}	// End of System class

// Prototype for ACORNS lesson object
function Lesson(acornsObject, lessonType, lessonNode)
{
	var MAX_LAYERS = 10, MAX_LAYER_NAME_LENGTH = 15;
	var acorns = acornsObject;
	var layerNames = [];
	var backgroundImage = undefined;
	var lessonObject = undefined;
	
	var name,  index;
	for (var layerNo=1; layerNo<=MAX_LAYERS; layerNo++)  layerNames.push("");
	
	var layerList = lessonNode.getElementsByTagName("layer");
	for (var layerNo=0; layerNo<layerList.length; layerNo++)
	{
		name = layerList[layerNo].getAttribute("name");
		index = parseInt(layerList[layerNo].getAttribute("value"), 10);
		if (name != null  && name.length>0) layerNames[index] = name; 
		else layerNames[index] = undefined;
	}
	
	var title = lessonNode.getAttribute("title");
	if (title==null) title = "";
	
	var name = lessonNode.getAttribute("name");
	if (name==null) name = "";
	
    var type = lessonType;
    var lessonClass = type.split(' ').join('');
    var layer = 1;
	
	try
	{
		this.lessonObject = eval('new ' + lessonClass + '(acorns, this)');
	}
	catch(e) {}
	
    this.environment = new Environment(acorns, null, null, 20);
    this.objectList = new Array();
    this.paramList = new Array();
   
    this.isPlayable = function() { return this.lessonObject.isPlayable(layer); }
	this.parse = function(parseObject, node)	
	{  
		this.lessonObject.parse(parseObject, node); 
	}

	// Set the background picture for the lesson
	this.setBackground = function(picture)	{ backgroundImage = picture; }
	
	var setLayerName = function()
	{
		var layerName = document.getElementById("layername");
		var name = layerNames[layer];
		if (name!=undefined)
		{
			if (name.length > MAX_LAYER_NAME_LENGTH) name = name.substring(0,15);
			layerName.innerHTML = name;
			layerName.style.display = "inline";
		}
		else layerName.style.display = "none";
		
		var layerButton = document.getElementById("up");
		if (layer == MAX_LAYERS)
			layerButton.title = "Current layer " + layer + "is maximum";
		else
			layerButton.title = "Move to next layer from " + layer;
		
		layerButton = document.getElementById("down");
		if (layer == 1)
			layerButton.title = "Current layer " + layer + " is minimum";
		else
			layerButton.title = "Move to previous layer from " + layer;
	}
	
    this.play = function(reset, changeLesson) 
	{ 
		setLayerName();
		if (!reset) 
		{
			this.lessonObject.play(false, changeLesson);
			return;
		}
		
		var main = document.getElementById("main");
		if ( main.hasChildNodes() )
		{
			while (main.childNodes.length > 0)
			{
				main.removeChild( main.firstChild );       
			} 
		}

		var children = document.body.childNodes;
		for (c=children.length-1; c>=0; c--)
		{
			if (children[c].tagName.toLowerCase() == "audio") continue;
			if (children[c].tagName.toLowerCase() == "span") continue;
			if (children[c].tagName.toLowerCase() == "bgsound") continue;
			if (children[c].id  == "main") continue;
			if (children[c].id  == "controls") continue;
			if (children[c].id  == "popup") continue;
			document.body.removeChild(children[c]);
		}

		var windowSize = acorns.system.getWindowSize();
		document.body.style.height = (windowSize.height - 20) + "px";
		document.body.style.maxHeight = (windowSize.height -20) + "px";
		document.body.style.minHeight = (windowSize.height - 20) + "px";
		main.style.overflow = "hidden";		
		
		 // Background picture, colors, font size
		var body = document.getElementById("body");
		body.style.overflow = "hidden";
		this.environment.configureEnvironment(body, backgroundImage);

		var anchor = document.getElementById("anchor");  // Determine if the link tag should be visible.
		if (acorns.getLinkedLesson(false)) anchor.style.display = "inline";
		else anchor.style.display = "none";

		acorns.setTimeUpdateListener( null );
		acorns.stopAudio();
		
		if (window.audioTools)
			window.audioTools.reset();

		this.lessonObject.getOptions().initializeOptions(acorns.getCookie());
		this.lessonObject.configurePlayPanel(main);
		
		this.lessonObject.play(reset, changeLesson);
		var popupInfo = this.lessonObject.getOptions().getPopupInfo();
		acorns.widgets.configurePopupMenu(popupInfo);
	}
   
    /* Switch to next layer, but return a non-empty string if next layer is not playable */ 
    this.nextLayer = function()
    {
		if (layer>=MAX_LAYERS) return false;
		var playable = this.lessonObject.isPlayable(layer+1);
		if (!playable)	return false;

		layer++;  
		setLayerName();
		this.play(true, true);
		return true;	
    }
   
    /* Switch to previous layer, but return a non-empty string if next layer is not playable */ 
    this.previousLayer = function()
    {
		if (layer<=1) return false;
		var playable = this.lessonObject.isPlayable(layer-1);
		if (!playable)	return false;
		
		layer--;	
		setLayerName();
		this.play(true, true);
		return true;
    }
	
	this.setPlayableLayer = function()
	{
		var layerNo;
		for (layerNo=1; layerNo<MAX_LAYERS; layerNo++)
		{
			if (this.lessonObject.isPlayable(layerNo))
			{  
				layer = layerNo; return true; 
			}
		}
		return false;
	}
   
    this.getLayer = function() { return layer; }
	this.getName = function() { return name; }
	
	this.getHelpMessage = function() 
	{ return this.lessonObject.getHelpMessage(); };

}   // End of Lessoon class

// Prototype for Acorns layer object
function Layer(nameL, valueL, languageL, alignL)
{
   var name = (nameL==null) ? "" : nameL;
   var value = (valueL==null) ? 1 : valueL;
   var language = (languageL==undefined || languageL==null || languageL.length==0) ? "English" : languageL;
   var align = (alignL==undefined || alignL==null || alignL.length==0) ? "left" : alignL;
   
   this.objectList = new Array();  
   this.paramList = new Array();
   
   this.getAlign = function() { return align; }
   this.getLanguage = function() { return language; }
   this.getName = function() { return name; }
   this.getValue = function() { return value; }
}

// Prototype for related phrase category object
function Category(sentenceC, pictureC, audioC)
{
	var sentence = sentenceC;
	var picture = pictureC;
	var audio  = audioC;
	
	this.objectList = new Array();
	this.paramList = new Array();
	
	this.getSentence = function() { return sentence; }
	this.getPicture = function() { return picture; }
	this.getAudio = function() { return audio; }
	this.getPoints = function() { return this.objectList; }
}

// Prototype for ACORNS point object
function Point(acornsObject, xP, yP, typeP, glossP, spellP
						   , languageP, descriptionP, linkP, pictureP, soundP, rateP, playbackP, framesP)
{
	var acorns = acornsObject;
	var x = (xP==null) ? 0 : xP;
	var y = (yP==null) ? 0 : yP;
	var type = (typeP==null) ? "sound" : typeP;
	var gloss = glossP.replace(/\s+/g, " ");
	var spell = spellP.replace(/ +/g, " ");
	var language = (languageP==null) ? "Default" : languageP;
	var description = descriptionP;
	var link = linkP;
	var picture = (pictureP==null) ? "" : pictureP;
	var sound = (soundP==null) ? "" :  soundP;
	var rate = (rateP==null) ? "" : rateP;
	var playback = (playbackP==null) ? "" : playbackP;
	var frames = (framesP==null) ? "" : framesP;
	var category = undefined;
	var phrases = undefined;
	
	this.isAudio = function() { return (type=="sound") ? true : false; }
    this.isComplete = function(noSound) 
	{ 
		if (noSound)
			return spell.length>0 && gloss.length>0; 
 		return sound.length>0 && spell.length>0 && gloss.length>0; 
	}
	
	this.getLanguage = function() { return language; }
	
	this.isSentence = function()
	{
		if (spell.split(" ").length < 2) return false;
		return true;
	}

	this.display = function(block, options)
	{
		var types = [ "gloss", "spell", "sound", "description", "picture" ];
		var data = [ gloss, "", "", description, "" ];
		var index, element, optionValue;
		if (type != "sound") return;
		for (index=0; index<types.length; index++)
		{
			element = acorns.system.findElementByName(block, types[index]);
			if (element)  
			{
				optionValue = options[index];
				if (optionValue == "t") optionValue = "y";
				
				switch (types[index])
				{
					case "picture":
						if (optionValue=="y" && picture!="") picture.centerAndScalePicture(element);
						break;
					case "sound":
						if (optionValue=="y" && sound!="") acorns.playAudio(sound);
						break;
					case "spell":
						if (optionValue=="y") 
						{
							element.innerHTML = spell;
							this.applyFont(element, language);
						}
						else element.innerHTML = "";
						break;
					default:
						element.style.fontSize = Math.floor(12 * acorns.system.getFontRatio()) + "px";

						if (optionValue=="y") element.innerHTML = data[index];
						else element.innerHTML = "";
						break;
				}
			}
		}
	}	// End of display()
	
	// Play the audio
	this.playAudio = function()     { if (type=="sound") acorns.playAudio(sound); }
	
	this.playAudioSlow = function() { if (type=="sound") acorns.playSlow(sound); }
	
	// Apply an indigenous font to a particular tag		
	this.applyFont = function(tag, lang)
	{
		if (lang == undefined) lang = language;
		acorns.system.applyFont(tag, lang);
	}
		
    this.toString = function() { return x + "/" + y + " " + sound + "\n" + gloss + "\n" + spell + "\n\n"; }
	
	this.getCoordinates = function() 
	{ 
		return { x: x, y: y };
	}
	
	this.isSameCoordinates = function(point)
	{
		var coords = point.getCoordinates();
		return x == coords.x && y == coords.y;
	}
	
	this.clone = function()
	{
		var point = new Point(acorns, x, y, type, gloss, spell
						   , language, description, link, picture, sound, rate, playback, frames);
		return point;
	}
	
	this.getLink = function()   { return link; }
	this.getSpell = function()  { return spell; }
	this.setSpell = function(s) { spell = s; }
	this.getGloss = function()  { return gloss; }
	this.getFrames = function() { return frames * rate/playback; }
	this.getPicture = function() { return picture; }
	this.getAudio = function() { return sound; }
	this.getAudioFormat = function() { return { rate:rate, playback:playback, frames:frames }; }
	this.getXY = function() { return {x:x, y:y}; }
	this.getDescription = function() { return description; }
	this.getCategory = function() { return category; }
	this.setCategory = function(c) { category = c; }
	this.getPhrases	= function()	 { return phrases; }
	this.setPhrases	= function(p)	 { phrases = p; }

	
}   // End of Point class

// Prototype to represent a picture, which can be rotated and scales
function Picture(acornsObject, srcP, typeP, angleP, valueP)
{
	var acorns = acornsObject;
    var src = (srcP==null)? "" : srcP;

	/* Type: 0 - center in area, 1 - fill with distortion, 2 - fill and clip ends
		     3 - fill vertically and clip horizontally, 4 - fill horizontally and clip vertically
			 
			 If > 4, then it is a scale factor.
	*/
	var type = (typeP==null || typeP==undefined) ? "0" : typeP;
	var angle = (angleP==null || typeP==undefined) ? 0 : angleP;
	var value = (valueP==null || typeP==undefined) ? 0 : valueP;  // Lesson picture index

    var getSupportedProperty = function(propArray)
	{  var root=document.documentElement; 
	   for (var i=0; i<propArray.length; i++)
	   { 
		   if (typeof root.style[propArray[i]]=="string")
		   { 	
			   return propArray[i]; 
		   } 
	   }
	}

    var cssTransform = getSupportedProperty(['transform', 'MozTransform'
    	                             , 'WebkitTransform', 'msTransform', 'OTransform']);
    
	this.objectList = new  Array();
	
	var applyRotation = function(target, angle) 
	{
	   if (typeof cssTransform == "string")  
	   {
		  target.style[cssTransform] = "rotate(" + angle + "deg)";
	   }
	   else 
	   {
			target.style["filter"] 
				= "progid:DXImageTransform.Microsoft.BasicImage(rotation=" 
					+ Math.floor(angle/90) + ")";
		   }
	}
	
	// Apply transformation to picture and store in the img tag
	var transformPicture = function(img, image, size)
	{
		if (img.src != image.src)
		{
			img.src = image.src;
		}
		
		// Scale the loaded picture
		var origWidth = image.width, newWidth = image.width;
		var origHeight = image.height, newHeight = image.height;
		if (angle == 90 || angle == 270)
		{
		   newWidth = image.height;
		   newHeight = image.width;
		}
		
		var scaleX = size.width / newWidth;
		var scaleY = size.height / newHeight;
		var scale = 1;
		switch (type)
		{
		   case "0": 
		   default:
			 scale = Math.min(scaleX, scaleY);
			 break;
		   case "1":
			 scale = 1;
			 break;
		   case "2":
			 scale = Math.max(scaleX, scaleY);
			 break;
		   case "3":
			 scale = scaleY;
			 break;
		   case "4": 
			 scale = scaleX;
			 break;
		}
		applyRotation(img, angle);
	   
		newWidth = newWidth * scale;
		newHeight = newHeight * scale;	
		origWidth *= scale;
		origHeight *= scale;

		var x = Math.floor((size.width - ((typeof cssTransform == "string") ? origWidth : newWidth))/2 );
		var y = Math.floor((size.height - ((typeof cssTransform == "string") ? origHeight : newHeight))/2);
		if (type==1)
		{
			img.width = size.width; 
			img.height = size.height;
		}
		else
		{
		   // Note: IE8 apperantly doesn't correctly alter the size of a rotated image.
		   img.width = Math.ceil(origWidth);
		   img.height = Math.ceil(origHeight);
		   img.style.position = "absolute";
		   img.style.top = y + "px";
		   img.style.left = x + "px";
		   img.style.width = Math.ceil(origWidth) + "px";
		   img.style.height = Math.ceil(origHeight) + "px";
		}
		console.log(img.src + " loadStatus = " + img.complete);
		
		var count = 0;
 		while (!img.complete && count<10)
		{
			 sleep(20)
			 count++;
			 console.log("Waiting for img to load " + count);
		}

		img.title = src + " loaded";
	}

	// hack to simulate a sleep function
	var sleep = function(ms)
	{	
		const date = Date.now();
		var current = null;
		do 
		{
			current = Date.now();
			
		} while (current - date < ms);
	}

	/* Method that centers, rotates, and scales a picture
       Assumptions: 1) display area has an equal width and height
					2) angle doesn't have to be in 90 degree increments, but parts will be cut off if not
					3) The scaling is set so the original picture will fit in the canvas
					
	*/
	this.centerAndScalePicture = function(block)
	{
		block.style.background = acorns.BACKGROUND_COLOR;
		block.style.overflow = "hidden";
		block.style.textAlign = "center";
		block.style.position = "absolute";
		block.style.top = "0px";
		
		var size = { width: parseInt(block.style.width, 10), height: parseInt(block.style.height, 10) };
		
		// Find the img tag within the block tag
		var img = undefined;
		for (var i=0; i<block.childNodes.length; i++)
		{
			if (block.childNodes[i].nodeName.toLowerCase() == "img")
			{
			   img = block.childNodes[i];
			   break;
			}
		}

		var image = new Image();
		image.src = src;
		img.src = src;
		image.setAttribute('alt', 'Try reloading page');
		if (src == "") 
		{
			img.src = "";
			img.style.display = "none";
		}
		else img.style.display = "";
		
		image.onabort = function()	{ image.alt = "Image load aborted";	}
		if (!acorns.system.isOldIE() && image.complete) { if (src!="") transformPicture(img, image, size); }
		else
		{
			image.onload = function() {  if (src!="") transformPicture(img, image, size); }
		}

	};   // End of centerAndScalePicture()
	
	this.getValue = function() { return value; }
	this.getSrc = function() { return src; }
	
	this.getScaleFactor = function() 
	{ 
		var scale = parseInt(type,10);
		if (scale<=4) return 100; 
		return parseInt(scale,10);
	}

	// Determine if the picture is loaded by looking at the title attribute.
	this.isLoaded = function(tag)
	{
		return tag.title.length > 0;
	}
	
	this.setScaleFactor = function(scale)
	{
		type = "" + scale;
	}
	
	this.getAngle = function() { return angle; }
}   // End of Picture class

// Method to handle popup menu options for a lesson
function Options(acornsObject, lesson, items)
{
	var menuItems = items;
	var lessonData = lesson;
	var acorns = acornsObject;
	var popupInfo = undefined;
	
	var MIN_FONT_SIZE = 8, MAX_FONT_SIZE = 40;
    var MIN_DIFFICULTY = 1, MAX_DIFFICULTY = 5;
	var MIN_SCALE = 10, MAX_SCALE = 500;
	var CLICK_SCALE = 25;
	
	var difficultyLevel = 1;
	var difficulties = [ "", "basic", "moderate", "challenging", "hard", "extreme" ];
	var fontSize = 12;
	var scaleFactor = 100;
	var options = {gloss: "y", spell: "y", sound: "y", toggle: "y", continuous: "n", select: "y" };
	var categoryNo = 0;

	// Get the text for the popup option menu.
	var getMenuTextItems = function()
	{
		var text = [];
		for (var m=0; m<menuItems.length; m++)
		{
			switch(menuItems[m])
			{
				case GAP: 
					text.push("");
					break;
				case RESET:
					text.push("Reset lesson");
					break;
				case CATEGORY:
					text.push("Next category after " + categoryNo);
					break;
				case FONT:
					if (fontSize==MAX_FONT_SIZE)
						 text.push("Font size is now maximum");
					else text.push("Raise font size from " + fontSize);
					
					if (fontSize==MIN_FONT_SIZE)
						 text.push("Font size is now minimum");
					else text.push("Cut font size from " + fontSize);
					break;
				case DIFFICULTY:
					if (difficultyLevel==MAX_DIFFICULTY) 
						 text.push("Difficulty is now maximum");
					else text.push("Raise difficulty from " + difficulties[difficultyLevel]);
					if (difficultyLevel==MIN_DIFFICULTY)
						text.push("Difficulty is now minimum");
					else text.push("Cut difficulty from " + difficulties[difficultyLevel]);
					break;
				case SCALE:
					if (difficultyLevel==MAX_SCALE) 
						 text.push("Scale is now maximum");
					else text.push("Raise scale from " + scaleFactor);
					if (difficultyLevel==MIN_SCALE)
						text.push("Scale is now minimum");
					else text.push("Cut scale from " + scaleFactor);
					break;
				case OPTIONS:
					if (options["gloss"] == 'y') text.push("Turn translation off");
					else text.push("Turn translation on");
					
					if (options["spell"] == 'y') text.push("Turn indigenous font off");
					else text.push("Turn indigenous font on");
					
					if (options["sound"] == 'y') text.push("Turn audio off");
					else text.push("Turn audio on");
					break;
				case TOGGLE:
					if (options["toggle"] == 'y') text.push("Show first Language");
					else text.push("Show indigenous");
					break;
				case SELECT:
					if (options["select"] == "y") text.push("Select indigenous text");
					else text.push("Select first language");
					break;
				case CONTINUOUS:
					if (options["continuous"] == 'y') text.push("Turn continuous mode off");
					else text.push("Turn continuous mode on");
					break;
			}	// end switch
		}	// end for
		return text;
	}
	
	// Create a cookie based on the options for this lesson
	var makeCookie = function()
	{
		var cookie = "";
		for (var m=0; m<menuItems.length; m++)
		{
			switch(menuItems[m])
			{
				case CATEGORY:
					cookie += categoryNo;
					break;
				case FONT:
					cookie += fontSize;
					break;
				case DIFFICULTY:
					cookie += difficultyLevel;
					break;
				case SCALE:
					cookie += scaleFactor;
					break;
				case OPTIONS:
					cookie += options["gloss"] + options["spell"] + options["sound"];
					break;
				case TOGGLE:
					cookie += options["toggle"];
					break;
				case SELECT:
					cookie += options["select"];
					break;
				case CONTINUOUS:
					cookie += options["continuous"];
					break;
			}	// end switch
			if (menuItems[m]!=GAP && menuItems[m]!=RESET) cookie += "/";
		}
		if (cookie.length>0 && cookie.charAt(cookie.length-1) == '/') 
			cookie = cookie.substring(0,cookie.length-1);
		return cookie;
	}

	/* Handler to process lesson popup options */
    var menuHandler = function(type)
    {
		switch(type)
		{
			case "reset":
				categoryNo = 0;
				break;
				
			case "category":
				categoryNo++;
				break;
				
			case "toggle":
			case "select":
			case "gloss":
			case "spell":
			case "sound":
			case "continuous":
				if (options[type] == "y") options[type] = "n";
				else options[type] = "y";
				break;

			case "difficultyup":
				if (difficultyLevel >= MAX_DIFFICULTY) { acorns.beep();  }
				else difficultyLevel++;
				break;
				
			case "difficultydown":
				if (difficultyLevel <= MIN_DIFFICULTY) { acorns.beep();  }
				else difficultyLevel--;
				break;
				
			case "fontup":
				if (fontSize >= MAX_FONT_SIZE) { acorns.beep(); }
				else 
				{
				   fontSize = Math.floor(fontSize * 120 / 100);
				   if (fontSize > MAX_FONT_SIZE) 
				   { 
						fontSize = MAX_FONT_SIZE; 
				   }
				}
				break;
				
			case "fontdown":
				if (fontSize <= MIN_FONT_SIZE) { acorns.beep();  }
				else
				{
					fontSize = Math.floor(fontSize * 100 / 120);
				    if (fontSize < MIN_FONT_SIZE) 
				    { 
						fontSize = MIN_FONT_SIZE; 
				    }
				}
				break;
				
			case "scaleup":
				if (scaleFactor >= MAX_SCALE) { acorns.beep();  }
				else scaleFactor += Math.floor(scaleFactor * CLICK_SCALE / 100.);
				break;
				
			case "scaledown":
				if (scaleFactor <= MIN_SCALE) { acorns.beep();  }
				else scaleFactor -= Math.floor(scaleFactor * CLICK_SCALE / 100.);
				break;
		}

		acorns.setCookie(makeCookie());
		setTimeout( function() { lessonData.play(true) }, 250);
		
		var items = popupInfo.items;
		var text = popupInfo.text;
		var data = [];
		for (var i=0; i<items.length; i++)
		{
			data.push(items[i]);
			data.push(text[i]);
		}
		return data.join('~');
	}

	this.getPopupInfo = function() 
	{
		var items = [];
		for (var m=0; m<menuItems.length; m++)
		{
			switch(menuItems[m])
			{
				case GAP:
					items.push("");
					break;
				case RESET:
					items.push("reset");
					break;
				case CATEGORY:
					items.push("category");
					break;
				case FONT:
					items.push("fontup");
					items.push("fontdown");
					break;
				case TOGGLE:
					items.push("toggle");
					break;
				case SELECT:
					items.push("select");
					break;
				case CONTINUOUS:
					items.push("continuous");
					break;
				case DIFFICULTY:
					items.push("difficultyup");
					items.push("difficultydown");
					break;
				case SCALE:
					items.push("scaleup");
					items.push("scaledown");
					break;
				case OPTIONS:
					items.push("gloss");
					items.push("spell");
					items.push("sound");
					break;
			}	// end switch
		}
		
		var handler = menuHandler;
		popupInfo = { items: items, text: getMenuTextItems(), handler: handler } 
		return popupInfo;
	}
	
	this.initializeOptions = function(cookie) 
	{
		if (cookie==undefined) return;
	
		var cookies = cookie.split("/");
		
		var cookieNo = 0, value;
		for (var m=0; m<menuItems.length; m++)
		{
			if (cookieNo >= cookies.length) break;
			
			switch (menuItems[m])
			{
				case CATEGORY:
					value = parseInt(cookies[cookieNo++],10);
					categoryNo = value;
					break;
				case FONT:
					value = parseInt(cookies[cookieNo++],10);
					if (value>=MIN_FONT_SIZE && value<=MAX_FONT_SIZE) fontSize = value;
					break;
				case DIFFICULTY:
					value = parseInt(cookies[cookieNo++],10);
					if (value>=MIN_DIFFICULTY && value<=MAX_DIFFICULTY) difficultyLevel = value;
					break;
				case SCALE:
					value = parseInt(cookies[cookieNo++],10);
					if (value>=MIN_SCALE && value<=MAX_SCALE) scaleFactor = value;
					break;
				case OPTIONS:
					options["gloss"] = cookies[cookieNo][0];
					options["spell"] = cookies[cookieNo][1];
					options["sound"] = cookies[cookieNo++][2];
					break;
				case TOGGLE:
					options["toggle"] = cookies[cookieNo++][0];
					break;
				case SELECT:
					options["select"] = cookies[cookieNo++][0];
					break;
				case CONTINUOUS:
					options["continuous"] = cookies[cookieNo++][0];
					break;
			}
		}
	}
	
	this.getDifficultyLevel = function() 
	{ 
		return { difficulty: difficultyLevel, max: MAX_DIFFICULTY, min:MIN_DIFFICULTY }; 
	}
	this.getFontSize = function() { return fontSize; }
	this.getScaleFactor = function() { return scaleFactor; }
	this.getOptions = function() { return options; }
	this.getCategoryNo = function() { return categoryNo; }
	this.setCategoryNo = function(num) { categoryNo = num; }
	
	// Set an option (only some of the options can be set
	this.setOptions = function(type, value)
	{
		switch (type)
		{
			case CATEGORY:
				categoryNo = value;
				break;
		}
	}
	this.setFontSize = function(size) { fontSize = size; }
}

/** Class to make and configure components to interact with the user **/
function Widgets(acornsObject)
{
	var acorns = acornsObject;

	var BUTTON_SIZE = Math.ceil(40 * acorns.system.getResolutionRatio());
	var BUTTON_BORDER = Math.ceil(3 * acorns.system.getResolutionRatio());
	var CELL_SIZE = BUTTON_SIZE + 2 * BUTTON_BORDER;
	var GAP = Math.ceil(8 * acorns.system.getResolutionRatio());
	var SCORE_HEIGHT = Math.ceil(70 * acorns.system.getResolutionRatio());
	var MIN_ROW_WIDTH = 480;
	var MENU_FONT = 16;

	var body = undefined;
	var popupMenu = undefined;
	var resultPanel = undefined;
	
	/* Configure a particular popup menu option */
	this.configurePopupMenuItem = function(data, type, popupHandler)
	{
		/* Handle clicks of the popup menu items */
		var menuClick = function(tag, type, popupMenuHandler)
		{
			var data = popupHandler(type);
			var updates = data.split("~");
			
			if (updates.length == 1)
				tag.innerHTML = updates[0];
			else
			{
				var parent = tag.parentNode;
				var element
				for (var i = 1; i<updates.length; i+=2)
				{
				    if (updates[i-1].length > 0)
					{
						element = acorns.system.findElementByName(parent, updates[i-1]);
						if (element!=undefined)
							element.innerHTML = updates[i];
					}				}
			}
			tag.parentNode.style.display = "none";
		}
		
		var fontSize;
		var p = document.createElement("div");
		p.style.marginTop = "0px";
		p.style.marginBottom = "10px";
		if (data.length>0)
		{
			acorns.system.addListener(p, "click", function(e) { menuClick(p, type, popupHandler) });
			p.name = type;
			p.style.fontSize = "xx-large";; 
			p.style.color = "black";
			p.innerHTML = data + '<br />';
		}
		else  
		{
		   p.style.fontSize = "10pt";
		   p.style.textAlign = "center";
		   p.innerHTML = "<hr style='width:90%' />";
		}
		return p;
	}

	/* Configure the popup menu optioins */
	this.configurePopupMenu = function(info)
	{
		if (info==undefined) return;
		if (popupMenu == undefined)
			popupMenu = document.getElementById("popup");
		
		if (popupMenu.hasChildNodes() )
		{
			while (popupMenu.childNodes.length > 0)
			{
				popupMenu.removeChild( popupMenu.firstChild );       
			} 
		}
		
		var items = info.items;
		var text = info.text;
		var span, maxWidth = 0;
		var spanFont = Math.ceil(MENU_FONT * acorns.system.getFontRatio());
		var textFont = "xx-large";
		popupMenu.style.display = "block";
		
		for (var itemNo=0; itemNo<items.length; itemNo++)
		{
			span = this.configurePopupMenuItem(text[itemNo], items[itemNo], info.handler);
			popupMenu.appendChild(span);
		
			width = acorns.system.widthOfString(text[itemNo], textFont);
			if (width > maxWidth) maxWidth = width;
		}
		if (items.length != 0)
		{
			var input = document.createElement("input");
			input.type = "button";
			input.style.fontSize = spanFont + "px";
			input.value = "OK";
			input.style.position = "relative";
			input.style.height = Math.ceil(25 * acorns.system.getFontRatio()) + "px";
			input.style.borderStyle = "outset";
			acorns.system.addListener(input, "click", function(e) 
			{ 
				popupMenu.style.display='none'; 
				setTimeout( function() { acorns.getActiveLesson().play(true) }, 250); 
			});
			popupMenu.appendChild(input);
			input.style.left = ((maxWidth - input.clientWidth)/2) + "px";
			var br = document.createElement("div");
			br.style.height = GAP + "px";
			popupMenu.appendChild(br);
		}
		popupMenu.style.width = maxWidth + GAP + "px";
		popupMenu.style.display = "none";
	}
	
	this.makePopupMenuVisible = function()
	{
		var infoPosition;
		
		if (!popupMenu)
		{
			acorns.beep();
			return;
		}
		
		if (popupMenu.childNodes.length == 0) return;
		infoPosition = acorns.system.getPosition("info");
		var popWidth = parseInt(popupMenu.style.width);
		
		var left = Math.floor(infoPosition.left - popWidth/2);
		if (left<0) left = 0;
		popupMenu.style.left = left + "px"; 
		popupMenu.style.display = "block";
		
		var top = Math.max(infoPosition.top - popupMenu.clientHeight, 0);
		var height = Math.min(popupMenu.clientHeight, popupMenu.parentNode.clientHeight);

		popupMenu.style.top = top + "px";
		popupMenu.style.height = height + "px";
		popupMenu.style.overflow = "auto";
		acorns.system.scrollableDiv(popupMenu);
	}

	/** Create a popup menu for the control buttons **/
	var makePopupMenu = function()
	{
		var div = document.createElement("div");
		div.style.zIndex = 99;
		div.style.borderStyle = "solid";
		div.style.backgroundColor = acorns.BACKGROUND_COLOR;
		div.style.position = "absolute";
		div.style.display = "none";
		div.id = "popup";
		return div;
	}

	// Create panel of icon buttons
	this.makeButtonPanel = function(text, tips, left, width, buttonNo, controlHandler, controls)
	{
		var button, buttonSize = BUTTON_SIZE;
		if (text.length * (buttonSize + 2*BUTTON_BORDER + GAP) - GAP > width)
			buttonSize = Math.floor((width + GAP)/ text.length -2*BUTTON_BORDER - GAP);

			if (controls == undefined)
		{
			controls = document.createElement("div");
			controls.setAttribute("name", "" + buttonNo);
			controls.style.position = "absolute";
			controls.style.bottom = "8px";
			controls.style.width = (width -6) + "px";
			controls.style.marginLeft = controls.style.marginRight = "auto";
			controls.style.textAlign = "center";
			controls.style.left = (left +3) + "px";
			controls.style.height = (buttonSize + 2*BUTTON_BORDER) + "px";
		}


		for (var i=0; i<text.length; i++)
		{
			button = acorns.widgets.makeIconButton(text[i], tips[i], controlHandler , buttonSize);
			button.setAttribute("name", "" + buttonNo);
			if (i<text.length - 1) button.style.marginRight = (GAP-1) + "px";
			controls.appendChild(button);
		}
		return controls;
	}
	
	/* Make the score panel, which is used by several lesson types */
	this.configureScorePanel = function(lessonObject, main, scoreOnly)
	{
		var div = document.createElement("div");
		div.style.textAlign = "center";
		div.style.position = "absolute";
		div.style.fontSize = Math.ceil(8 * acorns.system.getFontRatio()) + "pt";
		div.style.lineHeight = Math.floor(SCORE_HEIGHT / 4) + "px";
		div.style.paddingTop = 2 + "px";
		div.style.top = 0 + "px";
		div.style.right = 0 + "px";
		div.style.width = "2.5em";
		div.style.height = SCORE_HEIGHT + 'px';
		div.style.borderStyle = "solid";
		div.style.borderWidth = "1px";
		div.style.overflow = "hidden";
		div.id = "result";
		div.innerHTML = "0<br>of<br>0";
		lessonObject.environment.setColors(div);
		main.appendChild(div);
		resultPanel = div;
		if (scoreOnly) return resultPanel.offsetHeight;
		
		var scoreWidth = div.offsetWidth;
		var width  = main.clientWidth;

		div = document.createElement("div");
		div.style.position = "absolute";
		div.style.top = 0 + "px";
		div.style.left = 0 + "px";
		div.style.fontSize = (12 * acorns.system.getFontRatio()) + "pt";
		div.style.right = "4em";
		div.style.lineHeight = Math.floor(SCORE_HEIGHT / 2.5) + "px";
		div.style.width = (width - scoreWidth) + "px";
		div.style.height = SCORE_HEIGHT + "px";
		div.style.borderStyle = "solid";
		div.style.borderWidth = "1px";
		div.style.overflow = "hidden";
		div.id = "scorepanel";
		lessonObject.environment.setColors(div);
		main.appendChild(div);
		
		var span = document.createElement("span");
		span.name = "gloss";
		span.style.whiteSpace = "pre";
		div.appendChild(span);
		
		var br = document.createElement("br");
		div.appendChild(br);
		
		span = document.createElement("span");
		span.name = "spell";
		span.style.whiteSpace = "pre";
		span.style.fontSize = (12 * acorns.system.getFontRatio()) + "pt";
		div.appendChild(span);
		return div.offsetHeight;
	}

	// Method to make a grid of rows and columns
	this.makeGrid = function(panel, cellWidth, cellHeight, top, pictureHandler)
	{		
		var GAP = 3, SCROLL = 20, TOP = 10, BUTTON_BORDER = 6;
		if (top==undefined) top = TOP;
		var width = panel.clientWidth, height = panel.clientHeight - SCROLL - top;
		
		var columns = Math.floor((width - SCROLL - GAP)/(2*BUTTON_BORDER + cellWidth + GAP));
		if (columns<=0) columns = 1;
		
		var rows = Math.floor((height - SCROLL - GAP)/(2*BUTTON_BORDER + cellHeight + GAP));
		if (rows<=0) rows = 1;
		
		var rowWidth = columns*(cellWidth + 2*BUTTON_BORDER) + (columns-1)*GAP;
		var numberCells = rows * columns;

		var left = (width - rowWidth)/2;
	    var block = document.createElement("div");
		block.style.position = "absolute";
		block.style.width  = rowWidth + "px";
		block.style.height = (cellHeight + 2*BUTTON_BORDER) + "px";
		block.style.overflow = "hidden";
		block.style.top = top + "px";
		block.style.left = left + "px";
	
	    var div;
		
	    panel.appendChild(block);
		for (var buttonNo=0; buttonNo<numberCells; buttonNo++)
	    {
			div = acorns.widgets.makePictureButton(buttonNo, cellWidth
			        , (buttonNo % columns) * (cellWidth + 2 * BUTTON_BORDER + GAP)
					, pictureHandler );
			div.style.height = cellHeight + "px";
			div.style.borderWidth = BUTTON_BORDER +"px";
			div.style.borderColor = "grey";
			
			var picture = new Picture(acorns, "", 0, 0, 0);
			picture.centerAndScalePicture(div);

			block.appendChild(div);

	        if ( ((buttonNo + 1) % columns == 0)  && buttonNo<numberCells -1) 
			{
				block = document.createElement("div");
				block.style.position = "absolute";
				block.style.width  = rowWidth + "px";
				top += cellHeight + 2*BUTTON_BORDER + GAP;
				block.style.top = top + "px";
				block.style.left =  left + "px";
				panel.appendChild(block);
			}
	    }
		return numberCells;
	}
	
	this.makeDisplayBoxHTML = function(parent, lessonData, id)
	{
		var position = acorns.system.getPosition(parent);
		var windowSize = acorns.system.getWindowSize();
		var div = document.createElement("div");
		div.style.zIndex = 99;
		div.style.backgroundColor = acorns.BACKGROUND_COLOR;
		div.style.borderStyle = "solid";
		div.style.position = "absolute";
		div.style.display = "inline";
		if (lessonData) lessonData.environment.setColors(div);
		div.id = id;
		return div;
}
	
	// Create category sentence as a header
	this.makeCategoryHTML = function(parent, text)
	{
		var sentence = undefined;
		if (text)
		{
			sentence = document.createElement("p");
			sentence.style.textAlign = "center";
			sentence.setAttribute("name", "sentence");
			sentence.style.marginTop = "0px";
			sentence.style.marginBottom = "-3px";
			sentence.style.fontSize = height = (16 + acorns.system.getFontRatio()) + "px";
			sentence.style.backgroundColor = acorns.BACKGROUND_COLOR;
			sentence.style.position = "relative";
			sentence.innerHTML = text;
			parent.appendChild(sentence);
		}
		return sentence;
	}
	
	// Create category picture and if ut exist
	this.makePictureHTML = function(parent, picture, pictureSize)
	{
		var pictureComponent = undefined;
		if (picture && picture.getSrc().length >0)
		{
			var src = picture.getSrc();
			pictureComponent = document.createElement("img");
			pictureComponent.style.cssFloat = "left";
			pictureComponent.style.width = pictureSize + "px";
			pictureComponent.style.height = pictureSize + "px";
			pictureComponent.style.marginTop = "2px";
			pictureComponent.style.marginRight = "5px";
			pictureComponent.src = src;
			parent.appendChild(pictureComponent);
		}
		return pictureComponent;
	}

	// Make category audio if it exists
	this.makeAudioHTML = function(parent, audio, size)
	{
		if (!size) size = BUTTON_SIZE;
		if (audio != undefined && audio.length>0)
		{
			var button = this.makeIconButton("play", "Playback audio", 
					function(e)
					{
						if (e.stopPropagation)    e.stopPropagation();
						if (e.cancelBubble!=null) e.cancelBubble = true;

						var source = (e.target) ? e.target : e.srcElement;
						source.style.border = "inset";
						acorns.playAudio(audio); 
						setTimeout( function() { source.style.border = "outset" }, 250); 
						
					}
			, size);
			
			button.style.margin = "5px";
			parent.appendChild(button);
			return button;
		}
	}

	// Create the display area for the sentence gloss, indigenous, and audio
	this.makePointHTML = function(parent, point, lessonData, options)
	{
		if (options==undefined) options = ['y', 'y', 'y', 'y', 'y'];
		
		parent.style.cssFloat = "left";

		var gloss = document.createElement("p");
		gloss.style.wordWrap = "break-word";

		gloss.setAttribute("name", "gloss");
		gloss.style.marginBottom = "0px";
		gloss.style.marginTop = "0px";
		if (lessonData) lessonData.environment.setColors(gloss);
		parent.appendChild(gloss);
		
		var spell = document.createElement("p");
		spell.style.wordWrap = "break-word";
		spell.style.marginTop = "-1px";
		spell.style.marginBottom = "-1px";
		spell.setAttribute("name", "spell");
		if (lessonData) lessonData.environment.setColors(spell);
		point.applyFont(spell);
		parent.appendChild(spell);

		var temp = point.getDescription();
		if (temp && temp.length > 0)
		{
			var description = document.createElement("p");
			description.style.clear = "left";

			description.style.wordWrap = "break-word";
			description.style.marginTop = "5px";
			description.style.marginBottom = "-1px";
			description.setAttribute("name", "description");
			description.style.whiteSpace = "pre-line";
			if (lessonData) lessonData.environment.setColors(description);
			parent.appendChild(description);
		}
		point.display(parent, options);
	}
	
	this.makeInterfaceButtonHTML = function(parent, id, text, handler)
	{
		var button = document.createElement("input");
		button.type = "button";
		button.value = text;
		button.style.backgroundColor = "white";
		button.style.fontSize = Math.ceil(12 * acorns.system.getFontRatio()) + "px";
		button.style.borderStyle = "outset";
		button.style.borderWidth = "2px";
		button.style.textAlign = "center";
		acorns.system.addListener(button, "click", function(e) 
			{ 
				if (!e) e = window.event;
				if (e.stopPropagation)    e.stopPropagation();
				if (e.cancelBubble!=null) e.cancelBubble = true;	

				var source = (e.target) ? e.target : e.srcElement;
				var child = document.getElementById(id);
				var parent = child.parentNode;
				parent.removeChild( child );
				if (handler) handler(source.value);
			});
		button.style.marginRight = "5px";
		button.style.marginLeft = "5px";
		button.style.marginTop = "2px";
		button.style.verticalAlign = "top";
		parent.appendChild(button);
	}
	
	// Position the interface button
	this.positionHTML = function(parent, block)
	{
		var SCROLL = 15;
		document.body.appendChild(block);

		// Position the div if it goes past the end of the window
		var windowSize = acorns.system.getWindowSize();
		var position = acorns.system.getPosition(parent);
		var left = position.left + 10;
		var top = position.top + 10;
		var right = block.clientWidth + left;
		var bottom = block.clientHeight + top;
		
		if (right > windowSize.width)
		{
			left = Math.max(windowSize.width - block.clientWidth - SCROLL, 0);
			block.style.maxWidth = (windowSize.width -left) + "px";
		}
		if (bottom > windowSize.height)
		{
			top = Math.max(windowSize.height - block.clientHeight - SCROLL, 0);
			block.style.maxHeight = (windowSize.height - top) + "px";
		}

		block.style.left = left + "px";
		block.style.top = top + "px";
	}
	
	// Display audio, description, gloss, and spell data
	// Note: the parent parameter can be a node or the id of a node
	this.makePointDisplay = function(parent, point, lessonData, picture, audio, text, options)
	{
		PICTURE_SIZE = 100;

		if (options==undefined) options = ['y', 'y', 'y', 'y', 'y'];

	    var id = parent;
		if (!(typeof parent == "string")) id = parent.id;
		
		buttonNo = "";
		if (id.length > 7) buttonNo = id.substring(7) + " ";
		var id = "data" + buttonNo;

		var div = this.makeDisplayBoxHTML(parent, lessonData, id);		
		var pictureComponent = this.makePictureHTML(div, picture, PICTURE_SIZE);
		
		var box = document.createElement("div");
		div.appendChild(box);
		if (audio!=undefined && audio.length>0)
			acorns.system.addListener(box, "click", function(e) 
			{ 
				var source = (e.target) ? e.target : e.srcElement;
				source.style.border = "inset";
				acorns.playAudio(audio); 
				setTimeout( function() { source.style.border = "outset" }, 250); 
			});
		
		box.style.border = "outset";
		if (pictureComponent)
			box.style.marginLeft = (parseInt(pictureComponent.style.width,10) + 3) + "px";

		var sentence = this.makeCategoryHTML(box, text);
		this.makePointHTML(box, point);

		var ok = document.createElement("div");
		ok.style.textAlign = "center";
		box.appendChild(ok);
		
		this.makeInterfaceButtonHTML(ok, id, "OK");

		if (point.getAudio().length>0)
		{
			var play = document.createElement("img");
			play.style.height = "30px";
			play.style.width = "30px";
			play.style.borderStyle = "outset";
			play.style.borderWidth = "2px";
			play.style.verticalAlign = "top";
			play.setAttribute("name", "sound");
			play.setAttribute("src", acorns.iconLink + "play.png");
			play.setAttribute("title", "play audio");
			ok.appendChild(play);

			acorns.system.addListener(play, "click", 
				function(e) 
				{ 
					if (!e) e = window.event;
					if (e.stopPropagation)    e.stopPropagation();
					if (e.cancelBubble!=null) e.cancelBubble = true;

					var source = (e.target) ? e.target : e.srcElement;
					source.style.border = "inset";
					point.playAudio(audio); 
					setTimeout( function() { source.style.border = "outset" }, 250); 
				}
			);
		}
		
		point.display(div, options);
		document.body.appendChild(div);
		
		this.positionHTML(parent, div);
		var top;
		if (pictureComponent!=undefined)
		{
			top = ((parseInt(div.clientHeight,10) - parseInt(pictureComponent.clientHeight,10))/2);
			if (top < 0) top = 0;
			pictureComponent.style.top = top + "px";
		}
		
		if (sentence!=undefined) sentence.style.width = box.style.width;
	}

	/* Update the currenct results part of the score panel */
	this.updateScore = function(correct)
	{
		var score = resultPanel.innerHTML.toLowerCase().split("<br>");
		score[2] = parseInt(score[2],10) + 1;
		if (correct) score[0] = parseInt(score[0], 10) + 1;
		resultPanel.innerHTML = score[0] + "<br>of<br>" + score[2];
	}

	/* Show error and then wait for user to acknowledge. */
	this.showMessage = function(data, center)
	{
	    var messagePanel = acorns.widgets.getMessageElement();
		var messageDataPanel = messagePanel.getElementsByTagName("div");
		
		if (center) 
		     messageDataPanel[0].style.textAlign = "center";
		else messageDataPanel[0].style.textAlign = "left";

		if (messageDataPanel[0].innerHTML == data) return;

		messageDataPanel[0].innerHTML = data;
		messagePanel.style.display = "block";
		messagePanel.style.fontSize = (20 + acorns.system.getFontRatio()) + "px";

		var windowSize = acorns.system.getWindowSize();	
		var top = Math.floor(windowSize.height - messagePanel.offsetHeight)/2;
		
		if (top<0) top = 0;
		var left = Math.floor(windowSize.width - messagePanel.offsetWidth)/2
		
		messagePanel.style.top = top + "px";
		messagePanel.style.left = left + "px";
		return messagePanel;
	}
	
	/* Create a button showing an icon */
    this.makeIconButton = function(iconName, tooltip, handler, buttonSize)
    {
		if (buttonSize==undefined) buttonSize = BUTTON_SIZE;
		
		var img = document.createElement("img");
		img.style.position = "relative";
		img.style.verticalAlign = "top";
		img.style.width = img.style.height = buttonSize + "px";
		img.style.borderStyle = "outset";
		img.style.borderWidth = BUTTON_BORDER + "px";
		img.style.margin = "0px -1px";
		
		img.alt = iconName;
		img.src = acorns.iconLink + iconName + ".png";
		img.title = tooltip;
		
		acorns.system.addListener(img, "click", function(e) {handler(e, img, acorns.getActiveLesson())});
		return img;
    }	// End of makeIconButton()
			
    /* Make the control panel of lesson buttons */
    var makeControls = function(controlHandler, recordEnabled, widgets)
    {
		var windowSize = acorns.system.getWindowSize();
		var rows = (windowSize.width < MIN_ROW_WIDTH) ? 2 : 1;

		if (body==undefined) body = document.getElementById("body");
		body.style.backgroundColor = acorns.BACKGROUND_COLOR;

		var table = document.createElement("div");
		table.style.minWidth = "240px";
		table.id = "controls";
		table.style.position = "fixed";
		table.style.bottom = "0px";
		table.style.width = windowSize.width + "px";
		table.style.height = (rows*(BUTTON_SIZE + 2*BUTTON_BORDER)) + "px";
		table.style.textAlign = "center";
		table.style.lineHeight = BUTTON_SIZE + "px";
		body.appendChild(table);
		
		// Layer name cell
		var span = document.createElement("span");
		span.style.height = (20 * acorns.system.getResolutionRatio()) + "px";
		span.style.backgroundColor = "black";
		span.style.color = "white";
		span.style.fontSize = (8 * acorns.system.getFontRatio()) + "pt";
		span.style.borderStyle = "solid";
		span.style.position = "relative";
		span.style.verticalAlign = "super";
		span.style.borderWidth = "1px";
		span.id = "layername";
		table.appendChild(span);
		
		var button = widgets.makeIconButton("up", "", controlHandler);
		button.id = "up";
		button.style.marginRight = "2px";
		table.appendChild(button);
		
		button = widgets.makeIconButton("down", "", controlHandler);
		button.style.marginRight = GAP + "px";
		button.id = "down";
		table.appendChild(button);

		// Cell for anchor tag
		button = widgets.makeIconButton("anchor", "Link to another lesson", controlHandler);
		button.id = "anchor";
		if (windowSize.width >= MIN_ROW_WIDTH) button.style.marginRight = GAP + "px";
		button.style.display = "none";
		table.appendChild(button);

		// Possible new row for smart cell devices
		if (windowSize.width < MIN_ROW_WIDTH)  // If pixel width < small tablet size, split into two lines
		{
			table.appendChild(document.createElement("br"));
		}
		
		// Cell for lesson options
		button = widgets.makeIconButton("info", "Options for this lesson", controlHandler);
		button.id = "info";
		button.style.marginRight = GAP + "px";
		table.appendChild(button);

		// Cells for navigation
		button = widgets.makeIconButton("begin", "Switch to first lesson", controlHandler);
		button.style.marginRight = "2px";
		table.appendChild(button);
				
		button = widgets.makeIconButton("prev", "Switch to previous lesson", controlHandler);
		button.style.marginRight = "2px";
		table.appendChild(button);
				
		button = widgets.makeIconButton("next", "Switch to next lesson", controlHandler);
		button.style.marginRight = "2px";
		table.appendChild(button);
				
		button = widgets.makeIconButton("end", "Switch to last lesson", controlHandler);
		button.style.marginRight = GAP + "px";
		table.appendChild(button);
		
		// Possible cell for audio recording
		if ((recordEnabled || window.audioTools)&& windowSize.width > 320)  // show audio recording for medium and large screens
		{
			button = widgets.makeIconButton("record", "Record audio", controlHandler);
			table.appendChild(button);
			
			button = widgets.makeIconButton("play", "Playback recording", controlHandler);
			table.appendChild(button);
			
			button = widgets.makeIconButton("stop", "Stop recording", controlHandler );
			button.style.marginRight = GAP + "px";
			table.appendChild(button);
		}
		
		// Cell for the help button
		button = widgets.makeIconButton("help", "Lesson help", controlHandler);
		table.appendChild(button);
		return table;
    }	// End of makeControls()
	
	/** Create a param element and add it to a parent element */
	var makeParam = function(tag, name, value)
	{
		var param = document.createElement("param");
		param.name = name;
		param.value = value;
		tag.appendChild(param);
	}
	
	/** Create the play panel object **/
	this.makePanels = function(controlHandler, recordEnabled)
	{
		var windowSize = acorns.system.getWindowSize();

		if (body==undefined) body = document.getElementById("body");
		body.style.backgroundColor = acorns.BACKGROUND_COLOR;
		
		// Empty the display of elements left over from before resizing window
		var stack, child = body.lastChild, childName, previousChild;
		while (child)
		{
			childName = child.tagName;
			previousChild = child.previousSibling;
			if (childName==undefined || childName.toLowerCase() != "audio")
				body.removeChild( child );
			child = previousChild
		} 
		
		var popup = makePopupMenu();

		body.appendChild(popup);

		// The lesson play panel
		var div = document.createElement("div");
		div.style.position = "absolute";
		div.style.left = "0px";
		div.style.top =  "0px";

		var rows = (windowSize.width < MIN_ROW_WIDTH) ? 2 : 1;
		div.style.height = (windowSize.height - rows * CELL_SIZE) + "px";
		div.style.minHeight =  (windowSize.height - rows * CELL_SIZE) + "px";
		div.style.maxHeight = (windowSize.height - rows * CELL_SIZE) + "px";
		
		div.style.width = "100%";
		div.style.marginTop = "-1px";
		div.style.marginBottom = "-1px";
		div.style.overflow = "hidden";
		div.id = "main";
		body.appendChild(div);
	
		// Lesson control panel at the bottom (eliminate scrollbar on some browsers)
		var controls = makeControls(controlHandler, recordEnabled, this);
		body.appendChild(controls);
		
		var audio = acorns.system.audioSupport();
		if (!audio["audio"] && audio["bgsound"])
		{
			var bgsound = document.createElement("bgsound");
			bgsound.setAttribute("loop", "1");
			bgsound.setAttribute("volume", -600);
			bgsound.setAttribute("src", acorns.audioLink + "silence.mp3"); 
			body.appendChild(bgsound);
		}
		
		// Recording always enabled for the Acorns gallery application
		if (acorns.system.isAndroid()) recordEnabled = true;
		
		if (recordEnabled)
		{
			if (acorns.system.isAndroid())
			{
				if (!window.audioTools) recordEnabled = false;
			}
			else if (!navigator.javaEnabled()) 
					recordEnabled = false;
			else if (acorns.system.isiPhone() || acorns.system.isiPod())
					recordEnabled = false;
			else if (acorns.system.isOldIE())
			{
					div = document.createElement("div");
					div.innerHTML = 
						 '<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" '
						+ 'codebase="http://java.sun.com/update/1.5.0/jinstall-1_5_0-windows-i586.cab" '
						+ 'height="0" width="0" id="AudioHandler" name="AudioHandler"> '
						+ '<param name="code" value="org.acorns.AudioHandler" />'
						+ '<param name="archive" value="DesktopAcornsAudioExtension.jar" />'
						+ '<param name="mayscript" value="true" />'
						+ '<param name="scriptable" value="true" />'
						+ '<strong>This browser does not have a Java Plug-in.</strong>'
						+ '</object> ';
					body.appendChild(div);
			}
			else 
			{
				// Create link to the audio recording signed applet
				var object = document.createElement("object");
				var appletName = acorns.assets + "DesktopAcornsAudioExtension.jar";

				object.setAttribute( "name", "AudioHandler");
				object.setAttribute("id", "AudioHandler");
				object.setAttribute( "archive", appletName);
				object.setAttribute( "height", "0");
				object.setAttribute( "width", "0");
				object.setAttribute( "type", "application/x-java-applet");				
				makeParam(object, "mayscript", "true");
				makeParam(object, "scriptable", "true");

				object.setAttribute( "classid", "java:org.acorns.AudioHandler.class");
				object.innerHTML = "Your browser does not support Java Applets";
				
				body.appendChild(object);
				
			}
		}
	}   // End of makePanels()

	this.removeMessageElement = function()
	{
		var div = document.getElementById("message");
	    if (!div)
		{ return; }

		var parent = div.parentNode.removeChild(div);
	}
	

	this.getMessageElement = function()
	{
		body = document.getElementById("body");
		var messageBackground = acorns.system.getColor(255, 255, 204);
		var windowSize = acorns.system.getWindowSize();

		var div = document.createElement("div");
		div.id = "message";
		div.style.borderStyle = "solid";
		div.style.backgroundColor = messageBackground;
		div.style.color = "black";
		div.style.textAlign = "center";
		div.style.position = "absolute";
		div.style.maxWidth = Math.max( 200, Math.ceil(body.clientWidth * 3 / 4)) + "px";
		div.style.minWidth = Math.max( 200, Math.ceil(body.clientWidth * 3 / 4)) + "px";
		div.style.width = Math.max( 200, Math.ceil(body.clientWidth * 3 / 4)) + "px";
		div.style.maxHeight = (windowSize.height - 50) + "px";
		div.style.margin = "auto";
		div.style.overflow = "auto";
		acorns.system.scrollableDiv(div);

		body.appendChild(div);
		
		var p = document.createElement("div");
		p.style.textAlign = "left";
		p.innerHTML = "";
		div.appendChild(p);
		
		var input = document.createElement("input");
		input.type = "button";
		input.style.fontSize = Math.ceil(14 * acorns.system.getFontRatio()) + "px";
		input.value = "OK";
		input.style.borderStyle = "outset";
		
		var clientPos;
		
		acorns.system.addListener(div, "mousedown",
			function(e) {
				clientPos = acorns.system.getCoords(e);
			});
			
		acorns.system.addListener(div, "mouseup",
			function(e) {
				try 
				{ 
					var newPos = acorns.system.getCoords(e);
					var equal = (clientPos.x == newPos.x && clientPos.y == newPos.y);
					
					if (equal && div.parent==body)
						body.removeChild(div); 
				} catch (e) {}					
			});
		
		acorns.system.addListener(input, "click", 
			function(e) 
			{
					if (div.parent==body)
					{
						try { body.removeChild(div); }
						catch (e) 
						{ console.log(e); } 
					}
			});

		
		div.appendChild(input);
		div.appendChild(document.createElement("br"));
		div.appendChild(document.createElement("br"));
		return div;
	}
	
	this.makePictureButton 
	     = function(buttonNo, buttonSize, left, pictureHandler)
	{
		var div = document.createElement("div");
		div.setAttribute("id", "button " + buttonNo);
		div.style.position = "absolute";
		div.style.top = "0px";
		div.style.height = buttonSize + "px";
		div.style.width = buttonSize + "px";
		div.style.borderStyle = "outset";
		div.style.borderWidth = (BUTTON_BORDER/2) + "px";
		div.style.left = left + "px";
		div.style.backgroundColor = acorns.BACKGROUND_COLOR;
	
		if (pictureHandler!=undefined)
			acorns.system.addListener(div, "click", function(e) { pictureHandler(e) });
		var img = document.createElement("img");
		div.appendChild(img);
		return div;
	}
	
	this.getButtonBorderSize = function() { return BUTTON_BORDER; }
	
}	// End of Widgets class

/* Class to parse an ACORNS lesson file 
 		If successful, acorns object created containing all the file lessons
*/
function ParseACORNSFile(fileName)
{ 
	var parseObject = this;

	/** Function to create a picture object **/
	var getPicture = function(acorns, node)
	{
		if (!node) return undefined;
	    var angle = node.getAttribute("angle");
	    var type = node.getAttribute("scale");
		var src = node.getAttribute("src");
		var value = node.getAttribute("value");
		return new Picture(acorns, src, type, angle, value);
	}
	
	/** Method to append node parameters to the corresponding object **/
	var getParams = function(node, object)
	{
		var child, attributeNo, name, value;	
		for (var childNo=0; childNo<node.childNodes.length; childNo++)
		{
		    child = node.childNodes[childNo];
			if (child.nodeName.toLowerCase() == "param")
			{
				for (attributeNo=0; attributeNo<child.attributes.length; attributeNo++)
				{
					name = child.attributes[attributeNo].nodeName;
					value = child.attributes[attributeNo].value;
					object.paramList[name] = value;
				}
			}
		}
	}   // end of getParams()
	
	/** Get list of points from the node and attach it to the associated object **/
	var getPoints = function(acorns, node,  object)
	{
	    var x, y, type, gloss, spell, language, sound, description
	    var rate = "", playback = "", frames = "";
		var points;  // array of nodes variables

	    points = node.getElementsByTagName("point");
		for (var pointNo=0; pointNo<points.length; pointNo++)
		{  
			x = y = 0;
			gloss = spell = sound = link = language = description = picture = "";
			 
			x = points[pointNo].getAttribute("x");
			y = points[pointNo].getAttribute("y"); 
			
			type = points[pointNo].getAttribute("type");
			 
			for (var childNo=0; childNo<points[pointNo].childNodes.length; childNo++)
			{
				child = points[pointNo].childNodes[childNo];
				
				switch(points[pointNo].childNodes[childNo].nodeName)
				{
				   case "gloss":
						if (child.childNodes.length>0)	{gloss = child.firstChild.nodeValue;}
						break;
				   case "spell":
						if (child.childNodes.length>0)	
						{
							spell = child.firstChild.nodeValue;
							language = child.getAttribute("language");
						}
						break;
				   case "description":
						if (child.childNodes.length>0)	{description = child.firstChild.nodeValue;}
						break;
					case "link":
						if (child.childNodes.length>0)	{link = child.firstChild.nodeValue;}
						break;
				   case "picture":
						picture = getPicture(acorns, child);
						break;
				   case "sound":
						sound = child.getAttribute("src");
						rate = child.getAttribute("rate");
						playback = child.getAttribute("playback");
						frames = child.getAttribute("frames");
						break;
				}       // end switch
			 }  // end for children of point
			 object.objectList.push(new Point(acorns, x, y, type, gloss, spell, language
	                 , description, link, picture, sound, rate, playback, frames));
		}  // end for pointNo (points)
	}  // end of getPoints()

	/** Extract the Acorns list of lessons from the XML DOM object **/
	var getAcorns = function(fileName, root)
	{
		var lesson, lessonNo, fonts;
		var background, foreground, size;
		var acorns  = new Acorns(fileName, root);
		
	 	var lessons = root.getElementsByTagName("lesson");
	    for	(lessonNo=0; lessonNo<lessons.length; lessonNo++)
		{
		    type = lessons[lessonNo].getAttribute("type");
		    if (type==null) continue;

			try { lesson = new Lesson(acorns, type, lessons[lessonNo]); }
			catch (e) 
			{ 
				throw ("Lesson " + type + " is not supported\n" + e.stack );
			}

			getParams(lessons[lessonNo], lesson);
			
		    fonts = lessons[lessonNo].getElementsByTagName("font");
		    if (fonts.length>0)
		    {
		        background = fonts[0].getAttribute("background");
			    foreground = fonts[0].getAttribute("foreground");
			    size = fonts[0].getAttribute("size");
			    lesson.environment = new Environment(acorns, foreground, background, size);
		    }
			
		    lesson.parse(parseObject, lessons[lessonNo]);
		    acorns.lessonList.push(lesson);
		   
		}  // End of lessons loop
		return acorns;
	}	// End of getAcorns()

	/** parse languages of the multiple pictures category **/
	this.parseMultiplePicturesCategory = function(acorns, lesson, lessonObject)
	{
	   var layer, picture;  // object variables
	   var layers;  // array of nodes variables

	   var images = lesson.getElementsByTagName("image");
	   for (var imageNo=0; imageNo<images.length; imageNo++)
	   {
	        picture = getPicture(acorns, images[imageNo]);
	 	    if (picture.getValue()<0)
		    {
		        lessonObject.setBackground(picture);
			    continue;
		    }

		    layers = images[imageNo].getElementsByTagName("layer");
	   	    for (var layerNo = 0; layerNo<layers.length; layerNo++)
			{
				layer  = new Layer(layers[layerNo].getAttribute("name"), layers[layerNo].getAttribute("value"));
				getParams(layers[layerNo], layer);
			    getPoints(acorns, layers[layerNo], layer);
			    picture.objectList.push(layer);
	        } // end for layerNo (layers)
				  
			lessonObject.objectList.push(picture);	   
		}  // end of imageNo (images in lesson)
	}  // parseMultiplePicturesCategory()

	/** parse languages of the multiple pictures category **/
	this.parseMultipleAudioCategory = function(acorns, lesson, lessonObject)
	{
	    var layer, picture;  // object variables
	    var layers;  // array of nodes variables

 	    var images = lesson.getElementsByTagName("image");
		picture = getPicture(acorns, images[0]);
		lessonObject.objectList.push(picture);	   

		layers = images[0].getElementsByTagName("layer");
		for (var layerNo = 0; layerNo<layers.length; layerNo++)
		{
			layer  = new Layer(layers[layerNo].getAttribute("name"), layers[layerNo].getAttribute("value"));
			getParams(layers[layerNo], layer);
			getPoints(acorns, layers[layerNo], layer);
			picture.objectList.push(layer);
		} // end for layerNo (layers)
	}

	/** Parse lessons of the related phrase category **/
	this.parseRelatedCategory = function(acorns, lesson, lessonObject)
	{
	    var layers, layer;
		var categories, category, sentence, categoryNode;
	    var audio;
		
	    var images = lesson.getElementsByTagName("image");
	    var picture = getPicture(acorns, images[0]);
		if (picture && picture.getValue()<0)
		{
			lessonObject.setBackground(picture);
		}

		layers = lesson.getElementsByTagName("layer");
		for (var layerNo = 0; layerNo<layers.length; layerNo++)
		{
		    layer  = new Layer(layers[layerNo].getAttribute("name"), layers[layerNo].getAttribute("value"));
			getParams(layers[layerNo], layer);
			categories = layers[layerNo].getElementsByTagName("category");
			if (categories.length==0) continue;
			
			var parent, categoryNo;
			for (categoryNo=0; categoryNo<categories.length; categoryNo++)
			{
				categoryNode = categories[categoryNo];
			    sentence = categoryNode.getAttribute("name");

				audio = null;
				sounds = categoryNode.getElementsByTagName("sound");
				if (sounds.length>0)
				{
					parent = sounds[0].parentNode.tagName;
					if (parent == "category" && sounds[0].getAttribute("value") != "")
					{
						audio = sounds[0].getAttribute("src");
					}
				}	
				
				picture =  null;
				images = categoryNode.getElementsByTagName("image");
				if (images.length>0 && images[0].parentNode.tagName == "category") 
					picture = getPicture(acorns, images[0]);
					
				category = new Category(sentence, picture, audio);
				getParams(categoryNode, category);
				getPoints(acorns, categoryNode, category);
				layer.objectList.push(category);
			}
			lessonObject.objectList.push(layer);
		} // end for layerNo (layers)
	}  // end of parseRelatedCategories

	/** Parse audio story book, hear and click, hear and respond lesssons **/
	this.parseAudioLessonCategory = function(acorns, lesson, lessonObject)
	{
		var picture, layers, layer;
		
	    var images = lesson.getElementsByTagName("image");
		if (images.length>0)
		{
			if (lesson.getAttribute("type")!="Story Book") 
			{
				picture = getPicture(acorns, images[0]);
				lessonObject.setBackground(picture);
			}
			else
			{
				lessonObject.paramList["image"] = getPicture(acorns, images[0]);
			}
		}

		layers = lesson.getElementsByTagName("layer");
		var value, align, language;
		for (var layerNo = 0; layerNo<layers.length; layerNo++)
		{
			align = layers[layerNo].getAttribute("align");
			value = layers[layerNo].getAttribute("value");
			language = layers[layerNo].getAttribute("language");
		    layer  = new Layer(layers[layerNo].getAttribute("name"), value, language, align);
			getParams(layers[layerNo], layer);
			getPoints(acorns, layers[layerNo], layer);
			lessonObject.objectList.push(layer);
		} // end for layerNo (layers)
		
	}  // end of parse audio lesson types
	
	/** Parse the XML from a string */
	this.parseXMLString = function(fileName, xmlString)
	{
		if (window.DOMParser)
		{
			parser=new DOMParser();
			xmlDoc=parser.parseFromString(xmlString,"text/xml");
			if (xmlDoc.documentElement.nodeName=="parsererror") return false; 
		}
		else // Internet Explorer
		{
			xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
			xmlDoc.async="false";
			xmlDoc.loadXML(xmlString); 
		} 
		var root = xmlDoc.documentElement;  // Doc now has the root element of the XML DOM
		if (root) return getAcorns(fileName, root); 
		return undefined;
	}

	this.parseJSON = function(acorns,  fonts, feedback)
	{
		acorns.setFeedback(feedback);
		acorns.setFonts(fonts);
	}

}   // End of ParseXML class
    