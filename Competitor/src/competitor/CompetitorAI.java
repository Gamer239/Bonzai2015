package competitor;

import java.util.Set;

import snowbound.api.*;
import snowbound.api.util.*;
import static snowbound.api.util.Utility.*;

@Agent(name = "Tundra Wookie Slayers")
public class CompetitorAI extends AI{

	Set<Position> spawns = null;
	Set<Base> bases = null;
	int numUnitsPerTeam = 0;
	
	public CompetitorAI(){

	}

	/**
	 *
	 **/
	public Action action(Turn turn) {

		// Setup
		if (spawns == null){
			spawns = turn.myTeam().spawns();
			bases = turn.allBases();
			numUnitsPerTeam = turn.myUnits().size();
		}

		Unit currentUnit = turn.actor();

		if (!currentUnit.isSpawned()){
			int numSpawned = 0;
			
			for (Unit unit: turn.myUnits()){
				if (unit.isSpawned()){
					numSpawned ++;
				}
			}
			
			for (Position possibleSpot: spawns){
				if (!turn.hasUnitAt(possibleSpot)){
					Perk newPerk;
					
					if (numSpawned == numUnitsPerTeam && numUnitsPerTeam > 3){
						newPerk = Perk.LAYERS; // One Layer member for our team if we have more than 3
					}else if (numSpawned == numUnitsPerTeam - 1 && numUnitsPerTeam > 3 || numSpawned == numUnitsPerTeam && numUnitsPerTeam <= 3){
						newPerk = Perk.BUCKET;
					}else{
						newPerk = Perk.CLEATS;
					}
					
					// Use Possible set
					return new SpawnAction(possibleSpot, newPerk);
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
		
			
			// Standing on a base
			if (turn.hasBaseAt(currentUnit.position())){
				return new CaptureAction();
			}
			
			// If there is an empty base, go after it.
			Base near = nearest(turn.allBases(), currentUnit);
			return new MoveAction(near.position());
		}


	}

}
