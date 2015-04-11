package competitor;

import java.util.Set;

import snowbound.api.*;
import snowbound.api.util.*;
import static snowbound.api.util.Utility.*;

@Agent(name = "Tundra Wookie Slayers")
public class CompetitorAI extends AI{

	Set<Position> spawns = null;

	public CompetitorAI(){

	}

	/**
	 *
	 **/
	public Action action(Turn turn) {

		// Setup
		if (spawns == null){
			spawns = turn.myTeam().spawns();
		}

		Unit currentUnit = turn.actor();

		if (!currentUnit.isSpawned()){
			for (Position possibleSpot: spawns){
				if (!turn.hasUnitAt(possibleSpot)){
					// Use Possible set
					return new SpawnAction(possibleSpot, Perk.CLEATS);
				}
			}
			// No spawn points available.
			try {
				throw new Exception("Could not spawn");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}else{
			return null;
		}


	}

}
