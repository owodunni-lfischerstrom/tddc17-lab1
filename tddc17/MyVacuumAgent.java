package tddc17;


import java.util.EmptyStackException;
import java.util.Stack;

import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.State;
import aima.core.agent.impl.*;


class MyAgentState
{
	public int[][] world = new int[21][21];
	public int initialized = 0;
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int ACTION_NONE 		= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;
	
	final static int WEST = 0;
	final static int NORTH = 1;
	final static int EAST = 2;
	final static int SOUTH = 3;
	
	
	
	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
		for (int i=0; i < world.length; i++)
			for (int j=0; j < world[i].length ; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = CLEAR;
		agent_last_action = ACTION_NONE;
	}
	
	public void updateWorld(int x_position, int y_position, int info)
	{
		world[x_position][y_position] = info;
	}
	
	public void printWorldDebug()
	{
		for (int i=0; i < world.length; i++)
		{
			for (int j=0; j < world[i].length ; j++)
			{
				if (world[j][i]==UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i]==WALL)
					System.out.print(" # ");
				if (world[j][i]==CLEAR)
					System.out.print(" . ");
				if (world[j][i]==DIRT)
					System.out.print(" D ");
			}
			System.out.println("");
		}
	}
}

class MyAgentProgram implements AgentProgram {

	// Here you can define your variables!
	public int iterationCounter = 100;
	public MyAgentState state = new MyAgentState();
	
	protected Stack<Integer> homeActions = null;
	protected boolean goingHome = false;

	public Stack<Integer> turningActions = null;
	public boolean isTurning = false;
	
	public static final int TURN_LEFT = 0;
	public static final int TURN_RIGHT = 1;
	public static final int FORWARD = 2;
	public static final int SUCK = 3;
	
	@Override
	public Action execute(Percept percept) {
		
		// This example agent program will update the internal agent state while only moving forward.
		// Note! It works under the assumption that the agent starts facing to the right.
		
		System.out.println("x: "+state.agent_x_position);
		System.out.println("y: "+state.agent_y_position);
		
	    iterationCounter--;
	    
	    if (iterationCounter==0)
	    	return NoOpAction.NO_OP;
	    
	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("percept: " + p);
	    
	    // State update based on the percept value and the last action
	    if (state.agent_last_action==state.ACTION_MOVE_FORWARD)
	    {
	    	if (!bump)
	    	{
	    		switch (state.agent_direction) {
				case MyAgentState.NORTH:
					state.agent_y_position--;
					break;
				case MyAgentState.EAST:
					state.agent_x_position++;				
					break;
				case MyAgentState.SOUTH:
					state.agent_y_position++;
					break;
				case MyAgentState.WEST:
					state.agent_x_position--;
					break;
	    		} 
	    	}
	    	else
	    	{
	    		state.updateWorld(state.agent_x_position+1,state.agent_y_position,state.WALL);
	    	}
	    }
	    if (dirt)
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
	    else
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);
	    
	    state.printWorldDebug();
	    
	    
	    // Next action selection based on the percept value
	    if(goingHome)
	    {
	    	if (home)
	    	{
	    		System.out.print("iterations: ");
	    		System.out.println(iterationCounter);
				return NoOpAction.NO_OP;
			}
	    	else
	    	{
	    		return goHome();
	    	}
	    }
	    else if (needToGoHome())
	    {
			initializeGoingHome();
			return goHome();
		}
	    else if (dirt)
		{
	    	return suck();
	    } 
	    else if(bump)
	    {
	    	if (isTurning)
	    	{
				initializeGoingHome();
				return goHome();
			}
	    	else
	    	{
	    		initializeUTurn();
	    		return uTurn();
	    	}
	    	
    	}
	    else if(isTurning)
	    {
	    	return uTurn();
	    }
	    else
    	{
	    	return forward();
    	}
	}

	/**
	 * Returns the next action to go home.
	 */
	protected Action goHome() {
		switch (homeActions.pop()) {
		case TURN_LEFT:
			return turnLeft();
		case TURN_RIGHT:
			return turnRight();
		case FORWARD:
			return forward();
		default:
			//BUG!
			return forward();
		}
	}
	
	/**
	 * Creates a stack with actions to get home and sets the goingHome flag.
	 */
	protected void initializeGoingHome() {
		goingHome = true;
		homeActions = new Stack<Integer>();
		
		for (int i = 0; i < state.agent_x_position; i++) {
			homeActions.push(FORWARD);
		}
		
		homeActions.push(TURN_LEFT);
		
		for (int i = 0; i < state.agent_y_position; i++) {
			homeActions.push(FORWARD);
		}
		
		switch (state.agent_direction) {
		case MyAgentState.WEST:
			homeActions.push(TURN_RIGHT);
			break;
		case MyAgentState.EAST:
			homeActions.push(TURN_LEFT);
			break;
		case MyAgentState.SOUTH:
			homeActions.push(TURN_LEFT);
			homeActions.push(TURN_LEFT);
			break;
		}
	}
	
	/**
	 * Check if it's time to go home.
	 * Returns true if it's time else false.
	 */
	protected boolean needToGoHome() {
		return (state.agent_x_position + state.agent_y_position + 2 == iterationCounter);
	}
	
	/**
	 * 
	 */
	private void initializeUTurn() {
		turningActions = new Stack<Integer>();
		switch (state.agent_direction) {
		case MyAgentState.WEST:
			isTurning = true;
			turningActions.push(TURN_LEFT);
			turningActions.push(FORWARD);
			turningActions.push(TURN_LEFT);
			break;
		case MyAgentState.EAST:
			isTurning = true;
			turningActions.push(TURN_RIGHT);
			turningActions.push(FORWARD);
			turningActions.push(TURN_RIGHT);
			break;
		default:
			System.out.println("WAT");
			break;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private Action uTurn() {
		try {
			switch (turningActions.pop()) {
			case TURN_LEFT:
				return turnLeft();
			case TURN_RIGHT:
				return turnRight();
			case FORWARD:
				return forward();
			default:
				//TODO::FIX!
				System.out.println("BUG!");
				return forward();
			}
		} catch (EmptyStackException e) {
			System.out.println("Empty Stack BUG!!!");
			isTurning = false;
			return forward();
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	private Action suck() {
		System.out.println("DIRT -> choosing SUCK action!");
    	state.agent_last_action=state.ACTION_SUCK;
		return LIUVacuumEnvironment.ACTION_SUCK;
	}
	
	/**
	 * 
	 * @return
	 */
	private Action forward() {
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}
	
	/**
	 * 
	 * @return
	 */
	protected Action turnLeft() {
		state.agent_last_action=state.ACTION_TURN_LEFT;
		state.agent_direction = (state.agent_direction-1)%4;
		if(state.agent_direction<0) state.agent_direction = 3;
		return LIUVacuumEnvironment.ACTION_TURN_LEFT;
	}
	
	/**
	 * 
	 * @return
	 */
	private Action turnRight() {
		state.agent_last_action=state.ACTION_TURN_RIGHT;
		state.agent_direction = (state.agent_direction+1)%4;
		return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
	}
	
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {
    	super(new MyAgentProgram());
	}
}
