package com.expensetracker.web;

import com.expensetracker.domain.Category;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference endpoint exposing the fixed {@link Category} set, for populating the
 * frontend dropdown ({@code GET /api/categories} in {@code docs/api-contracts.md}).
 *
 * <p>Returns the enum values in declaration order, serialized by name
 * ({@code ["FOOD","TRANSPORT",...]}). The enum is the single source of truth, so
 * adding a category here automatically surfaces it.
 */
@RestController
@RequestMapping("/api")
public class CategoryController {

    @GetMapping("/categories")
    public List<Category> categories() {
        return List.of(Category.values());
    }
}
