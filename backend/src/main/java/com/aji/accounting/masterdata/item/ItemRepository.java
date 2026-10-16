package com.aji.accounting.masterdata.item;

import com.aji.accounting.common.master.MasterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemRepository extends MasterRepository<Item> {

    @EntityGraph(attributePaths = {"uom", "category"})
    @Query("""
            select i from Item i
            where (:q is null or lower(i.code) like lower(concat('%', :q, '%'))
                   or lower(i.name) like lower(concat('%', :q, '%')))
              and (:active is null or i.active = :active)
              and (:type is null or i.itemType = :type)
            """)
    Page<Item> searchItems(@Param("q") String q, @Param("active") Boolean active, @Param("type") ItemType type,
                           Pageable pageable);

    @EntityGraph(attributePaths = {"uom"})
    List<Item> findByActiveTrueAndItemTypeInOrderByCodeAsc(Collection<ItemType> types);
}
