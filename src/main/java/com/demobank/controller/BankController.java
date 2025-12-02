package com.demobank.controller;

import com.demobank.entity.User;
import com.demobank.entity.Account;
import com.demobank.entity.CreditApplication;
import com.demobank.entity.Transaction;
import com.demobank.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class BankController {
    
    @Autowired
    private BankService bankService;
    
    @GetMapping("/")
    public String home() {
        return "login";
    }
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        User user = bankService.authenticateUser(username, password);
        
        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password");
            return "redirect:/login";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Account> accounts = bankService.getUserAccounts(user.getId().toString());
        
        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        return "dashboard";
    }
    
    @GetMapping("/accounts")
    public String accounts(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Account> accounts = bankService.getUserAccounts(user.getId().toString());
        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        return "accounts";
    }
    
    @GetMapping("/transfer")
    public String transferPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Account> accounts = bankService.getUserAccounts(user.getId().toString());
        model.addAttribute("user", user);
        model.addAttribute("accounts", accounts);
        return "transfer";
    }
    
    /**
     * Transfer endpoint
     */
    @PostMapping("/transfer")
    public String transfer(@RequestParam String fromAccount,
                          @RequestParam String toAccount,
                          @RequestParam String amount,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        boolean success = bankService.transferMoney(fromAccount, toAccount, amount);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Transfer failed!");
        }
        
        return "redirect:/accounts";
    }
    
    @GetMapping("/credit-application")
    public String creditApplicationPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "credit-application";
    }
    
    /**
     * Credit application endpoint
     */
    @PostMapping("/credit-application")
    public String submitCreditApplication(@RequestParam String requestedLimit,
                                        @RequestParam String annualIncome,
                                        @RequestParam String employmentStatus,
                                        @RequestParam String comments,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        boolean success = bankService.createCreditApplication(
            user.getId().toString(), requestedLimit, annualIncome, employmentStatus, comments
        );
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Credit application submitted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to submit credit application!");
        }
        
        return "redirect:/dashboard";
    }
    
    /**
     * Search endpoint
     */
    @GetMapping("/search")
    public String searchPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "search";
    }
    
    @PostMapping("/search")
    public String search(@RequestParam String searchTerm,
                        HttpSession session,
                        Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<CreditApplication> results = bankService.searchCreditApplications(searchTerm);
        
        model.addAttribute("user", user);
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("results", results);
        return "search";
    }

    @GetMapping("/transactions")
    public String transactionsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        return "transactions";
    }

    @PostMapping("/transactions")
    public String searchTransactions(
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String[] types,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Transaction> results = bankService.searchTransactions(
            user.getId().toString(), minAmount, maxAmount, startDate, endDate, description, types
        );

        model.addAttribute("user", user);
        model.addAttribute("results", results);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("description", description);
        model.addAttribute("types", types);

        return "transactions";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
