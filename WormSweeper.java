import java.util.concurrent.Executors; 
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc; 


public class WormSweeper extends Application {
	// Declare timer variables
 	private Timeline timeline;
 	IntegerProperty timeSeconds;
    boolean timeStarted;
    
    // Declare game play variables
    int gridSize;
	int wormsTotal;
 	int hooksTotal;
 	int unrevealedSafeCells;
 	ButtonStatus[] btnStatus;

 	// Declare some GUI variables at class level
 	VBox mainPane;
	GridPane appleField;
	Label hookCountLabel;
	Button hints;
	ImageView cameraView;
	ImageView hookImgView;
	Dialog<String> unlockHintDialog;
	Label instruction;
	
	// Declare camera/JavaCV variables
 	OpenCVFrameGrabber grabber;
 	ScheduledExecutorService cameraExecutor;
    boolean isCameraRunning;

    
    // Define button (cell) status in enum
    private enum ButtonStatus {
        UNREVEALED_SAFE,      // 0: Unrevealed, but safe
        UNREVEALED_UNSAFE,    // 1: Unrevealed, but unsafe (has a worm)
        REVEALED_SAFE,        // 2: Revealed, and safe
        REVEALED_UNSAFE, 	  // 3: Revealed, and unsafe (a worm was clicked)
        FLAGGED_SAFE,  		  // 4: Flagged, and safe
        FLAGGED_UNSAFE        // 5: Flagged, and unsafe (has a worm)
        
    }
 	@Override
	public void start(Stage stage) throws Exception
	{
 		// Initialize timer variables
 		timeline = new Timeline();
	 	timeSeconds = new SimpleIntegerProperty(0); //reset time to 0
	    timeStarted = false; //timer is off at first
	    
	    // Initialize game play variables to a basic level game
	    gridSize = 10; //10x10 grid
		wormsTotal = 10;// these are "mines"
	 	hooksTotal = 10; //these are "flags"
	 	unrevealedSafeCells = 90; //total cells without worms
	 	btnStatus = new ButtonStatus[gridSize * gridSize];
	 	
	 	// Initialize random worm placement for a new basic level game
	    placeWorms();
	    
	 	// Initialize camera flag: default is off
	    isCameraRunning = false;

	    // Set window size
	    stage.setHeight(1000);		
		stage.setWidth(600);

    	//We define a root pane which will contains all the other elements
		mainPane = new VBox();  
		mainPane.setPadding(new Insets(5,5,5,5));

		//We create a scene that contains mainPane as a root pane
		Scene scene = new Scene(mainPane); 
		
		//We initialize the User Interface (UI) of the application
		initGUI(mainPane);
		
		//We give a title to our stage
		stage.setTitle("wormSweeper");
		
		//We display the scene we just created in the stage
		stage.setScene(scene);
		
		//We display the stage
		stage.show();
	}
 	
