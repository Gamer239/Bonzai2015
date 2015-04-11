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
	
	private boolean privateBases(Turn turn){
		
		for (Base b: turn.allBases()){
			if (!b.hasOwner())
				return true;
		}
		return false;
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
		
		// List all not-our bases
		Set<Base> notOurBases = difference(bases, turn.myBases()); 

		

		if (!currentUnit.isSpawned()){
			// Find the correct perk
			Perk newPerk;

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
			
			boolean aggressor = false;
			if (currentUnit.perk() == Perk.BUCKET || currentUnit.perk() == Perk.LAYERS){
				aggressor = true;
			}
			
			if (aggressor && !privateBases(turn) && currentUnit.snowballs() == 0 && turn.tileAt(currentUnit).snow() == 0){
				// All bases are found. 
				Set<Tile> tilesWithSnow = retain(turn.tiles(), new TileHasSnow());
				Set<Position> positionsWithSnow = positions(tilesWithSnow); 
				Set<Position> inMoveRange = turn.actor().positionsInMoveRange(); 
				Set<Position> InRangeAndSnowy = intersect(positionsWithSnow, inMoveRange);
				goals[unit_id] = nearest(InRangeAndSnowy, currentUnit);
				return new MoveAction(goals[unit_id]);
			}
			
			//gather some snow
			if (unit_id >= 2 && turn.tileAt(currentUnit).snow() > 0)
			{
				System.out.println("gather");
				return new GatherAction();
			}
			// throw at an enemy
			if (any(currentUnit.enemyUnitsInThrowRange()) != null && currentUnit.snowballs() > 0)
			{
				System.out.println("throw");
				return new ThrowAction(any(currentUnit.enemyUnitsInThrowRange()).position());
			}
			
			// Standing on a base
			if (turn.hasBaseAt(currentUnit.position()) && !turn.baseAt(currentUnit.position()).isOwnedBy(turn.myTeam()))
			{
				return new CaptureAction();
			}
			else
			{
				goals[unit_id] = nearest(notOurBases, currentUnit).position();
			}

			// If there is an empty base, go after it.
			
			Base near;
			if (unit_id >= 2 && goals[unit_id] == currentUnit.position())
			{
				near = furthest(notOurBases, currentUnit);
				goals[unit_id] = near.position();
			}
			else if (unit_id < 2 && goals[unit_id] == currentUnit.position())
			{
				near = nearest(notOurBases, currentUnit);
				goals[unit_id] = near.position();
			}
			else if (goals[unit_id] == null)
			{
				near = nearest(notOurBases, currentUnit);
				goals[unit_id] = near.position();
			}
			
			return new MoveAction(goals[unit_id]);
		}


	}

}
