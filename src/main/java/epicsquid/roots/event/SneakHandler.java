package epicsquid.roots.event;

import epicsquid.mysticallib.network.PacketHandler;
import epicsquid.roots.Roots;
import epicsquid.roots.handler.QuiverHandler;
import epicsquid.roots.init.ModPotions;
import epicsquid.roots.item.ItemQuiver;
import epicsquid.roots.modifiers.instance.staff.ModifierSnapshot;
import epicsquid.roots.modifiers.instance.staff.StaffModifierInstanceList;
import epicsquid.roots.network.MessageServerTryPickupArrows;
import epicsquid.roots.spell.SpellExtension;
import epicsquid.roots.util.QuiverInventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@Mod.EventBusSubscriber(modid = Roots.MODID)
@SuppressWarnings("unused")
public class SneakHandler {
	private static boolean lastSneak = false;
	
	@SubscribeEvent
	public static void onPlayerVisibility(PlayerEvent.Visibility event) {
		if (event.getEntityPlayer().getActivePotionEffect(ModPotions.nondetection) != null) {
			ModifierSnapshot mods = StaffModifierInstanceList.fromSnapshot(event.getEntityPlayer().getEntityData(), SpellExtension.instance);
			if (mods.has(SpellExtension.SENSE_PLANTS)) {
				event.modifyVisibility(999);
			} else {
				event.modifyVisibility(0);
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onCheckAttack(LivingSetAttackTargetEvent event) {
		if (!(event.getTarget() instanceof EntityPlayer) || !(event.getEntityLiving() instanceof EntityMob)) {
			return;
		}
		
		EntityPlayer player = (EntityPlayer) event.getTarget();
		EntityMob attacker = (EntityMob) event.getEntityLiving();
		if (player.getActivePotionEffect(ModPotions.nondetection) != null) {
			attacker.setRevengeTarget(null);
			attacker.attackTarget = null;
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void onPlayerSneak(TickEvent.ClientTickEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		//noinspection ConstantConditions
		if (mc == null || mc.player == null) {
			return;
		}
		
		
		if (lastSneak != mc.player.isSneaking() && !lastSneak) {
			lastSneak = mc.player.isSneaking();
			
			List<EntityArrow> arrows = mc.world.getEntitiesWithinAABB(EntityArrow.class, ItemQuiver.bounding.offset(mc.player.getPosition()));
			if (arrows.isEmpty()) return;
			
			ItemStack quiver = QuiverInventoryUtil.getQuiver(mc.player);
			if (quiver.isEmpty()) return;
			
			QuiverHandler handler = QuiverHandler.getHandler(quiver);
			
			MessageServerTryPickupArrows packet = new MessageServerTryPickupArrows();
			PacketHandler.INSTANCE.sendToServer(packet);
		}
		
		lastSneak = mc.player.isSneaking();
	}
}
