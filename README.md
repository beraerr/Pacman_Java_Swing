Pacman Game - Java Swing Implementation

A fully-featured Pacman game built in Java using Swing. Features procedural maze generation, intelligent ghost AI with pathfinding, and a complete game loop with threading. This was a school project with some specific constraints that shaped how things were built.

---

What It Does

You control Pacman through procedurally generated mazes, collecting pellets while avoiding four ghosts. Each ghost has its own AI behavior. Eat power pellets to turn the tables and chase ghosts for bonus points. Multiple levels, power-ups, fruits, and high scores are all included.

---

How Maze Generation Works

Every game gets a fresh maze. The generation uses a multi-stage process to make sure mazes are always playable and interesting.

- DFS foundation: Starts with all walls, then uses depth-first search from (1,1) to carve paths. Moves in steps of two cells so paths stay separated. Directions get shuffled randomly so each maze is different.

- Adding loops: After the base maze is done, extra loops get added. Number scales with size - small mazes get fewer, big ones get more. This prevents boring tree structures and gives multiple routes between areas.

- Ghost house: A house gets carved in the center where ghosts spawn. Size adapts to the maze dimensions automatically.

- Making it symmetric: The left half gets mirrored to the right. Makes it feel balanced instead of just random.

- Ensuring connectivity: Uses BFS to check if everything is connected. If disconnected regions are found, it finds the shortest path between them and carves a connection. Keeps doing this until everything is reachable.

- Removing dead ends: Scans for cells with only one exit and extends paths from them. Number of removals scales with maze size. Makes navigation way better.

- Portal rows: Special rows where the edges are paths, so you can wrap around the screen. Small mazes get one portal row in the middle, bigger ones get multiple rows spread out.

End result: a unique maze every time that's always fully connected and actually fun to play.

---

Ghost AI and Pathfinding

Each ghost uses a different strategy. Makes them feel distinct and keeps gameplay interesting.

All ghosts use breadth-first search for pathfinding. BFS finds the shortest path from where the ghost is to where it wants to go, handling portal wrapping correctly. When a ghost needs to move, it calculates valid adjacent cells, runs BFS to get the optimal path, then takes the first step of that path.

Here's how each ghost behaves:

- Blinky (red) - The chaser: Always goes straight for Pacman's current position using BFS. Most dangerous one, constantly on your tail.

- Pinky (pink) - The ambusher: Looks four cells ahead in Pacman's direction and pathfinds there instead. Tries to cut you off before you get there.

- Inky (cyan) - The random: Just picks random valid moves. Unpredictable, harder to plan around.

- Clyde (orange) - The hybrid: If far from Pacman (more than 6 cells), chases like Blinky. If close, switches to fleeing and heads for a corner instead. Interesting dynamic.

When ghosts are frightened after eating a power pellet, they drop their normal strategies and move randomly. Easier to avoid but harder to catch for points.

Ghosts have state machines. They start in the house, transition to leaving, then normal behavior. When eaten, they pathfind back to the house, then cycle back to normal. The pathfinding handles portal wrapping so ghosts can use portals just like Pacman.

---

Architecture and Threading

Uses Model-View-Controller. Model has all the game logic and state. View handles rendering with Swing components. Controller manages input and coordinates everything.

Rendering is done through JTable with custom cell renderers. Each cell is a JPanel that paints whatever should be there - walls, pellets, Pacman, ghosts, fruits, upgrades. Had to do it this way because the project restrictions didn't allow using graphics classes like Canvas or Graphics2D directly.

Game loop runs on multiple threads:

- Pacman thread handles player movement and game updates
- Ghost thread manages ghost movement and AI
- Power pellet flash thread handles the blinking effect
- Effect timer thread manages temporary power-up durations

All UI updates get synchronized to the Event Dispatch Thread using SwingUtilities.invokeLater and invokeAndWait. Swing components can only be accessed from the EDT, so game logic runs on background threads but any UI changes get dispatched to the EDT.

Had to do manual thread management because the project required it. Couldn't use JavaFX animation timelines or game loop frameworks. Code carefully coordinates between threads to avoid race conditions and keep gameplay smooth.

---

Project Constraints

This was built under school restrictions that affected the design:

- No direct graphics classes: Couldn't use graphics classes directly, so ended up with JTable-based rendering. Required careful coordination between model data structure and view rendering.

- Manual threading: Had to manage threading manually with SwingUtilities instead of modern concurrency tools. Meant paying close attention to thread safety and making sure all UI updates happened on the EDT.

- Strict MVC: MVC architecture was required, so strict separation between model, view, and controller. Made code more organized but required careful design for component communication.

- Efficient algorithms: Maze generation had to work within constraints while still producing interesting mazes every time. Multi-stage approach ensures mazes are always valid and connected regardless of random seed.

- Real-time pathfinding: Ghost AI needed to be challenging but fair, and pathfinding had to be efficient enough for the game loop. BFS gives optimal paths while being fast enough for real-time.

---

Features

- Procedurally generated symmetric mazes with portal wrapping - different every time, always fully connected

- Four ghost types with distinct AI - Blinky chases, Pinky ambushes, Inky is random, Clyde switches between chasing and fleeing

- BFS pathfinding for ghosts - they always take optimal paths, makes them challenging

- Power pellets make ghosts vulnerable - eat them for increasing multipliers (200, 400, 800, 1600 points)

- Five upgrade types spawn randomly - freeze stops ghosts, slowdown makes them slower, speed boost makes Pacman faster, shield gives invincibility, health gives extra lives

- Fruit spawning - cherries, strawberries, and apples appear based on pellets eaten, give bonus points

- High score persistence - saves to both binary and text format for reliability

- Multiple levels with increasing difficulty - ghosts get faster as levels progress

- Sound effects and visual feedback - power pellets blink, ghosts change color when frightened, chomping sounds

- Lives system with respawn - lose a life and respawn at start, ghosts return to house

- Ghost state management - ghosts cycle through house, leaving, normal, frightened, and eaten states

---

Running the Game

Compile all Java files in the src directory. Make sure the assets folder is in the project root with all images and sounds. Run Main.java to start.

Controls:

- Arrow keys to control Pacman
- Ctrl+Shift+Q to quit to menu during gameplay  
- Sound button in top panel toggles sound effects

Game starts at main menu where you can start a new game, view high scores, or exit. When starting new game, you'll pick board dimensions between 10x10 and 100x100.

High scores save automatically when you make the top list. Tracks your best scores across sessions. Saves to scores.ser (binary) and scores.txt (text) in the project root. Both files get created automatically on first run.
