package com.shreedatta.BillingSoftware.Service;

import com.shreedatta.BillingSoftware.dto.item.ItemCreateRequest;
import com.shreedatta.BillingSoftware.dto.item.ItemResponse;
import com.shreedatta.BillingSoftware.dto.item.ItemUpdateRequest;
import com.shreedatta.BillingSoftware.Entity.Item;
import com.shreedatta.BillingSoftware.Entity.Shop;
import com.shreedatta.BillingSoftware.Mapper.item.ItemMapper;
import com.shreedatta.BillingSoftware.Repository.ItemRepository;
import com.shreedatta.BillingSoftware.Repository.ShopRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ItemService {

    private static final int SEARCH_LIMIT = 20;

    private final ItemRepository itemRepository;
    private final ShopRepository shopRepository;
    private final ItemMapper itemMapper;

    public ItemService(
            ItemRepository itemRepository,
            ShopRepository shopRepository,
            ItemMapper itemMapper
    ) {
        this.itemRepository = itemRepository;
        this.shopRepository = shopRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public ItemResponse createItem(
            UUID shopId,
            ItemCreateRequest request
    ) {

        Shop shop = getActiveShop(shopId);

        String name = normalizeRequired(request.name(), "Item name");
        String description = normalizeOptional(request.description());
        String sku = normalizeOptional(request.sku());
        String unit = normalizeRequired(request.unit(), "Unit");
        BigDecimal price = normalizePrice(request.price());

        if (sku != null && itemRepository.existsByShopIdAndSku(shopId, sku)) {
            throw new IllegalArgumentException(
                    "An item with this SKU already exists"
            );
        }

        Item item = new Item(
                shop,
                name,
                description,
                sku,
                unit,
                price
        );

        try {
            item = itemRepository.save(item);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Unable to create item. The SKU may already exist.",
                    exception
            );
        }

        return itemMapper.toResponse(item);
    }

    @Transactional
    public int increaseAllItemPrices(UUID shopId, BigDecimal percentage) {
        getActiveShop(shopId);

        if (percentage == null
                || percentage.compareTo(BigDecimal.ZERO) <= 0
                || percentage.compareTo(new BigDecimal("1000")) > 0) {
            throw new IllegalArgumentException(
                    "Percentage must be greater than 0 and at most 1000"
            );
        }

        return itemRepository.increasePricesForShop(shopId, percentage);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems(UUID shopId) {

        getActiveShop(shopId);

        return itemRepository
                .findByShopIdAndActiveTrueOrderByNameAsc(shopId)
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> searchItems(
            UUID shopId,
            String query
    ) {

        getActiveShop(shopId);

        String searchQuery = normalizeRequired(query, "Search query");

        Pageable pageable = PageRequest.of(0, SEARCH_LIMIT);

        return itemRepository
                .findByShopIdAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                        shopId,
                        searchQuery,
                        pageable
                )
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(
            UUID shopId,
            UUID itemId
    ) {

        getActiveShop(shopId);

        Item item = itemRepository
                .findByIdAndShopId(itemId, shopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"
                        )
                );

        return itemMapper.toResponse(item);
    }

    @Transactional
    public ItemResponse updateItem(
            UUID shopId,
            UUID itemId,
            ItemUpdateRequest request
    ) {

        getActiveShop(shopId);

        Item item = itemRepository
                .findByIdAndShopId(itemId, shopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"
                        )
                );

        String name = normalizeRequired(request.name(), "Item name");
        String description = normalizeOptional(request.description());
        String sku = normalizeOptional(request.sku());
        String unit = normalizeRequired(request.unit(), "Unit");
        BigDecimal price = normalizePrice(request.price());

        if (
                sku != null
                        && itemRepository.existsByShopIdAndSkuAndIdNot(
                        shopId,
                        sku,
                        itemId
                )
        ) {
            throw new IllegalArgumentException(
                    "An item with this SKU already exists"
            );
        }

        item.updateDetails(
                name,
                description,
                sku,
                unit,
                price
        );

        try {
            item = itemRepository.save(item);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Unable to update item. The SKU may already exist.",
                    exception
            );
        }

        return itemMapper.toResponse(item);
    }

    @Transactional
    public void deactivateItem(
            UUID shopId,
            UUID itemId
    ) {

        getActiveShop(shopId);

        Item item = itemRepository
                .findByIdAndShopId(itemId, shopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"
                        )
                );

        item.deactivate();
    }

    @Transactional
    public void activateItem(
            UUID shopId,
            UUID itemId
    ) {

        getActiveShop(shopId);

        Item item = itemRepository
                .findByIdAndShopId(itemId, shopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"
                        )
                );

        item.activate();
    }

    private Shop getActiveShop(UUID shopId) {

        Shop shop = shopRepository
                .findById(shopId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Shop not found"
                        )
                );

        if (!shop.isActive()) {
            throw new IllegalArgumentException(
                    "Shop account is inactive"
            );
        }

        return shop;
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal normalizePrice(BigDecimal price) {

        if (price == null) {
            throw new IllegalArgumentException(
                    "Price is required"
            );
        }

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Price cannot be negative"
            );
        }

        return price.setScale(2, RoundingMode.UNNECESSARY);
    }
}