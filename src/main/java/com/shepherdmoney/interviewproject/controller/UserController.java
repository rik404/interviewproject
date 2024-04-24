package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    UserRepository userRepository;

    @Autowired
    public UserController(UserRepository repo){
        this.userRepository = repo;
    }
    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response
        User newUser = new User();
        try {
            newUser.setEmail(payload.getEmail());
            newUser.setName(payload.getName());
        }catch (NullPointerException e){
            return new ResponseEntity<>(-1,HttpStatus.BAD_REQUEST);
            // Used -1 as return value for failed creation to retain method signature - ResponseEntity<Integer>
        }
        User savedUser;
        try {
             savedUser = userRepository.save(newUser);
             return new ResponseEntity<>(savedUser.getId(),HttpStatus.OK);
        }catch (Exception e){
            return  new ResponseEntity<>(-1,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        boolean found;
        try{
            found = userRepository.findById(userId).isPresent();
            if(found) {
                userRepository.deleteById(userId);
                return new ResponseEntity<>("User deletion successful",HttpStatus.OK);
            }else {
                return new ResponseEntity<>("User not found",HttpStatus.BAD_REQUEST);
            }
        }catch (Exception e){
            return  new ResponseEntity<>("Server Error",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
