package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.productprice.ChangedByDto;
import com.tamachihua_erp.api.dto.productprice.ProductPriceResponseDto;
import com.tamachihua_erp.api.entity.ProductPrice;
import org.springframework.stereotype.Component;

@Component
public class ProductPriceMapper {

    public ProductPriceResponseDto toResponseDto(ProductPrice productPrice) {

        return new ProductPriceResponseDto(
                productPrice.getId(),
                productPrice.getPrice(),
                productPrice.getStartDate(),
                productPrice.getEndDate(),
                new ChangedByDto(
                        productPrice.getUser().getId(),
                        productPrice.getUser().getFullName()
                )
        );
    }
}
