package jp.co.metateam.library.repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Map;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.metateam.library.model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    List<Stock> findAll();

    List<Stock> findByDeletedAtIsNull();

    List<Stock> findByDeletedAtIsNullAndStatus(Integer status);

	Optional<Stock> findById(String id);
    
    List<Stock> findByBookMstIdAndStatus(Long book_id,Integer status);

    @Query("SELECT s.bookMst.title AS bookTitle, COUNT(s) AS stockCount FROM Stock s WHERE s.status = 0 AND s.deletedAt IS NULL GROUP BY s.bookMst.title")
    List<Map<String, Object>> findAvailableStocksByBook();

    @Query(value = "SELECT COUNT(*) AS count FROM rental_manage rm JOIN stocks s ON rm.stock_id = s.id JOIN book_mst bm ON s.book_id = bm.id WHERE rm.expected_rental_on <= :day AND :day <= rm.expected_return_on AND bm.title = :title", nativeQuery = true)
    Long findByUnAvailableCount(Date day, String title);

}
