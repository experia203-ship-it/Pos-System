package com.connectors.pos.purchasesystem;

import com.connectors.pos.purchasesystem.purchasedtos.VendorCreateDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VendorServiceBusinessTest {

    @Test
    void updatesVendorPhoneWhenTheSubmittedNumberChanges() {
        VendorRepository repository = mock(VendorRepository.class);
        VendorService service = new VendorService(repository);
        Vendor vendor = Vendor.builder().id(5L).name("Supplier").mobile("111").build();
        when(repository.findById(5L)).thenReturn(Optional.of(vendor));

        service.updateById(5L, new VendorCreateDto(5L, "Supplier", null, "222", null));

        assertThat(vendor.getMobile()).isEqualTo("222");
    }
}
