package com.example.inventoryservice.config;

import com.example.inventoryservice.domain.Product;
import com.example.inventoryservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("[INVENTORY] Seeding demo products...");
            productRepository.save(Product.builder()
                    .id("PROD-001")
                    .name("Laptop Pro")
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build());
            productRepository.save(Product.builder()
                    .id("PROD-002")
                    .name("Wireless Mouse")
                    .availableQuantity(500)
                    .reservedQuantity(0)
                    .build());
            productRepository.save(Product.builder()
                    .id("PROD-003")
                    .name("USB-C Hub")
                    .availableQuantity(200)
                    .reservedQuantity(0)
                    .build());
            log.info("[INVENTORY] Demo products seeded.");
        }
    }
}
