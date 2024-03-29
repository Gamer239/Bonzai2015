package competitor;

import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import snowbound.api.*;
import snowbound.api.util.*;
import static snowbound.api.util.Utility.*;

@Agent(name = "Tundra Wookie Slayers")
public class CompetitorAI extends AI{

	Set<Position> spawns = null;
	Set<Base> bases = null;
	Position[] goals = null;
	boolean[] alive = null;
	int numUnitsPerTeam = 0;
	Color color = null;

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

	private boolean canCaptureBaseWithoutStalement(Turn turn, Unit currentUnit){

		Base base = turn.baseAt(currentUnit);

		Set<Position> tilesForBase = base.coverage();

		HashMap<Team, Integer> numPerBase = new HashMap<Team, Integer>();

		for (Position p: tilesForBase){
			if (turn.hasUnitAt(p)){
				Unit unitAtP = turn.unitAt(p);

				if (!numPerBase.containsKey(unitAtP.team())){
					numPerBase.put(unitAtP.team(), 1);
				}else{
					numPerBase.put(unitAtP.team(), numPerBase.get(unitAtP.team()) + 1);
				}
			}
		}

		int ourTeam = numPerBase.get(turn.myTeam());

		for (Team t: numPerBase.keySet()){
			if (t == turn.myTeam())
				continue;
			else{
				if (numPerBase.get(t) >= ourTeam){
					System.out.println("We cant do that! *Clap clap clap clap clap*");
					return false;
				}
			}
		}

		System.out.println("CAPTURE THAT BASE");
		return true;
	}

	private Base getBestBase(Turn turn, Unit currentUnit, boolean aggressive){

		Set<Base> bases = difference(turn.allBases(), turn.myBases());

		int maxNumEnemies = 1000000000;
		Set<Base> bestBase = new HashSet<Base>();

		baseCheck:
			for (Base base: bases){

				for (int i = 0; i< goals.length; i ++){
					if (goals[i] == null){
						continue;
					}else if (goals[i] == base.position()){
						continue baseCheck;
					}else{
						continue;
					}
				}

				Set<Tile> allTiles = turn.tiles();

				int numEnemiesAtThisBase = 0;

				for (Tile t: allTiles){

					// TODO Tweak here as needed
					if (t.position().distance(base.position()) <=4){
						// Acceptable distance to check 
						if (turn.hasUnitAt(t)){
							// Unit at t.
							Unit unit = turn.unitAt(t);

							if (unit.team() != turn.myTeam()){
								numEnemiesAtThisBase += 1;
							}
						}
					}
				}

				if (numEnemiesAtThisBase < maxNumEnemies){
					bestBase.clear();
					bestBase.add(base);
					maxNumEnemies = numEnemiesAtThisBase;
				}else if (numEnemiesAtThisBase == maxNumEnemies){
					bestBase.add(base);
				}
			}

		int randomBase = (int)(Math.random() * bestBase.size());
		int numBasesLookedAt = 0;

		for (Base base: bestBase){
			if (numBasesLookedAt == randomBase){
				return base;
			}else{
				numBasesLookedAt ++;
			}
		}


		return any(difference(turn.allBases(), turn.myBases()));
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
			alive = new boolean[numUnitsPerTeam];
			targetBases = turn.allBases();
			targetBases.clear();
			color = turn.myTeam().color();

		}
		
