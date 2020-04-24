package epicsquid.roots.library;

import epicsquid.roots.Roots;
import epicsquid.roots.init.ModItems;
import epicsquid.roots.spell.SpellBase;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: Not actually a capability
public class StaffSpellStorage implements INBTSerializable<NBTTagCompound> {
  private static int MAX_SPELL_SLOT = 5;
  private static int MIN_SPELL_SLOT = 1;

  private Int2ObjectOpenHashMap<StaffSpellInfo> spells = new Int2ObjectOpenHashMap<>();
  private int selectedSlot = 0;
  private ItemStack stack;

  public StaffSpellStorage(ItemStack stack) {
    this.stack = stack;
  }

  public boolean hasSpellInSlot() {
    return spells.get(this.selectedSlot) != null;
  }

  public boolean isEmpty() {
    for (StaffSpellInfo v : this.spells.values()) {
      if (v != null) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public StaffSpellInfo getSpellInSlot(int slot) {
    if (slot < MIN_SPELL_SLOT || slot > MAX_SPELL_SLOT) {
      throw new IllegalStateException("Tried to get spell for invalid slot " + slot);
    }
    return spells.get(slot-1);
  }

  public int getCooldownLeft() {
    StaffSpellInfo info = getSelectedInfo();
    if (info == null) {
      return -1;
    }
    return info.cooldownLeft();
  }

  public int getCooldown () {
    StaffSpellInfo info = getSelectedInfo();
    if (info == null) {
      return -1;
    }
    return info.cooldown();
  }

  // TODO: Is this used? Is it used usefully?
  public void setCooldown() {
    StaffSpellInfo info = getSelectedInfo();
    if (info != null) {
      info.use();
    }
    saveToStack();
  }

  @Nullable
  public StaffSpellInfo getSelectedInfo() {
    return spells.get(this.selectedSlot);
  }

  @SideOnly(Side.CLIENT)
  public String formatSelectedSpell() {
    StaffSpellInfo info = spells.get(this.selectedSlot);
    if (info == null) {
      return "";
    }

    SpellBase spell = info.getSpell();
    if (spell == null) {
      return "";
    }
    return "(" + spell.getTextColor() + TextFormatting.BOLD + I18n.format("roots.spell." + spell.getName() + ".name") + TextFormatting.RESET + ")";
  }

  public void clearSelectedSlot() {
    spells.put(this.selectedSlot, null);
    saveToStack();
  }

  public int getSelectedSlot() {
    return this.selectedSlot;
  }

  public void setSelectedSlot(int slot) {
    this.selectedSlot = slot;
    saveToStack();
  }

  public void previousSlot() {
    if (this.isEmpty()) {
      setSelectedSlot(0);
      return;
    }

    int originalSlot = selectedSlot;

    for (int i = selectedSlot - 1; i >= 0; i--) {
      if (spells.get(i) != null) {
        setSelectedSlot(i);
        return;
      }
    }
    for (int i = 5; i >= originalSlot; i--) {
      if (spells.get(i) != null) {
        setSelectedSlot(i);
        return;
      }
    }

    setSelectedSlot(originalSlot);
  }


  public void nextSlot() {
    if (this.isEmpty()) {
      setSelectedSlot(0);
      return;
    }

    int originalSlot = selectedSlot;

    for (int i = selectedSlot + 1; i < 5; i++) {
      if (spells.get(i) != null) {
        setSelectedSlot(i);
        return;
      }
    }
    for (int i = 0; i < originalSlot; i++) {
      if (spells.get(i) != null) {
        setSelectedSlot(i);
        return;
      }
    }

    setSelectedSlot(originalSlot);
  }

  public void setSpellToSlot(StaffSpellInfo spell) {
    if (hasFreeSlot()) {
      setSelectedSlot(getNextFreeSlot());
      this.spells.put(this.selectedSlot, spell);
      saveToStack();
    }
  }

  public int getNextFreeSlot() {
    for (int i = 0; i < 5; i++) {
      if (spells.getOrDefault(i, null) == null) {
        return i;
      }
    }
    return -1;
  }

  public boolean hasFreeSlot() {
    return getNextFreeSlot() != -1;
  }

  @Override
  public NBTTagCompound serializeNBT() {
    NBTTagCompound compound = new NBTTagCompound();
    NBTTagList spells = new NBTTagList();
    for (Int2ObjectMap.Entry<StaffSpellInfo> entry : this.spells.int2ObjectEntrySet()) {
      spells.appendTag((entry.getValue() == null) ? new NBTTagCompound() : entry.getValue().serializeNBT());
    }
    compound.setTag("spells", spells);

    compound.setInteger("selectedSlot", this.selectedSlot);
    return compound;
  }

  @Override
  public void deserializeNBT(NBTTagCompound tag) {
    if (tag.hasKey("spells", Constants.NBT.TAG_LIST)) {
      NBTTagList spells = tag.getTagList("spells", Constants.NBT.TAG_COMPOUND);
      if (spells.tagCount() > MAX_SPELL_SLOT) {
        Roots.logger.error("Invalid spell when deserializing storage: spells list is " + spells.tagCount() + " which is greater than MAX_SPELL_SLOT " + MAX_SPELL_SLOT + ": " + tag.toString());
      }
      for (int i = 0; i < spells.tagCount(); i++) {
        int slot = i + 1;
        this.spells.put(slot, StaffSpellInfo.fromNBT(spells.getCompoundTagAt(i)));
      }
    } else {
      for (int i = 0; i < 5; i++) {
        if (tag.hasKey("spell_" + i)) {
          spells.put(i, StaffSpellInfo.fromRegistry(tag.getString("spell_" + i)));
        }
      }
    }

    this.selectedSlot = tag.getInteger("selectedSlot");
  }

  @Nonnull
  public static StaffSpellStorage fromStack(ItemStack stack) {
    StaffSpellStorage result = new StaffSpellStorage(stack);
    NBTTagCompound tag = stack.getTagCompound();
    if (tag != null && tag.hasKey("spell_holder")) {
      result.deserializeNBT(tag.getCompoundTag("spell_holder"));
    }
    return result;
  }

  public void saveToStack() {
    boolean correct = stack.getItem() == ModItems.spell_dust || stack.getItem() == ModItems.staff;
    assert correct;

    NBTTagCompound tag = stack.getTagCompound();
    if (tag == null) {
      tag = new NBTTagCompound();
      stack.setTagCompound(tag);
    }
    tag.setTag("spell_holder", this.serializeNBT());
  }
}
