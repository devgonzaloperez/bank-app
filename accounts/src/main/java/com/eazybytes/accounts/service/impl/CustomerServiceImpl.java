package com.eazybytes.accounts.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CardsDto;
import com.eazybytes.accounts.dto.CustomerDetailsDto;
import com.eazybytes.accounts.dto.LoansDto;
import com.eazybytes.accounts.entity.Accounts;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.ICustomerService;
import com.eazybytes.accounts.service.client.CardsFeignClient;
import com.eazybytes.accounts.service.client.LoansFeignClient;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CustomerServiceImpl implements ICustomerService {

    @Autowired
    private AccountsRepository accountsRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CardsFeignClient cardsFeignClient;
    @Autowired
    private LoansFeignClient loansFeignClient;

    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId){

        //Information from this MS.
        Customer customer = customerRepository
            .findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        Accounts accounts = accountsRepository
            .findByCustomerId(customer.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()));

        //Information from other MSs (Fetch with Feign Client).
        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        if(loansDtoResponseEntity != null){
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }

        if(cardsDtoResponseEntity != null){
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }

        return customerDetailsDto;

    }
    
}
