package mazegen2;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class gen {
	
	public static void main(String[] args) {
		int width = 500;
		int height = 500;
		
		generate(width, height);
		
	}
	
	
	public static int[][] generateMaze(int width, int height) { 
		
		//initialize maze
		// select a start and finish square
		// assign sectors to each square
		Random rng = new Random();
		
		int[][] maze = new int[width][height];
		int[][] mazeDirections = new int[width][height];
		
		int start = rng.nextInt(width);
		int end = rng.nextInt(width);
		
		ArrayList<Integer> possibilitiesLeft = new ArrayList<Integer>();
		
		for(int y = 0; y < height; y++) {
			for(int x = 0; x< width; x++) {
				int temp = y*width + x;
				maze[x][y] = temp;
				mazeDirections[x][y] = 0;
				possibilitiesLeft.add(temp);
				
			}
		}
		
		
		//make it so that the finish has no side roads
		// to do this remove the squares around the finish from the list of possible squares
		// then assign a random rotation to the finish square
		int finishx = end;
		int finishy = height-1;

		//remove square with highest index first
		if(finishx != width-1)
			possibilitiesLeft.remove(finishy*(width) + finishx + 1);
		possibilitiesLeft.remove(finishy*(width) + finishx);
		if(finishx != 0)
			possibilitiesLeft.remove(finishy*(width) + finishx - 1);
		possibilitiesLeft.remove( (finishy - 1) * (width) + finishx);

		ArrayList<Direction> possibilities = new ArrayList<Direction>(); 
		
		if(finishx != 0) {
			possibilities.add(Direction.LEFT);
		}
		if(finishx != width-1) {
			possibilities.add(Direction.RIGHT);
		}
		possibilities.add(Direction.DOWN);
		
		switch (possibilities.get(rng.nextInt(possibilities.size()))) {
		case LEFT: 
			mazeDirections[finishx][finishy] = 0b0001;
			maze[finishx-1][finishy] = maze[finishx][finishy];
			break;
		case DOWN: 
			mazeDirections[finishx][finishy] = 0b1000;
			maze[finishx][finishy-1] = maze[finishx][finishy];
			break;
		case RIGHT: 
			mazeDirections[finishx][finishy] = 0b0100;
			maze[finishx+1][finishy] = maze[finishx][finishy];
			break;
		default:
			break;
		}
		
		
		
		//connect the maze without creating loops
		while (!possibilitiesLeft.isEmpty()) {
			int randomIndex = rng.nextInt(possibilitiesLeft.size());
			int randomTile = possibilitiesLeft.get(randomIndex);
			int randomx = randomTile % height;
			int randomy = (int) Math.floor(randomTile / width);
			
			possibilities = new ArrayList<Direction>(); 
			
			if( ((mazeDirections[randomx][randomy] & 0b0001) != 0b0001) && (randomx != 0) && (maze[randomx][randomy] != maze[randomx-1][randomy]) )
				possibilities.add(Direction.LEFT);
			if( ((mazeDirections[randomx][randomy] & 0b0010) != 0b0010) && (randomy != height-1) && (maze[randomx][randomy] != maze[randomx][randomy+1]) )
				possibilities.add(Direction.UP);
			if( ((mazeDirections[randomx][randomy] & 0b0100) != 0b0100) && (randomx != width-1) && (maze[randomx][randomy] != maze[randomx+1][randomy]) )
				possibilities.add(Direction.RIGHT);
			if( ((mazeDirections[randomx][randomy] & 0b1000) != 0b1000) && (randomy != 0) && (maze[randomx][randomy] != maze[randomx][randomy-1]) )
				possibilities.add(Direction.DOWN);
			
			if(possibilities.size() == 0)
				possibilitiesLeft.remove(randomIndex);
			else {
				Direction randomDirection = possibilities.get(rng.nextInt(possibilities.size()));
				
				int replacex = -1;
				int replacey = -1;
				
				switch (randomDirection) {
					case LEFT: {
						replacex = randomx - 1;
						replacey = randomy;
						mazeDirections[randomx][randomy] = mazeDirections[randomx][randomy] | 0b0001;
						break;
					}
					case UP: {
						replacex = randomx;
						replacey = randomy + 1;
						mazeDirections[randomx][randomy] = mazeDirections[randomx][randomy] | 0b0010;
						break;
					}
					case RIGHT: {
						replacex = randomx + 1;
						replacey = randomy;
						mazeDirections[randomx][randomy] = mazeDirections[randomx][randomy] | 0b0100;
						break;
					}
					case DOWN: {
						replacex = randomx;
						replacey = randomy - 1;
						mazeDirections[randomx][randomy] = mazeDirections[randomx][randomy] | 0b1000;
						break;
					}				
				}
				
				int valuetoreplace = maze[replacex][replacey];
				int replacementValue = maze[randomx][randomy];
				
				maze = propagate(maze, replacex, replacey, valuetoreplace, replacementValue);
			}
		}
		
		int[][] filledmaze = fillMaze(maze, mazeDirections);
		return finishmaze(filledmaze, width, start, end);
		
	}
	
	public static int[][] fillMaze(int[][] maze, int[][] directions) {
		int width = maze.length;
		int height = maze[0].length;
		int[][] filledmaze = new int[width*2-1][height*2-1];
		
		for( int[] row : filledmaze)
			for(int square : row)
				square = 0;
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				filledmaze[2*x][2*y] = 1;
				
				if( (directions[x][y] & 0b0001) == 0b0001)
					filledmaze[2*x-1][2*y] = 1;
				if( (directions[x][y] & 0b0010) == 0b0010)
					filledmaze[2*x][2*y+1] = 1;
				if( (directions[x][y] & 0b0100) == 0b0100)
					filledmaze[2*x+1][2*y] = 1;
				if( (directions[x][y] & 0b1000) == 0b1000)
					filledmaze[2*x][2*y-1] = 1;
					
			}
		}
		
		return filledmaze;
	}
	
	public static int[][] finishmaze(int[][] filledmaze, int width, int start, int end) {
		int[][] finishedmaze = new int[filledmaze.length+2][filledmaze[0].length+2];
		
		start = start * 2 + 1;
		end = end * 2 + 1;
		
		for(int x = 0; x < filledmaze.length; x++)
			for(int y = 0; y < filledmaze[0].length; y++) 
				finishedmaze[x+1][y+1] = filledmaze[x][y];
		
		finishedmaze[start][0] = 2;
		finishedmaze[end][finishedmaze.length-1] = 2;
		
		return finishedmaze;
	}
	
	public static int[][] propagate(int[][] maze, int x, int y, int valuetoreplace, int replacementvalue) {
		
		int width = maze.length;
		int height = maze[0].length;

		ArrayList<int[]> stack = new ArrayList<int[]>();
		stack.add(new int[] {x,y});
		
		while(!stack.isEmpty() ) {
			int queuex = stack.get(stack.size()-1)[0];
			int queuey = stack.get(stack.size()-1)[1];
			
			stack.remove(stack.size()-1);
			
			maze[queuex][queuey] = replacementvalue;

			if(queuex != 0 && maze[queuex-1][queuey] == valuetoreplace) {
				stack.add(new int[] {queuex-1,queuey});
			}
			if(queuey != height-1 && maze[queuex][queuey+1] == valuetoreplace) {
				stack.add(new int[] {queuex,queuey+1});
			}
			if(queuex != width-1 && maze[queuex+1][queuey] == valuetoreplace) {
				stack.add(new int[] {queuex+1,queuey});
			}
			if(queuey != 0 && maze[queuex][queuey-1] == valuetoreplace) {
				stack.add(new int[] {queuex,queuey-1});
			}
			
		}
		
		
		
		return maze;
	}
	