		int x = 0;
		for (Unit u : turn.myUnits())
		{
			alive[x] = u.isSpawned();
			x++;
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
			Base target = any(turn.allBases());
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
					if (difference(notOurBases, targetBases).size() == 0){
						continue;
					}
					
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
			
			if ( turn.hasBaseAt(currentUnit.position()) && canCaptureBaseWithoutStalement(turn, currentUnit) == false )
			{
				boolean flag = true;
				int j = 0;
				while(flag && j < 50)
				{
					goals[unit_id] = getBestBase(turn, currentUnit, aggressor).position();
					flag = false;
					for ( int i = 0; i < numUnitsPerTeam; i++ )
					{
						if ( goals[i] == goals[unit_id] && alive[i] == true)
						{
							flag = true;
						}
					}
					j++;
					if (j > 5)
					{
						goals[unit_id] = any(notOurBases).position();
					}
				}
			}

			// Standing on a base
			if (turn.hasBaseAt(currentUnit.position()) && !turn.baseAt(currentUnit.position()).isOwnedBy(turn.myTeam()))
			{
				return new CaptureAction();
			}

			// If there is a nearby base that is capturable, go get it.
			if (aggressor && !privateBases(turn) && currentUnit.snowballs() == 0 && turn.tileAt(currentUnit).snow() == 0  &&currentUnit.position().distance(goals[unit_id]) > 7){
				// All bases are found. 
				Set<Tile> tilesWithSnow = retain(turn.tiles(), new TileHasSnow());
				Set<Position> positionsWithSnow = positions(tilesWithSnow); 
				Set<Position> inMoveRange = turn.actor().positionsInMoveRange(); 
				Set<Position> InRangeAndSnowy = intersect(positionsWithSnow, inMoveRange);
				if (InRangeAndSnowy.size() > 0){
					// May be cases where you arent in range of snow
					goals[unit_id] = nearest(InRangeAndSnowy, currentUnit);
					return new MoveAction(goals[unit_id]);
				}
			}


			//gather some snow
			if (/*unit_id >= 2 &&*/ turn.tileAt(currentUnit).snow() > 0 && currentUnit.position().distance(goals[unit_id]) > 4)
			{
				System.out.println("gather");
				return new GatherAction();
			}
			// throw at an enemy
			if (any(currentUnit.enemyUnitsInThrowRange()) != null && currentUnit.snowballs() > 0)
			{
				System.out.println("throw");

				int lowestHealth = 3;
				Unit targetUnit = any(currentUnit.enemyUnitsInThrowRange());
				for(Unit enemyUnit: currentUnit.enemyUnitsInThrowRange()){
					if (enemyUnit.warmth() <= lowestHealth){
						if (turn.hasBaseAt(enemyUnit)){
							targetUnit = enemyUnit;
							lowestHealth = targetUnit.warmth();
							System.out.println("Throw at unit on base");
						}
					}else if (enemyUnit.warmth() < lowestHealth){
						targetUnit = enemyUnit;
						lowestHealth = targetUnit.warmth();
						System.out.println("Lower health unit found!  EXTERMINATE!");
					}
				}
				return new ThrowAction(targetUnit.position());
			}




			// If there is an empty base, go after it.

			Base near;
			// group up?
			if (unit_id >= 2 && goals[unit_id] == currentUnit.position())
			{
				near = furthest(notOurBases, currentUnit);
				goals[unit_id] = near.position();
			}
			// go to a new place
			else if (unit_id < 2 && goals[unit_id] == currentUnit.position())
			{
				boolean flag = true;
				int j = 0;
				while(flag && j < 50)
				{
					goals[unit_id] = getBestBase(turn, currentUnit, aggressor).position();
					flag = false;
					for ( int i = 0; i < numUnitsPerTeam; i++ )
					{
						if ( goals[i] == goals[unit_id] && alive[i] == true)
						{
							flag = true;
						}
					}
					j++;
					if (j > 5)
					{
						goals[unit_id] = any(notOurBases).position();
					}
				}

			}
			// go find a base that no one else is using
			else if (goals[unit_id] == null || currentUnit.isSpawned() == false)
			{
				boolean flag = true;
				int j = 0;
				while(flag && j < 50)
				{
					goals[unit_id] = getBestBase(turn, currentUnit, aggressor).position();
					flag = false;
					for ( int i = 0; i < numUnitsPerTeam; i++ )
					{
						if ( goals[i] == goals[unit_id] && alive[i] == true)
						{
							flag = true;
						}
					}
					j++;
					if (j > 5)
					{
						goals[unit_id] = any(notOurBases).position();
					}
				}
				if (goals[unit_id] == null)
				{
					goals[unit_id] = any(turn.allBases()).position();
				}
			}


			//check if the base that your going for is taken already
			if (turn.hasBaseAt(goals[unit_id]) == true )
			{
				if (turn.hasBaseAt(goals[unit_id]) == true && turn.baseAt(goals[unit_id]).isOwnedBy(turn.myTeam()))
				{
					goals[unit_id] = nearest(notOurBases, currentUnit.position()).position();
				}
				Set<Position> enemyPositions = turn.baseAt(goals[unit_id]).coverage();
				enemyPositions.removeAll(enemyPositions);
				for ( Unit e : turn.myUnits() )
				{
					enemyPositions.add(e.position());
				}
				Set<Position> res = union(turn.baseAt(goals[unit_id]).coverage(), enemyPositions);
	
				if (res.size() != turn.baseAt(goals[unit_id]).coverage().size())
				{
					System.out.println("size on target base " + res.size());
					boolean flag = true;
					int j = 0;
					while(flag && j < 50)
					{
						goals[unit_id] = getBestBase(turn, currentUnit, aggressor).position();
						flag = false;
						for ( int i = 0; i < numUnitsPerTeam; i++ )
						{
							if ( goals[i] == goals[unit_id] && alive[i] == true)
							{
								flag = true;
							}
						}
						j++;
						if (j > 5)
						{
							goals[unit_id] = any(notOurBases).position();
						}
					}
				}
			}



			if (currentUnit.position().distance(nearest(notOurBases, currentUnit.position()).position()) <= 4)
			{
				boolean flag = false;
				for ( int i = 0; i < numUnitsPerTeam; i++ )
				{
					if ( goals[i] == nearest(notOurBases, currentUnit.position()).position())
					{
						flag = true;
					}
				}
				if (flag == false)
				{
					goals[unit_id] = nearest(notOurBases, currentUnit.position()).position();
				}
			}

			

			//check if there is a teammate on a base

			return new MoveAction(goals[unit_id]);
		}


	}

}
