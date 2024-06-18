package jp.co.metateam.library.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.metateam.library.constants.Constants;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.StockDto;
import jp.co.metateam.library.repository.BookMstRepository;
import jp.co.metateam.library.repository.StockRepository;

@Service
public class StockService {
    private final BookMstRepository bookMstRepository;
    private final StockRepository stockRepository;

    @Autowired
    public StockService(BookMstRepository bookMstRepository, StockRepository stockRepository){
        this.bookMstRepository = bookMstRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public List<Stock> findAll() {
        List<Stock> stocks = this.stockRepository.findByDeletedAtIsNull();

        return stocks;
    }
    
    @Transactional
    public List <Stock> findStockAvailableAll() {
        List <Stock> stocks = this.stockRepository.findByDeletedAtIsNullAndStatus(Constants.STOCK_AVAILABLE);

        return stocks;
    }

    @Transactional
    public Stock findById(String id) {
        return this.stockRepository.findById(id).orElse(null);
    }

    @Transactional 
    public void save(StockDto stockDto) throws Exception {
        try {
            Stock stock = new Stock();
            BookMst bookMst = this.bookMstRepository.findById(stockDto.getBookId()).orElse(null);
            if (bookMst == null) {
                throw new Exception("BookMst record not found.");
            }

            stock.setBookMst(bookMst);
            stock.setId(stockDto.getId());
            stock.setStatus(stockDto.getStatus());
            stock.setPrice(stockDto.getPrice());

            // データベースへの保存
            this.stockRepository.save(stock);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional 
    public void update(String id, StockDto stockDto) throws Exception {
        try {
            Stock stock = findById(id);
            if (stock == null) {
                throw new Exception("Stock record not found.");
            }

            BookMst bookMst = stock.getBookMst();
            if (bookMst == null) {
                throw new Exception("BookMst record not found.");
            }

            stock.setId(stockDto.getId());
            stock.setBookMst(bookMst);
            stock.setStatus(stockDto.getStatus());
            stock.setPrice(stockDto.getPrice());

            // データベースへの保存
            this.stockRepository.save(stock);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<Object> generateDaysOfWeek(int year, int month, LocalDate startDate, int daysInMonth) {
        List<Object> daysOfWeek = new ArrayList<>();
        for (int dayOfMonth = 1; dayOfMonth <= daysInMonth; dayOfMonth++) {
            LocalDate date = LocalDate.of(year, month, dayOfMonth);
            DateTimeFormatter formmater = DateTimeFormatter.ofPattern("dd(E)", Locale.JAPANESE);
            daysOfWeek.add(date.format(formmater));
        }

        return daysOfWeek;
    }

    public List<List<String>> generateValues(Integer year, Integer month, Integer daysInMonth) {
        List<BookMst> books = this.bookMstRepository.findAll();
        List<Map<String, Object>> availableStocks = this.stockRepository.findAvailableStocksByBook();
        Map<String, Integer> availableStockCountMap = new HashMap<>();
        
        Map<String, String> stockMngNumberMap = new HashMap<>();
        for (Stock stock : stockRepository.findAll()) {
            stockMngNumberMap.put(stock.getBookMst().getTitle(), stock.getId());
        }


        for (Map<String, Object> stockData : availableStocks) {
            String bookTitle = (String) stockData.get("bookTitle");
            Long stockCount = (Long) stockData.get("stockCount");
            availableStockCountMap.put(bookTitle, stockCount.intValue());
        }

        List<List<String>> values = new ArrayList<>();
        for (BookMst book : books) {
           List<String> bookValues = new ArrayList<>();
           bookValues.add(book.getTitle()); // 書籍名
           int availableStockCount = availableStockCountMap.getOrDefault(book.getTitle(), 0);
           bookValues.add(String.valueOf(availableStockCount)); // 利用可能在庫数

           String stockMngNumber = stockMngNumberMap.getOrDefault(book.getTitle(), "");
           bookValues.add(stockMngNumber);
           
           
         for (int i = 1; i <= daysInMonth; i++) {
             LocalDate localDate = LocalDate.of(year, month, i);
             Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
             Long rentalCount = this.stockRepository.findByUnAvailableCount(date, book.getTitle());
             int remainingStock = availableStockCount - rentalCount.intValue();
                if (remainingStock <= 0) {
                    bookValues.add("✖");
                } else {
                    bookValues.add(String.valueOf(remainingStock)); //貸出可能数
                }
            }
            values.add(bookValues);
        }
        return values;
    }
}
