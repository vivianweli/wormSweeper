# wormSweeper
This is a twist on the classic minesweeper game focused on developing some new user interactions to make the game more fun. For example, player needs to drag and drop the hook (flag) onto the grid, and can show the webcam something red to unlock a hint.


## Game Objects and Functionalities ##
- Basic & Advanced level buttons
  - When each level button is pressed, a new game in that level is started
  - When hovering, a tooltip appears showing the difficulty level (grid size, worm count)
- Timer
    - Timer resets when a new game starts and starts
        - when the first button in the playing field is clicked
        - when the user tries to unlock a hint firsts
- Playing field
    - Same functionality as original minesweeper forleft click to reveal a cell, showing adjacent cell count recursively or a worm
- Placing a hook (flag)
  - New interaction: drag from the hook image and drop to any cell to mark that cell
  - Right click to remove the hook from a cell
  - The hook count label updates
- Unlock hint button
  - Press the button to open the dialog window to unlock a hint
  - New interaction: show something red to the camera to be detected
  - If red is detected, instruction updates and user has to click the newly activated Apply button
  - A worm is revealed as a hint
  - If user clicks Cancel, then no hint is revealed
- Game won
  - User sees the time used
  - User has to select restart or quit
- Game lost
  - User has to select restart or quit

## Compilation & Execution Instruction (Terminal) ##
  * This program is run in the terminal because Eclipse is not given permission for webcam in Mac.
  * JavaCV/OpenCV is used, so the modules need to be included

1. Set the environment variable PATH_TO_FX. Add a line in your /Users/your_user_name/.zshrc file:
-      export PATH_TO_FX=/Users/your_user_name/eclipse-workspace/javafx-sdk-18.0.2/lib
2. Download JavaCV library from https://github.com/bytedeco/javacv/releases/tag/1.5.10. Locate the path of the javacv-platform-1.5.10-bin folder in the zip file. This contains all necessary JavaCV/OpenCV jar files.
3. Set the environment variable PATH_TO_JAVACV. Add a line in your /Users/your_user_name/.zshrc file:
-      export PATH_TO_JAVACV=/Users/your_user_name/java_workspace/javacv-platform-1.5.10-bin
4. Navigate to /src folder of the wormSweeper project.

5. Compile using this line: 
-      javac --module-path "$PATH_TO_FX:$PATH_TO_JAVACV" --add-modules javafx.controls,org.bytedeco.javacv WormSweeper.java

6. Run it using this line:
-      java --module-path "$PATH_TO_FX" --add-modules javafx.controls -cp "$PATH_TO_JAVACV/*:." WormSweeper
## Console debug ##
- Console prints the minefield answer when a new game starts. (0 is safe, 1 is unsafe)
- Console prints camera on/off status
- Console prints error code
# Screenshots of the game #
<figure>
  <img src="https://github.com/user-attachments/assets/be104577-e5ce-4866-a7ba-45b1e389c2f0" alt="image" width="500"/>
  <figcaption>Hover for level difficulty</figcaption>
</figure>
<figure>
  <img src="https://github.com/user-attachments/assets/38c49900-a5f3-4f50-a6b3-8a23f6c1105b" alt="image" width="500"/>
  <figcaption>Basic level playground</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/788f5ff0-53ad-4d50-ac3c-028b330e9a7f" alt="image" width="500"/>
  <figcaption>Advanced level playground</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/65568977-5dd3-4fc4-b194-47aa19461cd1" alt="image" width="500"/>
  <figcaption>During play, place hooks, reveal cells</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/769e1927-07e3-45fb-9ab5-5add76ad9241" alt="image" width="500"/>
  <figcaption>Camera detecting...</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/74833f5e-8a5d-4253-8208-a0ff5276690f" alt="image" width="500"/>
  <figcaption>Camera captured RED!</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/68a04e4d-59b2-4c19-89b3-8386bcd53dd0" alt="image" width="500"/>
  <figcaption>A free hint revealed.</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/e2d330d4-73ce-476c-8b94-1d13223a78d6" alt="image" width="500"/>
  <figcaption>Lose Screen</figcaption>
</figure>

<figure>
  <img src="https://github.com/user-attachments/assets/e4f47c22-3720-4841-a8a6-3aeb275cdf74" alt="image" width="500"/>
  <figcaption>Win Screen</figcaption>
</figure>

## Images Attribution ##
All images are obtained from Freepik.
- greenapple.png - <a href="https://www.freepik.com/icon/fruit_15805008#fromView=search&page=1&position=24&uuid=646ed804-2cee-426e-a26f-5dc7f1fabcb5">Icon by Minh Do</a>
- redapple.png - <a href="https://www.freepik.com/icon/apple_701487#fromView=search&page=1&position=41&uuid=6ab71b3d-1c79-4715-bd32-7e8823c6d822">Icon by Good Ware</a>
- hook.png - <a href="https://www.freepik.com/icon/fishing-hook_6376133">Icon by mangsaabguru</a>
- wormInApple.png - <a href="https://www.freepik.com/icon/worm_4831317">Icon by Freepik</a>


