# Database ERD (Mermaid)

This ERD is derived from the JPA entities in `src/main/java/com/wing/ecommercebackendwing/model/entity`.

```mermaid
erDiagram
    USERS {
        uuid id PK
        string email UK
        string phone UK
        string google_id UK
        string auth_provider
        string role
    }

    ADDRESSES {
        uuid id PK
        uuid user_id FK
        string label
        string street
        string city
        string province
        boolean is_default
    }

    MERCHANTS {
        uuid id PK
        uuid user_id FK
        uuid address_id FK
        string store_name
        boolean is_verified
    }

    CARTS {
        uuid id PK
        uuid user_id FK
    }

    CART_ITEMS {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        uuid variant_id FK
        int quantity
        decimal price
    }

    CATEGORIES {
        uuid id PK
        uuid parent_id FK
        string name
        string slug UK
        boolean is_active
    }

    PRODUCTS {
        uuid id PK
        uuid merchant_id FK
        uuid category_id FK
        string name
        string slug UK
        decimal price
        int stock
        boolean is_active
    }

    PRODUCT_VARIANTS {
        uuid id PK
        uuid product_id FK
        string name
        decimal price
        int stock
        boolean is_active
    }

    WISHLISTS {
        uuid id PK
        uuid user_id FK
        uuid product_id FK
        timestamp created_at
    }

    ORDERS {
        uuid id PK
        uuid user_id FK
        uuid merchant_id FK
        uuid delivery_address_id FK
        string order_number UK
        string status
        decimal total
        timestamp created_at
    }

    ORDER_IDEMPOTENCY_RECORDS {
        uuid id PK
        uuid user_id FK
        string idempotency_key
        string request_hash
        uuid order_id FK UK
        timestamp created_at
        timestamp updated_at
    }

    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid product_id FK
        uuid variant_id FK
        int quantity
        decimal unit_price
        decimal subtotal
    }

    PAYMENTS {
        uuid id PK
        uuid order_id FK
        string transaction_id
        string method
        string status
        decimal amount
    }

    DELIVERIES {
        uuid id PK
        uuid order_id FK
        uuid driver_id FK
        string status
    }

    REVIEWS {
        uuid id PK
        uuid user_id FK
        uuid product_id FK
        uuid order_id FK
        int rating
        boolean is_verified_purchase
    }

    PROMOTIONS {
        uuid id PK
        string code UK
        string type
        decimal value
        timestamp start_date
        timestamp end_date
        boolean is_active
    }

    PROMOTION_USAGE {
        uuid id PK
        uuid promotion_id FK
        uuid user_id FK
        uuid order_id FK
        decimal discount_amount
        timestamp created_at
    }

    NOTIFICATIONS {
        uuid id PK
        uuid user_id FK
        string type
        boolean is_read
        timestamp created_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        string token UK
        timestamp expiry_date
        boolean revoked
    }

    SAVED_PAYMENT_METHODS {
        uuid id PK
        uuid user_id FK
        string method
        string last4
        boolean is_default
    }

    WING_POINTS {
        uuid id PK
        uuid user_id FK
        int points
        timestamp updated_at
    }

    WING_POINTS_TRANSACTIONS {
        uuid id PK
        uuid user_id FK
        uuid order_id FK
        int points
        string type
        timestamp created_at
    }

    USERS ||--o{ ADDRESSES : has
    USERS ||--|o CARTS : has
    USERS ||--o{ ORDERS : places
    USERS ||--o{ REVIEWS : writes
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ SAVED_PAYMENT_METHODS : saves
    USERS ||--|o MERCHANTS : merchant_profile
    USERS ||--|o WING_POINTS : balance
    USERS |o--o{ DELIVERIES : drives
    USERS ||--o{ WING_POINTS_TRANSACTIONS : earns_spends
    USERS ||--o{ PROMOTION_USAGE : uses
    USERS ||--o{ WISHLISTS : wishes
    USERS ||--o{ ORDER_IDEMPOTENCY_RECORDS : idempotency_keys

    ADDRESSES |o--o{ MERCHANTS : located_at
    ADDRESSES ||--o{ ORDERS : ships_to

    CATEGORIES |o--o{ CATEGORIES : parent_of
    CATEGORIES ||--o{ PRODUCTS : contains

    MERCHANTS |o--o{ PRODUCTS : sells
    MERCHANTS ||--o{ ORDERS : fulfills

    PRODUCTS ||--o{ PRODUCT_VARIANTS : has
    PRODUCTS ||--o{ CART_ITEMS : in_cart
    PRODUCTS ||--o{ ORDER_ITEMS : ordered
    PRODUCTS ||--o{ REVIEWS : reviewed
    PRODUCTS ||--o{ WISHLISTS : wished

    PRODUCT_VARIANTS |o--o{ CART_ITEMS : variant
    PRODUCT_VARIANTS |o--o{ ORDER_ITEMS : variant

    CARTS ||--o{ CART_ITEMS : contains

    ORDERS ||--o{ ORDER_ITEMS : includes
    ORDERS ||--|o PAYMENTS : payment
    ORDERS ||--|o DELIVERIES : delivery
    ORDERS ||--o{ PROMOTION_USAGE : discount_applied
    ORDERS |o--o{ REVIEWS : verified_by
    ORDERS |o--o{ WING_POINTS_TRANSACTIONS : related_order
    ORDERS |o--o| ORDER_IDEMPOTENCY_RECORDS : idempotent_request

    PROMOTIONS ||--o{ PROMOTION_USAGE : used_in
```
