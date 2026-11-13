package com.tamachihua_erp.api.mapper;

import com.tamachihua_erp.api.dto.presentation.PresentationSummaryDto;
import com.tamachihua_erp.api.dto.product.StockByPresentationDto;
import com.tamachihua_erp.api.dto.productpresentation.ProductPresentationResponseDto;
import com.tamachihua_erp.api.entity.Presentation;
import com.tamachihua_erp.api.entity.ProductPresentation;
import com.tamachihua_erp.api.util.StockStatusCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductPresentationMapper {

    // Presentación del producto con existencia, mínimo y precio vigente
    public ProductPresentationResponseDto toResponseDto(
            ProductPresentation productPresentation,
            BigDecimal stock,
            BigDecimal currentPrice
    ) {

        BigDecimal safeStock = stock == null ? BigDecimal.ZERO : stock;

        Presentation presentation = productPresentation.getPresentation();

        PresentationSummaryDto presentationDto = new PresentationSummaryDto(
                presentation.getId(),
                presentation.getName(),
                presentation.getMeasureUnit().getName(),
                presentation.getQuantity()
        );

        return new ProductPresentationResponseDto(
                productPresentation.getId(),
                productPresentation.getCode(),
                presentationDto,
                productPresentation.getMinStock(),
                currentPrice,
                safeStock,
                StockStatusCalculator.calculate(safeStock, productPresentation.getMinStock()),
                productPresentation.getIsActive()
        );
    }

    // Versión corta para el listado de productos
    public StockByPresentationDto toStockDto(
            ProductPresentation productPresentation,
            BigDecimal stock
    ) {

        BigDecimal safeStock = stock == null ? BigDecimal.ZERO : stock;

        return new StockByPresentationDto(
                productPresentation.getId(),
                productPresentation.getPresentation().getName(),
                safeStock,
                StockStatusCalculator.calculate(safeStock, productPresentation.getMinStock())
        );
    }
}
