package core.game.interaction;

import core.game.node.Node;
import core.game.node.entity.Entity;
import core.game.node.entity.npc.NPC;
import core.game.node.entity.player.Player;
import core.game.node.entity.player.link.diary.DiaryType;
import core.game.system.task.Pulse;
import core.game.world.map.Direction;
import core.game.world.map.Location;
import core.game.world.map.Point;
import core.game.world.map.path.Path;
import core.game.world.map.path.Pathfinder;
import core.net.packet.PacketRepository;
import core.net.packet.context.PlayerContext;
import core.net.packet.out.ClearMinimapFlag;
import core.tools.Vector3d;

import java.util.Deque;

/**
 * Handles a movement task.
 * @author Emperor
 */
public abstract class MovementPulse extends Pulse {

	/**
	 * The moving entity.
	 */
	protected Entity mover;

	/**
	 * The destination node.
	 */
	protected Node destination;

	/**
	 * The destination's last location.
	 */
	private Location last;

	/**
	 * The pathfinder.
	 */
	private Pathfinder pathfinder;

	/**
	 * If running should be forced.
	 */
	private boolean forceRun;

	/**
	 * The option handler.
	 */
	private OptionHandler optionHandler;

	/**
	 * The use with handler.
	 */
	private UseWithHandler useHandler;

	/**
	 * The destination flag.
	 */
	private DestinationFlag destinationFlag;

	/**
	 * The location to interact from.
	 */
	private Location interactLocation;

