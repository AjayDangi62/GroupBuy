package com.groupbuy.product.repository;

import com.groupbuy.TestDatabaseCleaner;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.product.entity.Product;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Product Repository Search Query Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    private User seller;
    private Category electronicsCategory;
    private Category fashionCategory;
    private Product product1;
    private Product product2;
    private Product product3;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        seller = new User("seller_repo@groupbuy.com", "seller_repo", passwordEncoder.encode("Pass123!"), "Repo Seller", User.Role.SELLER);
        seller = userRepository.save(seller);

        electronicsCategory = new Category("Electronics", "electronics", "Gadgets", null, true, 1);
        electronicsCategory = categoryRepository.save(electronicsCategory);

        fashionCategory = new Category("Fashion", "fashion", "Apparel", null, true, 2);
        fashionCategory = categoryRepository.save(fashionCategory);

        // Product 1: In Electronics, name "Sony WH-1000XM5 Wireless Headphones", desc "Industry-leading noise cancelling"
        product1 = new Product(
                seller, electronicsCategory, "SKU-SONY-01",
                "Sony WH-1000XM5 Wireless Headphones",
                "Industry-leading noise cancelling with multi-device Bluetooth support",
                new BigDecimal("24999.00"), "INR", 50, 50, Product.Status.ACTIVE, null
        );
        product1 = productRepository.save(product1);

        // Product 2: In Electronics, name "boAt Rockerz 450", desc "Lightweight on-ear headphones with 40mm drivers"
        product2 = new Product(
                seller, electronicsCategory, "SKU-BOAT-02",
                "boAt Rockerz 450",
                "Lightweight on-ear headphones with 40mm drivers and long battery",
                new BigDecimal("1799.00"), "INR", 100, 100, Product.Status.ACTIVE, null
        );
        product2 = productRepository.save(product2);

        // Product 3: In Fashion, name "Classic Cotton Kurta", desc "Handmade organic cotton comfort wear"
        product3 = new Product(
                seller, fashionCategory, "SKU-KURTA-03",
                "Classic Cotton Kurta",
                "Handmade organic cotton comfort wear for casual occasions",
                new BigDecimal("1499.00"), "INR", 80, 80, Product.Status.ACTIVE, null
        );
        product3 = productRepository.save(product3);

        // Inactive product that matches "Sony" but status is ARCHIVED
        inactiveProduct = new Product(
                seller, electronicsCategory, "SKU-SONY-INACTIVE",
                "Sony Vintage Radio",
                "Discontinued antique radio receiver",
                new BigDecimal("5000.00"), "INR", 5, 0, Product.Status.ARCHIVED, null
        );
        inactiveProduct = productRepository.save(inactiveProduct);
    }

    @Test
    @DisplayName("searchPublicProducts with NULL query and NULL category returns all active products")
    void searchWithNullQueryAndNullCategoryReturnsAllActive() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));
        Page<Product> result = productRepository.searchPublicProducts(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-SONY-01", "SKU-BOAT-02", "SKU-KURTA-03");
    }

    @Test
    @DisplayName("searchPublicProducts with non-empty query matching name (case-insensitive)")
    void searchWithNonEmptyQueryMatchingName() {
        Pageable pageable = PageRequest.of(0, 10);

        // Lowercase search for "sony"
        Page<Product> result = productRepository.searchPublicProducts("sony", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Sony WH-1000XM5 Wireless Headphones");
        // Verify inactive Sony Vintage Radio is excluded
        assertThat(result.getContent()).extracting(Product::getSku).doesNotContain("SKU-SONY-INACTIVE");
    }

    @Test
    @DisplayName("searchPublicProducts with non-empty query matching description (case-insensitive)")
    void searchWithNonEmptyQueryMatchingDescription() {
        Pageable pageable = PageRequest.of(0, 10);

        // "noise cancelling" appears only in product1's description
        Page<Product> result = productRepository.searchPublicProducts("noise cancelling", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("SKU-SONY-01");
    }

    @Test
    @DisplayName("searchPublicProducts with term matching multiple products in name or description")
    void searchWithQueryMatchingMultipleProducts() {
        Pageable pageable = PageRequest.of(0, 10);

        // "headphones" appears in product1 name ("Wireless Headphones") and product2 description ("on-ear headphones")
        Page<Product> result = productRepository.searchPublicProducts("headphones", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-SONY-01", "SKU-BOAT-02");
    }

    @Test
    @DisplayName("searchPublicProducts with non-matching query returns empty page")
    void searchWithNonMatchingQueryReturnsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.searchPublicProducts("NonExistentTermXYZ", null, pageable);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("searchPublicProducts with NULL query but categoryId filter")
    void searchWithNullQueryAndCategoryFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.searchPublicProducts(null, fashionCategory.getId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("SKU-KURTA-03");
    }

    @Test
    @DisplayName("searchPublicProducts with both query and categoryId filter")
    void searchWithBothQueryAndCategoryFilter() {
        Pageable pageable = PageRequest.of(0, 10);

        // "cotton" in Fashion category -> 1 match
        Page<Product> match = productRepository.searchPublicProducts("cotton", fashionCategory.getId(), pageable);
        assertThat(match.getTotalElements()).isEqualTo(1);

        // "cotton" in Electronics category -> 0 matches
        Page<Product> noMatch = productRepository.searchPublicProducts("cotton", electronicsCategory.getId(), pageable);
        assertThat(noMatch.isEmpty()).isTrue();
    }
}

