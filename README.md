# wormSweeper

Compilation & Execution Instruction (Terminal)
    • This program is run in the terminal because Eclipse is not given permission for webcam in Mac.
    • JavaCV/OpenCV is used, so the modules need to be included

1. Locate the path of the javacv-platform-1.5.10-bin folder in the zip file. This contains all necessary JavaCV/OpenCV jar files.

2. Set the environment variable PATH_TO_JAVACV. Add a line in your /Users/vivianli/.zshrc file:
export PATH_TO_JAVACV=/Users/vivianli/java_workspace/javacv-platform-1.5.10-bin

3. Navigate to /src folder of the wormSweeper project.

4. Compile using this line: 
javac --module-path "$PATH_TO_FX:$PATH_TO_JAVACV" --add-modules javafx.controls,org.bytedeco.javacv WormSweeper.java

5. Run it using this line:
java --module-path "$PATH_TO_FX" --add-modules javafx.controls -cp "$PATH_TO_JAVACV/*:." WormSweeper


Game Objects and Functionalities: 
    • Basic & Advanced level buttons
        ◦ When each level button is pressed, a new game in that level is started
        ◦ When hovering, a tooltip appears showing the difficulty level (grid size, worm count)
    • Timer
        ◦ Timer resets when a new game starts and starts 
            ▪ when the first button in the playing field is clicked
            ▪ when the user tries to unlock a hint firsts
    • Playing field
        ◦ Same functionality as original minesweeper for
            ▪ left click to reveal a cell, showing adjacent cell count recursively or a worm 
    • Placing a hook (flag)
        ◦ New interaction: drag from the hook image and drop to any cell to mark that cell
        ◦ Right click to remove the hook from a cell
        ◦ The hook count label updates
    • Unlock hint button
        ◦ Press the button to open the dialog window to unlock a hint
        ◦ New interaction: show something red to the camera to be detected
        ◦ If red is detected, instruction updates and user has to click the newly activated Apply button 
        ◦ A worm is revealed as a hint
        ◦ If user clicks Cancel, then no hint is revealed
    • Game won
        ◦ User sees the time used
        ◦ User has to select restart or quit
    • Game lost
        ◦ User has to select restart or quit
Console debug:
    • Console prints the minefield answer when a new game starts. (0 is safe, 1 is unsafe)
    • Console prints camera on/off status
    • Console prints error code