	/**
	 * If the path couldn't be fully found.
	 */
	private boolean near;

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 */
	public MovementPulse(Entity mover, Node destination) {
		this(mover, destination, null, false);
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param forceRun If the entity is forced to run.
	 */
	public MovementPulse(Entity mover, Node destination, boolean forceRun) {
		this(mover, destination, null, forceRun);

		// seers diary statue walk coords, from north west clockwise
		// TODO this kinda sucks, but it does work
		// 2739, 3492
		// 2742, 3492
		// 2742, 3489
		// 2739, 3489
		if (mover instanceof Player) {
			if (!forceRun
				&& mover.getLocation().isInside(Location.create(2739,3492), Location.create(2742,3489))
				&& destination.getLocation().isInside(Location.create(2739,3492), Location.create(2742,3489))) { // can only do seers task walking, also should cut down on processing done in all these pulses
				double origin_x = 2740.5;
				double origin_y = 3490.5;
				Vector3d origin = new Vector3d(origin_x, origin_y, 0);
				if (mover.asPlayer().getAttribute("diary:seers:statue-walk-start") != null) {
					Vector3d start = mover.asPlayer().getAttribute("diary:seers:statue-walk-start");
					Vector3d a = mover.asPlayer().getAttribute("diary:seers:statue-walk-a");
					Vector3d b = new Vector3d(destination.getLocation()).sub(origin);
					Vector3d c = new Vector3d(mover.getLocation()).sub(origin);
					Vector3d n = new Vector3d(0, 0, 1);

					double angle_a_b = Vector3d.signedAngle(a, b, n) * 360. / 2 / 3.1415926535;

					if (!mover.asPlayer().getWalkingQueue().isMoving()) {
						//System.out.println("removing, not moving");
						mover.asPlayer().removeAttribute("diary:seers:statue-walk-start");
					}
					if (angle_a_b >= 0) {
						//System.out.println("removing, not going clockwise");
						mover.asPlayer().removeAttribute("diary:seers:statue-walk-start");
					}
					if (c.epsilonEquals(start, .001)) {
						mover.asPlayer().getAchievementDiaryManager().finishTask(mover.asPlayer(), DiaryType.SEERS_VILLAGE, 0, 1);
						//System.out.println("removing, finished task");
						mover.asPlayer().removeAttribute("diary:seers:statue-walk-start");
					}

					mover.asPlayer().setAttribute("diary:seers:statue-walk-a", b);
				} else {
					//System.out.println("started");
					Vector3d start = new Vector3d(mover.getLocation()).sub(origin);
					Vector3d dest = new Vector3d(destination.getLocation()).sub(origin);
					mover.asPlayer().setAttribute("diary:seers:statue-walk-start", start);
					mover.asPlayer().setAttribute("diary:seers:statue-walk-a", dest);
				}
			} else {
				//System.out.println("removing, running or outside");
				mover.asPlayer().removeAttribute("diary:seers:statue-walk-start");
			}
		}

		// Enter the courtyard of the spooky mansion in Draynor Village
		if (mover.getLocation().isInside(Location.create(3100,3333,0), Location.create(3114,3346,0))) {
			mover.asPlayer().getAchievementDiaryManager().finishTask(mover.asPlayer(), DiaryType.LUMBRIDGE, 0, 8);
		}

		// Visit the Draynor Village market
		if (mover.getLocation().isInside(Location.create(3086,3255,0), Location.create(3074,3245,0))) {
			mover.asPlayer().getAchievementDiaryManager().finishTask(mover.asPlayer(), DiaryType.LUMBRIDGE, 0, 9);
		}

		// Visit Fred the Farmer's chicken and sheep farm
		if (mover.getLocation().isInside(Location.create(3188,3275,0), Location.create(3192,3270,0))) {
			mover.asPlayer().getAchievementDiaryManager().finishTask(mover.asPlayer(), DiaryType.LUMBRIDGE, 0, 19);
		}
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param pathfinder The pathfinder to use.
	 */
	public MovementPulse(Entity mover, Node destination, Pathfinder pathfinder) {
		this(mover, destination, pathfinder, false);
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param optionHandler The option handler used.
	 */
	public MovementPulse(Entity mover, Node destination, OptionHandler optionHandler) {
		this(mover, destination, null, false);
		this.optionHandler = optionHandler;
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param useHandler The use with handler used.
	 */
	public MovementPulse(Entity mover, Node destination, UseWithHandler useHandler) {
		this(mover, destination, null, false);
		this.useHandler = useHandler;
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param destinationFlag The destination flag.
	 */
	public MovementPulse(Entity mover, Node destination, DestinationFlag destinationFlag) {
		this(mover, destination, null, false);
		this.destinationFlag = destinationFlag;
	}

	/**
	 * Constructs a new {@code MovementPulse} {@code Object}.
	 * @param mover The moving entity.
	 * @param destination The destination node.
	 * @param pathfinder The pathfinder to use.
	 * @param forceRun If the entity is forced to run.
	 */
	public MovementPulse(Entity mover, Node destination, Pathfinder pathfinder, boolean forceRun) {
		super(1, mover, destination);
		this.mover = mover;
		this.destination = destination;
		if (pathfinder == null) {
			if (mover instanceof Player) {
				this.pathfinder = Pathfinder.SMART;
			} else {
				this.pathfinder = Pathfinder.DUMB;
			}
		} else {
			this.pathfinder = pathfinder;
		}
		this.forceRun = forceRun;
	}

	@Override
	public boolean update() {
		mover.face(null);
		if (mover == null || destination == null || mover.getViewport().getRegion() == null) {
			return false;
		}

		if (hasInactiveNode() || !mover.getViewport().getRegion().isActive()) {
			stop();
			return true;
		}
		if (!isRunning()) {
			return true;
		}
		findPath();
		if (mover.getLocation().equals(interactLocation)) {
			if (near || pulse()) {
				if (mover instanceof Player) {
					if (near) {
						((Player) mover).getPacketDispatch().sendMessage("I can't reach that.");
					}
					PacketRepository.send(ClearMinimapFlag.class, new PlayerContext((Player) mover));
				}
				stop();
				return true;
			}
		}
		return false;
	}

	@Override
	public void stop() {
		super.stop();
		if (destination instanceof Entity) {
			mover.face(null);
		}
		last = null;
	}

	/**
	 * Finds a path to the destination, if necessary.
	 */
	public void findPath() {
		if (mover instanceof NPC && mover.asNpc().isNeverWalks()) {
			return;
		}
		boolean inside = isInsideEntity(mover.getLocation());
		if (last != null && last.equals(destination.getLocation()) && !inside) {
			return;
		}
		Location loc = null;
		if (destinationFlag != null) {
			loc = destinationFlag.getDestination(mover, destination);
		}
		if (loc == null && optionHandler != null) {
			loc = optionHandler.getDestination(mover, destination);
		}
		if (loc == null && useHandler != null) {
			loc = useHandler.getDestination((Player) mover, destination);
		}
		if (loc == null && inside) {
			loc = findBorderLocation();
		}
		if(destination == null){
			return;
		}
		Path path = Pathfinder.find(mover, loc != null ? loc : destination, true, pathfinder);
		near = !path.isSuccessful() || path.isMoveNear();
		interactLocation = mover.getLocation();
		if (!path.getPoints().isEmpty()) {
			Point point = path.getPoints().getLast();
			interactLocation = Location.create(point.getX(), point.getY(), mover.getLocation().getZ());
			if (forceRun) {
				mover.getWalkingQueue().reset(forceRun);
			} else {
				mover.getWalkingQueue().reset();
			}
			int size = path.getPoints().toArray().length;
			Deque points = path.getPoints();
			for(int i = 0; i < size; i++) {
				point = path.getPoints().pop();
				mover.getWalkingQueue().addPath(point.getX(), point.getY());
				if (destination instanceof Entity) {
					mover.face((Entity) destination);
				} else {
					mover.face(null);
				}
			}
		}
		last = destination.getLocation();
	}

	/**
	 * Finds the closest location next to the node.
	 * @return The location to walk to.
	 */
	private Location findBorderLocation() {
		int size = destination.size();
		Location centerDest = destination.getLocation().transform(size >> 1, size >> 1, 0);
		Location center = mover.getLocation().transform(mover.size() >> 1, mover.size() >> 1, 0);
		Direction direction = Direction.getLogicalDirection(centerDest, center);
		Location delta = Location.getDelta(destination.getLocation(), mover.getLocation());
		main: for (int i = 0; i < 4; i++) {
			int amount = 0;
			switch (direction) {
			case NORTH:
				amount = size - delta.getY();
				break;
			case EAST:
				amount = size - delta.getX();
				break;
			case SOUTH:
				amount = mover.size() + delta.getY();
				break;
			case WEST:
				amount = mover.size() + delta.getX();
				break;
			default:
				return null;
			}
			for (int j = 0; j < amount; j++) {
				for (int s = 0; s < mover.size(); s++) {
					switch (direction) {
					case NORTH:
						if (!direction.canMove(mover.getLocation().transform(s, j + mover.size(), 0))) {
							direction = Direction.get((direction.toInteger() + 1) & 3);
							continue main;
						}
						break;
					case EAST:
						if (!direction.canMove(mover.getLocation().transform(j + mover.size(), s, 0))) {
							direction = Direction.get((direction.toInteger() + 1) & 3);
							continue main;
						}
						break;
					case SOUTH:
						if (!direction.canMove(mover.getLocation().transform(s, -(j + 1), 0))) {
							direction = Direction.get((direction.toInteger() + 1) & 3);
							continue main;
						}
						break;
					case WEST:
						if (!direction.canMove(mover.getLocation().transform(-(j + 1), s, 0))) {
							direction = Direction.get((direction.toInteger() + 1) & 3);
							continue main;
						}
						break;
					default:
						return null;
					}
				}
			}
			Location location = mover.getLocation().transform(direction, amount);
			return location;
		}
		return null;
	}

	/**
	 * Checks if the mover is standing on an invalid position.
	 * @param l The location.
	 * @return {@code True} if so.
	 */
	private boolean isInsideEntity(Location l) {
		if (!(destination instanceof Entity)) {
			return false;
		}
		if (((Entity) destination).getWalkingQueue().isMoving()) {
			return false;
		}
		Location loc = destination.getLocation();
		int size = destination.size();
		return Pathfinder.isStandingIn(l.getX(), l.getY(), mover.size(), mover.size(), loc.getX(), loc.getY(), size, size);
	}

	/**
	 * Gets the forceRun.
	 * @return The forceRun.
	 */
	public boolean isForceRun() {
		return forceRun;
	}

	/**
	 * Sets the forceRun.
	 * @param forceRun The forceRun to set.
	 */
	public void setForceRun(boolean forceRun) {
		this.forceRun = forceRun;
	}

	/**
	 * Sets the current destination.
	 * @param destination The destination.
	 */
	public void setDestination(Node destination) {
		this.destination = destination;
	}

	/**
	 * Sets the last location.
	 * @param last The last location.
	 */
	public void setLast(Location last) {
		this.last = last;
	}

}