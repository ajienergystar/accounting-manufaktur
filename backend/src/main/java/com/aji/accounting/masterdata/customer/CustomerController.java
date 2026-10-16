package com.aji.accounting.masterdata.customer;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.customer.CustomerService.CustomerRequest;
import com.aji.accounting.masterdata.customer.CustomerService.CustomerResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Pelanggan")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController extends AbstractMasterController<CustomerRequest, CustomerResponse> {
    public CustomerController(CustomerService service) {
        super(service);
    }
}
