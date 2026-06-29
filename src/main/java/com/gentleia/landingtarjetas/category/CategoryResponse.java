package com.gentleia.landingtarjetas.category;

public record CategoryResponse(
        Long id,
        String name,
        String color,
        boolean active
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor(), category.isActive());
    }
}
