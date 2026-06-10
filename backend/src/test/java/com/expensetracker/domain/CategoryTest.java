package com.expensetracker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    /**
     * The category set and its order are part of the API contract
     * ({@code docs/api-contracts.md}). Pinning both here makes any accidental
     * rename, removal, or reordering a test failure.
     */
    @Test
    void hasExactlyTheContractCategoriesInOrder() {
        assertThat(Category.values())
                .containsExactly(
                        Category.FOOD,
                        Category.TRANSPORT,
                        Category.RENT,
                        Category.UTILITIES,
                        Category.GROCERIES,
                        Category.ENTERTAINMENT,
                        Category.HEALTH,
                        Category.SHOPPING,
                        Category.OTHER);
    }

    @Test
    void namesMatchTheApiStringValues() {
        assertThat(Category.valueOf("GROCERIES")).isEqualTo(Category.GROCERIES);
        assertThat(Category.OTHER.name()).isEqualTo("OTHER");
    }
}
