package com.wing.ecommercebackendwing.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name="categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false , length = 100)
    private String name;
    @Column(nullable = false , length = 100 , unique = true)
    private String slug;
    private String description;
    private String icon;
    private String image;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category parent;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column (name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "category" , cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products;

    @OneToMany(mappedBy = "parent" , cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Category> subcategories;
}