public static int[][] fakestackpropagate(int[][] maze, int x, int y, int valuetoreplace, int replacementvalue) {
		
		int width = maze.length;
		int height = maze[0].length;

		ArrayList<int[]> Stack = new ArrayList<int[]>();
		Stack.add(new int[] {x,y});
		
		while(!Stack.isEmpty() ) {
			int queuex = Stack.get(0)[0];
			int queuey = Stack.get(0)[1];
			
			Stack.remove(0);
			
			maze[queuex][queuey] = replacementvalue;

			if(queuex != 0 && maze[queuex-1][queuey] == valuetoreplace) {
				Stack.add(0,new int[] {queuex-1,queuey});
			}
			if(queuey != height-1 && maze[queuex][queuey+1] == valuetoreplace) {
				Stack.add(0,new int[] {queuex,queuey+1});
			}
			if(queuex != width-1 && maze[queuex+1][queuey] == valuetoreplace) {
				Stack.add(0,new int[] {queuex+1,queuey});
			}
			if(queuey != 0 && maze[queuex][queuey-1] == valuetoreplace) {
				Stack.add(0,new int[] {queuex,queuey-1});
			}
			
		}
		
		
		
		return maze;
	}
	
	public static int[][] replaceAll(int[][] maze, int x, int y, int valuetoreplace, int replacementvalue) {
		maze[x][y] = replacementvalue;
		
		int width = maze.length;
		int height = maze[0].length;
		
		for(int a = 0; a < width; a++) {
			for(int b = 0; b < height; b++) {
				if(maze[a][b] == valuetoreplace) {
					maze[a][b] = replacementvalue;
				}
			}
		}
		
		return maze;
	}
	
	
	
	
	
	public static File drawMaze(int[][] maze,int width,int height) 
    {
		int pixelHeight = 1;
		int pixelWidth = 1;
		
	    JFrame jf = new JFrame();
	    JLabel jl = new JLabel();

	    //3 bands in TYPE_INT_RGB
	    int NUM_BANDS = 3;
	    int[] arrayimage = new int[(width) * (height) * pixelHeight * pixelWidth * NUM_BANDS];

	    for (int i = 0; i < height; i++)
	    {
		    for (int k = 0; k < pixelHeight; k++) {
		    	for (int j = 0; j < width; j++) {
			        for (int band = 0; band < NUM_BANDS * pixelWidth; band++)
			        	//arrayimage[((i * width * pixelWidth + k * width) + j)*NUM_BANDS*pixelWidth + band] = maze[j][height-i-1]*255;
			        	arrayimage[((i * width * pixelWidth + k * width) + j)*NUM_BANDS*pixelWidth + band] 
			        			= ((maze[j][height-i-1] == 1) || (maze[j][height-i-1] == 2 && band == 1) ? 1 : 0)*255;
			      }
			}	      
	    }
	    
	    Image image = getImageFromArray(arrayimage, (width) * pixelWidth, (height) * pixelHeight);

	    try {
		    File file = new File("maze.png");
			ImageIO.write(imageToBufferedImage(image),"png",file);
			System.out.println("printed image");
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		    return null;
		}
	    
    }
	
	public static BufferedImage imageToBufferedImage(Image im) {
	     BufferedImage bi = new BufferedImage
	        (im.getWidth(null),im.getHeight(null),BufferedImage.TYPE_INT_RGB);
	     Graphics bg = bi.getGraphics();
	     bg.drawImage(im, 0, 0, null);
	     bg.dispose();
	     return bi;
	  }
	
	 public static Image getImageFromArray(int[] pixels, int width, int height)
	  {
	    BufferedImage image =
	        new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    WritableRaster raster = (WritableRaster) image.getData();
	    raster.setPixels(0, 0, width, height, pixels);
	    image.setData(raster);
	    return image;
	  }
	 
	 public static File generate(int width, int height) {
		 int[][] maze = generateMaze(width, height);
		 return drawMaze(maze, maze.length, maze[0].length);
	 }
	
}
