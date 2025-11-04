package com.selimhorri.app.helper;

import java.util.Optional;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;

public interface CategoryMappingHelper {
	
	public static CategoryDto map(final Category category) {
		
		final var parentCategory = Optional.ofNullable(category
				.getParentCategory()).orElseGet(() -> new Category());
		
		return CategoryDto.builder()
				.categoryId(category.getCategoryId())
				.categoryTitle(category.getCategoryTitle())
				.imageUrl(category.getImageUrl())
				.parentCategoryDto(
						CategoryDto.builder()
							.categoryId(parentCategory.getCategoryId())
							.categoryTitle(parentCategory.getCategoryTitle())
							.imageUrl(parentCategory.getImageUrl())
							.build())
				.build();
	}
	
	public static Category map(final CategoryDto categoryDto) {
		
		// Crear builder de la categoría sin ID (para creación)
		Category.CategoryBuilder categoryBuilder = Category.builder()
				.categoryTitle(categoryDto.getCategoryTitle())
				.imageUrl(categoryDto.getImageUrl());
		
		// Solo establecer categoryId si NO es null (para updates)
		if (categoryDto.getCategoryId() != null) {
			categoryBuilder.categoryId(categoryDto.getCategoryId());
		}
		
		// Solo establecer categoría padre si parentCategoryDto existe y no es null
		if (categoryDto.getParentCategoryDto() != null && 
		    categoryDto.getParentCategoryDto().getCategoryId() != null) {
			
			Category.CategoryBuilder parentCategoryBuilder = Category.builder()
					.categoryTitle(categoryDto.getParentCategoryDto().getCategoryTitle())
					.imageUrl(categoryDto.getParentCategoryDto().getImageUrl())
					.categoryId(categoryDto.getParentCategoryDto().getCategoryId());
			
			categoryBuilder.parentCategory(parentCategoryBuilder.build());
		}
		
		return categoryBuilder.build();
	}
	
	
	
}










