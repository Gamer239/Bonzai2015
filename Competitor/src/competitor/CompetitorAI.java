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

	Set<Base> targetBases = null;
	Base AggressorBase = null;

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
			targetBases = turn.allBases();
			targetBases.clear();
			
		}

		Unit currentUnit = turn.actor();

		if (!currentUnit.isSpawned()){
			int numSpawned = 0;

			// Calculate how many guys we have spanwed
			for (Unit unit: turn.myUnits()){
				if (unit.isSpawned()){
					numSpawned ++;
				}
			}
			System.out.println(numSpawned);

			// Find the correct perk
			Perk newPerk;

			// TODO Wont work if a unit is killed
			if (numSpawned == numUnitsPerTeam-1 && numUnitsPerTeam > 3){
				newPerk = Perk.LAYERS; // One Layer member for our team if we have more than 3
				System.out.println("Layers");
			}else if (numSpawned == numUnitsPerTeam - 2 && numUnitsPerTeam > 3 || numSpawned == numUnitsPerTeam -1 && numUnitsPerTeam <= 3){
				newPerk = Perk.BUCKET;
				System.out.println("Bucket");
			}else{
				newPerk = Perk.CLEATS;
				System.out.println("Cleats");
			}

			// List all not-our bases
			Set<Base> notOurBases = difference(bases, turn.myBases()); 

			int shortestDistance = 100000000;
			int farthestDistance = 0;
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
					if (distance > farthestDistance){
						farthestDistance = distance;
						targetSpawn = checkSpot;
					}
				}
				// Check to make sure there is no one at that spawn point and that we are not already heading to that base
				else if (!turn.hasUnitAt(checkSpot) && !targetBases.contains(nearest(notOurBases, checkSpot))){
					// Find nearest base for this point
					Base nearest = nearest(notOurBases, checkSpot);
					int distance = nearest.position().distance(checkSpot);
					// Check to see if this is the shortest path
					if (distance < shortestDistance){
						shortestDistance = distance;
						targetSpawn = checkSpot;
					}
				}
			}

			if (newPerk == Perk.BUCKET || newPerk == Perk.LAYERS){
				// Aggressors
				AggressorBase = furthest(notOurBases, targetSpawn);
			}else{
				// Passive
				targetBases.add(nearest(notOurBases, targetSpawn));
			}


			System.out.println("Spawning");
			// Spawn the unit
			return new SpawnAction(targetSpawn, newPerk);

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
