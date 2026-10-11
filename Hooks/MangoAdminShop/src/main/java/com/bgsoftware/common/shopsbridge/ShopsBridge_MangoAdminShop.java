package com.bgsoftware.common.shopsbridge;

import com.bgsoftware.common.shopsbridge.internal.ItemStackCache;
import com.bgsoftware.common.shopsbridge.internal.PricesAccessorNoTransactions;
import com.bgsoftware.common.shopsbridge.internal.scheduler.Scheduler;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import uk.mangostudios.adminshop.AdminShopPlugin;
import uk.mangostudios.adminshop.config.impl.Category;
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
        return getPrice(findShopItem(itemStack, false), false, itemStack);
    }

    @Override
    public BigDecimal getBuyPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
        return this.getBuyPriceInternal(itemStack);
    }

    @Override
    public BigDecimal getBuyPriceInternal(ItemStack itemStack) {
        return getPrice(findShopItem(itemStack, true), true, itemStack);
    }

    @Override
    public CompletableFuture<Void> getWhenShopsLoaded() {
        return this.readyFuture;
    }

    @Override
    public BulkTransaction startBulkTransaction() {
        return new BulkTransactionImpl();
    }

    private ShopItem findShopItem(ItemStack itemStack, boolean buy) {
        ShopItem bestItem = null;
        double bestPrice = 0;
        for (Category category : this.adminShop.getCategories()) {
            for (Map.Entry<String, ShopItem> entry : category.getItems().entrySet()) {
                ShopItem shopItem = entry.getValue();
                double price = buy ? shopItem.buyPrice() : shopItem.sellPrice();
                if (!shopItem.enabled() || price == -1)
                    continue;

                ItemStack configuredItem = MaterialUtil.get(entry.getKey());
                if (!configuredItem.isSimilar(itemStack))
                    continue;

                if (bestItem == null || (buy ? price < bestPrice : price > bestPrice)) {
                    bestItem = shopItem;
                    bestPrice = price;
                }
            }
        }

        return bestItem;
    }

    private static BigDecimal getPrice(ShopItem shopItem, boolean buy, ItemStack itemStack) {
        if (shopItem == null)
            return BigDecimal.ZERO;

        double price = buy ? shopItem.buyPrice() : shopItem.sellPrice();
        return price == -1 ? BigDecimal.ZERO : BigDecimal.valueOf(price * itemStack.getAmount());
    }

    private class BulkTransactionImpl implements PricesAccessorNoTransactions, BulkTransaction {

        private final ItemStackCache<ShopItem> sellCache = new ItemStackCache<>();
        private final ItemStackCache<ShopItem> buyCache = new ItemStackCache<>();

        @Override
        public BigDecimal getSellPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
            return this.getSellPriceInternal(itemStack);
        }

        @Override
        public BigDecimal getSellPriceInternal(ItemStack itemStack) {
            return getPrice(this.sellCache.computeIfAbsent(itemStack, () -> findShopItem(itemStack, false)), false, itemStack);
        }

        @Override
        public BigDecimal getBuyPriceInternal(OfflinePlayer unused, ItemStack itemStack) {
            return this.getBuyPriceInternal(itemStack);
        }

        @Override
        public BigDecimal getBuyPriceInternal(ItemStack itemStack) {
            return getPrice(this.buyCache.computeIfAbsent(itemStack, () -> findShopItem(itemStack, true)), true, itemStack);
        }

    }

}
