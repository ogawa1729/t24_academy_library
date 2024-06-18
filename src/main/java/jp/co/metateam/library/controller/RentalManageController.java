package jp.co.metateam.library.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;


import jakarta.validation.Valid;

import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.values.StockStatus;
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import lombok.extern.log4j.Log4j2;

/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {

    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;

    @Autowired
    public RentalManageController(AccountService accountService, RentalManageService rentalManageService, StockService stockService) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }

    /**
     * 貸出一覧画面初期表示
     * 
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List<RentalManage> rentalManageList = this.rentalManageService.findAll();
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);
        // 貸出一覧画面に遷移
        return "rental/index";
    }

    @GetMapping("/rental/add")
    public String add(@RequestParam(value = "stockId", required = false) String stockId, @RequestParam(value = "expectedRentalOn", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expectedRentalOn, Model model) {
        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findAll();

        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());


        RentalManageDto rentalManageDto = new RentalManageDto();
        if (stockId != null && !stockId.isEmpty()) {
            rentalManageDto.setStockId(stockId);
        }

        if (expectedRentalOn != null) {
            Date expectedRentalOnDate = Date.from(expectedRentalOn.atStartOfDay(ZoneId.systemDefault()).toInstant());
            rentalManageDto.setExpectedRentalOn(expectedRentalOnDate);
        }

        model.addAttribute("rentalManageDto", rentalManageDto);
        
        return "rental/add";
    }

    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result,RedirectAttributes ra) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }

            Long available = rentalManageService.findByStockIdAndStatusIn(rentalManageDto.getStockId());
            Long notavailable = rentalManageService.findByStockIdAndDate(rentalManageDto.getStockId(),rentalManageDto.getExpectedReturnOn(), rentalManageDto.getExpectedRentalOn());

            if (available != notavailable) {
                FieldError fieldError = new FieldError("rentalManageDto", "expectedReturnOn", "期間が被っています");
                result.addError(fieldError);
                throw new Exception("期間が被っています");
            }

            // 登録処理
            this.rentalManageService.save(rentalManageDto, result);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            return "redirect:/rental/add";
        }
    }

    @GetMapping("/rental/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        RentalManage rentalManage = this.rentalManageService.findById(id);

        model.addAttribute("rentalManage", rentalManage);

        return "rental/detail";
    }

    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findAll();

        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            RentalManageDto rentalManageDto = new RentalManageDto();
            RentalManage rentalManage = this.rentalManageService.findById(id);
            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStockId(rentalManage.getStock().getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());

            model.addAttribute("rentalManageDto", rentalManageDto);
        }

        return "rental/edit";
    }

    @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") Long id, Model model,
            @Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, RedirectAttributes ra) {
        try {
            RentalManage rentalManage = this.rentalManageService.findById(id);
            Optional<String> statusError = rentalManageDto.isValidStatus(rentalManage.getStatus());

            if (statusError.isPresent()) {
                FieldError fieldError = new FieldError("rentalManageDto", "status", statusError.get());
                result.addError(fieldError);
                throw new Exception("Validation error.");
            }

            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }

            Long available = rentalManageService.findByStockIdAndId(rentalManageDto.getStockId(),rentalManageDto.getId());
            Long notavailable = rentalManageService.findByStockIdAndDateAndId(rentalManageDto.getStockId(),rentalManageDto.getExpectedReturnOn(), rentalManageDto.getExpectedRentalOn(), rentalManageDto.getId());

            if (available != notavailable) {
                FieldError fieldError = new FieldError("rentalManageDto", "expectedReturnOn", "期間が被っています");
                result.addError(fieldError);
                throw new Exception("期間が被っています");
            }

            rentalManageDto.dateCheck(result);
            // 変更処理
            rentalManageService.update(id, rentalManageDto, result);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);
            ra.addFlashAttribute("error1", e.getMessage());

            List<Account> accountList = this.accountService.findAll();
            List<Stock> stockList = this.stockService.findAll();

            model.addAttribute("accounts", accountList);
            model.addAttribute("stockList", stockList);
            model.addAttribute("rentalStatus", RentalStatus.values());

            return "rental/edit";
        }
    }

}