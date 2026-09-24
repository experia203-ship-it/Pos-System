package com.connectors.pos.products;

import jakarta.persistence.LockModeType;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository  extends JpaRepository<Products,Long> {

    @Transactional
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Products p where p.id in :ids ")
    List<Products> findAllByIds(@Param("ids") List<Long> ids);


@Query(value = "select is_active from Products where id=:id",nativeQuery = true)

    Boolean checkIsActiveStatusById(@Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByPartNumber(String partNumber);


@EntityGraph(attributePaths = {"category"})
Page<Products> findByCategoryId(Long id,Pageable pageable);


   boolean existsByCategoryId(Long id);




   @Query(value="select p from Products p where lower(p.name)  like lower(concat('%',:name,'%')) "+
   "or lower(p.description)  like lower(concat('%',:name,'%'))")

    Page<Products> searchByKeywordPartialMatch(@Param("name") String name , Pageable pageable);

@Query(value="select exists(select 1 from products where id =:id and  is_active = :status)" ,nativeQuery = true)
   boolean checkActiveStatus(@Param("id") Long id , @Param("status") boolean status);


@Query(value="select * from products where id=:id and is_active = false",nativeQuery = true)
Products findProductByIdAndIsActiveFalse(@Param("id") Long id );



Optional<Products> findByBarcode(String barcode);

@Query("select p from Products p where p.stock <= p.reorderPoint order by (p.stock-p.reorderPoint) asc")
Page<Products> extractAllProductsThatHitTheReorderPoint(Pageable pageable);
}
