package competitor;

import java.util.Set;

import snowbound.api.*;
import snowbound.api.util.*;
import static snowbound.api.util.Utility.*;

@Agent(name = "Tundra Wookie Slayers")
public class CompetitorAI extends AI{

	Set<Position> spawns = null;
	Set<Base> bases = null;
	Position[] goals = null;
	int numUnitsPerTeam = 0;

	Set<Base> targetBases = null;
	Base AggressorBase = null;

	public CompetitorAI(){

	}

	/**
	 *
	 **/
	public Action action(Turn turn) {
		
		Unit currentUnit = turn.actor();
		
		//fetch the unit id
		Object[] player_array = turn.myUnits().toArray();
		int unit_id = 0;
		for (unit_id = 0; unit_id < player_array.length; unit_id++)
		{
			if (player_array[unit_id] == currentUnit)
			{
				break;
			}
		}

		// Setup
		if (spawns == null){
			spawns = turn.myTeam().spawns();
			bases = turn.allBases();
			numUnitsPerTeam = turn.myUnits().size();
			goals = new Position[numUnitsPerTeam];
			targetBases = turn.allBases();
			targetBases.clear();
			
		}

		

		if (!currentUnit.isSpawned()){
			int numSpawned = 0;

			// Calculate how many guys we have spanwed
			/*for (Unit unit: turn.myUnits()){
				if (unit.isSpawned()){
					numSpawned ++;
				}
			}
			System.out.println(numSpawned);*/

			// Find the correct perk
			Perk newPerk;

			// TODO Wont work if a unit is killed
			if (unit_id == numUnitsPerTeam-1 && numUnitsPerTeam > 3){
				newPerk = Perk.LAYERS; // One Layer member for our team if we have more than 3
				System.out.println("Layers");
			}else if (unit_id == numUnitsPerTeam - 2 && numUnitsPerTeam > 3 || unit_id == numUnitsPerTeam -1 && numUnitsPerTeam <= 3){
				newPerk = Perk.BUCKET;
				System.out.println("Bucket");
			}else{
				newPerk = Perk.CLEATS;
				System.out.println("Cleats");
			}

			// List all not-our bases
			Set<Base> notOurBases = difference(bases, turn.myBases()); 

			int shortestDistance = 100000000;
			int farthestDistance = 100000000;
			Base target = null;
			Position targetSpawn = null;

			// For all possible spawns
			for (Position checkSpot: spawns){

				if (turn.hasUnitAt(checkSpot)){
					System.out.println("Skipping spawn pt.");
					continue;
				}
				
				// Aggressors
				if (newPerk == Perk.BUCKET || newPerk == Perk.LAYERS && !turn.hasUnitAt(checkSpot)){
					Base farthest = furthest(notOurBases, checkSpot);
					int distance = farthest.position().distance(checkSpot);
					if (distance < farthestDistance){
						farthestDistance = distance;
						targetSpawn = checkSpot;
						target = farthest;
					}
				}
				// Check to make sure there is no one at that spawn point and that we are not already heading to that base
				else if (!turn.hasUnitAt(checkSpot)){
					// Bugged Code: && !targetBases.contains(nearest(notOurBases, checkSpot))
					// Find nearest base for this point
					Base nearest = nearest(difference(notOurBases,targetBases), checkSpot);
					int distance = nearest.position().distance(checkSpot);
					// Check to see if this is the shortest path
					if (distance < shortestDistance){
						shortestDistance = distance;
						targetSpawn = checkSpot;
						target = nearest;
					}
				}
			}
			
			if (targetSpawn == null){
				targetSpawn = any(spawns);
				System.out.println("Failed to find spawn... selecting anything");
			}
			
			System.out.println("Selected spawn pt.");

			if (newPerk == Perk.BUCKET || newPerk == Perk.LAYERS){
				// Aggressors
				AggressorBase = furthest(notOurBases, targetSpawn);
			}else{
				// Passive
				targetBases.add(nearest(notOurBases, targetSpawn));
			}
			
			goals[unit_id] = target.position();


			System.out.println("Spawning");
			// Spawn the unit
			return new SpawnAction(targetSpawn, newPerk);

		}else{

			if (unit_id >= 2 && turn.tileAt(currentUnit).snow() > 0)
			{
				return new GatherAction();
			}
			// Standing on a base
			if (turn.hasBaseAt(currentUnit.position()) && !nearest(turn.allBases(), currentUnit).isOwnedBy(turn.myTeam()))
			{
				return new CaptureAction();
			}

			// If there is an empty base, go after it.
			
			Base near;
			if (unit_id >= 2)
			{
				near = furthest(turn.allBases(), currentUnit);
				goals[unit_id] = near.position();
			}
			else if (unit_id < 2)
			{
				near = nearest(turn.allBases(), currentUnit);
				goals[unit_id] = near.position();
			}
			
			return new MoveAction(goals[unit_id]);
		}


	}

}
