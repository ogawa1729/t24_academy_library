package jp.co.metateam.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.metateam.library.model.RentalManage;

@Repository
public interface RentalManageRepository extends JpaRepository<RentalManage, Long> {
    List<RentalManage> findAll();

	Optional<RentalManage> findById(Long id);

    // List<RentalManage> findAllByStatusIn(List<Integer> status);

   @Query
    ("SELECT count(rm)  FROM RentalManage rm  WHERE rm.status IN (0,1) AND rm.stock.id = ?1" )
    Long findByStockIdAndStatusIn(String stock_Id);

   @Query
    ("SELECT count(rm)  FROM RentalManage rm  WHERE rm.status IN (0,1) AND  rm.stock.id = ?1  AND (rm.expectedRentalOn > ?2 OR rm.expectedReturnOn < ?3)")
    Long findByStockIdAndDate(String stock_Id, Date expectedReturnOn, Date expectedRentalOn);

    @Query
    ("SELECT count(rm)  FROM RentalManage rm  WHERE rm.status IN (0,1) AND rm.stock.id = ?1 AND rm.id <> ?2 ")
    Long findByStockIdAndId(String stock_Id, Long id);

    @Query
    ("SELECT count(rm)  FROM RentalManage rm  WHERE rm.status IN (0,1) AND  rm.stock.id = ?1  AND (rm.expectedRentalOn > ?2 OR rm.expectedReturnOn < ?3) AND rm.id <> ?4")
    Long findByStockIdAndDateAndId(String stock_Id, Date expectedReturnOn, Date expectedRentalOn, Long Id);
}

