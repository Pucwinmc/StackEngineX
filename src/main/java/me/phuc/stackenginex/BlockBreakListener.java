@EventHandler
public void onBreak(BlockBreakEvent event) {

    if (event.isCancelled()) return;

    Player player = event.getPlayer();
    ItemStack tool = player.getInventory().getItemInMainHand();

    if (tool == null || tool.getType().isAir()) return;

    Collection<ItemStack> drops = event.getBlock().getDrops(tool);

    if (drops.isEmpty()) return;

    event.setDropItems(false); // Ngăn drop mặc định

    for (ItemStack drop : drops) {

        HashMap<Integer, ItemStack> leftover =
                player.getInventory().addItem(drop);

        // Nếu inventory full -> thả phần dư xuống đất
        if (!leftover.isEmpty()) {
            for (ItemStack remain : leftover.values()) {
                player.getWorld().dropItemNaturally(
                        event.getBlock().getLocation(),
                        remain
                );
            }
        }
    }
}