 	// All GUI elements
    private void initGUI(Pane root)
	{
		/* 
		 1. top layout panel that contains the level buttons
		 */
		HBox paneLevel = new HBox();
		paneLevel.setPadding(new Insets(10,10,0,10)); // no padding at the bottom
		paneLevel.setSpacing(10); //spacing between the buttons
		paneLevel.setAlignment(Pos.CENTER_LEFT); // aligned to the left
		mainPane.getChildren().add(paneLevel); // add it to our mainPane
		
		// load apple pictures for the level buttons 
		Image imageRedApple = new Image(getClass().getResourceAsStream("redapple.png"));
		Image imageGreenApple = new Image(getClass().getResourceAsStream("greenapple.png"));
		// put apple pictures into imageView and change size
		// red apple is for basic level
		ImageView redAppleView = new ImageView(imageRedApple);
		redAppleView.setFitWidth(30);  
		redAppleView.setFitHeight(30); 
		// green apple is for advanced level
		ImageView greenAppleView = new ImageView(imageGreenApple);
		greenAppleView.setFitWidth(30); 
		greenAppleView.setFitHeight(30); 
		
		//Create a button for the basic level with red apple icon
		Button levelBasic = new Button("Basic", redAppleView);
		// if the basic button is pressed, trigger game restart in basic level
		levelBasic.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e) {
				// basic level has 10x10 grid, 10 worms and 90 safe cells
				resetGame(10,10,90);
			}
		});
		// Create a Tooltip for the basic button displaying info about the level
        Tooltip basicToolTip = new Tooltip("10 x 10 grid\n 10 worms");
        // Attach the Tooltip to the basic level button
        Tooltip.install(levelBasic, basicToolTip);
        
		//Create a button for the advanced level with green apple icon
		Button levelAdvanced = new Button("Advanced", greenAppleView);
		// if the advanced button is pressed, trigger game restart in advanced level
		levelAdvanced.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e) {
				// advanced level has 15x15 grid, 30 worms and 195 safe cells
				resetGame(15,30,195);
			}
		});
		// Create a Tooltip for the basic button displaying info about the level
        Tooltip advToolTip = new Tooltip("15 x 15 grid\n 30 worms");
        // Attach the Tooltip to the basic level button
        Tooltip.install(levelAdvanced, advToolTip);

		//add the level buttons to the top layout panel
		paneLevel.getChildren().add(levelBasic);
		paneLevel.getChildren().add(levelAdvanced);
		
		/* 
		 2. timer pane that contains the stem, leaf and timer
		 */
		// create an anchor pane 
		AnchorPane timerPane = new AnchorPane();
		// add it to the mainPane
		mainPane.getChildren().add(timerPane);
		
		// Create stem with line
        Line stem = new Line(0, 50, 0, 30);
        stem.setStrokeWidth(10);	//set thickness
        stem.setStroke(Color.BROWN);	//set color
        // add the stem to the timerPane
        timerPane.getChildren().add(stem);
        
        // Bind stem's X coordinate to half the width of the pane so it always stays center
        stem.startXProperty().bind(Bindings.divide(timerPane.widthProperty(), 2));
        stem.endXProperty().bind(Bindings.divide(timerPane.widthProperty(), 2));

        // Create leaf with ellipse
        Ellipse leaf = new Ellipse(0, 40, 70, 10);
        leaf.setFill(Color.GREEN);	// set color
        // Bind leaf's position to the stem's position so it's always to the right of stem
        leaf.centerXProperty().bind(Bindings.add(stem.startXProperty(), 70)); 
        // add the leaf to the timer pane
        timerPane.getChildren().add(leaf);

        // Create timer Label
        Label timerLabel = new Label();
        // set the label to bind to timeSeconds that's being updated with Timeline
        timerLabel.textProperty().bind(Bindings.concat("Time: ", timeSeconds.asString()));
        timerLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: white;"); //set style
        // set the label coordinates so it lays on top of the leaf
        timerLabel.translateXProperty().bind(Bindings.subtract(leaf.centerXProperty(),30));
        timerLabel.translateYProperty().bind(Bindings.subtract(leaf.centerYProperty(),10));
        // add the timer label to the timerPane
        timerPane.getChildren().add(timerLabel);

        
		/* 3. middle pane for gameplay */
        // Initialize a new GridPane
		appleField = new GridPane();
		appleField.setPadding(new Insets(0,5,0,5)); // set padding
		appleField.setAlignment(Pos.CENTER); // set position
	    // initialize a hook count label
        hookCountLabel = new Label();
        
        // fills the apple field with buttons and set the button actions
        reDraw();
        
	    // add the gameplay field to the mainPane
	    mainPane.getChildren().add(appleField);
	
		/* 
		 4. Hook count pane for hook count display 
		 */
		HBox paneHookCount = new HBox();
		paneHookCount.setPadding(new Insets(5,0,0,0)); // add padding
		paneHookCount.setAlignment(Pos.CENTER); // set position
		mainPane.getChildren().add(paneHookCount); //add it to main pane
		
		// this label displays the hooks left
		hookCountLabel.setText(String.valueOf(hooksTotal));
		hookCountLabel.setStyle("-fx-font-size: 15px;");	//set font size
		// add it to the hook count pane
		paneHookCount.getChildren().add(hookCountLabel);
			
		// load hook picture
		Image imageHook = new Image(getClass().getResourceAsStream("hook.png"));
		// create a hook image view and set its size
		hookImgView = new ImageView(imageHook);
		hookImgView.setFitWidth(30);  
		hookImgView.setFitHeight(30); 
		// add it to the bottom pane
		paneHookCount.getChildren().add(hookImgView);
		
		// Set draggable event for the hook imageView
		hookImgView.setOnDragDetected(event -> {
			// if there are no hooks left
			if (hooksTotal == 0) {
                System.out.println("Max hook count reached. Cannot drag more hooks.");
                // Prevent dragging if max hook count is reached
                return; 
            }
	        // otherwise, start drag-and-drop gesture
			// Create a new dragboard
			ClipboardContent content = new ClipboardContent();
			// Attach the hook image to dragboard
	        content.putImage(hookImgView.getImage());  
	        // now the image can be moved to a different location
	        hookImgView.startDragAndDrop(TransferMode.MOVE).setContent(content);
	        // set the drag and drop view to the hook image so it shows when being dragged
	        hookImgView.startDragAndDrop(TransferMode.MOVE).setDragView(hookImgView.getImage());
	        // consume the event to prevent further handling of the drag event by other handlers
	        event.consume();
	    });
		
		// Create a label for the rest of the hook label instruction
    	Label hooksLabel = new Label("left to drag and drop"); 
    	hooksLabel.setStyle("-fx-font-size: 15px;");	//set font size
    	// add this label to the hook count pane
    	paneHookCount.getChildren().add(hooksLabel); 
    	
    	/*
    	 4. Hint Panel that contains the unlock hint button
    	 */
    	// Create an Hbox for this panel
    	HBox hintPanel = new HBox();
    	// set the alignment to top right
    	hintPanel.setAlignment(Pos.TOP_RIGHT);
    	// add it to the mainPane
    	mainPane.getChildren().add(hintPanel);
    	
    	// Create a button to unlock a hint
    	hints = new Button("Unlock a Hint");
    	// event handler for if the button is pressed
    	hints.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e) {
				// if timer has not started
				if (!timeStarted) {
					// start the timer
					startTimer();
				}
				// unlock hint dialog window pops up
				unlockHintDialog();
			}	
		});
    	// add this button to the hint panel
    	hintPanel.getChildren().add(hints);
	}
    
    // Refill the appleField with buttons and set actions
    private void reDraw() {
    	// redraw the playing field in 10x10 or 15x15
    	 		for (int i = 0; i < gridSize; i++) {
    	 			for (int j = 0; j < gridSize; j++) {
    	 				// Create a button
    	 				Button appleFieldBtn = new Button();
    	 				// Set the index of the button
    	 				GridPane.setRowIndex(appleFieldBtn, i);
    	 				GridPane.setColumnIndex(appleFieldBtn, j);
    	 	
    	 				// Set a fixed size to make buttons square
    	 				appleFieldBtn.setPrefWidth(40);  
    	 				appleFieldBtn.setPrefHeight(40);
    	 				// Set the button style (color, border, etc)
    	 				appleFieldBtn.setStyle("-fx-background-color: maroon; -fx-text-fill: white; " +
    	                         "-fx-border-color: white; -fx-border-width: 1px; " +
    	                         "-fx-border-style: solid;");
    	 				
    	 				// removes the color from the buttons, after clicking on them
    	 				appleFieldBtn.setFocusTraversable(false);

    	 				// adds an event handler for the button
    	 				appleFieldBtn.setOnAction(new EventHandler<ActionEvent>() {
    	 					// handles the action when the button is clicked
    	 					@Override public void handle(ActionEvent e) {
    	 						// if timer has not started
    	 						if (!timeStarted) {
    	 							// start the timer
    	 							startTimer();
    	 						}
    	 						// gets the row index of the clicked button
    	 						int btnIndexRow = GridPane.getRowIndex(appleFieldBtn);
    	 						// gets the column index of the clicked button
    	 						int btnIndexCol = GridPane.getColumnIndex(appleFieldBtn);
    	 	
    	 						// calls the revealCell() method to reveal content of this button cell 
    	 						revealCell(appleFieldBtn, btnIndexRow, btnIndexCol);
    	 						}
    	 					});
    	 				//  event handler for if something is dragged over the button
    	 				appleFieldBtn.setOnDragOver(event -> {
    	 					// Check if the source of the drag event is not from the button itself 
    	 				    // and if the data being dragged contains an image
    	 			        if (event.getGestureSource() != appleFieldBtn && event.getDragboard().hasImage()) {
    	 			        	// Accept the drag if the drag mode is "MOVE" (i.e., the user is moving the image)
    	 			        	event.acceptTransferModes(TransferMode.MOVE);
    	 			        }
    	 			        // Consume the event so that it doesn't propagate to other event handlers
    	 			        event.consume();
    	 			    });
    	 				// event handler if something is drag dropped on the button
    	 				appleFieldBtn.setOnDragDropped(event -> {
    	 					// if the dragged data is an image
    	 			        if (event.getDragboard().hasImage()) {
    	 			        	// Generate a new ImageView to duplicate the hook image
    	 			            ImageView flagImageView = new ImageView(event.getDragboard().getImage());
    	 			            // Set size of the hook image to be placed in the square
    	 			            flagImageView.setFitWidth(20);  
    	 			            flagImageView.setFitHeight(20);
    	 			            // if the button doesn't already have a hook
    	 			            if (appleFieldBtn.getGraphic() == null) {
    	 			            	// display the hook on the button
    	 				            appleFieldBtn.setGraphic(flagImageView);
    	 				            // Compute the 1D index of the button from 2D
    	 				            int index = GridPane.getRowIndex(appleFieldBtn) * gridSize + GridPane.getColumnIndex(appleFieldBtn);
    	 			            	// if the button was unrevealed and safe
    	 				            if (btnStatus[index] == ButtonStatus.UNREVEALED_SAFE) {
    	 				            	// set it to flagged and safe
    	 				            	btnStatus[index] = ButtonStatus.FLAGGED_SAFE;
    	 				            }
    	 				            // if it is unrevealed and unsafe
    	 				            else if (btnStatus[index] == ButtonStatus.UNREVEALED_UNSAFE){ 
    	 				            	// set it to flagged and unsafe
    	 				            	btnStatus[index] = ButtonStatus.FLAGGED_UNSAFE;

    	 				            }
    	 				            // Mark the drag-and-drop as complete
    	 				            event.setDropCompleted(true);  
    	 				            // Decrement the hooks left
    	 				            hooksTotal -= 1;
    	 				            // Update the hooks left label on the screen
    	 				    		hookCountLabel.setText(String.valueOf(hooksTotal));
    	 			            }			         
    	 			        } else { // if there is no image being dragged
    	 			        	// indicate unsuccessful drop
    	 			            event.setDropCompleted(false); 
    	 			        }
    	 			        // Consume the event so that it doesn't propagate to other event handlers
    	 			        event.consume();
    	 			    });
    	 				// event handler for mouse click on the button
    	 				appleFieldBtn.setOnMouseClicked(event -> {
    	 					// Check for right-click
    	 			        if (event.getButton() == MouseButton.SECONDARY) {  
    	 			            // if there is an image on the button
    	 			            if (appleFieldBtn.getGraphic() != null) {
    	 			                // Remove the image by setting the graphic to null
    	 			            	appleFieldBtn.setGraphic(null);
    	 				            // Compute the 1D index of the button from 2D
    	 			            	int index = GridPane.getRowIndex(appleFieldBtn) * gridSize + GridPane.getColumnIndex(appleFieldBtn);
    	 			            	// if the button was flagged and safe
    	 			            	if (btnStatus[index] == ButtonStatus.FLAGGED_SAFE) {
    	 			            		// change its status to unrevealed and safe
    	 				            	btnStatus[index] = ButtonStatus.UNREVEALED_SAFE;
    	 				            }
    	 			            	// if it is flagged and unsafe
    	 				            else if (btnStatus[index] == ButtonStatus.FLAGGED_UNSAFE){
    	 				            	// otherwise, change its status to unrevealed and unsafe
    	 				            	btnStatus[index] = ButtonStatus.UNREVEALED_UNSAFE;

    	 				            }
    	 			            	// increment the hooks left
    	 			            	hooksTotal += 1;
    	 			            	// update the hooks left label
    	 			        		hookCountLabel.setText(String.valueOf(hooksTotal));
    	 			            }
    	 			        }
    	 			    });
    	 				// adds the button to the field
    	 				appleField.getChildren().add(appleFieldBtn);
    	 			}
    	 		}
		
	}

	// Function that starts the timer
    private void startTimer() {
        // Initialize a timeline here
    	timeline = new Timeline();
    	// adding keyframes to timeline
    	timeline.getKeyFrames().add(
    			// create a keyframe that triggers every second
	            new KeyFrame(Duration.seconds(1), event -> {
	            	// first get the current time
	                int currentTime = timeSeconds.get();
	                // then increment the current time by 1 second 
	                timeSeconds.set(currentTime + 1);
	            })
	        );
    	// set the timeline to play indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        // start playing the timeline from start
        timeline.playFromStart();
        // indicate the timer has started
        timeStarted = true;

    }
    // Function that redraws the playing field and resets variables for a new game
    private void resetGame(int gridNum, int wormNum, int safecellNum) {
    	// Turn the timer status off
	    timeStarted = false;
	    // Safety measure: turn the timer off if it was running
	    if (timeline != null) {
            timeline.stop();  // Stop any previous timeline
        }
	    // Reset the time to the 0
        timeSeconds.set(0);  
        
    	// Reset game play variables for basic/advanced
    	gridSize = gridNum;
		wormsTotal = wormNum;
		hooksTotal = wormNum;
		unrevealedSafeCells = safecellNum;
		
		// Turn the unlock hints button on
		hints.setDisable(false);
		// set the total hook count back 
     	hookCountLabel.setText(String.valueOf(hooksTotal));
		// Clear all the button cells
		appleField.getChildren().clear();
		
		// Initialize a new empty btnStatus list according to the level
	    btnStatus = new ButtonStatus[gridSize * gridSize];
	    // place the worms randomly
     	placeWorms();
     	// Refill the appleField with buttons and set the button actions
     	reDraw();
	}

    // Unlock Hints Window
    private void unlockHintDialog() {
    	// Create a dialog window to display when the hint is being unlocked
        unlockHintDialog = new Dialog<>();
        // Set the title of the window
        unlockHintDialog.setTitle("Show Red Item");
        // Set the size of the window
        unlockHintDialog.setWidth(600);
        unlockHintDialog.setHeight(400);
        // Make the window not closable so the user has to select a button
        unlockHintDialog.getDialogPane().getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            event.consume(); // Prevent the window from being closed
        });

        // Create a vbox and configure it to display the dialog content
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);	//center the vbox
        content.setPadding(new Insets(20));	// add some padding
        // Create instruction and to display it in the window
        instruction = new Label("Show a red item to unlock a hint.");
        instruction.setStyle("-fx-font-size: 20px;");
        content.getChildren().add(instruction);
        // Create, configure and add an imageView that will contain the camera frame
        cameraView = new ImageView();
        cameraView.setFitWidth(320);	//maintain the same aspect ratio as the frame
        cameraView.setFitHeight(240);
        content.getChildren().add(cameraView);

        // Add the content to the dialog window
        unlockHintDialog.getDialogPane().setContent(content);

        // Add buttons cancel and apply 
        unlockHintDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.APPLY);
        // When apply is pressed (default it is disabled)
        unlockHintDialog.getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);unlockHintDialog.getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
			// disable the unlock hints button in the main playing field (only one hint)
        	hints.setDisable(true);
            // reveal a worm in the field as a hint
        	unlockHint(); 
        	// turn off the camera and release resources
            cameraStop();
        });
        // if cancel is pressed (even if red is detected)
        unlockHintDialog.getDialogPane().lookupButton(ButtonType.CANCEL).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
        	// turn off the camera and release resources
            cameraStop();
        });
        // start the camera and start to detect red
        cameraStart();
        // wait for user input
        unlockHintDialog.showAndWait();
    }
    
    // Function that starts the camera and detects red
    // Function that starts the camera and detect red
    private synchronized void cameraStart(){
    	// if camera is already running
 		if (isCameraRunning) {
            System.out.println("Camera is already running.");
            // break out of the function to prevent starting a new thread
            return;
        }
 		
 		// Initialize a grabber for frames that captures through built in webcam
 		grabber = new OpenCVFrameGrabber(0);
 		// set the aspect ratio of the grabber for the frames grabbed
        grabber.setImageWidth(160);
        grabber.setImageHeight(120);
        
 		System.out.println("Camera Starting");
        try {
        	// activate the grabber
            grabber.start();
            // set the camera running flag to true
            isCameraRunning = true;
            System.out.println("Grabber started");
        } catch (Exception e) {
        	// error condition
            System.out.println("Failed to start grabber" + e.getMessage());
        }
        
        // initialize a single thread executor for the camera
        cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        // at a fixed rate, we are grabbing frames
        cameraExecutor.scheduleAtFixedRate(() -> {
            try {
            	// Stop grabbing if the camera has been stopped
            	if (!isCameraRunning) {
                    return;  //exit out of the function
                }
            	
            	// Grab a frame from the camera
                Frame frame = grabber.grab();  
                
	        	try{
	        		// Additional safety check to check if the camera is not off
	        		if (!isCameraRunning) {
                        return;  //if it is turned off, exit this function
                    }
	        		// if there is a frame
	        		if (frame != null) {
	        			           
	                	// Initialize two Mats, one for the original frame in BGR and one for the original frame in HSV
	                	org.bytedeco.opencv.opencv_core.Mat src_BGR = new org.bytedeco.opencv.opencv_core.Mat();
	                	org.bytedeco.opencv.opencv_core.Mat src_HSV = new org.bytedeco.opencv.opencv_core.Mat();
	                	
	                	// Initialize a converter that converts from frame to Mat
	    	            OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat(); 
	    	            // convert the grabbed frame to Mat and store it 
	    	            src_BGR = matConverter.convert(frame);
	                	
	    	            // HSV color space allows for easier detection of colors, so
	                	// convert the color space of the src_BGR mat from BGR to HSV then store it in src_HSV mat
	                	opencv_imgproc.cvtColor(src_BGR, src_HSV, opencv_imgproc.COLOR_BGR2HSV);
	                	
	                	// Define lower and upper bounds for red color and store as Mat
	                	org.bytedeco.opencv.opencv_core.Mat lowerRed = new org.bytedeco.opencv.opencv_core.Mat(src_HSV.size(), opencv_core.CV_8UC3, new org.bytedeco.opencv.opencv_core.Scalar(161, 155, 84, 0)); // Lower limit of RED
	                	org.bytedeco.opencv.opencv_core.Mat upperRed = new org.bytedeco.opencv.opencv_core.Mat(src_HSV.size(), opencv_core.CV_8UC3, new org.bytedeco.opencv.opencv_core.Scalar(179, 255, 255, 0)); // Upper limit of RED

	                    // Create a mask to capture the red color detected
	                    org.bytedeco.opencv.opencv_core.Mat redMask = new org.bytedeco.opencv.opencv_core.Mat();
	                    // Apply the mask, the resulting redMask contains the detected red color pixels
	                    opencv_core.inRange(src_HSV, lowerRed, upperRed, redMask);

	                    // Check if there are any non-zero pixels in the mask (indicating red is detected)
	                    int nonZeroCount = opencv_core.countNonZero(redMask);
	                    // if there are non-zero pixels
	                    if (nonZeroCount > 0) {
	                    	// on the Java Application thread
	                    	// Change the text on the Unlock Hints window to show red is detected
	        				Platform.runLater(() -> instruction.setText("Red detected! Click Apply to unlock a hint."));
	        				// Highlight the text to make it pop
	        				Platform.runLater(() -> instruction.setStyle("-fx-background-color: yellow; -fx-font-size: 20px;"));  // Set background color and font size
	        				// Activate the apply button
	        				Platform.runLater(() -> unlockHintDialog.getDialogPane().lookupButton(ButtonType.APPLY).setDisable(false));
	                        
	                    }
	                    
	                    // Initialize a converter for frame to image conversion
	                    JavaFXFrameConverter imgConverter = new JavaFXFrameConverter();
	                    // convert the frame to an image
	    	            Image cameraImage = imgConverter.convert(frame);
	    				// Update the ImageView in the Unlock Hints window with the new frame captured
	    	            // on the Java Application thread
	    				Platform.runLater(() -> cameraView.setImage(cameraImage));
	    	            }
	        	} catch (Exception e) {
	        		// error condition
	                System.out.println("Error converting/displaying frame: " + e.getMessage());
	        	}
	            
            } catch (Exception e) {
            	// error condition
                System.out.println("Error capturing camera frame: " + e.getMessage());
            }
           }, 0, 33, TimeUnit.MILLISECONDS);  // grabbing frames at 30 FPS (33ms interval)
    }
    
    // Function that stops the camera and release resources
 	
    // Function that stops the camera to release resources
 	private synchronized void cameraStop(){
 		// If the camera is already stopped
 		if (!isCameraRunning) {
            System.out.println("Camera is not running.");
            // exit the function
            return;
        }
 		
        System.out.println("Stopping camera...");
        // Set the run camera flag to false to stop grabbing new frames
        isCameraRunning = false;  

        // Stop the scheduled executor to prevent new frame grabs
        // if the executor exists and is still running
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            try {
            	// initiate shut down the executor
                cameraExecutor.shutdown();
                // Wait for a second until all tasks have completed
                if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                	// Force shutdown if tasks are still running
                    cameraExecutor.shutdownNow();  
                }
            } catch (InterruptedException e) {
            	// force shutdown even if thread is interrupted
                cameraExecutor.shutdownNow();
            } finally {
            	// set executor to null so it's no longer usable
                cameraExecutor = null;
            }
        }

        // Stop and release the grabber
        try {
        	// if there is a grabber
            if (grabber != null) {
                grabber.stop();      // Stop the camera grabber
                grabber.release();   // Release resources
                grabber.close();     // Close the grabber to clean up
                grabber = null;		 // set it to null so it's not usable
            }
            System.out.println("Camera stopped.");
        } catch (Exception e) {
        	// error condition
            System.out.println("Error stopping camera: " + e.getMessage());
        }
       
    }
 	
 	// Function that reveals a worm in the field as a hint
    
 	// Function that gives a hint by revealing a worm 
 	private void unlockHint() {
 		// initialize row and column variables for the worm to be revealed
    	int wormRow = 0;
    	int wormCol = 0;
    	
    	// Find the worm coordinates first
    	// Looping through the button status list
    	for (int i = 0; i < btnStatus.length; i++) {
    		ButtonStatus status = btnStatus[i];
    		// if a button is found to be unrevealed and has a worm
			if (status == ButtonStatus.UNREVEALED_UNSAFE) {
				// then set this button status to revealed and unsafe
				btnStatus[i] = ButtonStatus.REVEALED_UNSAFE;
				// Then calculate the row and column index according to grid size (1D -> 2D)
				// Assign to row and column variables for the worm to be revealed
				wormRow = i / gridSize; 
				wormCol = i % gridSize;
				// Break when the first worm is found
	            break;
			}
    	}
    	
    	// Loop through the children nodes of appleField 
		for (javafx.scene.Node node : appleField.getChildren()) {
			// If the node is a Button
			if (node instanceof Button) { 
		        // and its coordinate match the coordinate of the worm to be revealed
	            if (GridPane.getRowIndex(node) == wormRow && GridPane.getColumnIndex(node) == wormCol) {
	            	// Create a button reference for this node
	            	Button button = (Button) node;
	                // Initialize an image of a worm
	            	Image imageWorm = new Image(getClass().getResourceAsStream("wormInApple.png"));
	            	// Set the worm image to an imageView
	            	ImageView wormImgView = new ImageView(imageWorm);
	            	// Set size of the worm imageView to square
	            	wormImgView.setFitWidth(20);  
	            	wormImgView.setFitHeight(20);
	
	            	// Show the worm on the cell
	                button.setGraphic(wormImgView);
	                // Make the button unclickable 
	                button.setDisable(true);
	                // Break out of the loop when worm is revealed
	                break;
	            }        
	        }
		}
    }
 	
   
    // Function that reveals a cell content when it is opened
    private void revealCell(Button cell, int x, int y) {
    	// initialize an index according to the cell row and column since the btnStatus list is 1D
        int index = x * gridSize + y;
        
        // If this cell has a worm, game over
        if (btnStatus[index] == ButtonStatus.UNREVEALED_UNSAFE) {
        	// Change the status of this cell to revealed and unsafe
            btnStatus[index] = ButtonStatus.REVEALED_UNSAFE;
            // Initialize an image of a worm
            Image imageWorm = new Image(getClass().getResourceAsStream("wormInApple.png"));
        	// Set the worm image to an imageView
            ImageView wormImgView = new ImageView(imageWorm);
            // Set size of the worm imageView to square
        	wormImgView.setFitWidth(20);  
        	wormImgView.setFitHeight(20);

            // Show the worm on the cell
            cell.setGraphic(wormImgView);	  
            // Trigger game over alert window
            gameOver();
        }
        // if the cell is safe
        else if (btnStatus[index] == ButtonStatus.UNREVEALED_SAFE) {
        	// set the cell status to revealed and safe
            btnStatus[index] = ButtonStatus.REVEALED_SAFE;
            // get the adjacent worm count for this cell
            int adjacentWorms = getAdjWormCnt(x, y);
            // Decrease count of total unrevealed safe cells
            unrevealedSafeCells--; 
            
            // if there are worms nearby
            if (adjacentWorms > 0) {
            	// display the number of adjacent worms in the cell
                cell.setText(String.valueOf(adjacentWorms));
                // make the cell button unclickable
                cell.setDisable(true);
            } else { // if there are no worms nearby
                cell.setText(""); // display nothing in the cell
                // make the cell button unclickable
                cell.setDisable(true);

                // Then reveal surrounding 3x3 cells 
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                    	//ignore the current cell
                        if (i == 0 && j == 0) continue; 
                        
                        int newX = x + i; //get a new row for each direction
                        int newY = y + j; //get a new column for each direction
                        
                        // if the new coordinate is within the playing field bounds
                        if (newX >= 0 && newX < gridSize && newY >= 0 && newY < gridSize) {
                        	// Loop through the children nodes of appleField to find the neighboring button
                            for (Node node : (appleField.getChildren())) {
                            	// if the coordinates of the child node during looping match our new coordinate
                                if (GridPane.getRowIndex(node) == newX && GridPane.getColumnIndex(node) == newY) {
                                    // and the node is a button
                                	if (node instanceof Button) {
                                		// then create a reference to refer to this button
                                        Button adjacentTile = (Button) node;
                                        // and recursively call revealCell function on this neighboring cell
                                        revealCell(adjacentTile, newX, newY);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Check for victory: if all the safe 
            if (unrevealedSafeCells == 0 ) {
            	// trigger game win alert window
                gameWin();
            }
        }
    }
    
    // Function that returns the number of worms in 3x3 grid vicinity
    private int getAdjWormCnt(int x, int y) {
    	// Initialize the count to zero
		int adjWormCnt = 0;
		// Initialize the coordinate direction of the 3x3 grid vicinity
		int[][] coordDirection = {
				{-1, -1}, {-1, 0}, {-1, 1},
				{0, -1},          {0, 1},
				{1, -1}, {1, 0}, {1, 1}
		};
		
		// Looping through the coordinate directions
		for (int [] direction : coordDirection) {
			// Calculate the new coordinate row for each direction
			int xPrime = x + direction[0];
			// Calculate the new coordinate column for each direction
			int yPrime = y + direction[1];
			
			//checks if the new coordinate is still within the bounds of the game field
			if (xPrime >= 0 && xPrime < gridSize && yPrime >= 0 && yPrime < gridSize) {
				// and if the new coordinate cell has any worms
				if (btnStatus[xPrime* gridSize + yPrime] == ButtonStatus.UNREVEALED_UNSAFE || btnStatus[xPrime* gridSize + yPrime] == ButtonStatus.REVEALED_UNSAFE || btnStatus[xPrime* gridSize + yPrime] == ButtonStatus.FLAGGED_UNSAFE) {
					// the adjacent worm count is incremented
					adjWormCnt++;
				}
			}
		}
		// return total adjacent worm count after checking the 3x3 vicinity
		return adjWormCnt;
	}
        
    // Function to place worms randomly in the field
    private void placeWorms() {
    	// Initialize a random object
        Random random = new Random();
        // Initialize a variable that tracks the number of worms placed in the field
        int placedWorms = 0;
        
        // Loop through the button Status list
        for (int i = 0; i < gridSize * gridSize; i++) {
        	// set the status of all buttons to unrevealed and safe first
            btnStatus[i] = ButtonStatus.UNREVEALED_SAFE;
        }
        
		// Then for the number of worms required for the game level
        while (placedWorms < wormsTotal) {
        	// Generate a random index within the total number of grid cells (e.g. 100 for basic)
            int randIndex = random.nextInt(gridSize* gridSize);
            // if a worm has not been placed in the cell 
            if (btnStatus[randIndex] == ButtonStatus.UNREVEALED_SAFE) {
            	// place a worm by setting the status to unrevealed and safe
            	btnStatus[randIndex] = ButtonStatus.UNREVEALED_UNSAFE;
            	// increment the number of worms placed
            	placedWorms ++;
            }
            // if a worm has already been placed in the cell
            else {
            	continue; // do nothing
            }
        }
        
        // Debugging: displays in console a answer map of where the worms are
        // If the current game is basic level 10x10
        if (gridSize == 10) {
        	int line = 0; // initialize a variable to track when to switch lines
            System.out.println();
            // Loop through all button status
            for (ButtonStatus status : btnStatus) {
            	// for the last 8 button status of each line, print in same line as the first
            	if (line < 9) {
                	if (status == ButtonStatus.UNREVEALED_SAFE) {
                		System.out.print(0); // 0 is safe
                	}
                	if (status == ButtonStatus.UNREVEALED_UNSAFE) {
                		System.out.print(1); // 1 is where the worm is
                	}
                	line ++;
            	} else { // For the first button status of each line, print in the next line
           
                	if (status == ButtonStatus.UNREVEALED_SAFE) {
                		System.out.println(0); // 0 is safe
                	}
                	if (status == ButtonStatus.UNREVEALED_UNSAFE) {
                		System.out.println(1); // 1 is where the worm is
                	}
                	line = 0; //reset tracker
            	}
            	
                
            }
        } else { // same mechanism for advanced game 15x15
        	int line = 0; // initialize a variable to track when to switch lines
            System.out.println();
            // Loop through all button status
            for (ButtonStatus status : btnStatus) {
            	// for the last 14 button status of each line, print in same line as the first
            	if (line < 14) {
                	if (status == ButtonStatus.UNREVEALED_SAFE) {
                		System.out.print(0); // 0 is safe
                	}
                	if (status == ButtonStatus.UNREVEALED_UNSAFE) {
                		System.out.print(1); // 1 is where the worm is
                	}
                	line ++;
            	} else {   // For the first button status of each line, print in the next line
                	if (status == ButtonStatus.UNREVEALED_SAFE) {
                		System.out.println(0); // 0 is safe
                	}
                	if (status == ButtonStatus.UNREVEALED_UNSAFE) {
                		System.out.println(1); // 1 is where the worm is
                	}
                	line = 0; // reset tracker
            	}	
            }
        }
        
    }
    
    // Window for Game Win
	private void gameWin() {
		// If a timer is running
		if (timeline != null) {
			// Stop the timer
            timeline.stop(); 
        }
		// Store the time used in the game in a variable
		int timeUsed = timeSeconds.get();
		
		// Create alert window for game over
        Alert alertGameWin = new Alert(AlertType.INFORMATION);
        // Display title and header about the game win situation
        alertGameWin.setTitle("Game Completed");
        alertGameWin.setHeaderText("You Won!"); 
        // Display instruction for buttons usage
        alertGameWin.setContentText("You used " + timeUsed + " seconds! Thanks for playing!\nPress Restart to start a new game or Quit to close game.");
        
        // Creating two buttons: Restart and Quit
        ButtonType buttonRestart = new ButtonType("Restart");
        ButtonType buttonQuit = new ButtonType("Quit");
        // Adding two buttons to the alert window
        alertGameWin.getButtonTypes().setAll(buttonRestart, buttonQuit);
        // When restart button is pressed, start a game in the same level
        alertGameWin.getDialogPane().lookupButton(buttonRestart).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // if current game played is basic level
        	if (gridSize == 10) {
            	resetGame(10,10,90); //Restart basic game 
            } else { // if current game played is advanced level
            	resetGame(15,30,195); //Restart advanced game
            }
        });
        // When quit button is pressed, close the entire game
        alertGameWin.getDialogPane().lookupButton(buttonQuit).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            System.exit(0); //Close the program
        });

        //Show the alert window and wait for user input
        alertGameWin.showAndWait();
	}
	
	// Window for game loss
	private void gameOver() {	
		// If a timer is running
	    if (timeline != null) {
	    	//Stop the timer
            timeline.stop(); 
        }
	    
		// Create alert window for game over
        Alert alertGameOver = new Alert(AlertType.INFORMATION);
        // Setting the title and header to display game lost information
        alertGameOver.setTitle("Game Over");
        alertGameOver.setHeaderText("You Lost!"); 
        // Display instruction for buttons usage
        alertGameOver.setContentText("Thanks for playing!\nPress Restart to start a new game, or Quit to close game.");

        // Creating two buttons: Restart and Quit
        ButtonType buttonRestart = new ButtonType("Restart");
        ButtonType buttonQuit = new ButtonType("Quit");
        // Adding the two buttons to the alert window
        alertGameOver.getButtonTypes().setAll(buttonRestart, buttonQuit);
        // When restart button is pressed, start a game in the same level
        alertGameOver.getDialogPane().lookupButton(buttonRestart).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // If the current game is basic level
        	if (gridSize == 10) {
            	resetGame(10,10,90); //Restart basic game 
            } else { //If the current game is advanced level
            	resetGame(15,30,195); //Restart advanced game
            }
        });
        // When quit button is pressed, close the entire game
        alertGameOver.getDialogPane().lookupButton(buttonQuit).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            System.exit(0); //Close the program
        });
        
        //Show the alert window and wait for user input
        alertGameOver.showAndWait();
	}
    
	public static void main(String[] args) 
	{
		//We launch the application
		launch(args);
	}
}

