package com.shreedatta.BillingSoftware.Mapper.item;

import com.shreedatta.BillingSoftware.dto.item.ItemResponse;
import com.shreedatta.BillingSoftware.Entity.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getSku(),
                item.getUnit(),
                item.getPrice(),
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}