package com.wj.jpademo.controller;

import com.wj.jpademo.controller.exception.FormValidationException;
import com.wj.jpademo.controller.request.NewCoffeeRequest;
import com.wj.jpademo.model.Coffee;
import com.wj.jpademo.service.CoffeeOrderService;
import com.wj.jpademo.service.CoffeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/coffee")
@Slf4j
public class CoffeeController {
    @Autowired
    private CoffeeService coffeeService;

    @Autowired
    private CoffeeOrderService orderService;
    @GetMapping("/getCoffee")
    public Coffee findSimpleCoffeFromCache(){
        Coffee coffee = new Coffee();
        Optional<Coffee> optionalCoffee =  coffeeService.findSimpleCoffeFromCache("latte");
        if (optionalCoffee.isPresent()){
            return optionalCoffee.get();
        }
        return null;
    }

    @GetMapping(path = "",params = "!name")
    @ResponseBody
    public List<Coffee> getAll(){
        return coffeeService.findAllCoffee();
    }

    @GetMapping(path = "",params = "name")
    @ResponseBody
    public Coffee getByName(@RequestParam String name){
        return coffeeService.getByName(name);
    }

    @RequestMapping(path = "/{id}",method = RequestMethod.GET
           /* ,produces = MediaType.APPLICATION_JSON_UTF8_VALUE*/
    )
    @ResponseBody
    public Coffee getById(@PathVariable long id){
        Coffee coffee = coffeeService.getById(id);
        log.info("getByID:{}",coffee);
        return coffee;
    }


   /* @PostMapping(path = "/",consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Coffee addJsonCoffeeWithOutBindingResult(@Valid @RequestBody NewCoffeeRequest newCoffee){
        return coffeeService.saveCoffee(newCoffee.getName(),newCoffee.getPrice());
    }  */

    @PostMapping(path = "/",consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Coffee addJsonCoffee(@Valid @RequestBody NewCoffeeRequest newCoffee,BindingResult result){
        if (result.hasErrors()){
            log.warn("Binding errors:{}",result);
            throw new ValidationException(result.toString());
        }
        return coffeeService.saveCoffee(newCoffee.getName(),newCoffee.getPrice());
    }
    @PostMapping(path = "/",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Coffee addCoffee(@Valid  NewCoffeeRequest newCoffee, BindingResult result){
        if (result.hasErrors()){
            log.error("Binding Errors:{}",result);
            throw new FormValidationException(result); //抛出异常
        }
        return coffeeService.saveCoffee(newCoffee.getName(),newCoffee.getPrice());
    }
/*
    @PostMapping(path = "/",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Coffee addCoffeeWithoutBindingResult(@Valid  NewCoffeeRequest newCoffee){
        return coffeeService.saveCoffee(newCoffee.getName(),newCoffee.getPrice());
    }*/

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping(path = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public List<Coffee> batchAddCoffee(@RequestParam("file") MultipartFile file) {
        List<Coffee> coffees = new ArrayList<>();
        if (!file.isEmpty()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream()));
                String str;
                while ((str = reader.readLine()) != null) {
                    String[] arr = StringUtils.split(str, " ");
                    if (arr != null && arr.length == 2) {
                        coffees.add(coffeeService.saveCoffee(arr[0],
                                Money.of(CurrencyUnit.of("CNY"),
                                        NumberUtils.createBigDecimal(arr[1]))));
                    }
                }
            } catch (IOException e) {
                log.error("exception", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        return coffees;
    }
}
