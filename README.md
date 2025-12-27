Pacman Game

A Java Swing implementation of the classic Pacman game, built as a school project with specific constraints and design choices.

What it does

You control Pacman through procedurally generated mazes, collecting pellets while avoiding ghosts. Eat power pellets to turn the tables and chase ghosts for bonus points. The game includes multiple levels, power-ups, fruits, and a high score system.

Technical approach

The game follows an MVC architecture. The model handles all game logic including maze generation, ghost AI, collision detection, and score tracking. The view uses Swing components, specifically JTable with custom cell renderers for rendering the game board. The controller manages user input and coordinates between model and view.

Maze generation uses a depth-first search algorithm to create connected paths, then adds loops and removes dead ends to make the maze more interesting. The maze is symmetric and includes portal rows that allow wrapping around the edges.

Ghost AI varies by type. Blinky chases directly, Pinky targets ahead of Pacman, Inky moves randomly, and Clyde switches between chasing and fleeing based on distance. Ghosts use BFS pathfinding to navigate toward their targets. When frightened, they move randomly.

Game loop and threading

The game runs multiple threads for different purposes. A pacman thread handles player movement and updates, a ghost thread manages ghost movement, a power pellet flash thread handles visual effects, and an effect timer thread manages temporary power-up durations.

All UI updates happen on the EDT using SwingUtilities.invokeLater and invokeAndWait to ensure thread safety. This was necessary because the project restrictions didn't allow using graphics classes like Canvas or Graphics2D directly for rendering, so we had to work with Swing components and coordinate threading carefully.

Project constraints

This was built under school project restrictions that influenced several design decisions. We couldn't use graphics classes directly, which is why the rendering happens through JTable cell renderers instead of drawing on a canvas. Threading had to be managed manually with SwingUtilities rather than using more modern approaches. The code structure follows strict MVC separation as required.

The maze generation algorithm had to work within these constraints while still producing playable, interesting mazes. Ghost AI needed to be challenging but fair, and all game state updates needed to be synchronized properly across threads.

Features

Procedurally generated symmetric mazes with portal wrapping
Four ghost types with different AI behaviors
Power pellets that make ghosts vulnerable
Five upgrade types: freeze, slowdown, speed boost, shield, and extra life
Fruit spawning system with cherries, strawberries, and apples
High score persistence
Multiple levels with increasing difficulty
Sound effects and visual feedback
Lives system with respawn mechanics

Running the game

Compile the Java files in the src directory. Make sure the assets folder is in the project root with all images and sound files. Run Main.java to start the game. Use arrow keys to control Pacman. Press Ctrl+Shift+Q to quit to menu during gameplay.

The game saves high scores to scores.ser and scores.txt files in the project root.

