package com.aji.accounting.common.master;

import com.aji.accounting.common.entity.MasterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface MasterRepository<E extends MasterEntity> extends JpaRepository<E, Long> {

    @Query("""
            select e from #{#entityName} e
            where (:q is null or lower(e.code) like lower(concat('%', :q, '%'))
                   or lower(e.name) like lower(concat('%', :q, '%')))
              and (:active is null or e.active = :active)
            """)
    Page<E> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    Optional<E> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<E> findByActiveTrueOrderByCodeAsc();
}
