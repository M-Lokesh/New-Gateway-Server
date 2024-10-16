package com.lokesh.accounts.service.impl;

import com.lokesh.accounts.dto.AccountsDto;
import com.lokesh.accounts.dto.CardsDto;
import com.lokesh.accounts.dto.CustomerDetailsDto;
import com.lokesh.accounts.dto.LoansDto;
import com.lokesh.accounts.entity.Accounts;
import com.lokesh.accounts.entity.Customer;
import com.lokesh.accounts.exception.ResourceNotFoundException;
import com.lokesh.accounts.mapper.AccountsMapper;
import com.lokesh.accounts.mapper.CustomerMapper;
import com.lokesh.accounts.repository.AccountsRepository;
import com.lokesh.accounts.repository.CustomerRepository;
import com.lokesh.accounts.service.ICustomersService;
import com.lokesh.accounts.service.client.CardsFeignClient;
import com.lokesh.accounts.service.client.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private CardsFeignClient cardsFeignClient;
    private LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber - Input Mobile Number
     *  @param correlationId - Correlation ID value generated at Edge server
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
        customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());

        return customerDetailsDto;

    }
}
