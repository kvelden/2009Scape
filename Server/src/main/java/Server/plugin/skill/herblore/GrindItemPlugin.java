package plugin.skill.herblore;

import core.game.interaction.NodeUsageEvent;
import core.game.interaction.UseWithHandler;
import core.game.node.item.Item;
import core.game.world.update.flag.context.Animation;
import core.plugin.InitializablePlugin;
import core.plugin.Plugin;

/**
 * Represents the plugin used to handle the grinding of an item.
 * @author 'Vexia
 * @version 1.0
 */
@InitializablePlugin
public final class GrindItemPlugin extends UseWithHandler {

	/**
	 * Represents the animation to use.
	 */
	private static final Animation ANIMATION = new Animation(364);

	/**
	 * Constructs a new {@code GrindItemPlugin} {@code Object}.
	 */
	public GrindItemPlugin() {
		super(233);
	}

	@Override
	public Plugin<Object> newInstance(Object arg) throws Throwable {
		for (GrindingItem grind : GrindingItem.values()) {
			for (Item i : grind.getItems()) {
				addHandler(i.getId(), ITEM_TYPE, this);
			}
		}
		return this;
	}

	@Override
	public boolean handle(NodeUsageEvent event) {
		final GrindingItem grind = GrindingItem.forItem(event.getUsedItem().getId() == 233 ? event.getBaseItem() : event.getUsedItem());
		final Item item = event.getUsedItem().getId() == 233 ? event.getBaseItem() : event.getUsedItem();
		if (event.getPlayer().getInventory().remove(item)) {
			event.getPlayer().animate(ANIMATION);
			event.getPlayer().lock(3);
			event.getPlayer().getInventory().add(new Item(grind.getProduct().getId(), item.getAmount()));
			event.getPlayer().getPacketDispatch().sendMessage("You grind down the " + grind.name().toLowerCase().replace("_", " ") + ".");
		}
		return true;
	}

}
