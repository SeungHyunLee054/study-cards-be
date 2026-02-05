package com.example.study_cards.domain.category.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_code", columnList = "code", unique = true),
        @Index(name = "idx_category_parent", columnList = "parent_id")
})
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    public Category(String code, String name, Category parent, Integer displayOrder) {
        this.code = code;
        this.name = name;
        this.parent = parent;
        this.depth = parent != null ? parent.getDepth() + 1 : 0;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public void update(String code, String name, Integer displayOrder) {
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public void updateParent(Category parent) {
        this.parent = parent;
        this.depth = parent != null ? parent.getDepth() + 1 : 0;
    }

    public boolean isRootCategory() {
        return this.parent == null;
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    public void addChild(Category child) {
        this.children.add(child);
        child.updateParent(this);
    }
}
