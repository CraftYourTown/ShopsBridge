package com.bgsoftware.common.shopsbridge;

import com.bgsoftware.common.shopsbridge.internal.ItemStackCache;
import com.bgsoftware.common.shopsbridge.internal.PricesAccessorNoTransactions;
import com.bgsoftware.common.shopsbridge.internal.scheduler.Scheduler;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import uk.mangostudios.adminshop.AdminShopPlugin;
import uk.mangostudios.adminshop.config.impl.ShopItem;
import uk.mangostudios.adminshop.util.MaterialUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopsBridge_MangoAdminShop implements PricesAccessorNoTransactions, IShopsBridge {

    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();
    private final AdminShopPlugin adminShop;

    public ShopsBridge_MangoAdminShop(Plugin plugin) {
        this.adminShop = AdminShopPlugin.get();
        Scheduler.runTaskLater(plugin, () -> this.readyFuture.complete(null), 1L);
    }

    @Override
    public BigDecimal getSellPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
        return this.getSellPriceInternal(itemStack);
    }

    @Override
    public BigDecimal getSellPriceInternal(ItemStack itemStack) {
        return getPrice(findShopItem(itemStack), false, itemStack);
    }

    @Override
    public BigDecimal getBuyPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
        return this.getBuyPriceInternal(itemStack);
    }

    @Override
    public BigDecimal getBuyPriceInternal(ItemStack itemStack) {
        return getPrice(findShopItem(itemStack), true, itemStack);
    }

    @Override
    public CompletableFuture<Void> getWhenShopsLoaded() {
        return this.readyFuture;
    }

    @Override
    public BulkTransaction startBulkTransaction() {
        return new BulkTransactionImpl();
    }

    private ShopItem findShopItem(ItemStack itemStack) {
        for (Map.Entry<String, ShopItem> entry : this.adminShop.getAllItems().entrySet()) {
            ShopItem shopItem = entry.getValue();
            if (!shopItem.enabled())
                continue;

            ItemStack configuredItem = MaterialUtil.get(entry.getKey());
            if (configuredItem.getType() != itemStack.getType())
                continue;

            if (configuredItem.isSimilar(itemStack))
                return shopItem;
        }

        return null;
    }

    private static BigDecimal getPrice(ShopItem shopItem, boolean buy, ItemStack itemStack) {
        if (shopItem == null)
            return BigDecimal.ZERO;

        double price = buy ? shopItem.buyPrice() : shopItem.sellPrice();
        return price == -1 ? BigDecimal.ZERO : BigDecimal.valueOf(price * itemStack.getAmount());
    }

    private class BulkTransactionImpl implements PricesAccessorNoTransactions, BulkTransaction {

        private final ItemStackCache<ShopItem> cache = new ItemStackCache<>();

        @Override
        public BigDecimal getSellPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
            return this.getSellPriceInternal(itemStack);
        }

        @Override
        public BigDecimal getSellPriceInternal(ItemStack itemStack) {
            return getPrice(this.cache.computeIfAbsent(itemStack, () -> findShopItem(itemStack)), false, itemStack);
        }

        @Override
        public BigDecimal getBuyPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
            return this.getBuyPriceInternal(itemStack);
        }

        @Override
        public BigDecimal getBuyPriceInternal(ItemStack itemStack) {
            return getPrice(this.cache.computeIfAbsent(itemStack, () -> findShopItem(itemStack)), true, itemStack);
        }

    }

}
