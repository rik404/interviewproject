package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    CreditCardRepository creditCardRepository;

    UserRepository userRepository;

    @Autowired
    public  CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository){
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        CreditCard creditCard = new CreditCard();
        User creditCardOwner;
        try{
            //get from payload and set to object
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());

            //find user
            Optional<User> user = userRepository.findById(payload.getUserId());
            if(!user.isPresent())
                throw new Exception();
            creditCardOwner = user.get();
        }catch (Exception e){
            return new ResponseEntity<>(-1, HttpStatus.BAD_REQUEST);
        }

        //set owner in cc object
        creditCard.setOwner(creditCardOwner);

        //add cc to user object
        List<CreditCard> creditCardList = creditCardOwner.getCreditCardList();
        creditCardList.add(creditCard);
        creditCardOwner.setCreditCardList(creditCardList);

        CreditCard savedCreditCard;
        try {
            //save and get id
            savedCreditCard = creditCardRepository.save(creditCard);
            userRepository.save(creditCardOwner);
            return new ResponseEntity<>(savedCreditCard.getId(),HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(-1,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        User creditCardOwner;

        //find user with id
        Optional<User> user = userRepository.findById(userId);
        if(!user.isPresent())
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        creditCardOwner = user.get();

        //get card list
        List<CreditCard> creditCards = creditCardOwner.getCreditCardList();
        List<CreditCardView> creditCardViews = new ArrayList<>();
        if(creditCards == null)
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        else {
            //convert to view
            for(CreditCard cc : creditCards){
                CreditCardView creditCardView = new CreditCardView(cc.getIssuanceBank(), cc.getNumber());
                creditCardViews.add(creditCardView);
            }
        }
        return new ResponseEntity<>(creditCardViews, HttpStatus.OK);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        
        //find cc and then associated user
        Optional<CreditCard> cc = creditCardRepository.findByNumber(creditCardNumber);
        if(!cc.isPresent()) 
            new ResponseEntity<>(-1,HttpStatus.BAD_REQUEST);
        CreditCard creditCard = cc.get();
        User user = creditCard.getOwner();
        return new ResponseEntity<>(user.getId(),HttpStatus.OK);
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        String cardNumber = payload[0].getCreditCardNumber(); //assuming all card numbers are same in the array

        //find cc object
        Optional<CreditCard> cc = creditCardRepository.findByNumber(cardNumber);
        if(!cc.isPresent()) return new ResponseEntity<>("Card not found", HttpStatus.BAD_REQUEST);
        CreditCard creditCard = cc.get();

        try{
            //reverse existing balance to get ascending order (since history was expected to be latest first)
            SortedMap<String, BalanceHistory> existingBalanceHistories = new TreeMap<>();
            existingBalanceHistories.putAll(creditCard.getBalanceHistories());

            for(UpdateBalancePayload p:payload){
                //convert to BalanceHistory object
                BalanceHistory balanceHistory = new BalanceHistory();
                balanceHistory.setBalance(p.getBalanceAmount());
                balanceHistory.setDate(p.getBalanceDate());
                
                //simple add
                if(!existingBalanceHistories.containsKey(balanceHistory.getDate().toString()))
                    existingBalanceHistories.put(balanceHistory.getDate().toString(),balanceHistory);
                else{
                    //add and check diff
                    BalanceHistory existingBalance = existingBalanceHistories.get(balanceHistory.getDate().toString());
                    double balanceDiff = existingBalance.getBalance() - balanceHistory.getBalance();
                    if(balanceDiff!=0){

                        //slice map with date greater than change date
                        SortedMap<String, BalanceHistory> toUpdate =
                                existingBalanceHistories.subMap(balanceHistory.getDate().toString(),existingBalanceHistories.lastKey());

                        System.out.print(toUpdate);
                        //update diff
                        toUpdate.forEach((k,v)->{
                            BalanceHistory b = new BalanceHistory();
                            b.setBalance(v.getBalance()-balanceDiff);
                            b.setDate(v.getDate());
                            existingBalanceHistories.put(k,b);
                        });
                        
                        //since submap is toKey exclusive
                        BalanceHistory b = new BalanceHistory();
                        b.setBalance(existingBalanceHistories.get(existingBalanceHistories.lastKey()).getBalance()-balanceDiff);
                        b.setDate(existingBalanceHistories.get(existingBalanceHistories.lastKey()).getDate());
                        existingBalanceHistories.put(existingBalanceHistories.lastKey(),b);

                    }else{
                        //just add if no diff
                        existingBalanceHistories.put(balanceHistory.getDate().toString(),balanceHistory);
                    }
                }
            }

            //reset to original reverse order
            SortedMap<String, BalanceHistory> updatedBalanceHistories =
                new TreeMap<>(Comparator.reverseOrder());
            updatedBalanceHistories.putAll(existingBalanceHistories);
            
            //update
            creditCard.setBalanceHistories(updatedBalanceHistories);
            creditCardRepository.save(creditCard);
            return new ResponseEntity<>("Success", HttpStatus.OK);
        }catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>("Server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
}
