# mazesolver
And You said, Let there be maze: and there was maze.

# Maze methods
## make
- Create a maze.

## xfs
- xfs(maze, Maze.ADD_BFS)
Execute a breadth first search.
- xfs(maze, Maze.ADD_DFS)
Execute a depth first search.

## main
- The entry point.
- Reads System.in and create a maze. 
- Solve the maze(optional).
- Write the maze to System.out(optional).

### stdin examples
- 11 13 0 0 -make -debug
- 5 15 0 0 -bfs -debug
- 5 15 100 0 -bfs -debug
- 5 21 0 20 -bfs -debug
- 5 21 0 20 -dfs -debug
- 2501 2501 50 1000 -make -no-debug
- 2501 2501 50 1000 -bfs -no-debug
- 2501 2501 50 1000 -dfs -no-debug
