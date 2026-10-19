package com.web.service;

import com.web.enums.BannerType;
import com.web.repository.BannerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

class BannerServiceTests {
    @Test void homePathDoesNotMatchEveryPage() {
        BannerRepository repository=mock(BannerRepository.class);
        BannerService service=new BannerService();
        ReflectionTestUtils.setField(service,"bannerRepository",repository);
        service.findByPageNameAndType("/",BannerType.SLIDER);
        service.findByPageNameAndType("/index",BannerType.SLIDER);
        verify(repository,times(2)).findByPageName("/",BannerType.SLIDER);
    }
    @Test void otherPageMatchingKeepsExistingConvention() {
        BannerRepository repository=mock(BannerRepository.class);
        BannerService service=new BannerService();
        ReflectionTestUtils.setField(service,"bannerRepository",repository);
        service.findByPageNameAndType("/blogs",BannerType.NORMAL);
        verify(repository).findByPageName("%/blogs%",BannerType.NORMAL);
    }
}
