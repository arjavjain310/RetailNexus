package com.retailnexus.config;

import com.retailnexus.entity.Batch;
import com.retailnexus.entity.Product;
import com.retailnexus.entity.User;
import com.retailnexus.repository.ProductRepository;
import com.retailnexus.repository.UserRepository;
import com.retailnexus.service.BatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final int DEFAULT_STOCK_PER_ITEM = 100;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BatchService batchService;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, ProductRepository productRepository,
                      BatchService batchService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.batchService = batchService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            User cashier = new User();
            cashier.setUsername("cashier");
            cashier.setPassword(passwordEncoder.encode("cashier123"));
            cashier.setRole(User.Role.CASHIER);
            userRepository.save(cashier);
        }
        if (productRepository.count() == 0) {
            List<Product> products = new ArrayList<>(List.of(
                product("Amul Full Cream Milk", "8901234567001", "Dairy", "52", "58", "5", Product.Unit.LITRE),
                product("Amul Butter", "8901234567002", "Dairy", "265", "295", "5", Product.Unit.KG),
                product("Amul Cheese Slice", "8901234567003", "Dairy", "180", "200", "12", Product.Unit.PIECES),
                product("Mother Dairy Curd", "8901234567004", "Dairy", "32", "36", "5", Product.Unit.LITRE),
                product("Nandini Ghee", "8901234567005", "Dairy", "520", "580", "5", Product.Unit.LITRE),
                product("Coca Cola", "8901234567010", "Beverages", "75", "95", "28", Product.Unit.LITRE),
                product("Pepsi", "8901234567011", "Beverages", "72", "92", "28", Product.Unit.LITRE),
                product("Bisleri Water", "8901234567012", "Beverages", "18", "22", "0", Product.Unit.LITRE),
                product("Tropicana Orange", "8901234567013", "Beverages", "165", "195", "12", Product.Unit.LITRE),
                product("Real Fruit Juice Mango", "8901234567014", "Beverages", "140", "165", "12", Product.Unit.LITRE),
                product("Brooke Bond Red Label Tea", "8901234567015", "Beverages", "220", "255", "5", Product.Unit.KG),
                product("Tata Tea Gold", "8901234567016", "Beverages", "280", "320", "5", Product.Unit.KG),
                product("Nescafe Classic", "8901234567017", "Beverages", "380", "440", "18", Product.Unit.PIECES),
                product("Lay's Classic", "8901234567020", "Snacks", "18", "22", "12", Product.Unit.PIECES),
                product("Kurkure Masala Munch", "8901234567021", "Snacks", "10", "12", "12", Product.Unit.PIECES),
                product("Parle-G Biscuit", "8901234567023", "Snacks", "28", "35", "18", Product.Unit.PIECES),
                product("Britannia Good Day", "8901234567024", "Snacks", "45", "55", "18", Product.Unit.PIECES),
                product("Kellogg's Corn Flakes", "8901234567027", "Snacks", "185", "220", "12", Product.Unit.PIECES),
                product("Kissan Tomato Ketchup", "8901234567028", "Snacks", "115", "140", "12", Product.Unit.PIECES),
                product("India Gate Basmati Rice", "8901234567030", "Grains", "95", "115", "5", Product.Unit.KG),
                product("Tata Sampann Dal", "8901234567031", "Grains", "125", "145", "5", Product.Unit.KG),
                product("Fortune Sunlite Oil", "8901234567032", "Grains", "165", "185", "5", Product.Unit.LITRE),
                product("Aashirvaad Atta", "8901234567033", "Grains", "245", "275", "5", Product.Unit.KG),
                product("Saffola Gold Oil", "8901234567034", "Grains", "195", "220", "5", Product.Unit.LITRE),
                product("Daal Moong", "8901234567035", "Pulses", "55", "65", "5", Product.Unit.KG),
                product("Daal Toor", "8901234567036", "Pulses", "65", "75", "5", Product.Unit.KG),
                product("Daal Chana", "8901234567037", "Pulses", "48", "58", "5", Product.Unit.KG),
                product("Daal Masoor", "8901234567038", "Pulses", "52", "62", "5", Product.Unit.KG),
                product("Daal Urad", "8901234567039", "Pulses", "85", "98", "5", Product.Unit.KG),
                product("Colgate Toothpaste", "8901234567040", "Toiletries", "95", "115", "18", Product.Unit.PIECES),
                product("Dove Soap", "8901234567042", "Toiletries", "48", "58", "18", Product.Unit.PIECES),
                product("Lux Soap", "8901234567043", "Toiletries", "28", "35", "18", Product.Unit.PIECES),
                product("Head & Shoulders Shampoo", "8901234567045", "Toiletries", "285", "330", "18", Product.Unit.PIECES),
                product("Vicks Vaporub", "8901234567047", "Toiletries", "125", "145", "12", Product.Unit.PIECES),
                product("Vim Dishwash Bar", "8901234567050", "Household", "18", "22", "18", Product.Unit.PIECES),
                product("Surf Excel", "8901234567051", "Household", "95", "115", "18", Product.Unit.KG),
                product("Tide Plus", "8901234567052", "Household", "185", "220", "18", Product.Unit.KG),
                product("Harpic Toilet Cleaner", "8901234567053", "Household", "165", "195", "18", Product.Unit.LITRE),
                product("Lizol Disinfectant", "8901234567054", "Household", "245", "285", "18", Product.Unit.LITRE),
                product("Britannia Milk Bread", "8901234567060", "Dairy", "38", "45", "5", Product.Unit.PIECES),
                product("Eggs", "8901234567062", "Dairy", "72", "85", "0", Product.Unit.PIECES),
                product("Maggi Noodles", "8901234567068", "Snacks", "12", "14", "12", Product.Unit.PIECES),
                product("Cadbury Dairy Milk", "8901234567071", "Snacks", "48", "55", "18", Product.Unit.PIECES),
                product("Nestle Kit Kat", "8901234567072", "Snacks", "32", "38", "18", Product.Unit.PIECES),
                product("Wheel Detergent", "8901234567079", "Household", "95", "115", "18", Product.Unit.KG)
            ));
            products.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
            productRepository.saveAll(products);
            // Default stock: 100 for every item
            LocalDate defaultExpiry = LocalDate.now().plusYears(1);
            for (Product p : products) {
                Batch batch = new Batch();
                batch.setProduct(p);
                batch.setBatchNumber("INIT-" + String.format("%04d", p.getId()));
                batch.setExpiryDate(defaultExpiry);
                batch.setQuantity(DEFAULT_STOCK_PER_ITEM);
                batchService.save(batch);
            }
        }
    }

    private static Product product(String name, String barcode, String category, String cost, String sell, String gst, Product.Unit unit) {
        Product p = new Product();
        p.setName(name);
        p.setBarcode(barcode);
        p.setCategory(category);
        p.setCostPrice(new BigDecimal(cost));
        p.setSellingPrice(new BigDecimal(sell));
        p.setGstPercent(new BigDecimal(gst));
        p.setUnit(unit);
        return p;
    }
}
