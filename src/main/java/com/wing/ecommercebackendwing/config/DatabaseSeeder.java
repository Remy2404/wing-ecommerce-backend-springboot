package com.wing.ecommercebackendwing.config;

import com.wing.ecommercebackendwing.model.entity.*;
import com.wing.ecommercebackendwing.model.enums.UserRole;
import com.wing.ecommercebackendwing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Central image pool for unique product images
    private int imageIndex = 0;
    private final List<String> imagePool = List.of(
        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80",  // headphones
        "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800&q=80",  // headphones alt
        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",  // watch
        "https://images.unsplash.com/photo-1589003077984-894e133dabab?w=800&q=80",  // speaker
        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&q=80",  // tshirt
        "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&q=80",  // sunglasses
        "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800&q=80",  // power bank
        "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=800&q=80",  // coffee mugs
        "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&q=80",  // yoga mat
        "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&q=80",  // gaming mouse
        "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80",  // denim jacket
        "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&q=80",  // bluetooth speaker
        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80",  // leather wallet
        "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&q=80",  // hoodie
        "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800&q=80",  // throw blanket
        "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&q=80",  // dumbbells
        "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&q=80",  // tablet
        "https://images.unsplash.com/photo-1585386959984-a4155224a1ad?w=800&q=80",  // backpack
        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80",  // sneakers
        "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80",  // wall art
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80"   // resistance bands
    );

    /**
     * Returns the next unique image URL from the pool.
     * Cycles through the pool if more products than images.
     */
    private String nextImage() {
        String image = imagePool.get(imageIndex % imagePool.size());
        imageIndex++;
        return image;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println("🌱 Database already has users. Skipping seed...");
            return;
        }

        System.out.println("🌱 Starting database seed...");

        // Clear existing data using TRUNCATE CASCADE for proper FK handling
        System.out.println("🗑️ Clearing existing data...");
        jdbcTemplate.execute("TRUNCATE TABLE promotion_usage, promotions, wing_points_transactions, wing_points, notifications, wishlists, reviews, order_items, payments, deliveries, orders, cart_items, carts, product_variants, products, merchants, addresses, categories, users RESTART IDENTITY CASCADE");

        // 1. Create Categories
        System.out.println("📁 Creating categories...");
        List<Category> categories = new ArrayList<>();
        
        Category electronics = new Category();
        electronics.setName("Electronics");
        electronics.setSlug("electronics");
        electronics.setIcon("Smartphone");
        electronics.setImage("https://images.unsplash.com/photo-1498049794561-7780e7231661?w=800&q=80");
        electronics.setSortOrder(1);
        categories.add(electronics);

        Category accessories = new Category();
        accessories.setName("Accessories");
        accessories.setSlug("accessories");
        accessories.setIcon("Watch");
        accessories.setImage("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80");
        accessories.setSortOrder(2);
        categories.add(accessories);

        Category clothing = new Category();
        clothing.setName("Clothing");
        clothing.setSlug("clothing");
        clothing.setIcon("Shirt");
        clothing.setImage("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&q=80");
        clothing.setSortOrder(3);
        categories.add(clothing);

        Category homeLiving = new Category();
        homeLiving.setName("Home & Living");
        homeLiving.setSlug("home-living");
        homeLiving.setIcon("House");
        homeLiving.setImage("https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&q=80");
        homeLiving.setSortOrder(4);
        categories.add(homeLiving);

        Category sportsFitness = new Category();
        sportsFitness.setName("Sports & Fitness");
        sportsFitness.setSlug("sports-fitness");
        sportsFitness.setIcon("Activity");
        sportsFitness.setImage("https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80");
        sportsFitness.setSortOrder(5);
        categories.add(sportsFitness);

        categoryRepository.saveAll(categories);
        System.out.println("✅ Created " + categories.size() + " categories");

        // 2. Create Users
        System.out.println("👥 Creating users...");
        String hashedPassword = passwordEncoder.encode("password123");
        List<User> users = new ArrayList<>();

        User merchant1 = new User();
        merchant1.setEmail("merchant1@store.com");
        merchant1.setPhone("+855123456789");
        merchant1.setPassword(hashedPassword);
        merchant1.setFirstName("John");
        merchant1.setLastName("Electronics");
        merchant1.setRole(UserRole.MERCHANT);
        merchant1.setCreatedAt(Instant.now());
        merchant1.setUpdatedAt(Instant.now());
        users.add(merchant1);

        User merchant2 = new User();
        merchant2.setEmail("merchant2@store.com");
        merchant2.setPhone("+855123456790");
        merchant2.setPassword(hashedPassword);
        merchant2.setFirstName("Sarah");
        merchant2.setLastName("Fashion");
        merchant2.setRole(UserRole.MERCHANT);
        merchant2.setCreatedAt(Instant.now());
        merchant2.setUpdatedAt(Instant.now());
        users.add(merchant2);

        User customer1 = new User();
        customer1.setEmail("customer1@example.com");
        customer1.setPhone("+855123456791");
        customer1.setPassword(hashedPassword);
        customer1.setFirstName("Mike");
        customer1.setLastName("Johnson");
        customer1.setRole(UserRole.CUSTOMER);
        customer1.setCreatedAt(Instant.now());
        customer1.setUpdatedAt(Instant.now());
        users.add(customer1);

        User customer2 = new User();
        customer2.setEmail("customer2@example.com");
        customer2.setPhone("+855123456792");
        customer2.setPassword(hashedPassword);
        customer2.setFirstName("Emily");
        customer2.setLastName("Chen");
        customer2.setRole(UserRole.CUSTOMER);
        customer2.setCreatedAt(Instant.now());
        customer2.setUpdatedAt(Instant.now());
        users.add(customer2);

        userRepository.saveAll(users);
        System.out.println("✅ Created " + users.size() + " users");

        // 3. Create Merchants
        System.out.println("🏪 Creating merchants...");
        List<Merchant> merchants = new ArrayList<>();

        Merchant m1 = new Merchant();
        m1.setUser(merchant1);
        m1.setStoreName("Tech Haven");
        m1.setStoreDescription("Premium electronics and gadgets");
        m1.setPhoneNumber("+855123456789");
        m1.setEmail("support@techhaven.com");
        m1.setIsVerified(true);
        m1.setRating(new BigDecimal("4.8"));
        m1.setCreatedAt(Instant.now());
        m1.setUpdatedAt(Instant.now());
        merchants.add(m1);

        Merchant m2 = new Merchant();
        m2.setUser(merchant2);
        m2.setStoreName("Fashion Forward");
        m2.setStoreDescription("Trendy clothing and accessories");
        m2.setPhoneNumber("+855123456790");
        m2.setEmail("support@fashionforward.com");
        m2.setIsVerified(true);
        m2.setRating(new BigDecimal("4.9"));
        m2.setCreatedAt(Instant.now());
        m2.setUpdatedAt(Instant.now());
        merchants.add(m2);

        merchantRepository.saveAll(merchants);
        System.out.println("✅ Created " + merchants.size() + " merchants");

        // 4. Create Products
        System.out.println("📦 Creating products...");
        List<Product> products = new ArrayList<>();

        Product p1 = Product.builder()
                .merchant(m1)
                .category(electronics)
                .name("Premium Wireless Headphones Pro")
                .slug("premium-wireless-headphones-pro")
                .description("Experience crystal-clear audio with our flagship wireless headphones. Features active noise cancellation, 40-hour battery life, and premium comfort.")
                .price(new BigDecimal("0.01"))
                .comparePrice(new BigDecimal("0.02"))
                .stockQuantity(15)
                .images(nextImage() + "," + nextImage())
                .rating(new BigDecimal("4.8"))
                .reviewCount(124)
                .isFeatured(true)
                .soldCount(450)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isActive(true)
                .build();
        products.add(p1);

        Product p2 = Product.builder()
                .merchant(m2)
                .category(accessories)
                .name("Minimalist Leather Watch")
                .slug("minimalist-leather-watch")
                .description("A timeless design meets modern craftsmanship. Swiss movement, sapphire crystal, and genuine Italian leather strap.")
                .price(new BigDecimal("0.01"))
                .stockQuantity(100)
                .images(nextImage())
                .rating(new BigDecimal("4.9"))
                .reviewCount(89)
                .isFeatured(true)
                .soldCount(320)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isActive(true)
                .build();
        products.add(p2);

        Product p3 = Product.builder()
                .merchant(m1)
                .category(electronics)
                .name("Smart Home Speaker")
                .slug("smart-home-speaker")
                .description("Voice-controlled smart speaker with premium 360° audio and smart home integration.")
                .price(new BigDecimal("0.01"))
                .comparePrice(new BigDecimal("0.02"))
                .stockQuantity(25)
                .images(nextImage())
                .rating(new BigDecimal("4.6"))
                .reviewCount(203)
                .isFeatured(true)
                .soldCount(580)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isActive(true)
                .build();
        products.add(p3);

        Product p4 = Product.builder()
                .merchant(m2)
                .category(clothing)
                .name("Organic Cotton T-Shirt")
                .slug("organic-cotton-tshirt")
                .description("Sustainably sourced 100% organic cotton. Soft, breathable, and eco-friendly.")
                .price(new BigDecimal("0.01"))
                .stockQuantity(50)
                .images(nextImage())
                .rating(new BigDecimal("4.5"))
                .reviewCount(67)
                .isFeatured(false)
                .soldCount(890)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isActive(true)
                .build();
        products.add(p4);

        Product p5 = Product.builder()
                .merchant(m2)
                .category(accessories)
                .name("Designer Sunglasses")
                .slug("designer-sunglasses")
                .description("UV400 protection with polarized lenses. Titanium frame for ultimate durability and style.")
                .price(new BigDecimal("0.01"))
                .comparePrice(new BigDecimal("0.02"))
                .stockQuantity(50)
                .images(nextImage())
                .rating(new BigDecimal("4.7"))
                .reviewCount(45)
                .isFeatured(false)
                .soldCount(123)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isActive(true)
                .build();
        products.add(p5);

        // Add more products to match the JS seed
        products.add(Product.builder().merchant(m1).category(electronics).name("Portable Power Bank").slug("portable-power-bank").description("20000mAh capacity with fast charging support. Charge multiple devices simultaneously.").price(new BigDecimal("0.01")).comparePrice(new BigDecimal("0.02")).stockQuantity(100).images(nextImage()).rating(new BigDecimal("4.4")).reviewCount(312).isFeatured(false).soldCount(1450).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(homeLiving).name("Ceramic Coffee Mug Set").slug("ceramic-coffee-mug-set").description("Handcrafted ceramic mugs. Set of 4 in earthy tones. Microwave and dishwasher safe.").price(new BigDecimal("0.01")).stockQuantity(30).images(nextImage()).rating(new BigDecimal("4.8")).reviewCount(156).isFeatured(false).soldCount(670).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(sportsFitness).name("Yoga Mat Premium").slug("yoga-mat-premium").description("Extra thick 6mm cushioning with non-slip surface. Perfect for yoga, pilates, and workouts.").price(new BigDecimal("0.01")).comparePrice(new BigDecimal("0.02")).stockQuantity(40).images(nextImage()).rating(new BigDecimal("4.6")).reviewCount(89).isFeatured(false).soldCount(340).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m1).category(electronics).name("Wireless Gaming Mouse").slug("wireless-gaming-mouse").description("Ultra-responsive with customizable RGB lighting. 16000 DPI sensor for precision.").price(new BigDecimal("0.01")).comparePrice(new BigDecimal("0.02")).stockQuantity(45).images(nextImage()).rating(new BigDecimal("4.7")).reviewCount(234).isFeatured(true).soldCount(560).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(clothing).name("Denim Jacket Classic").slug("denim-jacket-classic").description("Timeless denim jacket with a modern fit. Premium quality cotton denim.").price(new BigDecimal("0.01")).stockQuantity(20).images(nextImage()).rating(new BigDecimal("4.8")).reviewCount(98).isFeatured(true).soldCount(280).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m1).category(electronics).name("Bluetooth Portable Speaker").slug("bluetooth-portable-speaker").description("Waterproof wireless speaker with 360° sound and 12-hour battery life. Perfect for outdoor adventures.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(30).images(nextImage()).rating(new BigDecimal("4.5")).reviewCount(178).isFeatured(true).soldCount(420).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(accessories).name("Genuine Leather Wallet").slug("genuine-leather-wallet").description("Handcrafted genuine leather wallet with RFID protection and multiple card slots.").price(new BigDecimal("0.02")).stockQuantity(25).images(nextImage()).rating(new BigDecimal("4.7")).reviewCount(134).isFeatured(false).soldCount(290).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(clothing).name("Cotton Hoodie Premium").slug("cotton-hoodie-premium").description("Ultra-soft 100% cotton hoodie with kangaroo pocket and ribbed cuffs. Perfect for casual wear.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(35).images(nextImage()).rating(new BigDecimal("4.6")).reviewCount(87).isFeatured(false).soldCount(380).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(homeLiving).name("Faux Fur Throw Blanket").slug("faux-fur-throw-blanket").description("Luxuriously soft faux fur throw blanket. Machine washable and perfect for cozy evenings.").price(new BigDecimal("0.02")).stockQuantity(18).images(nextImage()).rating(new BigDecimal("4.9")).reviewCount(76).isFeatured(false).soldCount(195).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(sportsFitness).name("Adjustable Dumbbells Set").slug("adjustable-dumbbells-set").description("5-50lb adjustable dumbbells with quick-change mechanism. Space-saving design for home workouts.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(12).images(nextImage()).rating(new BigDecimal("4.8")).reviewCount(145).isFeatured(true).soldCount(167).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m1).category(electronics).name("10-inch Android Tablet").slug("10-inch-android-tablet").description("High-performance tablet with 128GB storage, 8MP camera, and all-day battery life.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(22).images(nextImage()).rating(new BigDecimal("4.4")).reviewCount(98).isFeatured(false).soldCount(310).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(accessories).name("Laptop Backpack Professional").slug("laptop-backpack-professional").description("Water-resistant laptop backpack with dedicated compartments and ergonomic design.").price(new BigDecimal("0.02")).stockQuantity(28).images(nextImage()).rating(new BigDecimal("4.6")).reviewCount(203).isFeatured(true).soldCount(445).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(clothing).name("Running Sneakers Lightweight").slug("running-sneakers-lightweight").description("Breathable mesh running shoes with cushioning and responsive sole. Perfect for daily runs.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(40).images(nextImage()).rating(new BigDecimal("4.7")).reviewCount(167).isFeatured(true).soldCount(520).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(homeLiving).name("Abstract Wall Art Canvas").slug("abstract-wall-art-canvas").description("Modern abstract canvas art print. 24x36 inches, ready to hang with wooden frame.").price(new BigDecimal("0.02")).stockQuantity(15).images(nextImage()).rating(new BigDecimal("4.5")).reviewCount(62).isFeatured(false).soldCount(134).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());
        products.add(Product.builder().merchant(m2).category(sportsFitness).name("Resistance Bands Set").slug("resistance-bands-set").description("5-piece resistance band set with varying resistance levels. Includes door anchor and carrying case.").price(new BigDecimal("0.02")).comparePrice(new BigDecimal("0.03")).stockQuantity(50).images(nextImage()).rating(new BigDecimal("4.6")).reviewCount(89).isFeatured(false).soldCount(278).createdAt(Instant.now()).updatedAt(Instant.now()).isActive(true).build());

        productRepository.saveAll(products);
        System.out.println("✅ Created " + products.size() + " products");

        // 5. Create Reviews
        System.out.println("⭐ Creating reviews...");
        List<Review> reviews = new ArrayList<>();

        Review r1 = new Review();
        r1.setUser(customer1);
        r1.setProduct(p1);
        r1.setRating(5);
        r1.setComment("Absolutely love these headphones! The sound quality is incredible and the noise cancellation works perfectly.");
        r1.setIsVerifiedPurchase(true);
        r1.setCreatedAt(Instant.now());
        r1.setUpdatedAt(Instant.now());
        reviews.add(r1);

        Review r2 = new Review();
        r2.setUser(customer2);
        r2.setProduct(p1);
        r2.setRating(4);
        r2.setComment("Great product, battery life is as advertised. Only minor issue is the case could be better.");
        r2.setIsVerifiedPurchase(true);
        r2.setCreatedAt(Instant.now());
        r2.setUpdatedAt(Instant.now());
        reviews.add(r2);

        Review r3 = new Review();
        r3.setUser(customer1);
        r3.setProduct(p2);
        r3.setRating(5);
        r3.setComment("Beautiful watch! The leather strap is very comfortable and the design is elegant.");
        r3.setIsVerifiedPurchase(true);
        r3.setCreatedAt(Instant.now());
        r3.setUpdatedAt(Instant.now());
        reviews.add(r3);

        reviewRepository.saveAll(reviews);
        System.out.println("✅ Created " + reviews.size() + " reviews");

        System.out.println("\n🎉 Database seed completed successfully!");
    }
}
